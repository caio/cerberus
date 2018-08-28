package co.caio.cerberus;

import org.junit.jupiter.api.Test;

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

    @Test
    Recipe basicBuild() {
        var recipe = new Recipe.Builder()
                .recipeId(1)
                .name("valid recipe 1")
                .description("valid recipe 1 description")
                .slug("recipe-1")
                .instructions("there is nothing to do")
                .imageUrl("image.jpg")
                .crawlUrl("https://nowhere.local")
                .build();
        return recipe;
    }

    @Test
    void jsonSerialization() {
        var recipe = basicBuild();
        assertEquals(recipe, Recipe.fromJson(Recipe.toJson(recipe).get()).get());
    }
}