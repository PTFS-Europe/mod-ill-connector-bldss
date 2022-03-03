package org.folio.service.action;

import io.vertx.core.Context;
import org.folio.rest.jaxrs.model.ActionResponse;
import org.folio.util.BLDSSActionResponse;
import org.folio.util.BLDSSRequest;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ActionService {
  /**
   * This method creates {@link BLDSSActionResponse}
   *
   * @param actionName Name of the action to be performed
   * @param payload A string representing arbitrary metadata
   * @param context Vert.X context
   * @param headers OKAPI headers
   * @return response {@link BLDSSActionResponse}
   */
  CompletableFuture<BLDSSActionResponse> performAction(String actionName, String payload, Context context, Map<String, String> headers);

  /**
   * This method receives an HttpResponse containing the supplier's API response
   * and transforms it into an ActionResponse object
   *
   * @param response The supplier's response body
   * @return An ActionResponse instance
   */
  ActionResponse prepareResponse(HttpResponse<String> response, BLDSSRequest request);
}
