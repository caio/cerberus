package co.caio.cerberus.db;

import co.caio.cerberus.model.Recipe;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

class RecipeMetadataRecipeAdapter implements RecipeMetadata {
  private final Recipe recipe;

  RecipeMetadataRecipeAdapter(Recipe recipe) {
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
  public String getSlug() {
    return recipe.slug();
  }

  @Override
  public String getCrawlUrl() {
    return recipe.crawlUrl();
  }

  @Override
  public String getSiteName() {
    return recipe.siteName();
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
  public OptionalInt getPrepTime() {
    return recipe.prepTime();
  }

  @Override
  public OptionalInt getCookTime() {
    return recipe.cookTime();
  }

  @Override
  public OptionalInt getTotalTime() {
    return recipe.totalTime();
  }

  @Override
  public OptionalInt getCalories() {
    return recipe.calories();
  }

  @Override
  public OptionalDouble getFatContent() {
    return recipe.fatContent();
  }

  @Override
  public OptionalDouble getProteinContent() {
    return recipe.proteinContent();
  }

  @Override
  public OptionalDouble getCarbohydrateContent() {
    return recipe.carbohydrateContent();
  }
}
