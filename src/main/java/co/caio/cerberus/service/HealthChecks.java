package co.caio.cerberus.service;

import io.vertx.core.Vertx;
import io.vertx.ext.healthchecks.HealthCheckHandler;

public class HealthChecks {
  public static HealthCheckHandler create(Vertx vertx) {
    var hc = HealthCheckHandler.create(vertx);

    hc.register(
        "native-transport",
        future -> {
          if (vertx.isNativeTransportEnabled()) {
            future.complete();
          } else {
            future.fail("native transport not enabled");
          }
        });

    return hc;
  }
}
