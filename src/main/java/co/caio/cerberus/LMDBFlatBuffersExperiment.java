package co.caio.cerberus;

import co.caio.cerberus.db.RecipeMetadata;
import co.caio.cerberus.db.RecipeMetadataDatabase;
import co.caio.cerberus.model.Recipe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import reactor.core.publisher.Flux;

public class LMDBFlatBuffersExperiment {

  private static final Serializer serializer = new Serializer();
  private static final String recipesFilename = "tmp/document.jsonlines";
  private static final int DB_SIZE_MB = 3_000;

  private static Stream<Recipe> recipeStream() throws Exception {
    return Files.lines(Path.of(recipesFilename))
        .map(serializer::readRecipe)
        .flatMap(Optional::stream);
  }

  private static void readDb(Path dbPath) throws Exception {
    var db = RecipeMetadataDatabase.Builder.open(dbPath, DB_SIZE_MB, true);

    recipeStream()
        .forEach(
            recipe -> {
              var recipeMetadata = db.findById(recipe.recipeId()).orElseThrow();

              if (recipeMetadata.getRecipeId() != recipe.recipeId()) {
                throw new RuntimeException(
                    "Uh, reading recipe "
                        + recipe.recipeId()
                        + " gave me recipe "
                        + recipeMetadata.getRecipeId());
              }

              if (!recipe.ingredients().equals(recipeMetadata.getIngredients())) {
                throw new RuntimeException(
                    "Wanted "
                        + recipe.ingredients()
                        + " but got "
                        + recipeMetadata.getIngredients());
              }

              if (!recipe.instructions().equals(recipeMetadata.getInstructions())) {
                throw new RuntimeException(
                    "Wanted "
                        + recipe.instructions()
                        + " but got "
                        + recipeMetadata.getInstructions());
              }
            });
  }

  private static void fancyWrite(Path dbPath) throws Exception {
    var db = RecipeMetadataDatabase.Builder.open(dbPath, DB_SIZE_MB, false);
    Flux.fromStream(recipeStream())
        .map(RecipeMetadata::fromRecipe)
        .buffer(250_000)
        .subscribe(
            batch -> {
              db.saveAll(batch);
              System.out.println("Wrote a batch of " + batch.size() + " recipes");
            });
    db.close();
    System.out.println("Done!");
  }

  private static void lmdbLoop(Path dbPath) throws Exception {
    System.out.println("Create DB at " + dbPath);
    fancyWrite(dbPath);
    System.out.println("Read DB from " + dbPath);
    readDb(dbPath);
  }

  public static void main(String[] args) throws Exception {
    var path = Path.of("tmp/lmdb-fancy-test");
    lmdbLoop(path);
  }
}
