package co.caio.cerberus.service;

import co.caio.cerberus.Environment;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
import co.caio.cerberus.search.Searcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.nio.file.Paths;

public class MainVerticle extends AbstractVerticle {

    public static void main(String[] args) {
        new CustomLauncher().dispatch(new String[]{"run", MainVerticle.class.getCanonicalName()});
    }

    @Override
    public void start() {
        var certificate = SelfSignedCertificate.create();
        var options = new HttpServerOptions()
                .setHandle100ContinueAutomatically(true)
                .setSsl(true)
                .setUseAlpn(true)
                .setPemKeyCertOptions(certificate.keyCertOptions())
                .setTrustOptions(certificate.trustOptions());

        // TODO make this assertion a healthcheck maybe?
        //assert vertx.isNativeTransportEnabled();
        //options.setTcpNoDelay(true);
        //options.setTcpFastOpen(true);

        var router = Router.router(vertx);
        var v1route = router.post("/api/v1/search");

        v1route.handler(BodyHandler.create());
        v1route.failureHandler(new V1APIErrorHandler());
        v1route.consumes("application/json").handler(new V1APIHandler());

        vertx.createHttpServer(options).requestHandler(router::accept).listen(8080);
    }

    abstract class BaseV1APIHandler implements Handler<RoutingContext> {
        ObjectMapper mapper = Environment.getObjectMapper();
        private static final String CONTENT_TYPE = "Content-type";
        private static final String APPLICATION_JSON = "application/json";

        SearchQuery readBody(RoutingContext routingContext) throws Exception {
            return mapper.readValue(routingContext.getBody().getBytes(), SearchQuery.class);
        }

        void writeSuccess(SearchResult sr, RoutingContext context) throws Exception {
            var response = context.response();
            response.putHeader(CONTENT_TYPE, APPLICATION_JSON);
            response.end(mapper.writeValueAsString(SearchResponse.success(sr)));
        }

        void writeFailure(SearchResponse.ErrorCode code, RoutingContext context) throws Exception {
            var response = context.response();
            response.putHeader(CONTENT_TYPE, APPLICATION_JSON);
            response.end(mapper.writeValueAsString(SearchResponse.failure(code, context.failure().getMessage())));
        }
    }

    class V1APIHandler extends BaseV1APIHandler {
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

    class V1APIErrorHandler extends BaseV1APIHandler {

        @Override
        public void handle(RoutingContext routingContext) {
            try {
                writeFailure(SearchResponse.ErrorCode.UNKNOWN_ERROR, routingContext);
            } catch (Exception e) {
                routingContext.fail(e);
            }
        }
    }

    static class CustomLauncher extends Launcher implements VertxLifecycleHooks {

        @Override
        public void beforeStartingVertx(VertxOptions options) {
            options.setPreferNativeTransport(true);
        }
    }
}
