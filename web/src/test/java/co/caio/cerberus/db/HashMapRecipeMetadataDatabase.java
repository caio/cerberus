package co.caio.cerberus.db;

import java.util.ArrayList;
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
  public List<RecipeMetadata> findAllById(List<Long> recipeIds) {
    var result = new ArrayList<RecipeMetadata>();
    recipeIds.forEach(id -> findById(id).ifPresent(result::add));
    return result;
  }

  @Override
  public void saveAll(List<RecipeMetadata> recipes) {
    recipes.forEach(
        recipe -> {
          map.put(recipe.getRecipeId(), recipe);
        });
  }
}
