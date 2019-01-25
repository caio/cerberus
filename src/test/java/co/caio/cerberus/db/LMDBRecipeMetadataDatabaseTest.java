package co.caio.cerberus.db;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import co.caio.cerberus.db.LMDBRecipeMetadataDatabase.RecipeDatabaseConfigurationError;
import co.caio.cerberus.db.LMDBRecipeMetadataDatabase.RecipeDatabaseDoesNotExist;
import co.caio.cerberus.db.LMDBRecipeMetadataDatabase.RecipeDatabaseIsReadOnly;
import co.caio.cerberus.model.Recipe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LMDBRecipeMetadataDatabaseTest {

  private static final Path dbPath;

  static {
    try {
      dbPath = Files.createTempDirectory("lmdb");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeAll
  static void createEmptyDb() {
    open(false);
  }

  private static RecipeMetadataDatabase open(boolean readOnly) {
    try {
      return RecipeMetadataDatabase.Builder.open(dbPath, 5_000, readOnly);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void cantOpenADbThatDoestExist() throws Exception {
    // db path doesn't exist
    assertThrows(
        RecipeDatabaseConfigurationError.class,
        () -> {
          RecipeMetadataDatabase.Builder.open(Path.of("/should/not/exist"), 5_000, false);
        });

    // path exists, but there is no db and we want to read it
    var emptyDir = Files.createTempDirectory("empty-recipe-db-path");
    emptyDir.toFile().deleteOnExit();
    assertThrows(
        RecipeDatabaseDoesNotExist.class,
        () -> {
          RecipeMetadataDatabase.Builder.open(emptyDir, 5_000, true);
        });
  }

  @Test
  void cantSaveReadOnlyDb() {
    var db = open(true);
    assertThrows(
        RecipeDatabaseIsReadOnly.class,
        () -> {
          db.saveAll(List.of(RecipeMetadata.fromRecipe(Util.getBasicRecipe())));
        });
  }

  @Test
  void unexistingIdResultIsEmpty() {
    var db = open(true);
    long unexistingId = -42;
    assertTrue(db.findById(unexistingId).isEmpty());
  }

  @Test
  void canWriteAndRead() {
    var db = open(false);
    var sample = RecipeMetadata.fromRecipe(Util.getBasicRecipe());

    db.saveAll(List.of(sample));

    var maybeSample = db.findById(sample.getRecipeId());
    assertTrue(maybeSample.isPresent());
    var retrievedSample = maybeSample.get();

    // XXX implement a common .equals?
    assertEquals(sample.getRecipeId(), retrievedSample.getRecipeId());
    assertEquals(sample.getName(), retrievedSample.getName());
  }

  @Test
  void findByIdsOnlyReturnsExistingData() {
    var allRecipeIds = Util.getSampleRecipes().map(Recipe::recipeId).collect(Collectors.toList());
    var recipesToAdd =
        Util.getSampleRecipes()
            .map(RecipeMetadata::fromRecipe)
            .limit(10)
            .collect(Collectors.toList());

    var db = open(false);

    db.saveAll(recipesToAdd);

    var storedRecipes = db.findAllById(allRecipeIds);

    int numStoredRecipes = 0;
    for (var rm : storedRecipes) {
      assertEquals(recipesToAdd.get(numStoredRecipes).getRecipeId(), rm.getRecipeId());
      numStoredRecipes++;
    }

    assertEquals(recipesToAdd.size(), numStoredRecipes);
  }
}
