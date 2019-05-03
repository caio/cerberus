package co.caio.cerberus.model;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.model.SearchQuery.Builder;
import org.junit.jupiter.api.Test;

class SearchQueryTest {
  @Test
  void canBuildEmptyQuery() {
    assertDoesNotThrow(() -> new SearchQuery.Builder().build());
  }

  @Test
  void defaults() {
    var builder = new Builder().fulltext("oil");
    assertEquals(builder.build(), builder.offset(0).maxResults(10).maxFacets(0).build());
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
    assertThrows(IllegalStateException.class, () -> builder.maxFacets(-1).build());
    assertThrows(IllegalStateException.class, () -> builder.offset(-1).build());
    assertDoesNotThrow(() -> builder.offset(0).maxResults(5).maxFacets(0).build());
  }

  @Test
  void addMatchDietAlias() {
    assertEquals(
        new Builder().addMatchDiet("keto").build(),
        new Builder().putDietThreshold("keto", 1f).build());
  }

  @Test
  void dietThresholds() {
    assertThrows(
        IllegalStateException.class, () -> new Builder().putDietThreshold("paleo", 0).build());
    assertThrows(
        IllegalStateException.class, () -> new Builder().putDietThreshold("lowcarb", -1).build());
    assertThrows(
        IllegalStateException.class, () -> new Builder().putDietThreshold("vegan", 1.1f).build());
    assertDoesNotThrow(() -> new Builder().putDietThreshold("paleo", 1.0f).build());
  }
}
