package co.caio.cerberus;

import co.caio.cerberus.search.IndexConfiguration;
import co.caio.cerberus.search.Indexer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Loader {
  private static final Logger logger = LoggerFactory.getLogger(Loader.class);

  public static void main(String[] args) throws Exception {
    var recipesFilename = System.getProperty("cerberus.recipes.filename");
    var dataDir = System.getProperty("cerberus.recipes.datadir");
    var serializer = new Serializer();

    logger.info("Starting with recipes at {} and data at {}", recipesFilename, dataDir);
    assert dataDir != null && recipesFilename != null;

    var indexer =
        new Indexer.Builder()
            .analyzer(IndexConfiguration.getAnalyzer())
            .dataDirectory(Path.of(dataDir))
            .createMode()
            .build();
    logger.info("Initialized indexer", indexer);
    Files.lines(Path.of(recipesFilename))
        .map(serializer::readRecipe)
        .flatMap(Optional::stream)
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
    logger.info("Done!");
    indexer.close();
  }
}
