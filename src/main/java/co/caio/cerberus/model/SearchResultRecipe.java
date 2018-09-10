package co.caio.cerberus.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable(builder=false)
@JsonSerialize(as = ImmutableSearchResultRecipe.class)
@JsonDeserialize(as = ImmutableSearchResultRecipe.class)
public
interface SearchResultRecipe {
    @Value.Parameter long recipeId();
    @Value.Parameter String name();
    @Value.Parameter String crawlUrl();

    static SearchResultRecipe of(long recipeId, String name, String crawlUrl) {
        return ImmutableSearchResultRecipe.of(recipeId, name, crawlUrl);
    }

    @Value.Check
    default void check() {
        if (recipeId() <= 0) {
            throw new IllegalStateException("invalid recipeId");
        }
        if (name().isEmpty()) {
            throw new IllegalStateException("name can't be empty");
        }
        if (crawlUrl().isEmpty()) {
            throw new IllegalStateException("crawlUrl can't be empty");
        }
    }
}
