package co.caio.cerberus.model;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
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
        () -> new Recipe.Builder().recipeId(1).name("").addInstructions("").build());
    assertThrows(
        IllegalStateException.class,
        () -> new Recipe.Builder().recipeId(1).name("not empty").addInstructions("").build());
    assertThrows(
        IllegalStateException.class,
        () -> new Recipe.Builder().recipeId(1).name("not empty").addInstructions("").build());
    assertThrows(
        IllegalStateException.class,
        () ->
            new Recipe.Builder()
                .recipeId(1)
                .name("not empty")
                .addInstructions("not empty")
                .build());
    assertThrows(
        IllegalStateException.class,
        () ->
            new Recipe.Builder()
                .recipeId(1)
                .name("not empty")
                .addInstructions("not empty")
                .crawlUrl("")
                .build());
    assertThrows(
        IllegalStateException.class,
        () ->
            new Recipe.Builder()
                .recipeId(1)
                .name("not empty")
                .crawlUrl("not empty")
                .addInstructions("not empty")
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
                .addInstructions("not empty")
                .addIngredients("item 1")
                .putDiets("paleo", 10)
                .build());
    assertDoesNotThrow(
        () ->
            new Recipe.Builder()
                .recipeId(1)
                .name("not empty")
                .slug("slug")
                .siteName("not-empty.com")
                .crawlUrl("not empty")
                .addInstructions("not empty")
                .addIngredients("item 1")
                .build());
  }

  @Test
  void loadAllSamples() {
    assertEquals(Util.expectedIndexSize(), Util.getSampleRecipes().count());
  }
}
