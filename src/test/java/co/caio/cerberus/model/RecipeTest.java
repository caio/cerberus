package co.caio.cerberus.model;

import co.caio.cerberus.Util;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
        assertThrows(IllegalStateException.class,
                () -> new Recipe.Builder().recipeId(1).siteId(1)
                        .name("not empty").slug("not empty").instructions("not empty").crawlUrl("").build());
        assertDoesNotThrow(() -> new Recipe.Builder().recipeId(1).siteId(1)
                .name("not empty").slug("not empty").crawlUrl("not empty").instructions("not empty").addIngredients("item 1").build());
    }

    @Test
    void jsonSerialization() {
        var recipe = Util.getBasicRecipe();
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
        assertEquals(225, Util.getSampleRecipes().count());
    }

}