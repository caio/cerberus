package co.caio.cerberus.db;

import java.util.List;
import java.util.Optional;

public interface RecipeMetadataDatabase {

  Optional<RecipeMetadata> findById(long recipeId);

  List<RecipeMetadata> findAllById(List<Long> recipeIds);

  void saveAll(List<RecipeMetadata> recipes);

  default void close() {}

  class RecipeMetadataDbException extends RuntimeException {
    RecipeMetadataDbException(String message) {
      super(message);
    }

    RecipeMetadataDbException(Exception ex) {
      super(ex);
    }
  }
}
