package co.caio.cerberus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecipeTest {
    @Test
    void cantBuildWithoutRequired() {
        var builder = new Recipe.Builder()
                .recipeId(12)
                .name("this is incomplete");
        assertThrows(IllegalStateException.class, () -> builder.build());
    }

    @Test
    void strictBuilder() {
        assertThrows(
                IllegalStateException.class,
                () -> new Recipe.Builder().recipeId(1).recipeId(1));
    }

    @Test
    Recipe basicBuild() {
        var recipe = new Recipe.Builder()
                .recipeId(1)
                .name("valid recipe 1")
                .description("valid recipe 1 description")
                .slug("recipe-1")
                .instructions("there is nothing to do")
                .imageUrl("image.jpg")
                .crawlUrl("https://nowhere.local")
                .build();
        return recipe;
    }

    @Test
    void jsonSerialization() throws Exception {
        var recipe = basicBuild();
        var mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());

        var recipeAsJson = mapper.writeValueAsString(recipe);
        assertEquals(recipe, mapper.readValue(recipeAsJson, Recipe.class));
    }
}