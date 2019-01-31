package co.caio.cerberus.db;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import co.caio.cerberus.db.RecipeMetadataDatabase.RecipeMetadataDbException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ChronicleRecipeMetadataDatabaseTest {

  private static Path rwdbPath;
  private static RecipeMetadataDatabase testRWDb;

  @BeforeAll
  static void createTmpDir() throws Exception {
    rwdbPath = Files.createTempDirectory("chronicle-test").resolve("test.db");
    testRWDb = ChronicleRecipeMetadataDatabase.create(rwdbPath, 2000, Util.expectedIndexSize());
  }

  @AfterAll
  static void closeDb() {
    testRWDb.close();
  }

  @Test
  void cantOpenADbThatDoesntExist() {
    assertThrows(
        RecipeMetadataDbException.class,
        () -> ChronicleRecipeMetadataDatabase.open(Path.of("/this/doesnt/exist.db")));
  }

  @Test
  void canOpenAfterCreation() {
    assertDoesNotThrow(
        () -> {
          var ro = ChronicleRecipeMetadataDatabase.open(rwdbPath);
          ro.close();
        });
  }

  @Test
  void cannotWriteToReadOnlyDb() {
    var roDb = ChronicleRecipeMetadataDatabase.open(rwdbPath);
    assertThrows(
        RecipeMetadataDbException.class,
        () -> roDb.saveAll(List.of(RecipeMetadata.fromRecipe(Util.getBasicRecipe()))));
    roDb.close();
  }

  @Test
  void canWriteToRwDb() {
    var recipes =
        Util.getSampleRecipes().map(RecipeMetadata::fromRecipe).collect(Collectors.toList());

    assertDoesNotThrow(() -> testRWDb.saveAll(recipes));

    for (RecipeMetadata rm : recipes) {
      var maybeRm = testRWDb.findById(rm.getRecipeId());
      assertTrue(maybeRm.isPresent());
      assertEquals(rm.getRecipeId(), maybeRm.get().getRecipeId());
    }

    var recipeIds =
        recipes.stream().limit(10).map(RecipeMetadata::getRecipeId).collect(Collectors.toSet());
    var fetched = testRWDb.findAllById(recipeIds);

    int numFetched = 0;
    for (RecipeMetadata rm : fetched) {
      assertTrue(recipeIds.contains(rm.getRecipeId()));
      numFetched++;
    }
    assertEquals(recipeIds.size(), numFetched);
  }

  @Test
  void unexistingIdResultIsEmpty() {
    assertTrue(testRWDb.findById(-42).isEmpty());
  }
}
