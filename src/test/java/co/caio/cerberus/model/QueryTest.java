package co.caio.cerberus.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryTest {
    @Test
    void cantBuildEmptyQuery() {
        assertThrows(IllegalStateException.class, () -> new Query.Builder().build());
    }

    @Test
    void rangedSpecValidation() {
        assertThrows(IllegalStateException.class, () -> Query.RangedSpec.of(3, 2));
        assertThrows(IllegalStateException.class, () -> Query.RangedSpec.of(-1, 2));
        assertThrows(IllegalStateException.class, () -> Query.RangedSpec.of(1, -2));
        assertThrows(IllegalStateException.class, () -> Query.RangedSpec.of(-1, -2));
        assertDoesNotThrow(() -> Query.RangedSpec.of(1,1));
        assertDoesNotThrow(() -> Query.RangedSpec.of(0,1));
    }

    @Test
    void jsonSerialization() {
        var query = new Query.Builder()
                .fulltext("keto cheese")
                .addWithoutIngredients("egg")
                .calories(Query.RangedSpec.of(0,200))
                .build();
        assertEquals(query, Query.fromJson(Query.toJson(query).get()).get());

        var maybeQuery = Query.fromJson("{\"numIngredients\": [0,3]}");
        assertTrue(maybeQuery.isPresent());
        var numIngredientsQuery = maybeQuery.get();
        assertTrue(numIngredientsQuery.numIngredients().isPresent());
        assertEquals(Query.RangedSpec.of(0,3), numIngredientsQuery.numIngredients().get());
    }
}
