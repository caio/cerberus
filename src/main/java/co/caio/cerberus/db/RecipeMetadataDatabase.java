package co.caio.cerberus.db;

import java.util.Optional;

public interface RecipeMetadataDatabase {

  Optional<RecipeMetadata> findById(long recipeId);

  Iterable<RecipeMetadata> findAllById(Iterable<Long> recipeIds);

  void saveAll(Iterable<RecipeMetadata> recipes);

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
