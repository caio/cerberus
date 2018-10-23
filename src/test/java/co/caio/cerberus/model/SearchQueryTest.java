package co.caio.cerberus.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SearchQueryTest {
  @Test
  void cantBuildEmptyQuery() {
    assertThrows(IllegalStateException.class, () -> new SearchQuery.Builder().build());
  }

  @Test
  void rangedSpecValidation() {
    assertThrows(IllegalStateException.class, () -> SearchQuery.RangedSpec.of(3, 2));
    assertThrows(IllegalStateException.class, () -> SearchQuery.RangedSpec.of(-1, 2));
    assertThrows(IllegalStateException.class, () -> SearchQuery.RangedSpec.of(1, -2));
    assertThrows(IllegalStateException.class, () -> SearchQuery.RangedSpec.of(-1, -2));
    assertDoesNotThrow(() -> SearchQuery.RangedSpec.of(1, 1));
    assertDoesNotThrow(() -> SearchQuery.RangedSpec.of(0, 1));
  }

  @Test
  void searchOptions() {
    var builder = new SearchQuery.Builder().fulltext("simplest buildable query");
    assertThrows(IllegalStateException.class, () -> builder.maxResults(0).build());
    assertThrows(IllegalStateException.class, () -> builder.maxResults(123123).build());
    assertThrows(IllegalStateException.class, () -> builder.maxFacets(-1).build());
    assertThrows(IllegalStateException.class, () -> builder.maxFacets(1232).build());
    assertDoesNotThrow(() -> builder.maxResults(5).maxFacets(0).build());
  }

  @Test
  void jsonSerialization() {
    var query =
        new SearchQuery.Builder()
            .fulltext("keto cheese")
            .addWithoutIngredients("egg")
            .calories(SearchQuery.RangedSpec.of(0, 200))
            .build();
    assertEquals(query, SearchQuery.fromJson(SearchQuery.toJson(query).get()).get());

    var maybeQuery = SearchQuery.fromJson("{\"numIngredients\": [0,3]}");
    assertTrue(maybeQuery.isPresent());
    var numIngredientsQuery = maybeQuery.get();
    assertTrue(numIngredientsQuery.numIngredients().isPresent());
    assertEquals(SearchQuery.RangedSpec.of(0, 3), numIngredientsQuery.numIngredients().get());
  }
}
