package co.caio.cerberus.service.api;

import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.search.Searcher;
import co.caio.cerberus.service.Serializer;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V1SearchHandler implements Handler<RoutingContext> {
  private final Searcher searcher;

  private static final Logger logger = LoggerFactory.getLogger(V1SearchHandler.class);

  private static final String CONTENT_TYPE = "Content-type";
  private static final String APPLICATION_JSON = "application/json";
  private static final String CONTEXT_ERROR_KEY = "error_code";

  private static final String unknownFailureResponse;

  static {
    try {
      unknownFailureResponse =
          Serializer.encode(
              V1SearchResponse.failure(
                  V1SearchResponse.ErrorCode.UNKNOWN_ERROR, "Unknown/unhandled error"));
    } catch (Exception shouldNeverHappen) {
      throw new RuntimeException(shouldNeverHappen);
    }
  }

  public V1SearchHandler(Path dataDirectory) {
    searcher = new Searcher.Builder().dataDirectory(dataDirectory).build();
  }

  @Override
  public void handle(RoutingContext routingContext) {
    readSearchQuery(routingContext, SearchQuery.class)
        .compose(r -> runSearch((SearchQuery) r, routingContext))
        .compose(Serializer::encodeAsync)
        .setHandler(
            ar -> {
              routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON);
              if (ar.succeeded()) {
                routingContext.response().end(ar.result());
              } else {
                writeError(routingContext, ar.cause());
              }
            });
  }

  private Future<V1SearchResponse> runSearch(
      SearchQuery searchQuery, RoutingContext routingContext) {
    return Future.future(
        future -> {
          try {
            // XXX maybe get maxResults from the query instead
            future.complete(V1SearchResponse.success(searcher.search(searchQuery, 10)));
          } catch (Exception exception) {
            routingContext.put(CONTEXT_ERROR_KEY, V1SearchResponse.ErrorCode.INTERNAL_SEARCH_ERROR);
            future.fail(exception);
          }
        });
  }

  private Future<?> readSearchQuery(RoutingContext routingContext, Class<?> clazz) {
    return Serializer.decodeAsync(routingContext.getBodyAsString(), clazz)
        .setHandler(
            ar -> {
              if (ar.failed()) {
                routingContext.put(
                    CONTEXT_ERROR_KEY, V1SearchResponse.ErrorCode.INPUT_DECODE_ERROR);
              }
            });
  }

  private void writeError(RoutingContext routingContext, Throwable cause) {
    logger.debug("Exception handling V1 /search request", cause);

    var errorCode = (V1SearchResponse.ErrorCode) routingContext.get(CONTEXT_ERROR_KEY);

    routingContext.response().setStatusCode(500);
    if (errorCode != null) {
      routingContext.response().end(renderErrorResponse(errorCode));
    } else {
      logger.error("No error code found, rendering unknown failure", cause);
      routingContext.response().end(unknownFailureResponse);
    }
  }

  private String renderErrorResponse(V1SearchResponse.ErrorCode code) {
    try {
      return Serializer.encode(V1SearchResponse.failure(code, errorCodeToMessage(code)));
    } catch (Exception exception) {
      logger.error("Exception rendering error response", exception);
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
