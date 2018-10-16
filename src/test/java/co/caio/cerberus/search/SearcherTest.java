package co.caio.cerberus.search;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import co.caio.cerberus.model.Recipe;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchQuery.SortOrder;
import co.caio.cerberus.model.SearchResultRecipe;
import java.nio.file.Paths;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SearcherTest {
  private static Searcher searcher;

  @BeforeAll
  public static void prepare() {
    searcher = Util.getTestIndexer().buildSearcher();
    assertEquals(225, searcher.numDocs());
  }

  @Test
  public void builder() {
    assertThrows(IllegalStateException.class, () -> new Searcher.Builder().build());
    assertThrows(
        Searcher.Builder.SearcherBuilderException.class,
        () -> new Searcher.Builder().dataDirectory(Paths.get("/this/doesnt/exist")).build());
  }

  @Test
  public void facets() throws Exception {
    var query = new SearchQuery.Builder().fulltext("vegan").build();
    var result = searcher.search(query, 1);

    var dietFacet =
        result
            .facets()
            .stream()
            .filter(x -> x.dimension().equals(IndexField.FACET_DIM_DIET))
            .findFirst();
    assertTrue(dietFacet.isPresent());
    // make sure that when searching for vegan we actually get a count for Diet => vegan
    var veganData =
        dietFacet.get().children().stream().filter(x -> x.label().equals("vegan")).findFirst();
    assertTrue(veganData.isPresent());
    assertEquals(5, veganData.get().count());

    // now lets drill down the same query on the vegan facet.
    //  it should give us just 5 results as verified above
    result =
        searcher.search(
            new SearchQuery.Builder().fulltext("vegan").addMatchDiet("vegan").build(), 1);
    assertEquals(5, result.totalHits());

    // but only searching for the vegan diet facet (i.e. not searching for the term<vegan>
    // in the whole index would give us AT LEAST the same number as above, but maybe
    // more since a recipe can be vegan without having to call itself vegan
    result = searcher.search(new SearchQuery.Builder().addMatchDiet("vegan").build(), 1);
    assertTrue(result.totalHits() >= 5);
  }

  @Test
  public void multipleFacetsAreOr() throws Exception {
    var queryBuilder = new SearchQuery.Builder().addMatchKeyword("oil");
    var justOilResult = searcher.search(queryBuilder.build(), 1);
    var oilAndSaltResult = searcher.search(queryBuilder.addMatchKeyword("salt").build(), 1);
    // since drilldown queries on same facets are OR'ed,
    // the expectation is that the more keywords are input,
    // more recipes are matched
    assertTrue(oilAndSaltResult.totalHits() >= justOilResult.totalHits());
  }

  @Test
  public void basicSorting() throws Exception {
    var queryBuilder = new SearchQuery.Builder().totalTime(SearchQuery.RangedSpec.of(10, 25));

    // default sort order is relevance
    assertEquals(queryBuilder.build(), queryBuilder.sort(SortOrder.RELEVANCE).build());

    var idToRecipe =
        Util.getSampleRecipes().collect(Collectors.toMap(Recipe::recipeId, Function.identity()));

    checkOrdering(
        queryBuilder.sort(SortOrder.NUM_INGREDIENTS).build(),
        r -> OptionalInt.of(idToRecipe.get(r.recipeId()).ingredients().size()));

    checkOrdering(
        queryBuilder.sort(SortOrder.COOK_TIME).build(),
        r -> idToRecipe.get(r.recipeId()).cookTime());

    checkOrdering(
        queryBuilder.sort(SortOrder.PREP_TIME).build(),
        r -> idToRecipe.get(r.recipeId()).prepTime());

    checkOrdering(
        queryBuilder.sort(SortOrder.TOTAL_TIME).build(),
        r -> idToRecipe.get(r.recipeId()).totalTime());
  }

  private void checkOrdering(SearchQuery query, Function<SearchResultRecipe, OptionalInt> retriever)
      throws Exception {
    var hits = searcher.search(query, 50);
    var lastValue = Integer.MIN_VALUE;
    for (SearchResultRecipe r : hits.recipes()) {
      final var value = retriever.apply(r).orElse(Integer.MAX_VALUE);
      assertTrue(lastValue <= value);
      lastValue = value;
    }
  }

  @Test
  public void findRecipes() throws Exception {
    // Recipes with up to 3 ingredients
    // $ cat sample_recipes.jsonlines |jq '.ingredients|length|. <= 3'|grep true|wc -l
    var query = new SearchQuery.Builder().numIngredients(SearchQuery.RangedSpec.of(0, 3)).build();
    assertEquals(12, searcher.search(query, 1).totalHits());

    // Recipes with exactly 5 ingredients
    // $ cat sample_recipes.jsonlines |jq '.ingredients|length|. == 5'|grep true|wc -l
    query = new SearchQuery.Builder().numIngredients(SearchQuery.RangedSpec.of(5, 5)).build();
    assertEquals(14, searcher.search(query, 1).totalHits());

    // Recipes that can be done between 10 and 25 minutes
    // $ cat sample_recipes.jsonlines |jq '.totalTime| . >= 10 and . <= 25'|grep true|wc -l
    query = new SearchQuery.Builder().totalTime(SearchQuery.RangedSpec.of(10, 25)).build();
    assertEquals(44, searcher.search(query, 1).totalHits());

    // Assumption: fulltext should match more items
    var q1 = new SearchQuery.Builder().fulltext("low carb bacon eggs").build();
    // but drilling down on ingredients should be more precise
    var q2 =
        new SearchQuery.Builder()
            .fulltext("low carb")
            .addWithIngredients("bacon")
            .addWithIngredients("eggs")
            .build();

    var r1 = searcher.search(q1, 1);
    assertTrue(r1.totalHits() > 0);
    var r2 = searcher.search(q2, 1);
    assertTrue(r2.totalHits() > 0 && r2.totalHits() <= r1.totalHits());

    // This particular query should have the same doc as the top one
    assertEquals(r1.recipes().get(0), r2.recipes().get(0));
  }
}
