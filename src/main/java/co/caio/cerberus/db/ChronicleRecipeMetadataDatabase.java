package co.caio.cerberus.db;

import co.caio.cerberus.flatbuffers.FlatRecipe;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import net.openhft.chronicle.map.ChronicleMapBuilder;

class ChronicleRecipeMetadataDatabase implements RecipeMetadataDatabase {

  static final int NON_EXISTENT_OPTIONAL_INT = 0;
  private final Map<Long, ByteBuffer> backingMap;

  ChronicleRecipeMetadataDatabase(Path databasePath) throws IOException {
    backingMap =
        ChronicleMapBuilder.of(Long.class, ByteBuffer.class)
            .name("recipe-metadata")
            .constantKeySizeBySample(1L)
            .averageValueSize(1434)
            .entries(1_200_000)
            .createPersistedTo(databasePath.toFile());
  }

  private RecipeMetadata get(long recipeId) {
    var buffer = backingMap.get(recipeId);
    if (buffer != null) {
      return RecipeMetadata.fromFlatRecipeAsProxy(FlatRecipe.getRootAsFlatRecipe(buffer));
    } else {
      return null;
    }
  }

  @Override
  public Optional<RecipeMetadata> findById(long recipeId) {
    return Optional.ofNullable(get(recipeId));
  }

  @Override
  public Iterable<RecipeMetadata> findAllById(Iterable<Long> recipeIds) {
    // XXX iterable is super fucking annoying - change to List<> ?
    var result = new ArrayList<RecipeMetadata>(20); // TMI
    recipeIds.forEach(
        id -> {
          var recipe = get(id);
          if (recipe != null) {
            result.add(recipe);
          }
        });
    return result;
  }

  @Override
  public void saveAll(Iterable<RecipeMetadata> recipes) {
    recipes.forEach(
        rm -> {
          backingMap.put(rm.getRecipeId(), Flattener.INSTANCE.flattenRecipe(rm));
        });
  }
}
