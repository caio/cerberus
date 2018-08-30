package co.caio.cerberus.search;

import co.caio.cerberus.Environment;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

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
    long siteId();
    String slug();
    String name();
    String description();
    String instructions();
    List<String> ingredients();
    Set<String> labels();
    Set<String> keywords();

    OptionalInt prepTime();
    OptionalInt cookTime();
    OptionalInt totalTime();

    OptionalInt calories();
    OptionalInt carbohydrateContent();
    OptionalInt fatContent();
    OptionalInt proteinContent();

    class Builder extends ImmutableRecipe.Builder {}

    static Optional<Recipe> fromJson(String json) {
        try {
            return Optional.of(Environment.getObjectMapper().readValue(json, Recipe.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static Optional<String> toJson(Recipe recipe) {
        try {
            return Optional.of(Environment.getObjectMapper().writeValueAsString(recipe));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

