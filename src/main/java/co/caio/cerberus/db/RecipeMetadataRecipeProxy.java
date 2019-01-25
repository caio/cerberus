package co.caio.cerberus.db;

import co.caio.cerberus.model.Recipe;
import java.util.List;
import java.util.OptionalInt;

class RecipeMetadataRecipeProxy implements RecipeMetadata {
  private final Recipe recipe;

  RecipeMetadataRecipeProxy(Recipe recipe) {
    this.recipe = recipe;
  }

  @Override
  public long getRecipeId() {
    return recipe.recipeId();
  }

  @Override
  public String getName() {
    return recipe.name();
  }

  @Override
  public String getCrawlUrl() {
    return recipe.crawlUrl();
  }

  @Override
  public String getSiteName() {
    // FIXME implement this
    return null;
  }

  @Override
  public String getInstructions() {
    return recipe.instructions();
  }

  @Override
  public List<String> getIngredients() {
    return recipe.ingredients();
  }

  @Override
  public int getNumIngredients() {
    return recipe.ingredients().size();
  }

  @Override
  public OptionalInt getTotalTime() {
    return recipe.totalTime();
  }

  @Override
  public OptionalInt getCalories() {
    return recipe.calories();
  }
}
