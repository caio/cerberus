package co.caio.cerberus.db;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import co.caio.cerberus.flatbuffers.FlatRecipe;
import org.junit.jupiter.api.Test;

class RecipeMetadataFlatRecipeAdapterTest {

  @Test
  void correctProxying() {
    var sample = Util.getBasicRecipe();
    var flatRecipe =
        FlatRecipe.getRootAsFlatRecipe(
            FlatBufferSerializer.INSTANCE.flattenRecipe(RecipeMetadata.fromRecipe(sample)));

    var copied = RecipeMetadata.fromFlatRecipe(flatRecipe);

    assertEquals(sample.recipeId(), copied.getRecipeId());
    assertEquals(sample.name(), copied.getName());
    assertEquals(sample.slug(), copied.getSlug());
    assertEquals(sample.siteName(), copied.getSiteName());
    assertEquals(sample.crawlUrl(), copied.getCrawlUrl());

    assertEquals(sample.ingredients().size(), copied.getNumIngredients());
    assertEquals(sample.ingredients(), copied.getIngredients());

    assertEquals(sample.prepTime(), copied.getPrepTime());
    assertEquals(sample.cookTime(), copied.getCookTime());
    assertEquals(sample.totalTime(), copied.getTotalTime());

    assertEquals(sample.calories(), copied.getCalories());
    assertEquals(sample.fatContent(), copied.getFatContent());
    assertEquals(sample.proteinContent(), copied.getProteinContent());
    assertEquals(sample.carbohydrateContent(), copied.getCarbohydrateContent());
    assertEquals(sample.similarRecipeIds(), copied.getSimilarRecipeIds());
  }
}
