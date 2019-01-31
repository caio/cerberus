package co.caio.cerberus;

import co.caio.cerberus.db.ChronicleRecipeMetadataDatabase;
import co.caio.cerberus.db.FlatBufferSerializer;
import co.caio.cerberus.db.RecipeMetadata;
import co.caio.cerberus.model.Recipe;
import co.caio.cerberus.search.IndexConfiguration;
import co.caio.cerberus.search.Indexer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
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

  private Loader(Path configFile, Path inputFile) throws IOException {

    assert configFile.toFile().isFile();
    assert inputFile.toFile().isFile();

    recipesFile = inputFile;

    var conf = new Properties();
    conf.load(Files.newBufferedReader(configFile));

    luceneIndexDir = Path.of(conf.getProperty("cerberus.lucene.directory"));
    chronicleFilename = Path.of(conf.getProperty("cerberus.chronicle.filename"));

    assert !luceneIndexDir.toFile().isFile() && chronicleFilename.toFile().isFile();

    movePrefix = "old." + System.currentTimeMillis() + ".";
  }

  private Stream<Recipe> recipeStream() throws IOException {
    return Files.lines(recipesFile).map(serializer::readRecipe).flatMap(Optional::stream);
  }

  void createIndex() throws IOException {

    moveExistingDirs(luceneIndexDir);
    luceneIndexDir.toFile().mkdirs();

    logger.info("Initializing indexer");
    var indexer =
        new Indexer.Builder()
            .analyzer(IndexConfiguration.getAnalyzer())
            .dataDirectory(luceneIndexDir)
            .createMode()
            .build();

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

    var settings = computeChronicleRWSettings();
    logger.info("Creating metadata database at {}", chronicleFilename);
    var db =
        ChronicleRecipeMetadataDatabase.create(chronicleFilename, settings.avg, settings.count);

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

  ChronicleRWSettings computeChronicleRWSettings() throws Exception {
    Map<String, Long> computed = new HashMap<>();

    logger.info("Computing ChronicleMap creation parameters");
    recipeStream()
        .parallel()
        .map(FlatBufferSerializer.INSTANCE::flattenRecipe)
        .forEach(
            bb -> {
              var bufferSize = bb.limit() - bb.position();

              computed.compute("bytes", (key, orig) -> (orig == null ? 0 : orig) + bufferSize);

              computed.compute("count", (key, orig) -> (orig == null ? 0 : orig) + 1);
            });

    System.out.println(computed);

    long count = computed.get("count");
    long bytes = computed.get("bytes");
    double avg = ((double) bytes) / count;

    logger.info("Computed averageValueByteSize={} and numRecipes={}", avg, count);
    return new ChronicleRWSettings(avg, count);
  }

  class ChronicleRWSettings {
    double avg;
    long count;

    ChronicleRWSettings(double avg, long count) {
      this.avg = avg;
      this.count = count;
    }
  }

  public static void main(String[] args) throws Exception {

    if (args.length != 2) {
      logger.error("Required arguments: application.properties document.jsonlines");
      System.exit(1);
    }

    var loader = new Loader(Path.of(args[0]), Path.of(args[1]));

    loader.createDatabase();
    loader.createIndex();

    logger.info("Done!");
  }
}
