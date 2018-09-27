package co.caio.cerberus.service;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.service.api.V1SearchResponse;
import co.caio.cerberus.service.api.V1SearchResponse.ErrorCode;
import io.vertx.core.buffer.Buffer;
import io.vertx.junit5.VertxTestContext;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class V1SearchAPITest extends MainVerticleTestCase {

  @Test
  void simpleSearch(VertxTestContext testContext) {
    var searchQuery = new SearchQuery.Builder().fulltext("keto bacon").build();
    var maybeBody = SearchQuery.toJson(searchQuery);
    assertTrue(maybeBody.isPresent());
    searchRequest(
        testContext,
        maybeBody.get(),
        200,
        (sr) -> {
          testContext.verify(
              () -> {
                assertTrue(sr.metadata().success());
                assertFalse(sr.metadata().error().isPresent());
                assertTrue(sr.result().isPresent());
                assertTrue(sr.result().get().totalHits() > 0);
              });
          testContext.completeNow();
        });
  }

  @Test
  void jsonOnBadInput(VertxTestContext testContext) {
    searchRequest(
        testContext,
        "bad input that isnt even json",
        500,
        (sr) -> {
          testContext.verify(
              () -> {
                assertFalse(sr.metadata().success());
                assertTrue(sr.metadata().error().isPresent());
                assertEquals(sr.metadata().error().get().code(), ErrorCode.INPUT_DECODE_ERROR);
              });
          testContext.completeNow();
        });
  }

  private void searchRequest(
      VertxTestContext testContext,
      String body,
      int wantedStatus,
      Consumer<V1SearchResponse> responseConsumer) {
    client
        .post("/api/v1/search")
        .putHeader("Content-type", "application/json")
        .sendBuffer(
            Buffer.buffer(body),
            ar -> {
              if (ar.succeeded()) {
                var response = ar.result();
                testContext.verify(
                    () -> {
                      assertEquals(wantedStatus, response.statusCode());
                      assertEquals("application/json", response.getHeader("Content-type"));
                    });

                var maybeSR = V1SearchResponse.fromJson(response.bodyAsString());
                assertTrue(maybeSR.isPresent());
                responseConsumer.accept(maybeSR.get());
              } else {
                testContext.failNow(ar.cause());
              }
            });
  }
}
