package co.caio.cerberus.db;

import co.caio.cerberus.db.RecipeMetadataDatabase.RecipeMetadataDbException;
import co.caio.cerberus.flatbuffers.FlatRecipe;
import com.carrotsearch.hppc.LongIntHashMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class SimpleRecipeMetadataDatabase implements RecipeMetadataDatabase {

  private static final String FILE_OFFSETS = "offsets.sdb";
  private static final String FILE_DATA = "data.sdb";

  private static final int OFFSET_NOT_FOUND = -1;

  private final LongIntHashMap idToOffset;
  private final ByteBuffer rawData;

  public int size() {
    return idToOffset.size();
  }

  public SimpleRecipeMetadataDatabase(Path baseDir) {

    if (!baseDir.toFile().isDirectory()) {
      throw new RecipeMetadataDbException("Not a directory: " + baseDir);
    }

    var offsetsPath = baseDir.resolve(FILE_OFFSETS);
    try (var raf = new RandomAccessFile(offsetsPath.toFile(), "r")) {

      var mapped = raf.getChannel().map(MapMode.READ_ONLY, 0, Files.size(offsetsPath));

      int size = mapped.getInt();
      if (size < 0) {
        throw new RecipeMetadataDbException("Invalid offsets file length");
      }

      idToOffset = new LongIntHashMap(size);

      while (size-- > 0) {
        idToOffset.put(mapped.getLong(), mapped.getInt());
      }

    } catch (IOException e) {
      throw new RecipeMetadataDbException(e);
    }

    try {
      var dataPath = baseDir.resolve(FILE_DATA);

      rawData =
          new RandomAccessFile(dataPath.toFile(), "rw")
              .getChannel()
              .map(MapMode.READ_ONLY, 0, Files.size(dataPath));

    } catch (IOException e) {
      throw new RecipeMetadataDbException(e);
    }
  }

  @Override
  public Optional<RecipeMetadata> findById(long recipeId) {
    int offset = idToOffset.getOrDefault(recipeId, OFFSET_NOT_FOUND);

    if (offset == OFFSET_NOT_FOUND) {
      return Optional.empty();
    }

    var buffer = rawData.asReadOnlyBuffer().position(offset);

    return Optional.of(RecipeMetadata.fromFlatRecipe(FlatRecipe.getRootAsFlatRecipe(buffer)));
  }

  @Override
  public void saveAll(List<RecipeMetadata> recipes) {
    throw new RecipeMetadataDbException("Read-only! Use the Writer inner class to create a db");
  }

  public static class Writer {

    int numRecipes;
    final FileChannel dataChannel;
    final RandomAccessFile offsetsFile;

    public Writer(Path baseDir) {

      this.numRecipes = 0;

      try {
        Files.createDirectories(baseDir);
      } catch (IOException wrapped) {
        throw new RecipeMetadataDbException(wrapped);
      }

      var dataPath = baseDir.resolve(FILE_DATA);
      var offsetsPath = baseDir.resolve(FILE_OFFSETS);

      if (dataPath.toFile().exists() || offsetsPath.toFile().exists()) {
        throw new RecipeMetadataDbException("Database already exists at given path");
      }

      try {
        this.dataChannel = new RandomAccessFile(dataPath.toFile(), "rw").getChannel();
        this.offsetsFile = new RandomAccessFile(offsetsPath.toFile(), "rw");

      } catch (FileNotFoundException wrapped) {
        throw new RecipeMetadataDbException(wrapped);
      }

      try {
        // First bytes are for the number of items in the database
        // we set to -1 here and, during close(), configure the
        // correct value
        this.offsetsFile.writeInt(-1);
      } catch (IOException wrapped) {
        throw new RecipeMetadataDbException(wrapped);
      }
    }

    public void addRecipe(RecipeMetadata recipe) {
      // XXX Not thread safe
      try {
        int offset = (int) dataChannel.position();
        dataChannel.write(FlatBufferSerializer.INSTANCE.flattenRecipe(recipe));

        offsetsFile.writeLong(recipe.getRecipeId());
        offsetsFile.writeInt(offset);

        this.numRecipes++;
      } catch (IOException e) {
        throw new RecipeMetadataDbException(e);
      }
    }

    public void close() {
      try {
        // Write the number of recipes to the beginning of the offsets file
        offsetsFile.seek(0);
        offsetsFile.writeInt(numRecipes);

        dataChannel.close();
        offsetsFile.close();
      } catch (IOException wrapped) {
        throw new RecipeMetadataDbException(wrapped);
      }
    }
  }
}
