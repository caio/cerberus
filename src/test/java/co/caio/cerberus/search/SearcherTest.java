package co.caio.cerberus.search;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.caio.cerberus.Util;
import co.caio.cerberus.model.FacetData.LabelData;
import co.caio.cerberus.model.Recipe;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchQuery.SortOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.function.Function;
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

    var result = searcher.search(builder.maxFacets(2).build());
    result.facets().forEach(facetData -> assertTrue(facetData.children().size() <= 2));
  }

  @Test
  void respectMaxResults() {
    var builder = new SearchQuery.Builder().fulltext("garlic");
    assertEquals(1, searcher.search(builder.maxResults(1).build()).recipeIds().size());
    assertTrue(searcher.search(builder.maxResults(42).build()).recipeIds().size() <= 42);
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
  void basicSorting() {
    var queryBuilder =
        new SearchQuery.Builder().totalTime(SearchQuery.RangedSpec.of(10, 25)).maxResults(50);

    // default sort order is relevance
    assertEquals(queryBuilder.build(), queryBuilder.sort(SortOrder.RELEVANCE).build());

    checkOrdering(
        queryBuilder.sort(SortOrder.NUM_INGREDIENTS).build(),
        r -> OptionalInt.of(Util.getRecipe(r).ingredients().size()));

    checkOrdering(
        queryBuilder.sort(SortOrder.COOK_TIME).build(), r -> Util.getRecipe(r).cookTime());

    checkOrdering(
        queryBuilder.sort(SortOrder.PREP_TIME).build(), r -> Util.getRecipe(r).prepTime());

    checkOrdering(
        queryBuilder.sort(SortOrder.TOTAL_TIME).build(), r -> Util.getRecipe(r).totalTime());

    checkOrdering(queryBuilder.sort(SortOrder.CALORIES).build(), r -> Util.getRecipe(r).calories());
  }

  private void checkOrdering(SearchQuery query, Function<Long, OptionalInt> retriever) {
    var hits = searcher.search(query);
    var lastValue = Integer.MIN_VALUE;
    for (long r : hits.recipeIds()) {
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
    assertEquals(wantedRecipesSize, result.recipeIds().size());
  }

  @Test
  void offsetAfterMatchesYieldEmptyResults() {
    var builder = new SearchQuery.Builder().fulltext("sweet potato");
    var result = searcher.search(builder.build());

    var testQuery = builder.offset((int) result.totalHits()).build();
    var testResult = searcher.search(testQuery);
    assertEquals(0, testResult.recipeIds().size());
    assertEquals(result.totalHits(), testResult.totalHits());
  }

  @Test
  void offsetDoesNotChangeOrder() {
    var builder = new SearchQuery.Builder().fulltext("flour").maxResults(30);
    var results = searcher.search(builder.build()).recipeIds().toArray();

    var offset = 1;
    while (offset < 30) {
      var offsetResult = searcher.search(builder.offset(offset).build());

      for (int i = offset; i < results.length; i++) {
        assertEquals(results[i], offsetResult.recipeIds().get(i - offset));
      }

      offset++;
    }
  }

  @Test
  void policyInspectLuceneQueryIsAlwaysCalled() {
    var policyMock = mock(SearchPolicy.class);

    var searcherWithPolicy =
        new Searcher.Builder()
            .searchPolicy(policyMock)
            .dataDirectory(Util.getTestDataDir())
            .build();

    searcherWithPolicy.search(new SearchQuery.Builder().fulltext("unused").build());

    verify(policyMock).inspectParsedFulltextQuery(any());
  }

  @Test
  void policyShouldComputeFacetsIsOnlyCalledWhenRelevant() {
    var policyMock = mock(SearchPolicy.class);

    var searcherWithPolicy =
        new Searcher.Builder()
            .searchPolicy(policyMock)
            .dataDirectory(Util.getTestDataDir())
            .build();

    // maxFacets is set to zero, policy shouldn't be called
    searcherWithPolicy.search(new SearchQuery.Builder().fulltext("unused").maxFacets(0).build());

    verify(policyMock, never()).shouldComputeFacets(any());

    // But with any other valid use it should
    searcherWithPolicy.search(new SearchQuery.Builder().fulltext("unused").maxFacets(10).build());
    verify(policyMock).shouldComputeFacets(any());
  }

  @Test
  void throwingFromPolicyIsAllowed() {
    var policyMock = mock(SearchPolicy.class);

    var searcherWithPolicy =
        new Searcher.Builder()
            .searchPolicy(policyMock)
            .dataDirectory(Util.getTestDataDir())
            .build();

    class CustomTestException extends RuntimeException {
      CustomTestException() {
        super();
      }
    }

    doThrow(CustomTestException.class).when(policyMock).inspectParsedFulltextQuery(any());

    assertThrows(
        CustomTestException.class,
        () -> searcherWithPolicy.search(new SearchQuery.Builder().fulltext("ignored").build()));
  }

  @Test
  void negatingShouldComputeFacetsSkipsFacetComputation() {
    var policyMock = mock(SearchPolicy.class);

    var searcherWithPolicy =
        new Searcher.Builder()
            .searchPolicy(policyMock)
            .dataDirectory(Util.getTestDataDir())
            .build();

    var queryWithFacets = new SearchQuery.Builder().fulltext("oil").maxFacets(4).build();

    // Allow computing facets irrespective of results
    when(policyMock.shouldComputeFacets(any())).thenReturn(true);

    // precondition: base query actually generates facets
    assertFalse(searcherWithPolicy.search(queryWithFacets).facets().isEmpty());

    // But when the policy is to deny, we get no facets in the result
    when(policyMock.shouldComputeFacets(any())).thenReturn(false);

    assertTrue(searcherWithPolicy.search(queryWithFacets).facets().isEmpty());
  }
}
