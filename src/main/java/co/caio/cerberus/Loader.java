package co.caio.cerberus;

import co.caio.cerberus.model.Recipe;
import co.caio.cerberus.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class Loader {
    private static final Logger logger = LoggerFactory.getLogger(Loader.class);

    public static void main(String[] args) throws Exception {
        var recipesFilename = System.getProperty("cerberus.recipes.filename");
        var dataDir = System.getProperty("cerberus.recipes.datadir");

        logger.info("Starting with recipes at {} and data at {}", recipesFilename, dataDir);
        assert dataDir != null && recipesFilename != null;

        var indexer = new Indexer.Builder().dataDirectory(Paths.get(dataDir)).createMode().build();
        logger.info("Initialized indexer", indexer);
        Files.lines(Paths.get(recipesFilename)).map(Recipe::fromJson).flatMap(Optional::stream).forEach(recipe -> {
            try {
                indexer.addRecipe(recipe);
            } catch (Exception e) {
                logger.error("Exception adding recipe to index", e);
            }
        });
        indexer.commit();
        logger.info("Done!");
    }
}
