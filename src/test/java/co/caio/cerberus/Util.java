package co.caio.cerberus;

import co.caio.cerberus.model.Recipe;
import co.caio.cerberus.search.Indexer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class Util {
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

  public static synchronized Indexer getTestIndexer() throws Exception {
    if (indexer == null) {
      var baseDir = Files.createTempDirectory("cerberus-test");
      indexer = new Indexer.Builder().dataDirectory(baseDir).createMode().build();

      getSampleRecipes()
          .forEach(
              recipe -> {
                try {
                  indexer.addRecipe(recipe);
                } catch (Exception ignored) {
                  // pass
                }
              });
      indexer.commit();
    }
    return indexer;
  }
}
