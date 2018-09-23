package co.caio.cerberus.service.api;

import co.caio.cerberus.Environment;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.search.Searcher;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

public class V1SearchHandler implements Handler<RoutingContext> {
    // FIXME configuration
    private Searcher searcher = new Searcher.Builder().dataDirectory(Paths.get("/tmp/hue")).build();

    private static final Logger logger = LoggerFactory.getLogger(V1SearchHandler.class);

    private static final String CONTENT_TYPE = "Content-type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTEXT_ERROR_KEY = "error_code";

    private static final String unknownFailureResponse;

    static {
        try {
            unknownFailureResponse = Environment.getObjectMapper().writeValueAsString(
                V1SearchResponse.failure(V1SearchResponse.ErrorCode.UNKNOWN_ERROR, "Unknown/unhandled error"));
        } catch (Exception shouldNeverHappen) {
            throw new RuntimeException(shouldNeverHappen);
        }
    }

    @Override
    public void handle(RoutingContext routingContext) {
        readSearchQuery(routingContext)
            .compose(r -> runSearch(r, routingContext))
            .compose(r -> renderResult(r, routingContext))
            .setHandler(ar -> {
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON);
                if (ar.succeeded()) {
                    routingContext.response().end(ar.result());
                } else {
                    writeError(routingContext, ar.cause());
                }
            });
    }

    private Future<String> renderResult(V1SearchResponse searchResponse, RoutingContext routingContext) {
        return Future.future(future -> {
            try {
                var serializedResult = Environment.getObjectMapper().writeValueAsString(searchResponse);
                future.complete(serializedResult);
            } catch (Exception e) {
                routingContext.put(CONTEXT_ERROR_KEY, V1SearchResponse.ErrorCode.OUTPUT_ENCODE_ERROR);
                future.fail(e);
            }
        });
    }

    private Future<V1SearchResponse> runSearch(SearchQuery searchQuery, RoutingContext routingContext) {
        return Future.future(future -> {
            try {
                // XXX maybe get maxResults from the query instead
                future.complete(V1SearchResponse.success(searcher.search(searchQuery, 10)));
            } catch (Exception rethrown) {
                routingContext.put(CONTEXT_ERROR_KEY, V1SearchResponse.ErrorCode.INTERNAL_SEARCH_ERROR);
                future.fail(rethrown);
            }
        });
    }

    private Future<SearchQuery> readSearchQuery(RoutingContext routingContext) {
        return Future.future(ar -> {
            try {
                ar.complete(Environment.getObjectMapper()
                        .readValue(routingContext.getBody().getBytes(), SearchQuery.class));
            } catch (Exception exception) {
                routingContext.put(CONTEXT_ERROR_KEY, V1SearchResponse.ErrorCode.INPUT_DECODE_ERROR);
                ar.fail(exception);
            }
        });
    }

    private void writeError(RoutingContext routingContext, Throwable cause) {
        logger.error("Exception handling V1 /search request", cause);

        var errorCode = (V1SearchResponse.ErrorCode) routingContext.get(CONTEXT_ERROR_KEY);

        routingContext.response().setStatusCode(500);
        if (errorCode != null) {
            routingContext.response().end(renderErrorResponse(errorCode));
        } else {
            logger.error("No error code found, rendering unknown failure");
            routingContext.response().end(unknownFailureResponse);
        }
    }

    private String renderErrorResponse(V1SearchResponse.ErrorCode code) {
        try {
            return Environment.getObjectMapper().writeValueAsString(
                    V1SearchResponse.failure(code, errorCodeToMessage(code)));
        } catch (Exception e) {
            logger.error("Exception rendering error response", e);
            return unknownFailureResponse;
        }
    }

    private String errorCodeToMessage(V1SearchResponse.ErrorCode code) {
        switch (code) {
            case INPUT_DECODE_ERROR:
                return "Failure decoding input data";
            case INTERNAL_SEARCH_ERROR:
                return "Error executing search";
            case OUTPUT_ENCODE_ERROR:
                return "Error encoding result for output";
            default:
                throw new RuntimeException(String.format("Unknown error code '%s'", code));
        }
    }
}
