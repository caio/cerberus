package co.caio.cerberus.db;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface RecipeMetadataDatabase {

  Optional<RecipeMetadata> findById(long recipeId);

  Iterable<RecipeMetadata> findAllById(Iterable<Long> recipeIds);

  void saveAll(List<RecipeMetadata> recipes);

  void close();

  class RecipeMetadataDbException extends RuntimeException {
    RecipeMetadataDbException(String message) {
      super(message);
    }
  }

  class Builder {
    public static RecipeMetadataDatabase open(
        Path databasePath, int maxSizeInMb, boolean isReadOnly) {
      return new LMDBRecipeMetadataDatabase(databasePath, maxSizeInMb, isReadOnly);
    }
  }
}
