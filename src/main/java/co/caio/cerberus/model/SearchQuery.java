package co.caio.cerberus.model;

import co.caio.cerberus.Environment;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Style(
        strictBuilder = true,
        visibility = Value.Style.ImplementationVisibility.PACKAGE,
        overshadowImplementation = true
)
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

    @Value.Immutable(builder=false)
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"start", "end"})
    @JsonSerialize(as = ImmutableRangedSpec.class)
    @JsonDeserialize(as = ImmutableRangedSpec.class)
    interface RangedSpec {
        @Value.Parameter int start();
        @Value.Parameter int end();

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
        if (fulltext().isPresent() ||
                !withIngredients().isEmpty() ||
                !withoutIngredients().isEmpty() ||
                numIngredients().isPresent() ||
                prepTime().isPresent() ||
                cookTime().isPresent() ||
                totalTime().isPresent() ||
                calories().isPresent() ||
                fatContent().isPresent() ||
                proteinContent().isPresent() ||
                carbohydrateContent().isPresent()) {
            return;
        }
        throw new IllegalStateException("At least one field must be set");
    }

    static Optional<SearchQuery> fromJson(String serializedQuery) {
        try {
            return Optional.of(Environment.getObjectMapper().readValue(serializedQuery, SearchQuery.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static Optional<String> toJson(SearchQuery searchQuery) {
        try {
            return Optional.of(Environment.getObjectMapper().writeValueAsString(searchQuery));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    class Builder extends ImmutableSearchQuery.Builder {};
}
