package co.caio.cerberus;

import co.caio.cerberus.model.Recipe;

public class Util {
    public static Recipe getBasicRecipe() {
        return new Recipe.Builder()
                .recipeId(1)
                .siteId(12)
                .slug("recipe-1")
                .name("valid recipe 1")
                .description("valid recipe 1 description")
                .instructions("there is nothing to do")
                .addIngredients("item a", "item b").build();

    }
}
