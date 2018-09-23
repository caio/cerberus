package co.caio.cerberus.model;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecipeTest {
  @Test
  void cantBuildWithoutRequired() {
    var builder = new Recipe.Builder().recipeId(12).name("this is incomplete");
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  void strictBuilder() {
    assertThrows(IllegalStateException.class, () -> new Recipe.Builder().recipeId(1).recipeId(1));
  }

  @Test
  void preconditions() {
    assertThrows(
        IllegalStateException.class,
        () -> new Recipe.Builder().recipeId(1).name("").instructions("").build());
    assertThrows(
        IllegalStateException.class,
        () -> new Recipe.Builder().recipeId(1).name("not empty").instructions("").build());
    assertThrows(
        IllegalStateException.class,
        () -> new Recipe.Builder().recipeId(1).name("not empty").instructions("").build());
    assertThrows(
        IllegalStateException.class,
        () -> new Recipe.Builder().recipeId(1).name("not empty").instructions("not empty").build());
    assertThrows(
        IllegalStateException.class,
        () ->
            new Recipe.Builder()
                .recipeId(1)
                .name("not empty")
                .instructions("not empty")
                .crawlUrl("")
                .build());
    assertDoesNotThrow(
        () ->
            new Recipe.Builder()
                .recipeId(1)
                .name("not empty")
                .crawlUrl("not empty")
                .instructions("not empty")
                .addIngredients("item 1")
                .build());
  }

  @Test
  void jsonSerialization() {
    var recipe = Util.getBasicRecipe();
    assertEquals(recipe, Recipe.fromJson(Recipe.toJson(recipe).get()).get());
  }

  @Test
  void loadSingleJson() throws IOException {
    var jsonBytes = getClass().getResourceAsStream("/single_recipe.json").readAllBytes();
    var recipe = Recipe.fromJson(new String(jsonBytes)).get();
    assertEquals(120, recipe.calories().getAsInt());
    assertEquals(2, recipe.ingredients().size());
    assertFalse(recipe.carbohydrateContent().isPresent());
    assertEquals(Set.of("a", "b", "c", "d"), recipe.keywords());
  }

  @Test
  void loadAllSamples() throws IOException {
    assertEquals(225, Util.getSampleRecipes().count());
  }
}
