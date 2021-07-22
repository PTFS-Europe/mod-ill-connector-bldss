package org.folio.service.action;

import io.vertx.core.Context;
import org.folio.rest.jaxrs.model.ActionResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ActionService {
  /**
   * This method creates {@link ActionResponse}
   *
   * @param actionName Name of the action to be performed
   * @param entityId The ID of the entity being actioned upon
   * @param payload An arbitrary serialised payload relating to the entity
   * @param context Vert.X context
   * @param headers OKAPI headers
   * @return response {@link ActionResponse}
   */
  CompletableFuture<ActionResponse> performAction(String actionName, String entityId, String payload, Context context, Map<String, String> headers);
}
