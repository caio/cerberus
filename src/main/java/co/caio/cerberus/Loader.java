package co.caio.cerberus;

import co.caio.cerberus.db.Flattener;
import co.caio.cerberus.db.RecipeMetadata;
import co.caio.cerberus.db.RecipeMetadataDatabase;
import co.caio.cerberus.model.Recipe;
import co.caio.cerberus.search.IndexConfiguration;
import co.caio.cerberus.search.Indexer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Loader {
  private static final Logger logger = LoggerFactory.getLogger(Loader.class);

  private final Path recipesFile;
  private final Path dataDir;
  private final Path dbDir;
  private final int maxDbSize;
  private final Serializer serializer = new Serializer();
  private final String movePrefix;

  private Loader(Path configFile, Path inputFile) throws IOException {

    assert configFile.toFile().isFile();
    assert inputFile.toFile().isFile();

    recipesFile = inputFile;

    var conf = new Properties();
    conf.load(Files.newBufferedReader(configFile));

    dataDir = Path.of(conf.getProperty("cerberus.search.location"));
    dbDir = Path.of(conf.getProperty("cerberus.search.lmdb-location"));
    maxDbSize = Integer.parseInt(conf.getProperty("cerberus.search.lmdb-max-size"));

    assert !dataDir.toFile().isFile() && !dbDir.toFile().isFile() && maxDbSize > 0;

    movePrefix = "old." + System.currentTimeMillis() + ".";
  }

  private Stream<Recipe> recipeStream() throws IOException {
    return Files.lines(recipesFile).map(serializer::readRecipe).flatMap(Optional::stream);
  }

  public void createIndex() throws IOException {

    moveExistingDirs(dataDir);
    dataDir.toFile().mkdirs();

    logger.info("Initializing indexer");
    var indexer =
        new Indexer.Builder()
            .analyzer(IndexConfiguration.getAnalyzer())
            .dataDirectory(dataDir)
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

  public void createDatabase() throws IOException {

    moveExistingDirs(dbDir);
    // dbDir.toFile().mkdirs();

    logger.info("Creating metadata database at {}", dbDir);
    var db = RecipeMetadataDatabase.Builder.open(dbDir);

    recipeStream()
        .map(RecipeMetadata::fromRecipe)
        .parallel()
        .forEach(rm -> db.saveAll(List.of(rm)));
    // Flux.fromStream(recipeStream().map(RecipeMetadata::fromRecipe))
    //     .buffer(100_000)
    //     .subscribe(
    //         recipes -> {
    //           logger.info("Writing a batch of {} recipes", recipes.size());
    //           db.saveAll(recipes);
    //         });
    logger.info("Finished creating metadata database");
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

  public void averageValueSize() throws Exception {
    var avg =
        recipeStream()
            .parallel()
            .map(Flattener.INSTANCE::flattenRecipe)
            .mapToInt(bb -> bb.limit() - bb.position())
            .average();
    System.out.println(avg);
  }

  public static void main(String[] args) throws Exception {

    if (args.length != 2) {
      logger.error("Required arguments: application.properties document.jsonlines");
      System.exit(1);
    }

    var loader = new Loader(Path.of(args[0]), Path.of(args[1]));

    // loader.averageValueSize();
    loader.createDatabase();
    // loader.createIndex();

    logger.info("Done!");
  }
}
