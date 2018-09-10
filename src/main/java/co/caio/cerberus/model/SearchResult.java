package co.caio.cerberus.model;

import co.caio.cerberus.Environment;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Value.Style(
        strictBuilder = true,
        visibility = Value.Style.ImplementationVisibility.PACKAGE,
        overshadowImplementation = true
)
@JsonSerialize(as = ImmutableSearchResult.class)
@JsonDeserialize(as = ImmutableSearchResult.class)
@Value.Immutable
public interface SearchResult {

    @Value.Default
    default long totalHits() {
        return 0;
    }

    List<Item> recipes();

    List<FacetData> facets();

    class Builder extends ImmutableSearchResult.Builder {
        public Builder addRecipe(long recipeId, String name, String crawlUrl) {
            return addRecipes(Item.of(recipeId, name, crawlUrl));
        }
    };

    @Value.Check
    default void check() {
        if (totalHits() < 0) {
            throw new IllegalStateException("totalHits must not be negative");
        }
        if (recipes().size() > totalHits()) {
            throw new IllegalStateException("totalHits must be >= recipes().length");
        }
    }

    @Value.Style(
            strictBuilder = true,
            visibility = Value.Style.ImplementationVisibility.PACKAGE,
            overshadowImplementation = true
    )
    @JsonSerialize(as = ImmutableFacetData.class)
    @JsonDeserialize(as = ImmutableFacetData.class)
    @Value.Immutable
    interface FacetData {
        String dimension();
        List<LabelData> children();

        class Builder extends ImmutableFacetData.Builder {
            public Builder addChild(String label, long count) {
                addChildren(LabelData.of(label, count));
                return this;
            }
        };

        @Value.Immutable(builder=false)
        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        @JsonPropertyOrder({"label", "count"})
        @JsonSerialize(as = ImmutableLabelData.class)
        @JsonDeserialize(as = ImmutableLabelData.class)
        interface LabelData {
            @Value.Parameter String label();
            @Value.Parameter long count();

            static LabelData of(String label, long count) {
                return ImmutableLabelData.of(label, count);
            }
        }
    }

    @Value.Immutable(builder=false)
    @JsonSerialize(as = ImmutableItem.class)
    @JsonDeserialize(as = ImmutableItem.class)
    interface Item {
        @Value.Parameter long recipeId();
        @Value.Parameter String name();
        @Value.Parameter String crawlUrl();

        static Item of(long recipeId, String name, String crawlUrl) {
            return ImmutableItem.of(recipeId, name, crawlUrl);
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

    static Optional<SearchResult> fromJson(String json) {
        try {
            return Optional.of(Environment.getObjectMapper().readValue(json, SearchResult.class));
        } catch (Exception ignored) {
            LoggerFactory.getLogger(SearchResult.class).error(
                    "Failed to read json <{}> as <{}>", json, SearchResult.class);
            return Optional.empty();
        }
    }

    static Optional<String> toJson(SearchResult sr) {
        try {
            return Optional.of(Environment.getObjectMapper().writeValueAsString(sr));
        } catch (Exception ignored) {
            LoggerFactory.getLogger(SearchResult.class).error(
                    "Failed to serialize {} to json", sr);
            return Optional.empty();
        }
    }
}
