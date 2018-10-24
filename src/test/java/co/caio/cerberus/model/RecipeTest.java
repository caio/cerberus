package co.caio.cerberus.model;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class RecipeTest {
  @Test
  void cantBuildWithoutRequired() {
    var builder = new Recipe.Builder().recipeId(12).name("this is incomplete");
    assertThrows(IllegalStateException.class, builder::build);
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
    assertThrows(
        IllegalStateException.class,
        () ->
            new Recipe.Builder()
                .recipeId(1)
                .name("not empty")
                .crawlUrl("not empty")
                .instructions("not empty")
                .addIngredients("item 1")
                .putDiets("keto", -1)
                .build());
    assertThrows(
        IllegalStateException.class,
        () ->
            new Recipe.Builder()
                .recipeId(1)
                .name("not empty")
                .crawlUrl("not empty")
                .instructions("not empty")
                .addIngredients("item 1")
                .putDiets("paleo", 10)
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
  void loadAllSamples() throws IOException {
    assertEquals(299, Util.getSampleRecipes().count());
  }
}
