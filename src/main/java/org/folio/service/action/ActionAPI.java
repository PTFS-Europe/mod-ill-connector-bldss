package org.folio.service.action;

import io.vertx.core.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.ActionResponse;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ActionAPI implements ActionService {

  protected final Logger logger = LogManager.getLogger(this.getClass());

  @Override
  public CompletableFuture<ActionResponse> performAction(String actionName, String entityId, String payload, Context context, Map<String, String> headers) {
    CompletableFuture<String> future = new CompletableFuture<>();

    try {
      logger.info("Performing action {}", actionName);
      HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI("https://www.google.com"))
        .GET()
        .build();
    } catch (Exception e) {

    }
    return null;
  }
}
