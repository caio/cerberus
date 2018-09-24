package co.caio.cerberus.service;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.service.api.V1SearchResponse;
import co.caio.cerberus.service.api.V1SearchResponse.ErrorCode;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.net.ServerSocket;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class MainVerticleTest {

  private static WebClient client;

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

  @BeforeAll
  static void setUp(Vertx vertx, VertxTestContext testContext) throws Exception {
    // record ephemeral port number obtained by opening a socket
    // so that we may use it as the test webserver port
    var serverSocket = new ServerSocket(0);
    var portNumber = serverSocket.getLocalPort();
    serverSocket.close();

    client =
        WebClient.create(
            vertx, new WebClientOptions().setDefaultHost("localhost").setDefaultPort(portNumber));

    var config =
        new DeploymentOptions()
            .setConfig(
                new JsonObject()
                    .put(MainVerticle.CONFIG_SERVICE_SSL, false)
                    .put(MainVerticle.CONFIG_SERVICE_PORT, portNumber)
                    .put(MainVerticle.CONFIG_SERIVCE_DATA_DIR, Util.getTestDataDir().toString()));

    vertx.deployVerticle(
        new MainVerticle(), config, testContext.succeeding(fut -> testContext.completeNow()));
  }

  @AfterAll
  static void tearDown(Vertx vertx, VertxTestContext testContext) {
    vertx.close(testContext.succeeding(fut -> testContext.completeNow()));
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
