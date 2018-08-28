package co.caio.cerberus;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

@Value.Style(
        strictBuilder = true,
        visibility = Value.Style.ImplementationVisibility.PACKAGE,
        overshadowImplementation = true
)
@JsonDeserialize(as = ImmutableRecipe.class)
@JsonSerialize(as = ImmutableRecipe.class)
@Value.Immutable
public interface Recipe {
    long recipeId();
    String name();
    String slug();
    String description();
    String instructions();
    String imageUrl();
    String crawlUrl();
    List<String> labels();
    List<String> ingredients();
    List<String> parsedIngredients();

    OptionalInt prepTime();
    OptionalInt cookTime();
    OptionalInt totalTime();

    OptionalInt calories();
    OptionalInt carbohydrateContent();
    OptionalInt fatContent();
    OptionalInt proteinContent();

    class Builder extends co.caio.cerberus.ImmutableRecipe.Builder {}

    public static Optional<Recipe> fromJson(String json) {
        try {
            return Optional.of(Environment.getObjectMapper().readValue(json, Recipe.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<String> toJson(Recipe recipe) {
        try {
            return Optional.of(Environment.getObjectMapper().writeValueAsString(recipe));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

