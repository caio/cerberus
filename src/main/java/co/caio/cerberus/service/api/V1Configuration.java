package co.caio.cerberus.service.api;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class V1Configuration {
    public static Router getRouter(Vertx vertx) {
        var router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.route().failureHandler(new V1ErrorHandler());
        router.route("/search").consumes("application/json").handler(new V1SearchHandler());

        return router;
    }
}
