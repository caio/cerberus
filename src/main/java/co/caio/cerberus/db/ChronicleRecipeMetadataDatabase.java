package co.caio.cerberus.db;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

public class ChronicleRecipeMetadataDatabase implements RecipeMetadataDatabase {

  private static final String DATABASE_NAME = "recipe-metadata";

  final ChronicleMap<Long, ByteBuffer> backingMap;

  private ChronicleRecipeMetadataDatabase(ChronicleMap<Long, ByteBuffer> backingMap) {
    this.backingMap = backingMap;
  }

  public static RecipeMetadataDatabase open(Path databasePath) {
    try {
      var map =
          ChronicleMapBuilder.of(Long.class, ByteBuffer.class)
              .name(DATABASE_NAME)
              .createPersistedTo(databasePath.toFile());
      return new ChronicleRecipeMetadataDatabase(map);
    } catch (IOException rethrown) {
      throw new RecipeMetadataDbException(rethrown);
    }
  }

  public static RecipeMetadataDatabase create(
      Path databasePath, double averageValueBytesSize, long numberOfEntries) {
    try {
      var map =
          ChronicleMapBuilder.of(Long.class, ByteBuffer.class)
              .name(DATABASE_NAME)
              .constantKeySizeBySample(1L)
              .averageValueSize(averageValueBytesSize)
              .entries(numberOfEntries)
              .createPersistedTo(databasePath.toFile());
      return new WriteableChronicleRecipeMetadataDatabase(map);
    } catch (IOException rethrown) {
      throw new RecipeMetadataDbException(rethrown);
    }
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
  public void close() {
    backingMap.close();
  }

  @Override
  public Optional<RecipeMetadata> findById(long recipeId) {
    return Optional.ofNullable(get(recipeId));
  }

  @Override
  public List<RecipeMetadata> findAllById(List<Long> recipeIds) {
    var result = new ArrayList<RecipeMetadata>(recipeIds.size());
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
  public void saveAll(List<RecipeMetadata> recipes) {
    throw new RecipeMetadataDbException("Database is open as read-only");
  }

  static class WriteableChronicleRecipeMetadataDatabase extends ChronicleRecipeMetadataDatabase {

    WriteableChronicleRecipeMetadataDatabase(ChronicleMap<Long, ByteBuffer> backingMap) {
      super(backingMap);
    }

    @Override
    public void saveAll(List<RecipeMetadata> recipes) {
      recipes.forEach(
          rm -> backingMap.put(rm.getRecipeId(), FlatBufferSerializer.INSTANCE.flattenRecipe(rm)));
    }
  }
}
