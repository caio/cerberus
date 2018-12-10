package co.caio.cerberus.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;

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
}
