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
                () -> new Searcher.Builder().dataDirectory(Paths.get("/this/doesnt/exist")).build());
    }

    @Test
    public void facets() {
        var searcher = inMemoryIndexer.buildSearcher();
        var query = new SearchQuery.Builder().fulltext("vegan").build();
        var result = searcher.search(query, 1);

        var dietFacet = result.facets().stream().filter(x -> x.dimension().equals(IndexField.FACET_DIM_DIET)).findFirst();
        assertTrue(dietFacet.isPresent());
        // make sure that when searching for vegan we actually get a count for Diet => vegan
        var veganData = dietFacet.get().children().stream().filter(x -> x.label().equals("vegan")).findFirst();
        assertTrue(veganData.isPresent());
        assertEquals(5, veganData.get().count());
    }

    @Test
    public void findRecipes() {
        var searcher = inMemoryIndexer.buildSearcher();

        // Recipes with up to 3 ingredients
        // $ cat sample_recipes.jsonlines |jq '.ingredients|length|. <= 3'|grep true|wc -l
        var query = new SearchQuery.Builder().numIngredients(SearchQuery.RangedSpec.of(0,3)).build();
        assertEquals(12, searcher.search(query, 1).totalHits());

        // Recipes with exactly 5 ingredients
        // $ cat sample_recipes.jsonlines |jq '.ingredients|length|. == 5'|grep true|wc -l
        query = new SearchQuery.Builder().numIngredients(SearchQuery.RangedSpec.of(5,5)).build();
        assertEquals(14, searcher.search(query, 1).totalHits());

        // Recipes that can be done between 10 and 25 minutes
        // $ cat sample_recipes.jsonlines |jq '.totalTime| . >= 10 and . <= 25'|grep true|wc -l
        query = new SearchQuery.Builder().totalTime(SearchQuery.RangedSpec.of(10, 25)).build();
        assertEquals(44, searcher.search(query, 1).totalHits());

        // Assumption: fulltext should match more items
        var q1 = new SearchQuery.Builder().fulltext("low carb bacon eggs").build();
        // but drilling down on ingredients should be more precise
        var q2 = new SearchQuery.Builder().fulltext("low carb")
                .addWithIngredients("bacon")
                .addWithIngredients("eggs").build();

        var r1 = searcher.search(q1, 1);
        assertTrue(r1.totalHits() > 0);
        var r2 = searcher.search(q2, 1);
        assertTrue(r2.totalHits() > 0 && r2.totalHits() <= r1.totalHits());

        // This particular query should have the same doc as the top one
        assertEquals(r1.recipes().get(0), r2.recipes().get(0));
    }
}