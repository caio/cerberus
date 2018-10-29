package co.caio.cerberus.service;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Environment;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

// Tests here serve mostly to verify that the /health
// endpoint is not broken
class HealthTest extends MainVerticleTestCase {
  @Test
  void nativeTransport(VertxTestContext testContext) {
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

  @Test
  void buildStatus(VertxTestContext testContext) {
    healthRequest(
        "build-status",
        testContext,
        result -> {
          var wanted = Environment.getBuildStatus().isValid() ? "UP" : "DOWN";
          assertEquals(wanted, result.getString("status"));
          testContext.completeNow();
        });
  }

  @Test
  void numDocs(VertxTestContext testContext) {
    healthRequest(
        "num-docs",
        testContext,
        result -> {
          assertEquals("UP", result.getString("status"));
          testContext.completeNow();
        });
  }

  private void healthRequest(
      String checkId, VertxTestContext testContext, Consumer<JsonObject> consumer) {
    client
        .get("/health/" + checkId)
        .send(
            ar -> {
              if (ar.succeeded()) {
                var response = ar.result();

                testContext.verify(
                    () ->
                        assertTrue(
                            response.getHeader("Content-type").startsWith("application/json")));

                var result = response.bodyAsJsonObject();
                testContext.verify(() -> assertEquals(checkId, result.getString("id")));

                consumer.accept(result);

              } else {
                testContext.failNow(ar.cause());
              }
            });
  }
}
