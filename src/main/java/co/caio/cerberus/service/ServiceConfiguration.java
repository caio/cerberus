package co.caio.cerberus.service;

import io.vertx.core.json.JsonObject;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ServiceConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(ServiceConfiguration.class);

  static final String CONFIG_HEALTH_NUM_DOCS = "cerberus.health.num_docs";
  static final String CONFIG_SERVICE_PORT = "cerberus.service.port";
  static final String CONFIG_SERVICE_DATA_DIR = "cerberus.service.data_dir";
  static final String CONFIG_SERVICE_SSL = "cerberus.service.ssl";

  final int portNumber;
  final int wantedNumDocs;

  final boolean useSsl;
  final String dataDirectory;

  ServiceConfiguration(JsonObject config) {
    portNumber = config.getInteger(CONFIG_SERVICE_PORT, 0);
    wantedNumDocs = config.getInteger(ServiceConfiguration.CONFIG_HEALTH_NUM_DOCS, -1);
    useSsl = config.getBoolean(CONFIG_SERVICE_SSL, false);
    dataDirectory = config.getString(CONFIG_SERVICE_DATA_DIR);

    if (portNumber <= 0 || portNumber > 65535) {
      throw new ConfigurationException(
          String.format("[%s] Invalid port number: %d", CONFIG_SERVICE_PORT, portNumber));
    }

    if (wantedNumDocs < 0) {
      logger.error(
          "Configuration {} must be >= 0. Health check effectively disabled",
          ServiceConfiguration.CONFIG_HEALTH_NUM_DOCS);
    }

    if (dataDirectory == null || !new File(dataDirectory).isDirectory()) {
      throw new ConfigurationException(
          String.format("[%s] Invalid configuration: not a directory", CONFIG_SERVICE_DATA_DIR));
    }
  }

  class ConfigurationException extends IllegalStateException {
    ConfigurationException(String message) {
      super(message);
    }
  }
}
