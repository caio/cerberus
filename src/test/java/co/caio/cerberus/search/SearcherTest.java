package co.caio.cerberus.search;

import co.caio.cerberus.Util;
import org.apache.lucene.document.LongPoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearcherTest {
    private static Indexer inMemoryIndexer;
    private static List<Long> recipeIds;

    @BeforeAll
    public static void prepare() throws IOException {
        recipeIds = new LinkedList<>();
        inMemoryIndexer = new Indexer.Builder().inMemory().createMode().build();
        Util.getSampleRecipes().forEach(recipe -> {
            try {
                inMemoryIndexer.addRecipe(recipe);
                recipeIds.add(recipe.recipeId());
            } catch (Exception ignored) {}
        });
        inMemoryIndexer.commit();
    }

    @Test
    public void builder() {
        assertThrows(IllegalStateException.class,
                () -> new Searcher.Builder().build());
        assertThrows(Searcher.Builder.SearcherBuilderException.class,
                () -> new Searcher.Builder().directory(Paths.get("/this/doesnt/exist")).build());

        assertDoesNotThrow(() -> new Searcher.Builder().directory(inMemoryIndexer.getDirectory()).build());
    }

    @Test
    public void findRecipes() throws IOException {
        var searcher = new Searcher.Builder().directory(inMemoryIndexer.getDirectory()).build();
        for (long recipeId: recipeIds) {
            var query = LongPoint.newExactQuery("recipeId", recipeId);
            var result = searcher.indexSearcher.search(query, 1);
            assertEquals(1, result.totalHits);
        }
    }
}