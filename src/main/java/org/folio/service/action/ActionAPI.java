package org.folio.service.action;

import io.vertx.core.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.ActionMetadata;
import org.folio.rest.jaxrs.model.ActionResponse;
import org.folio.rest.jaxrs.model.Header;
import org.folio.util.BLDSSRequest;
import org.folio.util.BLDSSResponse;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ActionAPI implements ActionService {

  protected final Logger logger = LogManager.getLogger(this.getClass());

  @Override
  public CompletableFuture<ActionResponse> performAction(String actionName, ActionMetadata payload, Context context, Map<String, String> headers) {
    String path =  "/orders";
    HashMap<String, String> params = new HashMap<>();
    CompletableFuture<ActionResponse> future = new CompletableFuture<>();
    BLDSSRequest req = new BLDSSRequest("POST", path, params);
    req.setPayload(payload);
    req.makeRequest().thenApply(respObj -> {
      String entityId = payload.getHeader().getRequestingAgencyRequestId();
      ActionResponse actionResponse = prepareResponse(respObj, entityId);
      future.complete(actionResponse);
      return actionResponse;
    });

    return future;
  }

  @Override
  public ActionResponse prepareResponse(BLDSSResponse response, String localRequestId) {
    ActionResponse actionResponse = new ActionResponse();
    String status = response.getStatus();
    // We have an error
    if (!status.equals("0")) {
      Header errorHeader = new Header()
        .withErrorData(Header.ErrorData.BADLY_FORMED_MESSAGE);
      actionResponse.setHeader(errorHeader);
    } else {
      Date now = new Date();
      Header header = new Header()
        .withTimestamp(now)
        .withTimestampReceived(now)
        .withMessageStatus(Header.MessageStatus.OK);
      actionResponse.setHeader(header);
    }

    return actionResponse;
  }

}
