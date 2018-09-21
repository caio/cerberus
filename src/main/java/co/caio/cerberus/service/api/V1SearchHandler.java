package co.caio.cerberus.service.api;

import co.caio.cerberus.search.Searcher;
import io.vertx.ext.web.RoutingContext;

import java.nio.file.Paths;

public class V1SearchHandler extends V1BaseHandler {
    // FIXME configuration
    Searcher searcher = new Searcher.Builder().dataDirectory(Paths.get("/tmp/hue")).build();

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            var searchQuery = readBody(routingContext);
            writeSuccess(searcher.search(searchQuery, 10), routingContext);
        } catch (Exception e) {
            routingContext.fail(e);
        }
    }
}
