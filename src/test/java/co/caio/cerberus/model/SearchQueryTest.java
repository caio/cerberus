package co.caio.cerberus.model;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.model.SearchQuery.Builder;
import co.caio.cerberus.model.SearchQuery.DrillDownSpec;
import co.caio.cerberus.model.SearchQuery.RangedSpec;
import co.caio.cerberus.model.SearchQuery.SortOrder;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
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
            .calories(SearchQuery.RangedSpec.of(0, 200))
            .build();
    assertEquals(query, SearchQuery.fromJson(SearchQuery.toJson(query).get()).get());

    var maybeQuery = SearchQuery.fromJson("{\"numIngredients\": [0,3]}");
    assertTrue(maybeQuery.isPresent());
    var numIngredientsQuery = maybeQuery.get();
    assertTrue(numIngredientsQuery.numIngredients().isPresent());
    assertEquals(SearchQuery.RangedSpec.of(0, 3), numIngredientsQuery.numIngredients().get());
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
        IllegalStateException.class,
        () -> new Builder().putDietThreshold("unknown diet", 1).build());
    assertThrows(
        IllegalStateException.class, () -> new Builder().putDietThreshold("paleo", 0).build());
    assertThrows(
        IllegalStateException.class, () -> new Builder().putDietThreshold("lowcarb", -1).build());
    assertThrows(
        IllegalStateException.class, () -> new Builder().putDietThreshold("vegan", 1.1f).build());
    assertDoesNotThrow(() -> new Builder().putDietThreshold("paleo", 1.0f).build());
  }

  @Test
  void moreLikeThisValidations() {
    var mltBuilder = new Builder();
    assertThrows(IllegalStateException.class, () -> mltBuilder.similarity("").build());
    assertThrows(IllegalStateException.class, () -> mltBuilder.similarity("       ").build());
    assertThrows(IllegalStateException.class, () -> mltBuilder.similarity("short text").build());
    var text = "query with enough characters to pass the length restriction";
    assertDoesNotThrow(() -> mltBuilder.similarity(text).build());
    // can't build with similarity and fulltext set
    assertThrows(
        IllegalStateException.class, () -> mltBuilder.similarity(text).fulltext(text).build());
  }

  @Test
  void drillDownSpecValidation() {
    assertThrows(
        IllegalStateException.class, () -> DrillDownSpec.of("unknown field", "unknown label"));
    assertThrows(
        IllegalStateException.class, () -> DrillDownSpec.of(DrillDown.COOK_TIME, "unknown label"));
    assertDoesNotThrow(() -> DrillDownSpec.of(DrillDown.NUM_INGREDIENTS, "5-10"));
  }

  @Test
  void builderSortAcceptsString() {
    var builder = new SearchQuery.Builder().fulltext("generic query");

    for (SortOrder order : SortOrder.values()) {
      assertDoesNotThrow(() -> builder.sort(order.name().toLowerCase()));

      var query = builder.build();
      assertEquals(order, query.sort());
    }

    // Any other value should throw
    assertThrows(IllegalStateException.class, () -> builder.sort("invalid sort"));
  }

  @Test
  void rangedSpecStringParsing() {
    // Plain numbers are treated as [0,number]
    assertEquals(RangedSpec.of(0, 10), RangedSpec.fromString("10"));
    // Ranges are encoded as "numberA,numberB"
    assertEquals(RangedSpec.of(1, 10), RangedSpec.fromString("1,10"));

    assertThrows(NumberFormatException.class, () -> RangedSpec.fromString("asd"));

    assertThrows(NoSuchElementException.class, () -> RangedSpec.fromString(",10"));
    assertThrows(NoSuchElementException.class, () -> RangedSpec.fromString("10,"));

    assertThrows(InputMismatchException.class, () -> RangedSpec.fromString("1,notANumber"));
    assertThrows(InputMismatchException.class, () -> RangedSpec.fromString("1,10hue"));
    assertThrows(InputMismatchException.class, () -> RangedSpec.fromString("10,10 "));
    assertThrows(InputMismatchException.class, () -> RangedSpec.fromString("  10,10"));

    // Verify that we throw when there's still stuff after the range spec
    assertThrows(IllegalStateException.class, () -> RangedSpec.fromString("10,10,10"));
    // And that inverted ranges are handled as errors
    assertThrows(IllegalStateException.class, () -> RangedSpec.fromString("5,1"));
  }
}
