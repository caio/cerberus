package co.caio.cerberus.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchResultTest {
    @Test
    void resultBuild() {
        assertThrows(IllegalStateException.class, () -> new SearchResult.Builder().totalHits(-1).build());
        assertThrows(IllegalStateException.class, () -> new SearchResult.Builder().totalHits(0).addRecipe(1, "recipe 1", "https://nowhere.local/1").build());
        assertDoesNotThrow(() -> new SearchResult.Builder().build());
        assertDoesNotThrow(() -> new SearchResult.Builder().totalHits(0).build());
        var sr = simple();
        assertEquals(10, sr.totalHits());
        assertEquals(2, sr.recipes().size());
        assertEquals("recipe 2", sr.recipes().get(1).name());
    }

    @Test
    void itemBuild() {
        assertThrows(IllegalStateException.class, () -> SearchResult.Item.of(-1, "n", "c"));
        assertThrows(IllegalStateException.class, () -> SearchResult.Item.of(1, "", "c"));
        assertThrows(IllegalStateException.class, () -> SearchResult.Item.of(1, "n", ""));
    }

    private SearchResult simple() {
            return new SearchResult.Builder().totalHits(10)
                .addRecipe(1, "recipe 1", "https://nowhere.local/1")
                .addRecipe(2, "recipe 2", "https://nowhere.local/2")
                .build();
    }

    @Test
    void jsonSerialization() {
        var sr = simple();
        assertDoesNotThrow(
                () -> assertEquals(sr, SearchResult.fromJson(SearchResult.toJson(sr).get()).get())
        );
    }
}