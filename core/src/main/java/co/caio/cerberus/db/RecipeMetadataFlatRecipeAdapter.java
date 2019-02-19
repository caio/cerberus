package co.caio.cerberus.db;

import co.caio.cerberus.flatbuffers.FlatRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

class RecipeMetadataFlatRecipeAdapter implements RecipeMetadata {

  private final FlatRecipe recipe;

  RecipeMetadataFlatRecipeAdapter(FlatRecipe recipe) {
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
  public String getSlug() {
    return recipe.slug();
  }

  @Override
  public String getCrawlUrl() {
    return recipe.source();
  }

  @Override
  public String getSiteName() {
    return recipe.siteName();
  }

  @Override
  public List<String> getInstructions() {
    // XXX cache?
    var numInstructions = recipe.instructionsLength();
    var instructions = new ArrayList<String>(numInstructions);
    for (int i = 0; i < numInstructions; i++) {
      instructions.add(recipe.instructions(i));
    }
    return instructions;
  }

  @Override
  public List<String> getIngredients() {
    // XXX cache?
    var numIngredients = recipe.ingredientsLength();
    var ingredients = new ArrayList<String>(numIngredients);
    for (int i = 0; i < numIngredients; i++) {
      ingredients.add(recipe.ingredients(i));
    }
    return ingredients;
  }

  @Override
  public int getNumIngredients() {
    return recipe.ingredientsLength();
  }

  @Override
  public OptionalInt getTotalTime() {
    return FlatBufferSerializer.INSTANCE.readOptionalInt(recipe.totalTime());
  }

  @Override
  public OptionalInt getCalories() {
    return FlatBufferSerializer.INSTANCE.readOptionalInt(recipe.calories());
  }
}
