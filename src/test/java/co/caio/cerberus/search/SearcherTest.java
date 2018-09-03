package co.caio.cerberus.search;

import co.caio.cerberus.Util;
import co.caio.cerberus.model.SearchQuery;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.ScoreDoc;
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
            var query = LongPoint.newExactQuery(IndexField.RECIPE_ID, recipeId);
            var result = searcher.indexSearcher.search(query, 1);
            assertEquals(1, result.totalHits);
        }

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

        // TODO test more complex queries
    }

    private void checkTotalHits(Searcher searcher, int expectedHits, SearchQuery query) throws IOException {
        var interpreter = new QueryInterpreter();
        var result = searcher.indexSearcher.search(interpreter.toLuceneQuery(query), 1);
        assertEquals(expectedHits, result.totalHits);


    }
}