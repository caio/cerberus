package co.caio.cerberus;

import co.caio.cerberus.model.Recipe;
import co.caio.cerberus.search.CategoryExtractor;
import co.caio.cerberus.search.Indexer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {
  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

  public static Recipe getBasicRecipe() {
    return new Recipe.Builder()
        .recipeId(1)
        .name("valid recipe 1")
        .slug("very-simple-slug")
        .siteName("recipes.recipes")
        .crawlUrl("https://recipes.recipes/1")
        .addInstructions("there is nothing to do")
        .addIngredients("item a", "item b")
        .addSimilarRecipeIds(2, 3, 4, 5)
        .build();
  }

  public static Stream<Recipe> getSampleRecipes() {
    var samplesStream = Util.class.getResourceAsStream("/sample_recipes.jsonlines");
    var reader = new BufferedReader(new InputStreamReader(samplesStream));
    try {
      return reader.lines().map(Util::readRecipe);
    } catch (Exception wrapped) {
      throw new RuntimeException(wrapped);
    }
  }

  private static Recipe readRecipe(String line) {
    try {
      return mapper.readValue(line, Recipe.class);
    } catch (Exception wrapped) {
      throw new RuntimeException(wrapped);
    }
  }

  private static final Indexer indexer;
  private static final Map<Long, Recipe> recipeMap;
  private static final Path testDataDir;
  private static final Properties assertionNumbers;

  static {
    var tmpRecipeMap = new HashMap<Long, Recipe>();

    try {
      testDataDir = Files.createTempDirectory("cerberus-test");
    } catch (Exception rethrown) {
      throw new RuntimeException(rethrown);
    }

    var extractor =
        new CategoryExtractor.Builder()
            .addCategory(
                "diet",
                true,
                recipe ->
                    recipe
                        .diets()
                        .entrySet()
                        .stream()
                        .filter(es -> es.getValue() == 1f)
                        .map(Entry::getKey)
                        .collect(Collectors.toSet()))
            .build();

    indexer = Indexer.Factory.open(testDataDir, extractor);

    getSampleRecipes()
        .forEach(
            recipe -> {
              try {
                indexer.addRecipe(recipe);
                tmpRecipeMap.put(recipe.recipeId(), recipe);
              } catch (Exception wrapped) {
                throw new RuntimeException(wrapped);
              }
            });
    try {
      indexer.commit();
    } catch (Exception rethrown) {
      throw new RuntimeException(rethrown);
    }

    recipeMap = Collections.unmodifiableMap(tmpRecipeMap);

    assertionNumbers = new Properties();
    try {
      assertionNumbers.load(Util.class.getResource("/assertions.properties").openStream());
    } catch (Exception rethrown) {
      throw new RuntimeException(rethrown);
    }

    deleteOnExit(testDataDir);
  }

  private static void deleteOnExit(Path path) {
    path.toFile().deleteOnExit();

    if (!path.toFile().isDirectory()) {
      return;
    }

    try (var items = Files.list(path)) {
      items.forEach(Util::deleteOnExit);
    } catch (IOException wrapped) {
      throw new RuntimeException(wrapped);
    }
  }

  public static Path getTestDataDir() {
    return testDataDir;
  }

  public static int getAssertionNumber(String propertyName) {
    return Integer.parseInt(assertionNumbers.getProperty(propertyName));
  }

  public static int expectedIndexSize() {
    return getAssertionNumber("test.index_size");
  }

  public static Indexer getTestIndexer() {
    return indexer;
  }

  public static Recipe getRecipe(long recipeId) {
    return recipeMap.get(recipeId);
  }

  public static Map<Long, Recipe> getRecipeMap() {
    return recipeMap;
  }
}
