package co.caio.cerberus.model;

import co.caio.cerberus.Environment;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
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

  List<String> withIngredients();

  List<String> withoutIngredients();

  Optional<RangedSpec> numIngredients();

  Optional<RangedSpec> prepTime();

  Optional<RangedSpec> cookTime();

  Optional<RangedSpec> totalTime();

  Optional<RangedSpec> calories();

  Optional<RangedSpec> fatContent();

  Optional<RangedSpec> proteinContent();

  Optional<RangedSpec> carbohydrateContent();

  List<String> matchDiet();

  List<String> matchKeyword();

  @Value.Default
  default int maxResults() {
    return 10;
  };

  @Value.Default
  default int maxFacets() {
    return 10;
  };

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

  @Value.Check
  default void check() {
    if (maxResults() < 1 || maxResults() > 100) {
      throw new IllegalStateException("maxResults needs to be in [1,100]");
    }
    if (maxFacets() < 0 || maxFacets() > 100) {
      throw new IllegalStateException("maxFacets needs to be in [0,100]");
    }
    if (fulltext().isPresent()
        || !withIngredients().isEmpty()
        || !withoutIngredients().isEmpty()
        || !matchDiet().isEmpty()
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
      LoggerFactory.getLogger(SearchQuery.class)
          .error("Failed to read json <{}> as <{}>", serializedQuery, SearchQuery.class);
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

  class Builder extends ImmutableSearchQuery.Builder {}
}
