package co.caio.cerberus.db;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.EnvFlags;

class LMDBRecipeMetadataDatabase implements RecipeMetadataDatabase {

  static final int NON_EXISTENT_OPTIONAL_INT = 0;

  private final Env<ByteBuffer> env;
  private final Dbi<ByteBuffer> recipeTableDbi;

  LMDBRecipeMetadataDatabase(Path databasePath, int maxSizeInMb, boolean isReadOnly) {
    if (!databasePath.toFile().isDirectory()) {
      throw new RecipeDatabaseConfigurationError("databasePath must be an existing directory");
    }

    if (isReadOnly && !databasePath.resolve("data.mdb").toFile().exists()) {
      throw new RecipeDatabaseDoesNotExist(databasePath.toString());
    }

    if (isReadOnly) {
      env = Env.open(databasePath.toFile(), maxSizeInMb, EnvFlags.MDB_RDONLY_ENV);
      recipeTableDbi = env.openDbi("recipe");
    } else {
      env = Env.open(databasePath.toFile(), maxSizeInMb);
      recipeTableDbi = env.openDbi("recipe", DbiFlags.MDB_CREATE);
    }
  }

  class RecipeDatabaseConfigurationError extends RecipeMetadataDbException {
    RecipeDatabaseConfigurationError(String message) {
      super(message);
    }
  }

  class RecipeDatabaseDoesNotExist extends RecipeMetadataDbException {
    RecipeDatabaseDoesNotExist(String message) {
      super(message);
    }
  }

  class RecipeDatabaseIsReadOnly extends RecipeMetadataDbException {
    RecipeDatabaseIsReadOnly(String message) {
      super(message);
    }
  }

  @Override
  public Optional<RecipeMetadata> findById(long recipeId) {
    var bbKey = allocateKeyBuffer();
    bbKey.putLong(recipeId);
    bbKey.flip();

    try (var txn = env.txnRead()) {
      var buffer = recipeTableDbi.get(txn, bbKey);

      if (buffer == null) {
        return Optional.empty();
      }

      var flatRecipe = FlatBufferSerializer.INSTANCE.readRecipe(buffer);
      // The buffer is not valid after the transaction ends
      return Optional.of(RecipeMetadata.fromFlatRecipe(flatRecipe));
    }
  }

  @Override
  public Iterable<RecipeMetadata> findAllById(Iterable<Long> recipeIds) {
    // XXX TMI: I shouldn't need to know the page size here
    var result = new ArrayList<RecipeMetadata>(20);
    var bbKey = allocateKeyBuffer();

    try (var txn = env.txnRead()) {
      for (long recipeId : recipeIds) {
        bbKey.putLong(recipeId);
        bbKey.flip();
        var buffer = recipeTableDbi.get(txn, bbKey);

        if (buffer == null) {
          continue;
        }

        var flatRecipe = FlatBufferSerializer.INSTANCE.readRecipe(buffer);
        result.add(RecipeMetadata.fromFlatRecipe(flatRecipe));
      }
    }

    return result;
  }

  @Override
  public void saveAll(Iterable<RecipeMetadata> recipes) {
    if (env.isReadOnly()) {
      throw new RecipeDatabaseIsReadOnly("Illegal operation on read-only db");
    }

    var bbKey = allocateKeyBuffer();
    try (var txn = env.txnWrite()) {
      for (var recipeMetadata : recipes) {
        bbKey.putLong(recipeMetadata.getRecipeId());
        bbKey.flip();
        recipeTableDbi.put(txn, bbKey, FlatBufferSerializer.INSTANCE.flattenRecipe(recipeMetadata));
      }
      txn.commit();
    }
  }

  private ByteBuffer allocateKeyBuffer() {
    return ByteBuffer.allocateDirect(Long.BYTES);
  }
}
