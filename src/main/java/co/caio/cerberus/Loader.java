package co.caio.cerberus;

import co.caio.cerberus.db.ChronicleRecipeMetadataDatabase;
import co.caio.cerberus.db.FlatBufferSerializer;
import co.caio.cerberus.db.RecipeMetadata;
import co.caio.cerberus.model.Recipe;
import co.caio.cerberus.search.Indexer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LongSummaryStatistics;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

public class Loader {
  private static final Logger logger = LoggerFactory.getLogger(Loader.class);

  private final Path recipesFile;
  private final Path luceneIndexDir;
  private final Path chronicleFilename;
  private final Serializer serializer = new Serializer();
  private final String movePrefix;

  private Loader(Path luceneIndexDir, Path chronicleFilename, Path recipesFile) {

    assert recipesFile.toFile().isFile();
    assert !luceneIndexDir.toFile().isFile() && chronicleFilename.toFile().isFile();

    this.luceneIndexDir = luceneIndexDir;
    this.chronicleFilename = chronicleFilename;
    this.recipesFile = recipesFile;

    movePrefix = "old." + System.currentTimeMillis() + ".";
  }

  private Stream<Recipe> recipeStream() throws IOException {
    return Files.lines(recipesFile).map(serializer::readRecipe).flatMap(Optional::stream);
  }

  void createIndex() throws IOException {

    moveExistingDirs(luceneIndexDir);
    //noinspection ResultOfMethodCallIgnored
    luceneIndexDir.toFile().mkdirs();

    logger.info("Initializing indexer");
    var indexer = new Indexer.Builder().dataDirectory(luceneIndexDir).createMode().build();

    logger.info("Ingesting all recipes. This will take a while...");
    recipeStream()
        .parallel()
        .forEach(
            recipe -> {
              try {
                indexer.addRecipe(recipe);
              } catch (Exception e) {
                logger.error("Exception adding recipe to index", e);
              }
            });

    indexer.commit();
    logger.info("Optimizing index for read-only usage");
    indexer.mergeSegments();
    indexer.close();
    logger.info("Finished creating lucene index");
  }

  void createDatabase() throws Exception {

    moveExistingFile(chronicleFilename);

    var stats = computeStats();
    logger.info("Computed db stats as {}", stats);
    logger.info("Creating metadata database at {}", chronicleFilename);

    var db =
        ChronicleRecipeMetadataDatabase.create(
            chronicleFilename, stats.getAverage(), stats.getCount());

    Flux.fromStream(recipeStream().map(RecipeMetadata::fromRecipe))
        .buffer(100_000)
        .subscribe(
            recipes -> {
              logger.info("Writing a batch of {} recipes", recipes.size());
              db.saveAll(recipes);
            });

    db.close();
    logger.info("Finished creating metadata database");
  }

  private void moveExistingFile(Path filename) {
    if (!filename.toFile().exists()) {
      return;
    }

    var newName = movePrefix + filename.getFileName();
    var dest = filename.resolveSibling(newName);

    if (filename.toFile().renameTo(dest.toFile())) {
      logger.info("Renamed {} to {}", filename, dest);
    } else {
      logger.error("Failed renaming {} to {}", filename, dest);
      throw new RuntimeException("Unexpected error. Aborting...");
    }
  }

  private void moveExistingDirs(Path... paths) {
    for (Path p : paths) {

      if (!p.toFile().exists()) {
        continue;
      }

      var newName = movePrefix + p.getFileName();
      var dest = p.resolveSibling(newName);

      if (p.toFile().renameTo(dest.toFile())) {
        logger.info("Moved {} to {}", p, dest);
      } else {
        logger.error("Failed moving {} to {}", p, dest);
        throw new RuntimeException("Unexpected error. Aborting...");
      }
    }
  }

  LongSummaryStatistics computeStats() throws Exception {
    logger.info("Computing ChronicleMap creation parameters");

    return recipeStream()
        .map(FlatBufferSerializer.INSTANCE::flattenRecipe)
        .collect(Collectors.summarizingLong(bb -> bb.limit() - bb.position()));
  }

  public static void main(String[] args) throws Exception {

    if (args.length != 3) {
      logger.error("Required arguments: lucene.dir chronicle.file document.jsonlines");
      System.exit(1);
    }

    var loader = new Loader(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]));

    loader.createDatabase();
    loader.createIndex();

    logger.info("Done!");
  }
}
