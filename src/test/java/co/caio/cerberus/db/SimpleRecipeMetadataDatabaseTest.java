package co.caio.cerberus.db;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import co.caio.cerberus.db.RecipeMetadataDatabase.RecipeMetadataDbException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SimpleRecipeMetadataDatabaseTest {

  @Test
  void canSaveAndReadSamples() throws IOException {
    var numSamples = 10;
    var dbPath = Files.createTempDirectory("sdb-");

    var writer = new SimpleRecipeMetadataDatabase.Writer(dbPath);

    var samples =
        Util.getSampleRecipes()
            .map(RecipeMetadata::fromRecipe)
            .limit(numSamples)
            .peek(writer::addRecipe)
            .collect(Collectors.toList());

    writer.close();

    var db = new SimpleRecipeMetadataDatabase(dbPath);

    samples.forEach(
        r -> {
          var dbRecipe = db.findById(r.getRecipeId());
          assertTrue(dbRecipe.isPresent());
          assertEquals(r.getRecipeId(), dbRecipe.get().getRecipeId());
        });

    assertEquals(numSamples, db.size());
  }

  @Test
  void canCreateEmptyDb() throws IOException {
    var dbPath = Files.createTempDirectory("sdb-empty-");
    new SimpleRecipeMetadataDatabase.Writer(dbPath).close();
    assertDoesNotThrow(() -> new SimpleRecipeMetadataDatabase(dbPath));
    assertEquals(0, new SimpleRecipeMetadataDatabase(dbPath).size());
  }

  @Test
  void cannotOpenInvalidDir() {
    assertThrows(
        RecipeMetadataDbException.class,
        () -> new SimpleRecipeMetadataDatabase(Path.of("/does/not/exist")));
  }

  @Test
  void cannotWriteToExistingDb() throws IOException {
    var dbPath = Files.createTempDirectory("sdb-empty-");

    // First open+close should work
    assertDoesNotThrow(() -> new SimpleRecipeMetadataDatabase.Writer(dbPath).close());
    // Trying to open a write when a database exists should fail
    assertThrows(
        RecipeMetadataDbException.class, () -> new SimpleRecipeMetadataDatabase.Writer(dbPath));
  }

  @Test
  void saveAllIsNotAllowed() throws IOException {
    var dbPath = Files.createTempDirectory("sdb-empty-");
    new SimpleRecipeMetadataDatabase.Writer(dbPath).close();
    var db = new SimpleRecipeMetadataDatabase(dbPath);
    assertThrows(
        RecipeMetadataDbException.class,
        () -> db.saveAll(List.of(RecipeMetadata.fromRecipe(Util.getBasicRecipe()))));
  }
}
