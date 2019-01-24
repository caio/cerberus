package co.caio.cerberus.db;

import co.caio.cerberus.flatbuffers.FlatRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

class RecipeMetadataFlatRecipeProxy implements RecipeMetadata {
  private final FlatRecipe recipe;

  RecipeMetadataFlatRecipeProxy(FlatRecipe recipe) {
    this.recipe = recipe;
  }

  @Override
  public long getRecipeId() {
    return recipe.id();
  }

  @Override
  public String getName() {
    return recipe.name();
  }

  @Override
  public String getCrawlUrl() {
    return recipe.source();
  }

  @Override
  public String getSiteName() {
    // FIXME implement
    return null;
  }

  @Override
  public String getInstructions() {
    return recipe.instructions();
  }

  @Override
  public List<String> getIngredients() {
    var numIngredients = recipe.ingredientsLength();
    var result = new ArrayList<String>(numIngredients);
    for (int i = 0; i < numIngredients; i++) {
      result.add(recipe.ingredients(i));
    }
    return result;
  }

  @Override
  public OptionalInt getTotalTime() {
    var totalTime = recipe.totalTime();
    // FIXME ehh this "zero as default" will bite me in the future i'm sure
    if (totalTime != 0) {
      return OptionalInt.of(totalTime);
    }
    return OptionalInt.empty();
  }

  @Override
  public OptionalInt getCalories() {
    var calories = recipe.calories();
    // FIXME ehh this "zero as default" will bite me in the future i'm sure
    if (calories != 0) {
      return OptionalInt.of(calories);
    }
    return OptionalInt.empty();
  }
}
