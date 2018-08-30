package co.caio.cerberus.search;

import co.caio.cerberus.search.Recipe;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RecipeTest {
    @Test
    void cantBuildWithoutRequired() {
        var builder = new Recipe.Builder()
                .recipeId(12)
                .name("this is incomplete");
        assertThrows(IllegalStateException.class, () -> builder.build());
    }

    @Test
    void strictBuilder() {
        assertThrows(
                IllegalStateException.class,
                () -> new Recipe.Builder().recipeId(1).recipeId(1));
    }

    static Recipe basicBuild() {
        var recipe = new Recipe.Builder()
                .recipeId(1)
                .siteId(12)
                .slug("recipe-1")
                .name("valid recipe 1")
                .description("valid recipe 1 description")
                .instructions("there is nothing to do")
                .build();
        return recipe;
    }

    @Test
    void jsonSerialization() {
        var recipe = basicBuild();
        assertEquals(recipe, Recipe.fromJson(Recipe.toJson(recipe).get()).get());
    }

    @Test
    void loadSingleJson() throws IOException {
        var jsonBytes = getClass().getResourceAsStream("/single_recipe.json")
                .readAllBytes();
        var recipe = Recipe.fromJson(new String(jsonBytes)).get();
        assertEquals(120, recipe.calories().getAsInt());
        assertEquals(2, recipe.ingredients().size());
        assertEquals(12, recipe.siteId());
        assertFalse(recipe.carbohydrateContent().isPresent());
        assertEquals(Set.of("a", "b", "c", "d"), recipe.keywords());
    }

    @Test
    void loadAllSamples() throws IOException {
        var samples = getClass().getResource("/sample_recipes.jsonlines").getFile();
        var numSamples = Files.lines(Paths.get(samples)).map(Recipe::fromJson).count();
        assertEquals(226, numSamples);
    }
}