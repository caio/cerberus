package co.caio.cerberus.service;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Environment;
import co.caio.cerberus.Util;
import co.caio.cerberus.search.Indexer;
import co.caio.cerberus.service.api.V1SearchResponse;
import co.caio.cerberus.service.api.V1SearchResponse.ErrorCode;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.net.ServerSocket;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class MainVerticleTest {

  int port = 0;
  String dataPath = null;

  @Test
  void returnsJsonOnBadInput(Vertx vertx, VertxTestContext testContext) {

    var client = WebClient.create(vertx);

    vertx.deployVerticle(
        new MainVerticle(),
        getConfig(),
        testContext.succeeding(
            id -> {
              client
                  .post(port, "localhost", "/api/v1/search")
                  .sendJson(
                      "huehue?",
                      ar -> {
                        var response = ar.result();
                        testContext.verify(
                            () -> {
                              assertEquals(response.statusCode(), 500);
                              assertEquals(response.getHeader("Content-type"), "application/json");
                            });
                        try {
                          var sr =
                              Environment.getObjectMapper()
                                  .readValue(response.bodyAsString(), V1SearchResponse.class);

                          testContext.verify(
                              () -> {
                                assertFalse(sr.metadata().success());
                                assertTrue(sr.metadata().error().isPresent());
                                assertEquals(
                                    sr.metadata().error().get().code(),
                                    ErrorCode.INPUT_DECODE_ERROR);
                              });

                          testContext.completeNow();
                        } catch (Exception exception) {
                          testContext.failNow(exception);
                        }
                      });
            }));
  }

  @BeforeEach
  void setUp() throws Exception {
    if (dataPath != null) {
      return;
    }

    // record ephemeral port number obtained by opening a socket
    // so that we may use it as the test webserver port
    var serverSocket = new ServerSocket(0);
    port = serverSocket.getLocalPort();
    serverSocket.close();

    var dataDir = Files.createTempDirectory("main-verticle-test");
    dataPath = dataDir.toString();

    // Initialize the test directory with all the sample recipes
    Indexer indexer = new Indexer.Builder().createMode().dataDirectory(dataDir).build();
    Util.getSampleRecipes()
        .forEach(
            recipe -> {
              try {
                indexer.addRecipe(recipe);
              } catch (Exception ignored) {
                // pass
              }
            });
    indexer.commit();
  }

  private DeploymentOptions getConfig() {
    assert (dataPath != null);
    return new DeploymentOptions()
        .setConfig(
            new JsonObject()
                .put(MainVerticle.CONFIG_SERVICE_SSL, false)
                .put(MainVerticle.CONFIG_SERVICE_PORT, port)
                .put(MainVerticle.CONFIG_SERIVCE_DATA_DIR, dataPath));
  }
}
