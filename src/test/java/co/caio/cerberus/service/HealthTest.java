package co.caio.cerberus.service;

import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

// Tests here serve mostly to verify that the /health
// endpoint is not broken
class HealthTest extends MainVerticleTestCase {
  @Test
  void nativeTransport(Vertx vertx, VertxTestContext testContext) {
    healthRequest(
        "native-transport",
        testContext,
        result -> {
          // native-transport is only up when started with specific
          // vertx options, which we don't for tests
          assertEquals("DOWN", result.getString("status"));
          testContext.completeNow();
        });
  }

  private void healthRequest(
      String checkId, VertxTestContext testContext, Consumer<JsonObject> consumer) {
    client
        .get("/health")
        .send(
            ar -> {
              if (ar.succeeded()) {
                var response = ar.result();

                testContext.verify(
                    () ->
                        assertTrue(
                            response.getHeader("Content-type").startsWith("application/json")));

                var checkResult =
                    response
                        .bodyAsJsonObject()
                        .getJsonArray("checks")
                        .stream()
                        .filter(j -> ((JsonObject) j).getString("id").equals(checkId))
                        .findFirst();
                testContext.verify(() -> assertTrue(checkResult.isPresent()));
                consumer.accept((JsonObject) checkResult.get());
              } else {
                testContext.failNow(ar.cause());
              }
            });
  }
}
