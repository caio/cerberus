package co.caio.cerberus.model;

import co.caio.cerberus.Environment;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;
import org.slf4j.LoggerFactory;

@Value.Style(
    visibility = Value.Style.ImplementationVisibility.PACKAGE,
    overshadowImplementation = true)
@JsonSerialize(as = ImmutableSearchQuery.class)
@JsonDeserialize(as = ImmutableSearchQuery.class)
@Value.Immutable
public interface SearchQuery {
  Optional<String> fulltext();

  Optional<String> similarity();

  Optional<RangedSpec> numIngredients();

  Optional<RangedSpec> prepTime();

  Optional<RangedSpec> cookTime();

  Optional<RangedSpec> totalTime();

  Optional<RangedSpec> calories();

  Optional<RangedSpec> fatContent();

  Optional<RangedSpec> proteinContent();

  Optional<RangedSpec> carbohydrateContent();

  Map<String, Float> dietThreshold();

  List<DrillDownSpec> drillDown();

  List<String> matchKeyword();

  @Value.Default
  default int maxResults() {
    return 10;
  }

  @Value.Default
  default int maxFacets() {
    return 10;
  }

  enum SortOrder {
    RELEVANCE,
    NUM_INGREDIENTS,
    PREP_TIME,
    COOK_TIME,
    TOTAL_TIME
  }

  @Value.Default
  default SortOrder sort() {
    return SortOrder.RELEVANCE;
  }

  @Value.Immutable(builder = false)
  @JsonFormat(shape = JsonFormat.Shape.ARRAY)
  @JsonPropertyOrder({"start", "end"})
  @JsonSerialize(as = ImmutableRangedSpec.class)
  @JsonDeserialize(as = ImmutableRangedSpec.class)
  interface RangedSpec {
    @Value.Parameter
    int start();

    @Value.Parameter
    int end();

    static RangedSpec of(int start, int end) {
      return ImmutableRangedSpec.of(start, end);
    }

    @Value.Check
    default void check() {
      if (start() > end()) {
        throw new IllegalStateException("Range start must be before range end");
      }
      if (start() < 0 || end() < 0) {
        throw new IllegalStateException("Range must not contain negative numbers");
      }
    }
  }

  @Value.Immutable(builder = false)
  @JsonFormat(shape = JsonFormat.Shape.ARRAY)
  @JsonPropertyOrder({"field", "label"})
  @JsonSerialize(as = ImmutableDrillDownSpec.class)
  @JsonDeserialize(as = ImmutableDrillDownSpec.class)
  interface DrillDownSpec {
    @Value.Parameter
    String field();

    @Value.Parameter
    String label();

    static DrillDownSpec of(String field, String label) {
      return ImmutableDrillDownSpec.of(field, label);
    }

    @Value.Check
    default void check() {
      if (!DrillDown.isValidRangeLabel(field(), label())) {
        throw new IllegalStateException(
            String.format("Invalid label `%s` for field `%s`", label(), field()));
      }
    }
  }

  @Value.Check
  default void check() {
    if (maxResults() < 1 || maxResults() > 100) {
      throw new IllegalStateException("maxResults needs to be in [1,100]");
    }
    if (maxFacets() < 0 || maxFacets() > 100) {
      throw new IllegalStateException("maxFacets needs to be in [0,100]");
    }
    dietThreshold()
        .forEach(
            (diet, score) -> {
              if (score <= 0 || score > 1) {
                throw new IllegalStateException("score must be in ]0,1]");
              }
              if (!Diet.isKnown(diet)) {
                throw new IllegalStateException(String.format("Unknown diet `%s`", diet));
              }
            });
    if (fulltext().isPresent() && similarity().isPresent()) {
      throw new IllegalStateException("Can't use fulltext and similarity at the same time");
    }
    if (similarity().isPresent() && similarity().get().strip().length() < 30) {
      throw new IllegalStateException("similarity queries requires at least 30 characters");
    }
    if (fulltext().isPresent() && fulltext().get().strip().length() < 3) {
      throw new IllegalStateException("fulltext queries require at least 2 characters");
    }
    if ((fulltext().isPresent() || similarity().isPresent())
        || !dietThreshold().isEmpty()
        || !matchKeyword().isEmpty()
        || numIngredients().isPresent()
        || prepTime().isPresent()
        || cookTime().isPresent()
        || totalTime().isPresent()
        || calories().isPresent()
        || fatContent().isPresent()
        || proteinContent().isPresent()
        || carbohydrateContent().isPresent()) {
      return;
    }
    throw new IllegalStateException("At least one field must be set");
  }

  static Optional<SearchQuery> fromJson(String serializedQuery) {
    try {
      return Optional.of(
          Environment.getObjectMapper().readValue(serializedQuery, SearchQuery.class));
    } catch (Exception e) {
      LoggerFactory.getLogger(SearchQuery.class).error("Failed to read json <{}>", serializedQuery);
      return Optional.empty();
    }
  }

  static Optional<String> toJson(SearchQuery searchQuery) {
    try {
      return Optional.of(Environment.getObjectMapper().writeValueAsString(searchQuery));
    } catch (Exception e) {
      LoggerFactory.getLogger(SearchQuery.class)
          .error("Failed to serialize {} to json", searchQuery);
      return Optional.empty();
    }
  }

  class Builder extends ImmutableSearchQuery.Builder {
    public Builder addMatchDiet(String dietName) {
      putDietThreshold(dietName, 1f);
      return this;
    }

    public Builder addDrillDown(String field, String label) {
      addDrillDown(DrillDownSpec.of(field, label));
      return this;
    }
  }
}
