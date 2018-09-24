package co.caio.cerberus.service.api;

import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.search.Searcher;
import co.caio.cerberus.service.api.V1SearchResponse.ErrorCode;
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

  private static final String unknownFailureResponse;

  static {
    unknownFailureResponse =
        V1SearchResponse.toJson(
                V1SearchResponse.failure(
                    V1SearchResponse.ErrorCode.UNKNOWN_ERROR, "Unknown/unhandled error"))
            .orElseThrow();
  }

  public V1SearchHandler(Path dataDirectory) {
    searcher = new Searcher.Builder().dataDirectory(dataDirectory).build();
  }

  @Override
  public void handle(RoutingContext routingContext) {
    readSearchQuery(routingContext)
        .compose(this::runSearch)
        .setHandler(
            ar -> {
              routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON);
              if (ar.succeeded()) {
                var maybeResult = V1SearchResponse.toJson(ar.result());
                routingContext.response().end(maybeResult.orElse(unknownFailureResponse));
              } else {
                writeError(routingContext, ar.cause());
              }
            });
  }

  private Future<V1SearchResponse> runSearch(SearchQuery searchQuery) {
    return Future.future(
        future -> {
          try {
            // XXX maybe get maxResults from the query instead
            future.complete(V1SearchResponse.success(searcher.search(searchQuery, 10)));
          } catch (Exception wrapped) {
            future.fail(
                new APIErrorMessage(
                    ErrorCode.INTERNAL_SEARCH_ERROR, "An error occurred during search", wrapped));
          }
        });
  }

  private Future<SearchQuery> readSearchQuery(RoutingContext routingContext) {
    return Future.future(
        future -> {
          var maybeSq = SearchQuery.fromJson(routingContext.getBodyAsString());
          if (maybeSq.isPresent()) {
            future.complete(maybeSq.get());
          } else {
            future.fail(
                new APIErrorMessage(
                    ErrorCode.INPUT_DECODE_ERROR, "Failed to decode input as a SearchQuery", null));
          }
        });
  }

  private void writeError(RoutingContext routingContext, Throwable cause) {
    logger.debug("Exception handling V1 /search request", cause);

    routingContext.response().setStatusCode(500);
    if (cause instanceof APIErrorMessage) {
      var errorCode = ((APIErrorMessage) cause).code;
      var errorMessage = ((APIErrorMessage) cause).message;
      routingContext
          .response()
          .end(
              V1SearchResponse.toJson(V1SearchResponse.failure(errorCode, errorMessage))
                  .orElse(unknownFailureResponse));

    } else {
      logger.error("No error code found, rendering unknown failure", cause);
      routingContext.response().end(unknownFailureResponse);
    }
  }

  private class APIErrorMessage extends Exception {
    final ErrorCode code;
    final String message;

    APIErrorMessage(ErrorCode code, String message, Throwable cause) {
      super(message, cause);
      this.message = message;
      this.code = code;
    }
  }
}
