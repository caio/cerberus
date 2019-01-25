package co.caio.cerberus.db;

import co.caio.cerberus.flatbuffers.FlatRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

class RecipeMetadataFlatRecipeCopy implements RecipeMetadata {
  private final long id;
  private final String name;
  private final String crawlUrl;
  private final String instructions;
  private final List<String> ingredients;
  private final OptionalInt totalTime;
  private final OptionalInt calories;

  RecipeMetadataFlatRecipeCopy(FlatRecipe recipe) {
    id = recipe.id();
    name = recipe.name();
    crawlUrl = recipe.source();

    // XXX rarely used
    instructions = recipe.instructions();

    var tt = recipe.totalTime();
    totalTime =
        tt == LMDBRecipeMetadataDatabase.NON_EXISTENT_OPTIONAL_INT
            ? OptionalInt.empty()
            : OptionalInt.of(tt);
    var cal = recipe.calories();
    calories =
        cal == LMDBRecipeMetadataDatabase.NON_EXISTENT_OPTIONAL_INT
            ? OptionalInt.empty()
            : OptionalInt.of(cal);

    var numIngredients = recipe.ingredientsLength();
    var ings = new ArrayList<String>(numIngredients);
    for (int i = 0; i < numIngredients; i++) {
      ings.add(recipe.ingredients(i));
    }
    ingredients = ings;
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
  public String getCrawlUrl() {
    return crawlUrl;
  }

  @Override
  public String getSiteName() {
    // FIXME implement
    return null;
  }

  @Override
  public String getInstructions() {
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
