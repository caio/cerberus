package co.caio.cerberus.search;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import co.caio.cerberus.model.FacetData;
import co.caio.cerberus.model.FacetData.LabelData;
import co.caio.cerberus.model.Recipe;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchQuery.Builder;
import co.caio.cerberus.model.SearchQuery.SortOrder;
import co.caio.cerberus.model.SearchResultRecipe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.lucene.facet.range.LongRange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SearcherTest {
  private static Searcher searcher;

  @BeforeAll
  static void prepare() {
    searcher = Util.getTestIndexer().buildSearcher();
    assertEquals(Util.expectedIndexSize(), searcher.numDocs());
  }

  @Test
  void builder() {
    // Missing indexReader
    assertThrows(IllegalStateException.class, () -> new Searcher.Builder().build());
    // Bad directory
    assertThrows(
        Searcher.Builder.SearcherBuilderException.class,
        () -> new Searcher.Builder().dataDirectory(Path.of("/this/doesnt/exist")).build());
  }

  @Test
  void respectMaxFacets() {
    var builder = new SearchQuery.Builder().fulltext("egg");
    assertTrue(searcher.search(builder.maxFacets(0).build()).facets().isEmpty());

    var result = searcher.search(builder.maxFacets(100).build());
    result.facets().forEach(facetData -> assertTrue(facetData.children().size() <= 100));
  }

  @Test
  void respectMaxResults() {
    var builder = new SearchQuery.Builder().fulltext("garlic");
    assertEquals(1, searcher.search(builder.maxResults(1).build()).recipes().size());
    assertTrue(searcher.search(builder.maxResults(42).build()).recipes().size() <= 42);
  }

  @Test
  void facetCountsAreDistinct() {
    // Commit 2eaef6c8da caused a bug where all counts of the diet facet
    // were the same - that's because the data model started emitting
    // (float) values for all known diet types and the indexer was
    // blindly accepting them as if they were all 1.0f's
    // This test is just to prevent a regression
    var result = searcher.search(new SearchQuery.Builder().fulltext("oil").maxFacets(50).build());
    var nrDistinctPerDietCounts =
        result
            .facets()
            .stream()
            .filter(fd -> fd.dimension().equals("diet"))
            .flatMap(fd -> fd.children().stream())
            .map(LabelData::count)
            .distinct()
            .count();
    assertTrue(nrDistinctPerDietCounts > 1);
  }

  @Test
  void facets() {
    var query = new SearchQuery.Builder().fulltext("vegan").maxResults(1).maxFacets(10).build();
    var result = searcher.search(query);

    var dietFacet =
        result
            .facets()
            .stream()
            .filter(x -> x.dimension().equals(IndexField.FACET_DIET))
            .findFirst();
    assertTrue(dietFacet.isPresent());
    // make sure that when searching for vegan we actually get a count for Diet => vegan
    var veganData =
        dietFacet.get().children().stream().filter(x -> x.label().equals("vegan")).findFirst();
    assertTrue(veganData.isPresent());
    var nrVeganRecipes = veganData.get().count();
    assertTrue(veganData.get().count() > 0);

    // now lets drill down the same query on the vegan facet.
    //  it should give us just `nrVeganRecipes` results as verified above
    result =
        searcher.search(
            new SearchQuery.Builder()
                .fulltext("vegan")
                .addMatchDiet("vegan")
                .maxResults(1)
                .build());
    assertEquals(nrVeganRecipes, result.totalHits());

    // but only searching for the vegan diet facet (i.e. not searching for the term<vegan>
    // in the whole index would give us AT LEAST the same number as above, but maybe
    // more since a recipe can be vegan without having to call itself vegan
    result = searcher.search(new SearchQuery.Builder().addMatchDiet("vegan").maxResults(1).build());
    assertTrue(result.totalHits() >= nrVeganRecipes);
  }

  @Test
  void rangeFacets() {

    // capture the maximum range length so that if this ever grows
    // too big we can fail querying (due to maxFacets() validation)
    // and then figure out what to do
    var maxRangeLength =
        Searcher.fieldToRanges.values().stream().mapToInt(lr -> lr.length).max().getAsInt();

    var results =
        searcher.search(
            new SearchQuery.Builder()
                .fulltext("garlic")
                .maxResults(100)
                .maxFacets(maxRangeLength)
                .build());

    // So that we can verify the counts correctly
    assertEquals(results.totalHits(), results.recipes().size());
    // Just so we get a diverse set of recipes
    assertTrue(results.totalHits() > 50);

    var recipeData =
        results
            .recipes()
            .stream()
            .map(srr -> Util.getRecipe(srr.recipeId()))
            .collect(Collectors.toList());

    assertEquals(results.totalHits(), recipeData.size());

    // assemble fake facet data
    var fieldRangeToManualCount = new HashMap<String, Long>();
    recipeData.forEach(
        recipe -> {
          incFieldRange(IndexField.COOK_TIME, recipe.cookTime(), fieldRangeToManualCount);
          incFieldRange(IndexField.PREP_TIME, recipe.prepTime(), fieldRangeToManualCount);
          incFieldRange(IndexField.TOTAL_TIME, recipe.totalTime(), fieldRangeToManualCount);
          incFieldRange(
              IndexField.NUM_INGREDIENTS,
              OptionalInt.of(recipe.ingredients().size()),
              fieldRangeToManualCount);
        });

    Searcher.fieldToRanges.forEach(
        (field, ranges) -> {
          var maybeFacetData =
              results.facets().stream().filter(fd -> fd.dimension().equals(field)).findFirst();
          assertTrue(maybeFacetData.isPresent());
          var facetData = maybeFacetData.get();
          assertEquals(facetData.dimension(), field);

          facetData
              .children()
              .forEach(
                  ld -> {
                    var key = String.format("%s_%s", field, ld.label());
                    assertTrue(fieldRangeToManualCount.containsKey(key));
                    assertEquals((long) fieldRangeToManualCount.get(key), ld.count());
                  });
        });
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void incFieldRange(String field, OptionalInt value, Map<String, Long> accumulator) {
    if (value.isEmpty()) {
      return;
    }
    for (LongRange range : Searcher.fieldToRanges.get(field)) {
      if (range.accept(value.getAsInt())) {
        var key = String.format("%s_%s", field, range.label);
        accumulator.put(key, accumulator.getOrDefault(key, 0L) + 1);
      }
    }
  }

  @Test
  void dietThreshold() throws Exception {
    var indexer =
        new Indexer.Builder()
            .dataDirectory(Files.createTempDirectory("threshold-test"))
            .createMode()
            .build();

    var recipeBuilder =
        new Recipe.Builder()
            .recipeId(1)
            .name("none")
            .slug("nope")
            .siteName("who.cares")
            .crawlUrl("https://who.cares")
            .addIngredients("doesnt matter")
            .addInstructions("nothing to do");
    indexer.addRecipe(recipeBuilder.putDiets("keto", 0.8f).putDiets("paleo", 0.5f).build());
    indexer.addRecipe(recipeBuilder.putDiets("keto", 0.6f).putDiets("paleo", 0.1f).build());
    indexer.commit();

    var searcher = indexer.buildSearcher();
    var result = searcher.search(new SearchQuery.Builder().putDietThreshold("keto", 0.9f).build());
    assertEquals(0, result.totalHits());

    result =
        searcher.search(
            new SearchQuery.Builder()
                .putDietThreshold("keto", 0.8f)
                .putDietThreshold("paleo", 0.1f)
                .maxFacets(10)
                .build());
    assertEquals(1, result.totalHits());
    var dietFacets =
        result.facets().stream().filter(fd -> fd.dimension().equals("diet")).findFirst();
    assertTrue(dietFacets.isPresent());
    // we get { "keto": 1, "paleo": 1 } since only one doc matched both criteria
    assertTrue(dietFacets.get().children().stream().map(LabelData::count).allMatch(l -> l == 1));
  }

  @Test
  void multipleFacetsAreOr() {
    var queryBuilder = new SearchQuery.Builder().addMatchKeyword("oil").maxResults(1);
    var justOilResult = searcher.search(queryBuilder.build());
    var oilAndSaltResult = searcher.search(queryBuilder.addMatchKeyword("salt").build());
    // since drilldown queries on same facets are OR'ed,
    // the expectation is that the more keywords are input,
    // more recipes are matched
    assertTrue(oilAndSaltResult.totalHits() >= justOilResult.totalHits());
  }

  @Test
  void basicSorting() {
    var queryBuilder =
        new SearchQuery.Builder().totalTime(SearchQuery.RangedSpec.of(10, 25)).maxResults(50);

    // default sort order is relevance
    assertEquals(queryBuilder.build(), queryBuilder.sort(SortOrder.RELEVANCE).build());

    checkOrdering(
        queryBuilder.sort(SortOrder.NUM_INGREDIENTS).build(),
        r -> OptionalInt.of(Util.getRecipe(r.recipeId()).ingredients().size()));

    checkOrdering(
        queryBuilder.sort(SortOrder.COOK_TIME).build(),
        r -> Util.getRecipe(r.recipeId()).cookTime());

    checkOrdering(
        queryBuilder.sort(SortOrder.PREP_TIME).build(),
        r -> Util.getRecipe(r.recipeId()).prepTime());

    checkOrdering(
        queryBuilder.sort(SortOrder.TOTAL_TIME).build(),
        r -> Util.getRecipe(r.recipeId()).totalTime());

    checkOrdering(
        queryBuilder.sort(SortOrder.CALORIES).build(),
        r -> Util.getRecipe(r.recipeId()).calories());
  }

  private void checkOrdering(
      SearchQuery query, Function<SearchResultRecipe, OptionalInt> retriever) {
    var hits = searcher.search(query);
    var lastValue = Integer.MIN_VALUE;
    for (SearchResultRecipe r : hits.recipes()) {
      final var value = retriever.apply(r).orElse(Integer.MAX_VALUE);
      assertTrue(lastValue <= value);
      lastValue = value;
    }
  }

  @Test
  void findRecipes() {
    // Recipes with up to 3 ingredients
    var query =
        new SearchQuery.Builder()
            .numIngredients(SearchQuery.RangedSpec.of(0, 3))
            .maxResults(1)
            .build();
    assertEquals(
        Util.getAssertionNumber("test.up_to_three_ingredients"),
        searcher.search(query).totalHits());

    // Recipes with exactly 5 ingredients
    query =
        new SearchQuery.Builder()
            .numIngredients(SearchQuery.RangedSpec.of(5, 5))
            .maxResults(1)
            .build();
    assertEquals(
        Util.getAssertionNumber("test.five_ingredients"), searcher.search(query).totalHits());

    // Recipes that can be done between 10 and 25 minutes
    query =
        new SearchQuery.Builder()
            .totalTime(SearchQuery.RangedSpec.of(10, 25))
            .maxResults(1)
            .build();
    assertEquals(
        Util.getAssertionNumber("test.total_time_10_15"), searcher.search(query).totalHits());
  }

  @Test
  void similarityQueries() {
    var builder = new SearchQuery.Builder().maxResults(100).maxFacets(0);

    // very inefficiently pick 30 random recipes
    // (maybe make random seed stable later)
    var recipes = List.copyOf(Util.getRecipeMap().values());
    var pickedRecipes =
        new Random()
            .ints(30, 0, recipes.size())
            .mapToObj(recipes::get)
            .collect(Collectors.toList());

    for (Recipe r : pickedRecipes) {
      var text =
          String.format(
              "%s\n%s\n%s", r.name(), String.join("\n", r.ingredients()), r.instructions());
      var results = searcher.search(builder.similarity(text).build());

      var foundIndex = -1;
      for (SearchResultRecipe rr : results.recipes()) {
        foundIndex++;
        if (rr.recipeId() == r.recipeId()) {
          break;
        }
      }

      assertTrue(foundIndex >= 0);
      // Quality assertion: we should be able to find the original
      // recipe in the top 10% of all the documents that matched
      assertTrue(foundIndex / results.totalHits() <= 0.1);
    }
  }

  @Test
  void simpleRangeDrillDown() {
    var result = searcher.search(new SearchQuery.Builder().fulltext("salt").maxResults(1).build());

    var notARange = Set.of(IndexField.FACET_DIET, IndexField.FACET_KEYWORD);
    for (FacetData fd : result.facets()) {
      if (notARange.contains(fd.dimension())) {
        continue;
      }

      // Drilling down on a single <field,label> pair should yield the same
      // number of items as the original result facet data result
      for (LabelData ld : fd.children()) {
        var drilledResult =
            searcher.search(
                new Builder()
                    .fulltext("salt")
                    .maxResults(1)
                    .addDrillDown(fd.dimension(), ld.label())
                    .build());
        assertEquals(ld.count(), drilledResult.totalHits());

        for (FacetData fd2 : drilledResult.facets()) {
          if (notARange.contains(fd2.dimension()) || fd2.dimension().equals(fd.dimension())) {
            continue;
          }

          // The same should be valid for multiple drill downs on *distinct* fields
          for (LabelData ld2 : fd2.children()) {
            var drilledDrilledResult =
                searcher.search(
                    new Builder()
                        .fulltext("salt")
                        .maxResults(1)
                        .addDrillDown(fd.dimension(), ld.label())
                        .addDrillDown(fd2.dimension(), ld2.label())
                        .build());
            assertEquals(ld2.count(), drilledDrilledResult.totalHits());
          }
        }
      }
    }
  }

  @Test
  void fulltextWithNotQuery() {
    var builder = new SearchQuery.Builder();

    var withOil = searcher.search(builder.fulltext("oil").build());
    var withoutOil = searcher.search(builder.fulltext("-oil").build());

    // We expect that the result of searching for documents
    // matching a term PLUS the results of searching for docs
    // that do NOT match the same term ends up hitting every
    // document in the index
    assertEquals(Util.expectedIndexSize(), withOil.totalHits() + withoutOil.totalHits());

    // Same for phrases
    var withYam = searcher.search(builder.fulltext("\"sweet potato\"").build());
    var withoutYam = searcher.search(builder.fulltext("-\"sweet potato\"").build());
    assertEquals(Util.expectedIndexSize(), withOil.totalHits() + withoutOil.totalHits());
  }

  @Test
  void offsetChangesResultSizeOnBoundary() {
    var builder = new SearchQuery.Builder().fulltext("apple");
    var totalHits = (int) searcher.search(builder.build()).totalHits();

    var wantedRecipesSize = 3;
    assert totalHits > wantedRecipesSize;
    var result = searcher.search(builder.offset(totalHits - wantedRecipesSize).build());

    assertEquals(totalHits, result.totalHits());
    assertEquals(wantedRecipesSize, result.recipes().size());
  }

  @Test
  void offsetAfterMatchesYieldEmptyResults() {
    var builder = new SearchQuery.Builder().fulltext("sweet potato");
    var result = searcher.search(builder.build());

    var testQuery = builder.offset((int) result.totalHits()).build();
    var testResult = searcher.search(testQuery);
    assertEquals(0, testResult.recipes().size());
    assertEquals(result.totalHits(), testResult.totalHits());
  }

  @Test
  void offsetDoesNotChangeOrder() {
    var builder = new SearchQuery.Builder().fulltext("flour").maxResults(30);
    var results = searcher.search(builder.build()).recipes().toArray();

    var offset = 1;
    while (offset < 30) {
      var offsetResult = searcher.search(builder.offset(offset).build());

      for (int i = offset; i < results.length; i++) {
        assertEquals(results[i], offsetResult.recipes().get(i - offset));
      }

      offset++;
    }
  }
}
