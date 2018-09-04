package co.caio.cerberus.search;

import co.caio.cerberus.Util;
import co.caio.cerberus.model.SearchQuery;
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
    public void findRecipes() {
        var searcher = new Searcher.Builder().directory(inMemoryIndexer.getDirectory()).build();

        // Recipes with up to 3 ingredients
        // $ cat sample_recipes.jsonlines |jq '.ingredients|length|. <= 3'|grep true|wc -l
        var query = new SearchQuery.Builder().numIngredients(SearchQuery.RangedSpec.of(0,3)).build();
        checkTotalHits(searcher, 12, query);

        // Recipes with exactly 5 ingredients
        // $ cat sample_recipes.jsonlines |jq '.ingredients|length|. == 5'|grep true|wc -l
        query = new SearchQuery.Builder().numIngredients(SearchQuery.RangedSpec.of(5,5)).build();
        checkTotalHits(searcher, 14, query);

        // Recipes that can be done between 10 and 25 minutes
        // $ cat sample_recipes.jsonlines |jq '.totalTime| . >= 10 and . <= 25'|grep true|wc -l
        query = new SearchQuery.Builder().totalTime(SearchQuery.RangedSpec.of(10, 25)).build();
        checkTotalHits(searcher, 44, query);

        assertDoesNotThrow(
                () -> {
                    // Assumption: fulltext should match more items
                    var q1 = new SearchQuery.Builder().fulltext("keto bagel bacon").build();
                    // but drilling down on ingredients should be more precise
                    var q2 = new SearchQuery.Builder().fulltext("keto bagel").addWithIngredients("bacon").build();

                    var r1 = searcher.search(q1, 1);
                    assertTrue(r1.totalHits > 0);
                    var r2 = searcher.search(q2, 1);
                    assertTrue(r2.totalHits > 0 && r2.totalHits <= r1.totalHits);

                    // This particular query should have the same doc as the top one
                    assertEquals(r1.scoreDocs[0].doc, r2.scoreDocs[0].doc);
                }
        );
    }

    private void checkTotalHits(Searcher searcher, int expectedHits, SearchQuery query) {
        assertDoesNotThrow(
                () -> assertEquals(expectedHits, searcher.search(query, 1).totalHits)
        );
    }
}