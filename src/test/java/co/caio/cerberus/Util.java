package co.caio.cerberus;

import co.caio.cerberus.model.Recipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class Util {
    public static Recipe getBasicRecipe() {
        return new Recipe.Builder()
                .recipeId(1)
                .siteId(12)
                .slug("recipe-1")
                .name("valid recipe 1")
                .crawlUrl("https://recipes.recipes/1")
                .description("valid recipe 1 description")
                .instructions("there is nothing to do")
                .addIngredients("item a", "item b").build();

    }

    public static Stream<Recipe> getSampleRecipes() {
        var samplesFile = Util.class.getResource("/sample_recipes.jsonlines").getFile();
        try {
            return Files.lines(Paths.get(samplesFile)).map(Recipe::fromJson).flatMap(Optional::stream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
