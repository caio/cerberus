package co.caio.cerberus.service;

import static co.caio.cerberus.service.ServiceConfiguration.ServiceConfigurationImpl.*;
import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

class ServiceConfigurationTest {
  @Test
  void validations() {
    assertThrows(
        ServiceConfiguration.ConfigurationException.class,
        () -> {
          ServiceConfiguration.create(new JsonObject());
        });
    assertThrows(
        ServiceConfiguration.ConfigurationException.class,
        () -> {
          ServiceConfiguration.create(new JsonObject().put(CONFIG_SERVICE_PORT, -1));
        });
    assertThrows(
        ServiceConfiguration.ConfigurationException.class,
        () -> {
          ServiceConfiguration.create(new JsonObject().put(CONFIG_SERVICE_PORT, 70000));
        });
    assertThrows(
        ServiceConfiguration.ConfigurationException.class,
        () -> {
          ServiceConfiguration.create(
              new JsonObject().put(CONFIG_SERVICE_DATA_DIR, "/path/doesnt/exist"));
        });
    assertThrows(
        ServiceConfiguration.ConfigurationException.class,
        () -> {
          ServiceConfiguration.create(new JsonObject().put(CONFIG_SERVICE_DATA_DIR, "/etc/passwd"));
        });
    assertDoesNotThrow(
        () -> {
          ServiceConfiguration.create(
              new JsonObject()
                  .put(CONFIG_SERVICE_PORT, 80)
                  .put(CONFIG_SERVICE_DATA_DIR, "/tmp/")
                  .put(CONFIG_SERVICE_SSL, true)
                  .put(CONFIG_HEALTH_NUM_DOCS, 1));
        });
  }
}
