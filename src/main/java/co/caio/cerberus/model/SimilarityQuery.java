package co.caio.cerberus.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Style(
    visibility = Value.Style.ImplementationVisibility.PACKAGE,
    overshadowImplementation = true)
@JsonSerialize(as = ImmutableSimilarityQuery.class)
@JsonDeserialize(as = ImmutableSimilarityQuery.class)
@Value.Immutable
public interface SimilarityQuery {

  // Using fulltext instead of recipeId because using recipeId
  // would require having the index data in a single node but
  // if my hopes and dreams (eh) come true I'll have to shard
  // the index pretty soon
  String fulltext();

  // TODO wrap search options in an object so we can reuse it
  // FIXME sort and facet options
  @Value.Default
  default int maxResults() {
    return 10;
  };

  @Value.Check
  default void check() {
    if (maxResults() < 1 || maxResults() > 100) {
      throw new IllegalStateException("maxResults needs to be in [1,100]");
    }
    if (fulltext().isEmpty() || fulltext().isBlank()) {
      throw new IllegalStateException("Field `fulltext` must not be empty");
    }
    if (fulltext().length() < 30) { // arbitrary
      throw new IllegalStateException("Field `fulltext` must be at least 30 characters");
    }
  }

  class Builder extends ImmutableSimilarityQuery.Builder {}
}
