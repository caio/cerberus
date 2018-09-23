package co.caio.cerberus.service;

import co.caio.cerberus.service.api.V1SearchHandler;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

  static final String CONFIG_SERVICE_PORT = "cerberus.service.port";
  static final String CONFIG_SERIVCE_DATA_DIR = "cerberus.service.data_dir";
  static final String CONFIG_SERVICE_SSL = "cerberus.service.ssl";
  static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  public static void main(String[] args) {
    new CustomLauncher().dispatch(new String[] {"run", MainVerticle.class.getCanonicalName()});
  }

  private int portNumber;
  private String dataDirectory;
  private boolean useSsl;

  @Override
  public void start(Future<Void> startFuture) {
    readConfiguration().compose(v -> startWebServer()).setHandler(startFuture.completer());
  }

  private Future<Void> startWebServer() {
    return Future.future(
        future -> {
          var options =
              new HttpServerOptions()
                  .setHandle100ContinueAutomatically(true)
                  .setTcpNoDelay(true)
                  .setTcpFastOpen(true);

          if (useSsl) {
            var certificate = SelfSignedCertificate.create();
            options
                .setSsl(true)
                .setUseAlpn(true)
                .setPemKeyCertOptions(certificate.keyCertOptions())
                .setTrustOptions(certificate.trustOptions());
          }

          var router = getRouter();

          vertx
              .createHttpServer(options)
              .requestHandler(router::accept)
              .listen(
                  portNumber,
                  ar -> {
                    if (ar.succeeded()) {
                      logger.info(
                          "Web server started at {}://localhost:{}",
                          useSsl ? "https" : "http",
                          portNumber);
                      future.complete();
                    } else {
                      future.fail(future.cause());
                    }
                  });
        });
  }

  private Future<Void> readConfiguration() {

    var configRetriever = ConfigRetriever.create(vertx);

    return Future.future(
        fut ->
            configRetriever.getConfig(
                ar -> {
                  if (ar.succeeded()) {
                    var config = ar.result();
                    portNumber = config.getInteger(CONFIG_SERVICE_PORT, 0);
                    dataDirectory = config.getString(CONFIG_SERIVCE_DATA_DIR);
                    useSsl = config.getBoolean(CONFIG_SERVICE_SSL, false);

                    if (portNumber <= 0) {
                      fut.fail(
                          String.format(
                              "Bad configuration for %s: %d", CONFIG_SERVICE_PORT, portNumber));
                    } else if (dataDirectory == null) {
                      fut.fail(
                          String.format(
                              "Must configure %s with a valid path", CONFIG_SERIVCE_DATA_DIR));
                    } else {
                      fut.complete();
                    }
                  } else {
                    fut.fail(ar.cause());
                  }
                }));
  }

  private Router getRouter() {
    var router = Router.router(vertx);

    var healthChecks = HealthCheckHandler.create(vertx);
    healthChecks.register(
        "native-transport",
        future -> {
          if (vertx.isNativeTransportEnabled()) {
            future.complete();
          } else {
            future.fail("native transport not enabled");
          }
        });

    router.get("/health*").handler(healthChecks);
    router
        .post("/api/v1/search")
        .consumes("application/json")
        .handler(BodyHandler.create())
        .handler(new V1SearchHandler(Paths.get(dataDirectory)));

    return router;
  }

  static class CustomLauncher extends Launcher implements VertxLifecycleHooks {

    @Override
    public void beforeStartingVertx(VertxOptions options) {
      options.setPreferNativeTransport(true);
    }
  }
}
