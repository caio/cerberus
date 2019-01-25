package co.caio.cerberus.db;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import co.caio.cerberus.flatbuffers.FlatRecipe;
import org.junit.jupiter.api.Test;

class RecipeMetadataFlatRecipeCopyTest {

  @Test
  void correctProxying() {
    var sample = Util.getBasicRecipe();
    var flatRecipe =
        FlatRecipe.getRootAsFlatRecipe(
            LMDBRecipeMetadataDatabase.flattenRecipe(RecipeMetadata.fromRecipe(sample)));

    var copied = RecipeMetadata.fromFlatRecipe(flatRecipe);

    assertEquals(sample.recipeId(), copied.getRecipeId());
    assertEquals(sample.name(), copied.getName());
    assertEquals(sample.ingredients().size(), copied.getNumIngredients());
    assertEquals(sample.ingredients(), copied.getIngredients());
    assertEquals(sample.instructions(), copied.getInstructions());
    assertEquals(sample.calories(), copied.getCalories());
    assertEquals(sample.crawlUrl(), copied.getCrawlUrl());
    assertEquals(sample.totalTime(), copied.getTotalTime());

    // FIXME need to add to the data model
    assertEquals(null, copied.getSiteName());
  }
}
