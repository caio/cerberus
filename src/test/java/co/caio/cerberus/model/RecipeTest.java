package co.caio.cerberus.model;

import co.caio.cerberus.Util;
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
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void strictBuilder() {
        assertThrows(
                IllegalStateException.class,
                () -> new Recipe.Builder().recipeId(1).recipeId(1));
    }

    @Test
    void preconditions() {
        assertThrows(IllegalStateException.class,
                () -> new Recipe.Builder().recipeId(1).siteId(1)
                        .name("").slug("").instructions("").build());
        assertThrows(IllegalStateException.class,
                () -> new Recipe.Builder().recipeId(1).siteId(1)
                        .name("not empty").slug("").instructions("").build());
        assertThrows(IllegalStateException.class,
                () -> new Recipe.Builder().recipeId(1).siteId(1)
                        .name("not empty").slug("not empty").instructions("").build());
        assertThrows(IllegalStateException.class,
                () -> new Recipe.Builder().recipeId(1).siteId(1)
                        .name("not empty").slug("not empty").instructions("not empty").build());
        new Recipe.Builder().recipeId(1).siteId(1)
                .name("not empty").slug("not empty").instructions("not empty").addIngredients("item 1").build();
        assertDoesNotThrow(() -> new Recipe.Builder().recipeId(1).siteId(1)
                .name("not empty").slug("not empty").instructions("not empty").addIngredients("item 1").build());
    }

    @Test
    void jsonSerialization() {
        var recipe = Util.basicBuild();
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