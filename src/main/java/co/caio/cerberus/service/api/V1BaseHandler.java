package co.caio.cerberus.service.api;

import co.caio.cerberus.Environment;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

abstract class V1BaseHandler implements Handler<RoutingContext> {
    private ObjectMapper mapper = Environment.getObjectMapper();
    private static final String CONTENT_TYPE = "Content-type";
    private static final String APPLICATION_JSON = "application/json";

    SearchQuery readBody(RoutingContext routingContext) throws Exception {
        return mapper.readValue(routingContext.getBody().getBytes(), SearchQuery.class);
    }

    void writeSuccess(SearchResult sr, RoutingContext context) throws Exception {
        var response = context.response();
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON);
        response.end(mapper.writeValueAsString(V1SearchResponse.success(sr)));
    }

    void writeFailure(V1SearchResponse.ErrorCode code, RoutingContext context) throws Exception {
        var response = context.response();
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON);
        response.end(mapper.writeValueAsString(V1SearchResponse.failure(code, context.failure().getMessage())));
    }
}
