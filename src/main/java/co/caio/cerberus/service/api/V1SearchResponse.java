package co.caio.cerberus.service.api;

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
@JsonSerialize(as = ImmutableV1SearchResponse.class)
@JsonDeserialize(as = ImmutableV1SearchResponse.class)
@Value.Immutable
public interface V1SearchResponse {
    Optional<SearchResult> result();

    ResponseMetadata metadata();

    enum ErrorCode {
        UNKNOWN_ERROR, INPUT_DECODE_ERROR, INTERNAL_SEARCH_ERROR, OUTPUT_ENCODE_ERROR
    }

    @Value.Immutable(builder=false)
    @Value.Style(visibility = Value.Style.ImplementationVisibility.PACKAGE)
    @JsonSerialize(as = ImmutableResponseMetadata.class)
    @JsonDeserialize(as = ImmutableResponseMetadata.class)
    interface ResponseMetadata {
        @Value.Parameter
        boolean success();
        @Value.Parameter
        Optional<ErrorMetadata> error();

        static ResponseMetadata successMetadata() {
            return ImmutableResponseMetadata.of(true, Optional.empty());
        }

        static ResponseMetadata errorMetadata(ErrorMetadata meta) {
            return ImmutableResponseMetadata.of(false, Optional.of(meta));
        }

        static ResponseMetadata errorMetadata(ErrorCode code, String cause) {
            return errorMetadata(ErrorMetadata.of(code, cause));
        }
    }

    @Value.Immutable(builder=false)
    @Value.Style(visibility = Value.Style.ImplementationVisibility.PACKAGE)
    @JsonSerialize(as = ImmutableErrorMetadata.class)
    @JsonDeserialize(as = ImmutableErrorMetadata.class)
    interface ErrorMetadata {
        @Value.Default
        @Value.Parameter
        default ErrorCode code() {
            return ErrorCode.UNKNOWN_ERROR;
        }
        @Value.Parameter
        String message();

        static ErrorMetadata of(ErrorCode code, String message) {
            return ImmutableErrorMetadata.of(code, message);
        }

        static ErrorMetadata of(String message) {
            return ImmutableErrorMetadata.of(ErrorCode.UNKNOWN_ERROR, message);
        }
    }

    static V1SearchResponse success(SearchResult result) {
        return new Builder().result(result).metadata(ResponseMetadata.successMetadata()).build();
    }

    static V1SearchResponse failure(ErrorCode code, String cause) {
        return new Builder().metadata(ResponseMetadata.errorMetadata(code, cause)).build();
    }

    class Builder extends ImmutableV1SearchResponse.Builder {};
}
