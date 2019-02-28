package co.caio.cerberus.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SearchResultTest {
  @Test
  void resultBuild() {
    assertThrows(
        IllegalStateException.class, () -> new SearchResult.Builder().totalHits(-1).build());
    assertThrows(
        IllegalStateException.class,
        () -> new SearchResult.Builder().totalHits(0).addRecipe(1).build());
    assertDoesNotThrow(() -> new SearchResult.Builder().build());
    assertDoesNotThrow(() -> new SearchResult.Builder().totalHits(0).build());
    var sr = simple();
    assertEquals(10, sr.totalHits());
    assertEquals(2, sr.recipeIds().size());
  }

  private SearchResult simple() {
    return new SearchResult.Builder().totalHits(10).addRecipe(1).addRecipe(2).build();
  }
}
