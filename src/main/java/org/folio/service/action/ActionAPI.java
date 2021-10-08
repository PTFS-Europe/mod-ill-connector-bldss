package org.folio.service.action;

import io.vertx.core.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.ActionResponse;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Success;
import org.folio.util.BLDSSRequest;
import org.folio.util.BLDSSResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ActionAPI implements ActionService {

  protected final Logger logger = LogManager.getLogger(this.getClass());

  @Override
  public CompletableFuture<ActionResponse> performAction(String actionName, String entityId, String payload, Context context, Map<String, String> headers) {
    String path =  "/orders";
    HashMap<String, String> params = new HashMap<>();
    CompletableFuture<ActionResponse> future = new CompletableFuture<>();
    BLDSSRequest req = new BLDSSRequest("POST", path, params, payload );
    req.makeRequest().thenApply(respObj -> {
      ActionResponse actionResponse = prepareResponse(respObj);
      future.complete(actionResponse);
      return actionResponse;
    });

    return future;
  }

  @Override
  public ActionResponse prepareResponse(BLDSSResponse response) {
    ActionResponse actionResponse = new ActionResponse();

    String status = response.getStatus();
    // We have an error
    if (!status.equals("0")) {
      Error error = new Error()
        .withCode(status)
        .withMessage(response.getMessage());
      actionResponse.setError(error);
    } else {
      Success success = new Success()
        .withCode(status)
        .withMessage(response.getMessage())
        .withResult(response.getResult());
      actionResponse.setSuccess(success);
    }

    return actionResponse;
  }
}
