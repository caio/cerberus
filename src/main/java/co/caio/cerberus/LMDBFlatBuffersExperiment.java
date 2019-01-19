package co.caio.cerberus;

import co.caio.cerberus.flatbuffers.FlatRecipe;
import co.caio.cerberus.model.Recipe;
import com.google.flatbuffers.FlatBufferBuilder;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.EnvFlags;
import reactor.core.publisher.Flux;

public class LMDBFlatBuffersExperiment {

  static class DirectByteBufferFactory implements FlatBufferBuilder.ByteBufferFactory {
    @Override
    public ByteBuffer newByteBuffer(int capacity) {
      return ByteBuffer.allocateDirect(capacity).order(ByteOrder.LITTLE_ENDIAN);
    }
  }

  private static final Serializer serializer = new Serializer();
  private static final String recipesFilename = "tmp/document.jsonlines";

  private static Stream<Recipe> recipeStream() throws Exception {
    return Files.lines(Path.of(recipesFilename))
        .map(serializer::readRecipe)
        .flatMap(Optional::stream);
  }

  private static void stats(File dest) throws Exception {
    var env = Env.open(dest, 2_000, EnvFlags.MDB_RDONLY_ENV);
    var db = env.openDbi("recipe");

    try (var tnx = env.txnRead()) {
      System.out.println(db.stat(tnx));
    }
  }

  private static void readDb(File dest) throws Exception {
    var env = Env.open(dest, 2_000, EnvFlags.MDB_RDONLY_ENV);
    var db = env.openDbi("recipe");

    var bbKey = ByteBuffer.allocateDirect(Long.SIZE);
    recipeStream()
        .forEach(
            recipe -> {
              try (var txn = env.txnRead()) {
                bbKey.putLong(recipe.recipeId());
                bbKey.flip();

                var result = db.get(txn, bbKey);

                if (result == null) {
                  System.out.println("Couldn't find recipe " + recipe.recipeId());
                  return;
                  // throw new RuntimeException("Couldn't find recipe " + recipe.recipeId());
                }

                var flatRecipe = FlatRecipe.getRootAsFlatRecipe(result);

                if (flatRecipe.id() != recipe.recipeId()) {
                  throw new RuntimeException(
                      "Uh, reading recipe " + recipe.recipeId() + " gave me recipe " + flatRecipe);
                }

                var flatIngSize = flatRecipe.ingredientsLength();
                if (flatIngSize != recipe.ingredients().size()) {
                  throw new RuntimeException(
                      String.format(
                          "Expected %d ingredients, got %d",
                          recipe.ingredients().size(), flatIngSize));
                }

                for (int i = 0; i < flatIngSize; i++) {
                  if (! flatRecipe.ingredients(i).equals(recipe.ingredients().get(i))) {
                    throw new RuntimeException(
                        String.format(
                            "Expected ingredient to be '%s' but got '%s'",
                            recipe.ingredients().get(i), flatRecipe.ingredients(i)));
                  }
                }
              }
            });
  }

  private static ByteBuffer flattenRecipe(Recipe recipe) {
    var builder = new FlatBufferBuilder(5_000, new DirectByteBufferFactory());

    var nameOffset = builder.createString(recipe.name());
    var sourceOffset = builder.createString(recipe.crawlUrl());

    var ingredientsOffsets =
        recipe.ingredients().stream().mapToInt(builder::createString).toArray();
    var ingredientsVectorOffset = FlatRecipe.createIngredientsVector(builder, ingredientsOffsets);

    var rootTable =
        FlatRecipe.createFlatRecipe(
            builder,
            recipe.recipeId(),
            nameOffset,
            sourceOffset,
            ingredientsVectorOffset,
            recipe.totalTime().orElse(0),
            recipe.calories().orElse(0));

    builder.finish(rootTable);
    return builder.dataBuffer();
  }

  private static void createDb(File dest) throws Exception {
    var env = Env.open(dest, 3_000);
    var db = env.openDbi("recipe", DbiFlags.MDB_CREATE);

    var bbKey = ByteBuffer.allocateDirect(Long.SIZE);

    Flux.fromStream(recipeStream())
        .buffer(250_000)
        .subscribe(
            recipes -> {
              try (var tnx = env.txnWrite()) {
                for (Recipe r : recipes) {
                  bbKey.putLong(r.recipeId());
                  bbKey.flip();
                  db.put(tnx, bbKey, flattenRecipe(r));
                }
                tnx.commit();
                System.out.println("Wrote a batch of " + recipes.size() + " recipes");
              }
            });

    db.close();
    env.close();
    System.out.println("Done!");
  }

  private static void lmdbLoop() throws Exception {
    var tmpDir = new File("tmp/lmdb-test");
    System.out.println("Create DB at " + tmpDir);
    createDb(tmpDir);
    System.out.println("Read DB from " + tmpDir);
    readDb(tmpDir);
  }

  public static void main(String[] args) throws Exception {
    //lmdbLoop();
    stats(new File("tmp/lmdb-test"));
  }
}
