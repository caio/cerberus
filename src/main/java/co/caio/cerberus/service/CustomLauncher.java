package co.caio.cerberus.service;

import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;

public class CustomLauncher extends Launcher implements VertxLifecycleHooks {

  public static void main(String[] args) {
    new CustomLauncher().dispatch(args);
  }

  @Override
  public void beforeStartingVertx(VertxOptions options) {
    options.setPreferNativeTransport(true);
    options.setMetricsOptions(new DropwizardMetricsOptions().setEnabled(true));
  }
}
