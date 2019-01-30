package co.caio.cerberus.db;

import co.caio.cerberus.flatbuffers.FlatRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

class RecipeMetadataFlatRecipeCopy implements RecipeMetadata {
  private final long id;
  private final String name;
  private final String crawlUrl;
  private final String slug;
  private final String siteName;
  private final List<String> instructions;
  private final List<String> ingredients;
  private final OptionalInt totalTime;
  private final OptionalInt calories;

  RecipeMetadataFlatRecipeCopy(FlatRecipe recipe) {
    id = recipe.id();
    name = recipe.name();
    crawlUrl = recipe.source();
    slug = recipe.slug();
    siteName = recipe.siteName();

    // XXX rarely used
    var numInstructions = recipe.instructionsLength();
    instructions = new ArrayList<>(numInstructions);
    for (int i = 0; i < numInstructions; i++) {
      instructions.add(recipe.instructions(i));
    }

    totalTime = FlatBufferSerializer.INSTANCE.readOptionalInt(recipe.totalTime());
    calories = FlatBufferSerializer.INSTANCE.readOptionalInt(recipe.calories());

    var numIngredients = recipe.ingredientsLength();
    ingredients = new ArrayList<>(numIngredients);
    for (int i = 0; i < numIngredients; i++) {
      ingredients.add(recipe.ingredients(i));
    }
  }

  @Override
  public long getRecipeId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getSlug() {
    return slug;
  }

  @Override
  public String getCrawlUrl() {
    return crawlUrl;
  }

  @Override
  public String getSiteName() {
    return siteName;
  }

  @Override
  public List<String> getInstructions() {
    return instructions;
  }

  @Override
  public List<String> getIngredients() {
    return ingredients;
  }

  @Override
  public int getNumIngredients() {
    return ingredients.size();
  }

  @Override
  public OptionalInt getTotalTime() {
    return totalTime;
  }

  @Override
  public OptionalInt getCalories() {
    return calories;
  }
}
