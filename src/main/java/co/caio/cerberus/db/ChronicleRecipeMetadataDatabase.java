package co.caio.cerberus.db;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import net.openhft.chronicle.map.ChronicleMapBuilder;

class ChronicleRecipeMetadataDatabase implements RecipeMetadataDatabase {

  private final Map<Long, ByteBuffer> backingMap;

  ChronicleRecipeMetadataDatabase(Path databasePath) throws IOException {
    backingMap =
        ChronicleMapBuilder.of(Long.class, ByteBuffer.class)
            .name("recipe-metadata")
            // FIXME these settings are only necessary when creating the db
            .constantKeySizeBySample(1L)
            .averageValueSize(1434)
            .entries(1_200_000)
            .createPersistedTo(databasePath.toFile());
  }

  private RecipeMetadata get(long recipeId) {
    var buffer = backingMap.get(recipeId);
    if (buffer != null) {
      return RecipeMetadata.fromFlatRecipeAsProxy(FlatBufferSerializer.INSTANCE.readRecipe(buffer));
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
          backingMap.put(rm.getRecipeId(), FlatBufferSerializer.INSTANCE.flattenRecipe(rm));
        });
  }
}
