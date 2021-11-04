package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.folio.exception.ConnectorQueryException;
import org.folio.rest.jaxrs.model.ActionMetadata;
import org.folio.rest.jaxrs.model.ActionRequest;
import org.folio.rest.jaxrs.model.SupplyingAgencyMessage;
import org.folio.rest.jaxrs.resource.IllConnector;
import org.folio.service.action.ActionService;
import org.folio.service.search.SearchService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.CQLUtil;
import org.folio.util.SupplyingAgency;
import org.folio.util.XMLUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.vertx.core.Future.succeededFuture;

public class ConnectorAPI extends BaseApi implements IllConnector {

  @Autowired
  private SearchService illSearchService;
  @Autowired
  private ActionService illActionService;

  // TODO: Remove me, I am just hardcoding the RA port,
  // ultimately this will just be on OKAPI and we'll target by URL
  private static final String raApi = "http://localhost:6666/ill-ra";

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
  public void postIllConnectorAction(ActionRequest request, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String action = request.getActionName();
    ActionMetadata payload = request.getActionMetadata();

    // Determine what to do based on the action name
    if (action.equals("submitRequest")) {
      illActionService.performAction(action, payload, vertxContext, okapiHeaders)
        .thenAccept(acceptResult -> asyncResultHandler.handle(succeededFuture(buildOkResponse(acceptResult))))
        .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
    }
  }

  @Override
  public void postIllConnectorSaUpdate(String entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    /* Pass the message forward to the main API:
       - Receiving a BLDSS XML payload from the BL containing an orderline update
       - Convert it to an JSON ISO18626 Supplying Agency Message
       - Send the message to the main API
       - Receive a JSON ISO18626 Supplying Agency Message Confirmation
       - Convert it to a BLDSS XML orderline update response
       - Return to the BL
    */
    SupplyingAgencyMessage sam = new SupplyingAgency().buildMessage(entity);

    // TODO: Remove me, I am just here to allow the connection to main API
    // to be made on the non-OKAPI port during dev
    okapiHeaders.remove("x-okapi-url");
    HttpClient client = HttpClient.newBuilder()
      .build();

    HttpRequest.Builder request = HttpRequest.newBuilder()
      .uri(URI.create(raApi + "/sa-update"))
      .POST(HttpRequest.BodyPublishers.ofString(JsonObject.mapFrom(sam).toString()));
    // Add our existing headers
    for (Map.Entry<String, String> entry : okapiHeaders.entrySet()) {
      request.header(entry.getKey(), entry.getValue());
    }
    // Add additional missing headers
    request.header("Content-type", "application/json");
    request.header("Accept", "application/json");

    HttpRequest builtRequest = request.build();
    // Send the request, receive the response, convert it into a response object
    // then complete the future with it
    CompletableFuture<HttpResponse<String>> future = client.sendAsync(builtRequest, HttpResponse.BodyHandlers.ofString());
    // Receive the response from the main API, translate it into BLDSS and return it
    future.thenApply(apiResponse -> {
      JsonObject responseJson =  new JsonObject(apiResponse.body());
      String confirmationToSend = new SupplyingAgency().buildConfirmation(responseJson);
      asyncResultHandler.handle(succeededFuture(buildOkResponse(confirmationToSend)));
      return null;
    });
  }

}
