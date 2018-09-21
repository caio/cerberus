package co.caio.cerberus.service.api;

import io.vertx.ext.web.RoutingContext;

public class V1ErrorHandler extends V1BaseHandler {

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            writeFailure(V1SearchResponse.ErrorCode.UNKNOWN_ERROR, routingContext);
        } catch (Exception e) {
            routingContext.fail(e);
        }
    }
}
