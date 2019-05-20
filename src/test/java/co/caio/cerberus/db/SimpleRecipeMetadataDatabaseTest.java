package co.caio.cerberus.db;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SimpleRecipeMetadataDatabaseTest {

  @Test
  void create() throws IOException {
    var dbPath = Files.createTempDirectory("sdb");

    var writer = new SimpleRecipeMetadataDatabase.Writer(dbPath);

    var samples =
        Util.getSampleRecipes()
            .map(RecipeMetadata::fromRecipe)
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
  }
}
