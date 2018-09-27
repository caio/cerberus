package co.caio.cerberus.service;

import static co.caio.cerberus.service.ServiceConfiguration.ServiceConfigurationImpl.*;

import co.caio.cerberus.Util;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.net.ServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
abstract class MainVerticleTestCase {

  static WebClient client;

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
                    .put(CONFIG_HEALTH_NUM_DOCS, 100)
                    .put(CONFIG_SERVICE_SSL, false)
                    .put(CONFIG_SERVICE_PORT, portNumber)
                    .put(CONFIG_SERVICE_DATA_DIR, Util.getTestDataDir().toString()));

    vertx.deployVerticle(
        new MainVerticle(), config, testContext.succeeding(fut -> testContext.completeNow()));
  }

  @AfterAll
  static void tearDown(Vertx vertx, VertxTestContext testContext) {
    vertx.close(testContext.succeeding(fut -> testContext.completeNow()));
  }
}
