package co.caio.cerberus.service;

import co.caio.cerberus.service.api.V1SearchHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;

public class MainVerticle extends AbstractVerticle {

  public static void main(String[] args) {
    new CustomLauncher().dispatch(new String[] {"run", MainVerticle.class.getCanonicalName()});
  }

  @Override
  public void start() {
    var certificate = SelfSignedCertificate.create();
    var options =
        new HttpServerOptions()
            .setHandle100ContinueAutomatically(true)
            .setTcpNoDelay(true)
            .setTcpFastOpen(true)
            .setSsl(true)
            .setUseAlpn(true)
            .setPemKeyCertOptions(certificate.keyCertOptions())
            .setTrustOptions(certificate.trustOptions());

    var router = Router.router(vertx);

    var healthChecks = HealthCheckHandler.create(vertx);
    healthChecks.register(
        "native-transport",
        fut -> {
          if (vertx.isNativeTransportEnabled()) {
            fut.complete();
          } else {
            fut.fail("native transport not enabled");
          }
        });

    router.route("/health*").handler(healthChecks);
    router.route("/api/v1/search").handler(new V1SearchHandler());

    vertx.createHttpServer(options).requestHandler(router::accept).listen(8080);
  }

  static class CustomLauncher extends Launcher implements VertxLifecycleHooks {

    @Override
    public void beforeStartingVertx(VertxOptions options) {
      options.setPreferNativeTransport(true);
    }
  }
}
