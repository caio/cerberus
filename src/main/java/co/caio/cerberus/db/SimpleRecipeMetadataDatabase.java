package co.caio.cerberus.db;

import co.caio.cerberus.flatbuffers.FlatRecipe;
import com.carrotsearch.hppc.LongIntHashMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class SimpleRecipeMetadataDatabase implements RecipeMetadataDatabase {

  static final String FILE_OFFSETS = "offsets.sdb";
  static final String FILE_DATA = "data.sdb";

  final Path baseDir;
  // Fixme maybe scatter? rewrite key?
  final LongIntHashMap idToOffset;
  final ByteBuffer rawData;

  public SimpleRecipeMetadataDatabase(Path baseDir) {

    if (!baseDir.toFile().isDirectory()) {
      throw new InvalidPathException(baseDir.toString(), "Not a directory");
    }

    this.baseDir = baseDir;

    try (var raf = new RandomAccessFile(baseDir.resolve(FILE_OFFSETS).toFile(), "r")) {

      int size = raf.readInt();
      assert size > 0;

      idToOffset = new LongIntHashMap(size);

      while (size-- > 0) {
        idToOffset.put(raf.readLong(), raf.readInt());
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      var dataPath = baseDir.resolve(FILE_DATA);

      rawData =
          new RandomAccessFile(dataPath.toFile(), "rw")
              .getChannel()
              .map(MapMode.READ_ONLY, 0, Files.size(dataPath));

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static final int OFFSET_NOT_FOUND = -1;

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
  public List<RecipeMetadata> findAllById(List<Long> recipeIds) {
    return null;
  }

  @Override
  public void saveAll(List<RecipeMetadata> recipes) {
    throw new RuntimeException("Read-only!");
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
        throw new RuntimeException(wrapped);
      }

      var dataPath = baseDir.resolve(FILE_DATA);
      var offsetsPath = baseDir.resolve(FILE_OFFSETS);

      if (dataPath.toFile().exists() || offsetsPath.toFile().exists()) {
        throw new InvalidPathException(baseDir.toString(), "Database already exists at given path");
      }

      try {
        this.dataChannel = new RandomAccessFile(dataPath.toFile(), "rw").getChannel();
        this.offsetsFile = new RandomAccessFile(offsetsPath.toFile(), "rw");

      } catch (FileNotFoundException wrapped) {
        throw new RuntimeException(wrapped);
      }

      try {
        this.offsetsFile.writeInt(0);
      } catch (IOException wrapped) {
        throw new RuntimeException(wrapped);
      }
    }

    public void addRecipe(RecipeMetadata recipe) {
      try {
        int offset = (int) dataChannel.position();
        dataChannel.write(FlatBufferSerializer.INSTANCE.flattenRecipe(recipe));

        offsetsFile.writeLong(recipe.getRecipeId());
        offsetsFile.writeInt(offset);

        this.numRecipes++;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public void close() {
      try {
        dataChannel.close();
        offsetsFile.seek(0);
        offsetsFile.writeInt(numRecipes);
        offsetsFile.close();
      } catch (IOException wrapped) {
        throw new RuntimeException(wrapped);
      }
    }
  }
}
