package co.caio.cerberus.service;

import co.caio.cerberus.model.SearchResult;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Style(
        strictBuilder = true,
        visibility = Value.Style.ImplementationVisibility.PACKAGE,
        overshadowImplementation = true
)
@JsonSerialize(as = ImmutableSearchResponse.class)
@JsonDeserialize(as = ImmutableSearchResponse.class)
@Value.Immutable
public interface SearchResponse {
    Optional<SearchResult> result();
    Optional<String> error();

    static SearchResponse success(SearchResult result) {
        return new Builder().result(result).build();
    }

    static SearchResponse failure(Throwable throwable) {
        return new Builder().error(throwable.getMessage()).build();
    }

    class Builder extends ImmutableSearchResponse.Builder {};
}
