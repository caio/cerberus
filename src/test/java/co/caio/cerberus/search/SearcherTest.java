package co.caio.cerberus.search;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.caio.cerberus.Util;
import co.caio.cerberus.model.Recipe;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchQuery.SortOrder;
import co.caio.cerberus.search.Searcher.SearcherException;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Function;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SearcherTest {
  private static Searcher searcher;

  @BeforeAll
  static void prepare() {
    searcher = Searcher.Factory.open(Util.getTestDataDir());
    assertEquals(Util.expectedIndexSize(), searcher.numDocs());
  }

  @Test
  void throwsOnInvalidDir() {
    assertThrows(SearcherException.class, () -> Searcher.Factory.open(Path.of("/does/not/exist")));
    assertThrows(
        SearcherException.class,
        () -> Searcher.Factory.open(Path.of("/does/not/exist"), mock(SearchPolicy.class)));
  }

  @Test
  void respectMaxFacets() {
    var builder = new SearchQuery.Builder().fulltext("egg");
    assertTrue(searcher.search(builder.maxFacets(0).build()).facets().isEmpty());

    var result = searcher.search(builder.maxFacets(2).build());
    result.facets().values().forEach(facetData -> assertTrue(facetData.children().size() <= 2));
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
        result.facets().get("diet").children().values().stream().distinct().count();
    assertTrue(nrDistinctPerDietCounts > 1);
  }

  @Test
  void facets() {
    var query =
        new SearchQuery.Builder().fulltext("vegetarian").maxResults(1).maxFacets(10).build();
    var result = searcher.search(query);

    var dietFacet = result.facets().get("diet");
    assertNotNull(dietFacet);
    // make sure that when searching for vegetarian we actually get a
    // count for Diet => vegetarian
    var nrVegetarianRecipes = dietFacet.children().get("vegetarian");
    assertNotNull(nrVegetarianRecipes);
    assertTrue(nrVegetarianRecipes > 0);

    // now lets drill down the same query on the vegetarian facet.
    //  it should give us just `nrVegetarianRecipes` results as verified above
    result =
        searcher.search(
            new SearchQuery.Builder()
                .fulltext("vegetarian")
                .diet("vegetarian")
                .maxResults(1)
                .build());
    assertEquals(nrVegetarianRecipes, result.totalHits());

    // but only searching for the vegetarian diet facet (i.e. not searching for the
    // term<vegetarian> in the whole index would give us AT LEAST the same number
    // as above, but maybe more since a recipe can be vegetarian without having to
    // call itself vegetarian
    result = searcher.search(new SearchQuery.Builder().diet("vegetarian").maxResults(1).build());
    assertTrue(result.totalHits() >= nrVegetarianRecipes);
  }

  @Test
  void dietThreshold(@TempDir Path tmpDir) throws Exception {
    var indexer = Indexer.Factory.open(tmpDir, CategoryExtractor.NOOP);

    var recipeBuilder =
        new Recipe.Builder()
            .recipeId(1)
            .name("none")
            .slug("nope")
            .siteName("who.cares")
            .crawlUrl("https://who.cares")
            .addIngredients("doesnt matter")
            .addInstructions("nothing to do");
    indexer.addRecipe(recipeBuilder.putDiets("keto", 0.8F).build());
    indexer.addRecipe(recipeBuilder.putDiets("keto", 0.6F).build());
    indexer.addRecipe(recipeBuilder.putDiets("keto", 1F).build());
    indexer.commit();
    indexer.close();

    var searcher = Searcher.Factory.open(tmpDir);
    var sqb = new SearchQuery.Builder();

    assertEquals(1, searcher.search(sqb.diet("keto").build()).totalHits());
    assertEquals(1, searcher.search(sqb.diet("keto", 1F).build()).totalHits());
    assertEquals(2, searcher.search(sqb.diet("keto", 0.7F).build()).totalHits());
    assertEquals(3, searcher.search(sqb.diet("keto", 0.6F).build()).totalHits());
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

    given(policyMock.rewriteParsedFulltextQuery(any())).willReturn(new MatchAllDocsQuery());

    var searcherWithPolicy = Searcher.Factory.open(Util.getTestDataDir(), policyMock);

    searcherWithPolicy.search(new SearchQuery.Builder().fulltext("unused").build());

    verify(policyMock).rewriteParsedFulltextQuery(any());
  }

  @Test
  void policyShouldComputeFacetsIsOnlyCalledWhenRelevant() {
    var policyMock = mock(SearchPolicy.class);

    given(policyMock.rewriteParsedFulltextQuery(any())).willReturn(new MatchAllDocsQuery());

    var searcherWithPolicy = Searcher.Factory.open(Util.getTestDataDir(), policyMock);

    // maxFacets is set to zero, policy shouldn't be called
    searcherWithPolicy.search(new SearchQuery.Builder().fulltext("unused").maxFacets(0).build());

    verify(policyMock, never()).shouldComputeFacets(anyInt());

    // But with any other valid use it should
    searcherWithPolicy.search(new SearchQuery.Builder().fulltext("unused").maxFacets(10).build());
    verify(policyMock).shouldComputeFacets(anyInt());
  }

  @Test
  void policyRewriteIsCalled() {
    var policyMock = mock(SearchPolicy.class);

    // Policy will rewrite to MatchNoDocsQuery()
    given(policyMock.rewriteParsedFulltextQuery(any())).willReturn(new MatchNoDocsQuery());

    var searcherWithPolicy = Searcher.Factory.open(Util.getTestDataDir(), policyMock);

    // So even when searching for all docs, we should
    // get zero results
    var allQuery = new SearchQuery.Builder().fulltext("*").build();
    assertEquals(0, searcherWithPolicy.search(allQuery).totalHits());
  }

  @Test
  void throwingFromPolicyIsAllowed() {
    var policyMock = mock(SearchPolicy.class);

    var searcherWithPolicy = Searcher.Factory.open(Util.getTestDataDir(), policyMock);

    class CustomTestException extends RuntimeException {
      CustomTestException() {
        super();
      }
    }

    doThrow(CustomTestException.class).when(policyMock).rewriteParsedFulltextQuery(any());

    assertThrows(
        CustomTestException.class,
        () -> searcherWithPolicy.search(new SearchQuery.Builder().fulltext("ignored").build()));
  }

  @Test
  void negatingShouldComputeFacetsSkipsFacetComputation() {
    var policyMock = mock(SearchPolicy.class);

    given(policyMock.rewriteParsedFulltextQuery(any())).willReturn(new MatchAllDocsQuery());

    var searcherWithPolicy = Searcher.Factory.open(Util.getTestDataDir(), policyMock);

    var queryWithFacets = new SearchQuery.Builder().fulltext("oil").maxFacets(4).build();

    // Allow computing facets irrespective of results
    when(policyMock.shouldComputeFacets(anyInt())).thenReturn(true);

    // precondition: base query actually generates facets
    assertFalse(searcherWithPolicy.search(queryWithFacets).facets().isEmpty());

    // But when the policy is to deny, we get no facets in the result
    when(policyMock.shouldComputeFacets(anyInt())).thenReturn(false);

    assertTrue(searcherWithPolicy.search(queryWithFacets).facets().isEmpty());
  }

  @Test
  void similaritySearch() {

    Util.getSampleRecipes()
        .forEach(
            testRecipe -> {
              var similar = searcher.findSimilar(recipeText(testRecipe), 10);

              assertTrue(similar.totalHits() > 0);

              var foundIndex = similar.recipeIds().indexOf(testRecipe.recipeId());

              // Searching for similar recipes using the content of a recipe
              // we have in the database should find said recipe in the top 5
              // similar items
              assertTrue(foundIndex != -1 && foundIndex < 5);

              // if (foundIndex != 0) {
              //   System.out.println(testRecipe.name());
              //   similar
              //       .recipeIds()
              //       .forEach(
              //           simId -> {
              //             var sr = Util.getRecipe(simId);
              //             System.out.println(" - " + sr.name());
              //           });
              // }
            });
  }

  String recipeText(Recipe testRecipe) {
    return String.join(
        "\n",
        List.of(
            testRecipe.name(),
            String.join("\n", testRecipe.ingredients()),
            String.join("\n", testRecipe.instructions())));
  }

  @Test
  void rewriteParsedSimilarityQuery() {
    var policyMock = mock(SearchPolicy.class);

    given(policyMock.rewriteParsedSimilarityQuery(any())).willReturn(new MatchNoDocsQuery());

    var searcherWithPolicy = Searcher.Factory.open(Util.getTestDataDir(), policyMock);

    var text = recipeText(Util.getSampleRecipes().skip(10).findFirst().get());

    // Searching without policy should yield some results
    assertTrue(searcher.findSimilar(text, 10).totalHits() > 0);

    // But with a policy that rewrites it all to a MatchNoDocsQuery
    // it should be zero
    assertEquals(0, searcherWithPolicy.findSimilar(text, 10).totalHits());
  }

  @Test
  void emptySearchQueryYieldsEmptyResults() {
    assertEquals(0, searcher.search(new SearchQuery.Builder().build()).totalHits());
  }
}
