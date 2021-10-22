package org.folio.service.action;

import io.vertx.core.Context;
import org.folio.rest.jaxrs.model.ActionMetadata;
import org.folio.rest.jaxrs.model.ActionResponse;
import org.folio.util.BLDSSRequest;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ActionService {
  /**
   * This method creates {@link ActionResponse}
   *
   * @param actionName Name of the action to be performed
   * @param payload An ActionMetadata object representing the ISO18626 metadata
   * @param context Vert.X context
   * @param headers OKAPI headers
   * @return response {@link ActionResponse}
   */
  CompletableFuture<ActionResponse> performAction(String actionName, ActionMetadata payload, Context context, Map<String, String> headers);

  /**
   * This method receives an HttpResponse containing the supplier's API response
   * and transforms it into an ActionResponse object
   *
   * @param response The supplier's response body
   * @return An ActionResponse instance
   */
  ActionResponse prepareResponse(HttpResponse<String> response, BLDSSRequest request);
}
