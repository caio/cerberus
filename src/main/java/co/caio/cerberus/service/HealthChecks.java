package co.caio.cerberus.service;

import co.caio.cerberus.service.api.V1SearchHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthChecks {

  public static final String CONFIG_HEALTH_NUM_DOCS = "cerberus.health.num_docs";
  private static final Logger logger = LoggerFactory.getLogger(HealthChecks.class);

  public static HealthCheckHandler create(
      Vertx vertx, V1SearchHandler v1handler, JsonObject configuration) {
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

    var wantedNumDocs = configuration.getInteger(CONFIG_HEALTH_NUM_DOCS, -1);

    if (wantedNumDocs < 0) {
      logger.error(
          "Configuration {} must be >= 0. Health check effectively disabled",
          CONFIG_HEALTH_NUM_DOCS);
    }

    hc.register(
        "num-docs",
        future -> {
          var current = v1handler.numDocs();

          if (v1handler.numDocs() > wantedNumDocs) {
            future.complete();
          } else {
            future.fail(
                String.format(
                    "Too few documents in index. Got %d, wanted > %d", current, wantedNumDocs));
          }
        });

    return hc;
  }
}
