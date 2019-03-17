package co.caio.cerberus.db;

import co.caio.cerberus.flatbuffers.FlatRecipe;
import co.caio.cerberus.model.Recipe;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public interface RecipeMetadata {
  long getRecipeId();

  String getName();

  String getSlug();

  String getCrawlUrl();

  String getSiteName();

  List<String> getIngredients();

  int getNumIngredients();

  OptionalInt getPrepTime();

  OptionalInt getCookTime();

  OptionalInt getTotalTime();

  OptionalInt getCalories();

  OptionalDouble getFatContent();

  OptionalDouble getProteinContent();

  OptionalDouble getCarbohydrateContent();

  static RecipeMetadata fromRecipe(Recipe recipe) {
    return new RecipeMetadataRecipeAdapter(recipe);
  }

  static RecipeMetadata fromFlatRecipe(FlatRecipe recipe) {
    return new RecipeMetadataFlatRecipeAdapter(recipe);
  }
}
