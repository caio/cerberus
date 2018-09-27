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
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
  private ServiceConfiguration configuration;

  public static void main(String[] args) {
    new CustomLauncher().dispatch(new String[] {"run", MainVerticle.class.getCanonicalName()});
  }

  @Override
  public void start(Future<Void> startFuture) {
    readConfiguration().compose(this::startWebServer).setHandler(startFuture.completer());
  }

  @Override
  public void stop() {
    logger.info("Service stopped");
  }

  private Future<Void> startWebServer(ServiceConfiguration config) {
    return Future.future(
        future -> {
          var options =
              new HttpServerOptions()
                  .setHandle100ContinueAutomatically(true)
                  .setTcpNoDelay(true)
                  .setTcpFastOpen(true);

          if (config.useSsl) {
            var certificate = SelfSignedCertificate.create();
            options
                .setSsl(true)
                .setUseAlpn(true)
                .setPemKeyCertOptions(certificate.keyCertOptions())
                .setTrustOptions(certificate.trustOptions());
          }

          var router = getRouter(config);

          vertx
              .createHttpServer(options)
              .requestHandler(router::accept)
              .listen(
                  config.portNumber,
                  ar -> {
                    if (ar.succeeded()) {
                      logger.info(
                          "Service started at {}://localhost:{}",
                          config.useSsl ? "https" : "http",
                          config.portNumber);
                      future.complete();
                    } else {
                      future.fail(future.cause());
                    }
                  });
        });
  }

  private Future<ServiceConfiguration> readConfiguration() {

    var configRetriever = ConfigRetriever.create(vertx);

    return Future.future(
        fut ->
            configRetriever.getConfig(
                ar -> {
                  if (ar.succeeded()) {
                    configuration = new ServiceConfiguration(ar.result());
                    fut.complete(configuration);
                  } else {
                    fut.fail(ar.cause());
                  }
                }));
  }

  private Router getRouter(ServiceConfiguration config) {
    var router = Router.router(vertx);
    var v1handler = new V1SearchHandler(Paths.get(config.dataDirectory));

    router
        .post("/api/v1/search")
        .consumes("application/json")
        .handler(BodyHandler.create())
        .handler(v1handler);

    router.get("/health*").handler(HealthChecks.create(vertx, v1handler, configuration));

    return router;
  }

  static class CustomLauncher extends Launcher implements VertxLifecycleHooks {

    @Override
    public void beforeStartingVertx(VertxOptions options) {
      options.setPreferNativeTransport(true);
    }
  }
}
