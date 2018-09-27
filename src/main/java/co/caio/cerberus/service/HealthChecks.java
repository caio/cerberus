package co.caio.cerberus.service;

import co.caio.cerberus.service.api.V1SearchHandler;
import io.vertx.core.Vertx;
import io.vertx.ext.healthchecks.HealthCheckHandler;

public class HealthChecks {

  public static HealthCheckHandler create(
      Vertx vertx, V1SearchHandler v1handler, ServiceConfiguration configuration) {
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

    hc.register(
        "num-docs",
        future -> {
          var current = v1handler.numDocs();

          if (v1handler.numDocs() > configuration.wantedNumDocs()) {
            future.complete();
          } else {
            future.fail(
                String.format(
                    "Too few documents in index. Got %d, wanted > %d",
                    current, configuration.wantedNumDocs()));
          }
        });

    return hc;
  }
}
