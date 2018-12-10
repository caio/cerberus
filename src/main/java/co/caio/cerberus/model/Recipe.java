package co.caio.cerberus.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import org.immutables.value.Value;

@Value.Style(
    visibility = Value.Style.ImplementationVisibility.PACKAGE,
    overshadowImplementation = true)
@JsonDeserialize(as = ImmutableRecipe.class)
@JsonSerialize(as = ImmutableRecipe.class)
@Value.Immutable
public interface Recipe {
  long recipeId();

  String name();

  String crawlUrl();

  String instructions();

  List<String> ingredients();

  Map<String, Float> diets();

  Set<String> keywords();

  OptionalInt prepTime();

  OptionalInt cookTime();

  OptionalInt totalTime();

  OptionalInt calories();

  OptionalInt carbohydrateContent();

  OptionalInt fatContent();

  OptionalInt proteinContent();

  @Value.Default
  default String description() {
    return "";
  }

  @Value.Check
  default void check() {
    RecipePrecondition.check(this);
  }

  class RecipePrecondition {
    static void check(Recipe recipe) {
      nonEmpty("name", recipe.name());
      nonEmpty("crawlUrl", recipe.crawlUrl());
      nonEmpty("instructions", recipe.instructions());
      nonEmpty("ingredients", recipe.ingredients());

      recipe
          .diets()
          .forEach(
              (diet, score) -> {
                if (!Diet.isKnown(diet)) {
                  throw new IllegalStateException(String.format("Unknown diet `%s`", diet));
                }
                if (score < 0 || score > 1) {
                  throw new IllegalStateException(
                      String.format("Score for diet `%s` (%f) should be [0,1]", diet, score));
                }
              });
    }

    static void nonEmpty(String fieldName, String fieldValue) {
      if (fieldValue.isEmpty()) {
        throw new IllegalStateException(String.format("Field `%s` can't be empty", fieldName));
      }
    }

    static void nonEmpty(String fieldName, List items) {
      if (items.isEmpty()) {
        throw new IllegalStateException(String.format("Field `%s` must have items", fieldName));
      }
    }
  }

  class Builder extends ImmutableRecipe.Builder {}
}
