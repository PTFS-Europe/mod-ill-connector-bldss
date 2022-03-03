package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.folio.exception.ConnectorQueryException;
import org.folio.rest.jaxrs.model.ActionRequest;
import org.folio.rest.jaxrs.model.ActionResponse;
import org.folio.rest.jaxrs.model.ISO18626.SupplyingAgencyMessage;
import org.folio.rest.jaxrs.resource.IllConnector;
import org.folio.service.action.ActionService;
import org.folio.service.getter.GetterService;
import org.folio.service.search.SearchService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.config.Constants.CONNECTOR_ABILITIES;
import static org.folio.config.Constants.CONNECTOR_NAME;
import static org.folio.config.Constants.CONNECTOR_UID;

public class ConnectorAPI extends BaseApi implements IllConnector {

  @Autowired
  private SearchService illSearchService;
  @Autowired
  private ActionService illActionService;
  @Autowired
  private GetterService illGetterService;

  public ConnectorAPI() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  // Receive a string representing the resource we want to get from the supplier,
  // then get it
  public void getIllConnectorGetterByToGet(String toGet, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    illGetterService.getFromConnector(toGet)
      .thenAccept(results -> asyncResultHandler.handle(succeededFuture(buildOkResponse(results))))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
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
  public void getIllConnectorInfo(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    HashMap<String, Object> response = new HashMap<>();
    response.put("uid", CONNECTOR_UID);
    response.put("name", CONNECTOR_NAME);
    response.put("abilities", CONNECTOR_ABILITIES);
    asyncResultHandler.handle(succeededFuture(buildOkResponse(response)));
  }

  @Override
  public void postIllConnectorAction(ActionRequest request, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String action = request.getActionName();
    String payload = request.getActionMetadata();

    // Determine what to do based on the action name
    if (action.equals("submitRequest")) {
      // - Submit the request
      // - Return a confirmation
      // - Send a SupplyingAgency Message to the RA containing the BL response
      illActionService.performAction(action, payload, vertxContext, okapiHeaders)
        .thenAccept(acceptResult -> {
          String responseString = acceptResult.getActionResponseString();
          ActionResponse actionResponse = acceptResult.getActionResponse();
          BLDSSRequest bldssRequest = acceptResult.getBldssRequest();
          // Send the response
          asyncResultHandler.handle(succeededFuture(buildOkResponse(actionResponse)));
          // Construct and send a SupplyingAgency Message
          SupplyingAgency supplyingAgency = new SupplyingAgency();
          SupplyingAgencyMessage supplyingAgencyMessage = supplyingAgency.buildMessageFromBLResponse(
            responseString,
            bldssRequest
          );

          // Only proceed if we have a message to send
          if (supplyingAgencyMessage != null) {
            HttpRequest.Builder raRequest = RAUtils.buildRequestForSa(
              okapiHeaders,
              supplyingAgencyMessage
            );
            RAUtils.sendRequestToRa(raRequest, okapiHeaders);
          }

        })
        .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
    }
  }

  @Override
  public void postIllConnector6839f2bf5c47469cA80b29765eaa9417SaUpdate(String entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    /* Pass the message forward to the main API:
       - Receiving a BLDSS XML payload from the BL containing an orderline update
       - Convert it to an JSON ISO18626 Supplying Agency Message
       - Send the message to the main API
       - Receive a JSON ISO18626 Supplying Agency Message Confirmation
       - Convert it to a BLDSS XML orderline update response
       - Return to the BL
    */
    SupplyingAgencyMessage sam = new SupplyingAgency().buildMessageFromOrderlineUpdate(entity);

    HttpRequest.Builder request = RAUtils.buildRequestForSa(
      okapiHeaders,
      sam
    );

    CompletableFuture<HttpResponse<String>> future = RAUtils.sendRequestToRa(
      request,
      okapiHeaders
    );

    // Receive the response from the main API, translate it into BLDSS and return it
    future.thenApply(apiResponse -> {
      JsonObject responseJson =  new JsonObject(apiResponse.body());
      String confirmationToSend = new SupplyingAgency().buildConfirmation(responseJson);
      asyncResultHandler.handle(succeededFuture(buildOkResponse(confirmationToSend)));
      return null;
    });
  }
}
