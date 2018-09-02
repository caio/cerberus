package co.caio.cerberus.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        assertDoesNotThrow(() -> SearchQuery.RangedSpec.of(1,1));
        assertDoesNotThrow(() -> SearchQuery.RangedSpec.of(0,1));
    }

    @Test
    void jsonSerialization() {
        var query = new SearchQuery.Builder()
                .fulltext("keto cheese")
                .addWithoutIngredients("egg")
                .calories(SearchQuery.RangedSpec.of(0,200))
                .build();
        assertEquals(query, SearchQuery.fromJson(SearchQuery.toJson(query).get()).get());

        var maybeQuery = SearchQuery.fromJson("{\"numIngredients\": [0,3]}");
        assertTrue(maybeQuery.isPresent());
        var numIngredientsQuery = maybeQuery.get();
        assertTrue(numIngredientsQuery.numIngredients().isPresent());
        assertEquals(SearchQuery.RangedSpec.of(0,3), numIngredientsQuery.numIngredients().get());
    }
}
