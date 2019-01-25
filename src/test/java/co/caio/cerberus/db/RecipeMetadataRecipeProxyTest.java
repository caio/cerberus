package co.caio.cerberus.db;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import org.junit.jupiter.api.Test;

class RecipeMetadataRecipeProxyTest {
  @Test
  void correctProxying() {
    var sample = Util.getBasicRecipe();
    var proxied = RecipeMetadata.fromRecipe(sample);

    assertEquals(sample.recipeId(), proxied.getRecipeId());
    assertEquals(sample.name(), proxied.getName());
    assertEquals(sample.ingredients().size(), proxied.getNumIngredients());
    assertEquals(sample.ingredients(), proxied.getIngredients());
    assertEquals(sample.instructions(), proxied.getInstructions());
    assertEquals(sample.calories(), proxied.getCalories());
    assertEquals(sample.crawlUrl(), proxied.getCrawlUrl());
    assertEquals(sample.totalTime(), proxied.getTotalTime());

    // FIXME need to add to the data model
    assertEquals(null, proxied.getSiteName());
  }
}
