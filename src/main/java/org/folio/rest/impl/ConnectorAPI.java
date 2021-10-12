package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.exception.ConnectorQueryException;
import org.folio.rest.jaxrs.model.ActionPayload;
import org.folio.rest.jaxrs.model.ActionRequest;
import org.folio.rest.jaxrs.resource.IllConnector;
import org.folio.service.action.ActionService;
import org.folio.service.search.SearchService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.CQLUtil;
import org.folio.util.XMLUtil;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;

public class ConnectorAPI extends BaseApi implements IllConnector {

  @Autowired
  private SearchService illSearchService;
  @Autowired
  private ActionService illActionService;

  public ConnectorAPI() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getIllConnectorSearch(int offset, int limit, String query, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    // Parse the query into an XCQL document before passing it to performSearch
    if (query == null || query.length() == 0) {
      throw new ConnectorQueryException("Query not populated, syntax ?query=title=sleep");
    }
    CQLUtil parser = new CQLUtil();
    String xcql = parser.parseToXCQL(query);
    XMLUtil xmlParser = new XMLUtil();
    Document xcqlDoc = xmlParser.parse(xcql);
    illSearchService.performSearch(xcqlDoc, offset, limit, vertxContext, okapiHeaders)
      .thenAccept(results -> asyncResultHandler.handle(succeededFuture(buildOkResponse(results))))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  public void putIllConnectorAction(ActionRequest result, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String submissionId = result.getEntityId();
    String action = result.getActionName();
    ActionPayload payload = result.getActionPayload();
    JSONObject json = new JSONObject(payload.getAdditionalProperties());
    String xml = XML.toString(json);
    illActionService.performAction(action, submissionId, xml, vertxContext, okapiHeaders)
      .thenAccept(acceptResult -> asyncResultHandler.handle(succeededFuture(buildOkResponse(acceptResult))))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));

  }
}
