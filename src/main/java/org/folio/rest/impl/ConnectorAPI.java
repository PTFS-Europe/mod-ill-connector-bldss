package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.rest.jaxrs.model.ActionRequest;
import org.folio.rest.jaxrs.resource.IllConnector;
import org.folio.service.search.SearchService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.CQLUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;

public class ConnectorAPI extends BaseApi implements IllConnector {

  @Autowired
  private SearchService illSearchService;

  public ConnectorAPI() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getIllConnectorSearch(int offset, int limit, String query, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    // Parse the query into XCQL before passing it to performSearch
    CQLUtil parser = new CQLUtil();
    String xcql = parser.parseToXCQL(query);
    illSearchService.performSearch(xcql, offset, limit, vertxContext, okapiHeaders)
      .thenAccept(results -> asyncResultHandler.handle(succeededFuture(buildOkResponse(results))))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  public void putIllConnectorAction(ActionRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }
}
