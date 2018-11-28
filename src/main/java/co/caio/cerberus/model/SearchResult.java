package co.caio.cerberus.model;

import co.caio.cerberus.Environment;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import org.slf4j.LoggerFactory;

@Value.Style(
    strictBuilder = true,
    visibility = Value.Style.ImplementationVisibility.PACKAGE,
    overshadowImplementation = true)
@JsonSerialize(as = ImmutableSearchResult.class)
@JsonDeserialize(as = ImmutableSearchResult.class)
@Value.Immutable
public interface SearchResult {

  @Value.Default
  default long totalHits() {
    return 0;
  }

  List<SearchResultRecipe> recipes();

  List<FacetData> facets();

  class Builder extends ImmutableSearchResult.Builder {
    public Builder addRecipe(long recipeId, String name, String crawlUrl) {
      return addRecipes(SearchResultRecipe.of(recipeId, name, crawlUrl));
    }
  }

  @Value.Check
  default void check() {
    if (totalHits() < 0) {
      throw new IllegalStateException("totalHits must not be negative");
    }
    if (recipes().size() > totalHits()) {
      throw new IllegalStateException("totalHits must be >= recipes().length");
    }
  }

  static Optional<SearchResult> fromJson(String json) {
    try {
      return Optional.of(Environment.getObjectMapper().readValue(json, SearchResult.class));
    } catch (Exception ignored) {
      LoggerFactory.getLogger(SearchResult.class)
          .error("Failed to read json <{}> as <{}>", json, SearchResult.class);
      return Optional.empty();
    }
  }

  static Optional<String> toJson(SearchResult sr) {
    try {
      return Optional.of(Environment.getObjectMapper().writeValueAsString(sr));
    } catch (Exception ignored) {
      LoggerFactory.getLogger(SearchResult.class).error("Failed to serialize {} to json", sr);
      return Optional.empty();
    }
  }
}
