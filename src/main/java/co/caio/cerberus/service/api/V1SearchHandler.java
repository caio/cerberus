package co.caio.cerberus.service.api;

import co.caio.cerberus.Environment;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
import co.caio.cerberus.search.Searcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
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

    static public Router buildRouter(Vertx vertx) {
        var router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route("/search")
                .consumes(APPLICATION_JSON)
                .handler(new V1SearchHandler());
        return router;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            var searchQuery = readBody(routingContext);
            var searchResult = runSearch(searchQuery, routingContext);
            writeResponse(V1SearchResponse.success(searchResult), routingContext);
        } catch (Exception e) {
            logger.error("Exception caught handling V1 /search", e);

            var errorCode = (V1SearchResponse.ErrorCode) routingContext.get(CONTEXT_ERROR_KEY);

            routingContext.response().setStatusCode(500);
            if (errorCode != null) {
                writeResponse(V1SearchResponse.failure(errorCode, errorCodeToMessage(errorCode)), routingContext);
            } else {
                routingContext.response().end(unknownFailureResponse);
            }
        }
    }

    private String errorCodeToMessage(V1SearchResponse.ErrorCode code) {
        switch (code) {
            case INPUT_DECODE_ERROR:
                return "Failure decoding input data";
            case INTERNAL_SEARCH_ERROR:
                return "Error executing search";
            default:
                throw new RuntimeException(String.format("Unknown error code '%s'", code));
        }
    }

    private SearchResult runSearch(SearchQuery searchQuery, RoutingContext routingContext) throws Exception {
        try {
            // XXX maybe get maxResults from the query instead
            return searcher.search(searchQuery, 10);
        } catch (Exception rethrown) {
            routingContext.put(CONTEXT_ERROR_KEY, V1SearchResponse.ErrorCode.INTERNAL_SEARCH_ERROR);
            throw rethrown;
        }
    }

    private SearchQuery readBody(RoutingContext routingContext) throws Exception {
        try {
            return Environment.getObjectMapper().readValue(routingContext.getBody().getBytes(), SearchQuery.class);
        } catch (Exception rethrown) {
            routingContext.put(CONTEXT_ERROR_KEY, V1SearchResponse.ErrorCode.INPUT_DECODE_ERROR);
            throw rethrown;
        }
    }

    private void writeResponse(V1SearchResponse searchResponse, RoutingContext context) {
        try {
            context.response().putHeader(CONTENT_TYPE, APPLICATION_JSON);
            context.response().end(Environment.getObjectMapper().writeValueAsString(searchResponse));
        } catch (Throwable propagated) {
            context.fail(propagated);
        }
    }
}
