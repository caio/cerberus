package co.caio.cerberus.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HashMapRecipeMetadataDatabase implements RecipeMetadataDatabase {

  private final Map<Long, RecipeMetadata> map;

  public HashMapRecipeMetadataDatabase() {
    map = new HashMap<>();
  }

  @Override
  public Optional<RecipeMetadata> findById(long recipeId) {
    return Optional.ofNullable(map.get(recipeId));
  }

  @Override
  public void saveAll(List<RecipeMetadata> recipes) {
    recipes.forEach(
        recipe -> {
          map.put(recipe.getRecipeId(), recipe);
        });
  }
}
