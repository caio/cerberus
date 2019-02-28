package co.caio.cerberus;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
import org.junit.jupiter.api.Test;

class SerializerTest {

  private Serializer serializer = new Serializer();

  @Test
  void searchQuerySerDe() {
    var query =
        new SearchQuery.Builder()
            .fulltext("keto cheese")
            .calories(SearchQuery.RangedSpec.of(0, 200))
            .build();
    assertEquals(query, serializer.readSearchQuery(serializer.write(query).get()).get());

    var maybeQuery = serializer.readSearchQuery("{\"numIngredients\": [0,3]}");
    assertTrue(maybeQuery.isPresent());
    var numIngredientsQuery = maybeQuery.get();
    assertTrue(numIngredientsQuery.numIngredients().isPresent());
    assertEquals(SearchQuery.RangedSpec.of(0, 3), numIngredientsQuery.numIngredients().get());
  }

  @Test
  void searchResultSerDe() {
    var sr = new SearchResult.Builder().totalHits(10).addRecipe(1).addRecipe(2).build();

    assertDoesNotThrow(
        () -> assertEquals(sr, serializer.readSearchResult(serializer.write(sr).get()).get()));
  }

  @Test
  void recipeSerDe() {
    var recipe = Util.getBasicRecipe();
    assertEquals(recipe, serializer.readRecipe(serializer.write(recipe).get()).get());
  }
}
