package co.caio.cerberus.db;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import org.junit.jupiter.api.Test;

class RecipeMetadataRecipeAdapterTest {
  @Test
  void correctProxying() {
    var sample = Util.getBasicRecipe();
    var proxied = RecipeMetadata.fromRecipe(sample);

    assertEquals(sample.recipeId(), proxied.getRecipeId());
    assertEquals(sample.name(), proxied.getName());
    assertEquals(sample.slug(), proxied.getSlug());
    assertEquals(sample.siteName(), proxied.getSiteName());
    assertEquals(sample.crawlUrl(), proxied.getCrawlUrl());

    assertEquals(sample.ingredients().size(), proxied.getNumIngredients());
    assertEquals(sample.ingredients(), proxied.getIngredients());

    assertEquals(sample.prepTime(), proxied.getPrepTime());
    assertEquals(sample.cookTime(), proxied.getCookTime());
    assertEquals(sample.totalTime(), proxied.getTotalTime());

    assertEquals(sample.calories(), proxied.getCalories());
    assertEquals(sample.fatContent(), proxied.getFatContent());
    assertEquals(sample.proteinContent(), proxied.getProteinContent());
    assertEquals(sample.carbohydrateContent(), proxied.getCarbohydrateContent());

    assertEquals(sample.similarRecipeIds(), proxied.getSimilarRecipeIds());
  }
}
