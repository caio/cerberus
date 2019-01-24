package co.caio.cerberus.db;

import co.caio.cerberus.flatbuffers.FlatRecipe;
import co.caio.cerberus.model.Recipe;
import java.util.List;
import java.util.OptionalInt;

public interface RecipeMetadata {
  long getRecipeId();
  String getName();
  String getCrawlUrl();
  String getSiteName();

  String getInstructions();

  List<String> getIngredients();

  OptionalInt getTotalTime();
  OptionalInt getCalories();

  static RecipeMetadata fromRecipe(Recipe recipe) {
    return new RecipeMetadataRecipeProxy(recipe);
  }

  static RecipeMetadata fromFlatRecipe(FlatRecipe recipe) {
    return new RecipeMetadataFlatRecipeCopy(recipe);
  }
}
