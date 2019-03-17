package co.caio.cerberus.db;

import co.caio.cerberus.flatbuffers.FlatRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
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
  public OptionalInt getPrepTime() {
    return FlatBufferSerializer.INSTANCE.readOptionalInt(recipe.prepTime());
  }

  @Override
  public OptionalInt getCookTime() {
    return FlatBufferSerializer.INSTANCE.readOptionalInt(recipe.cookTime());
  }

  @Override
  public OptionalInt getTotalTime() {
    return FlatBufferSerializer.INSTANCE.readOptionalInt(recipe.totalTime());
  }

  @Override
  public OptionalInt getCalories() {
    return FlatBufferSerializer.INSTANCE.readOptionalInt(recipe.calories());
  }

  @Override
  public OptionalDouble getFatContent() {
    return FlatBufferSerializer.INSTANCE.readOptionalDouble(recipe.fatContent());
  }

  @Override
  public OptionalDouble getProteinContent() {
    return FlatBufferSerializer.INSTANCE.readOptionalDouble(recipe.proteinContent());
  }

  @Override
  public OptionalDouble getCarbohydrateContent() {
    return FlatBufferSerializer.INSTANCE.readOptionalDouble(recipe.carbohydrateContent());
  }
}
