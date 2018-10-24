package co.caio.cerberus;

import co.caio.cerberus.model.Recipe;
import co.caio.cerberus.search.Indexer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

  private static Logger logger = LoggerFactory.getLogger(Util.class);

  public static Recipe getBasicRecipe() {
    return new Recipe.Builder()
        .recipeId(1)
        .name("valid recipe 1")
        .crawlUrl("https://recipes.recipes/1")
        .description("valid recipe 1 description")
        .instructions("there is nothing to do")
        .addIngredients("item a", "item b")
        .build();
  }

  public static Stream<Recipe> getSampleRecipes() {
    var samplesFile = Util.class.getResource("/sample_recipes.jsonlines").getFile();
    try {
      return Files.lines(Paths.get(samplesFile)).map(Recipe::fromJson).flatMap(Optional::stream);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Indexer indexer;
  private static Path testDataDir;

  public static synchronized Indexer getTestIndexer() {
    if (indexer == null) {
      try {
        testDataDir = Files.createTempDirectory("cerberus-test");
      } catch (Exception rethrown) {
        throw new RuntimeException(rethrown);
      }

      indexer = new Indexer.Builder().dataDirectory(testDataDir).createMode().build();

      getSampleRecipes()
          .forEach(
              recipe -> {
                try {
                  indexer.addRecipe(recipe);
                } catch (Exception logged) {
                  logger.error(String.format("Failed to index recipe %s", recipe), logged);
                }
              });
      try {
        indexer.commit();
      } catch (Exception rethrown) {
        throw new RuntimeException(rethrown);
      }
    }
    return indexer;
  }

  public static synchronized Path getTestDataDir() {
    if (indexer == null) {
      getTestIndexer();
    }
    return testDataDir;
  }
}
