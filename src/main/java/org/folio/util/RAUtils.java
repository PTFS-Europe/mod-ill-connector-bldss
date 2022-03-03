package org.folio.util;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.common.OkapiParams;
import org.folio.rest.jaxrs.model.ISO18626.SupplyingAgencyMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class RAUtils {

  private static final Logger logger = LogManager.getLogger("RAUtils");

  private static final String raApi = "/ill-ra";

  // Prevent instantiation
  public RAUtils() {}

  // Receive a message and headers, create request and return the
  // resulting CompletableFuture
  public static CompletableFuture<HttpResponse<String>> sendRequestToRa(
    HttpRequest.Builder request,
    Map<String, String> headers
  ) {
    HttpClient client = HttpClient.newBuilder()
      .build();

    // Add our existing headers
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      request.header(entry.getKey(), entry.getValue());
    }
    // Add additional missing headers
    request.header("Content-type", "application/json");
    request.header("Accept", "application/json");

    HttpRequest builtRequest = request.build();
    // Send the request, receive the response, convert it into a response object
    // then complete the future with it
    return client.sendAsync(builtRequest, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpRequest.Builder buildRequestForSa(
    Map<String, String> okapiHeaders,
    SupplyingAgencyMessage supplyingAgencyMessage
  ) {
    OkapiParams okapiParams = new OkapiParams(okapiHeaders);

    logger.info("BLDSS connector sending message:");
    logger.info(JsonObject.mapFrom(supplyingAgencyMessage).toString());

    return HttpRequest.newBuilder()
      .uri(URI.create(okapiParams.getUrl() + raApi + "/sa-update"))
      .POST(HttpRequest.BodyPublishers.ofString(JsonObject.mapFrom(supplyingAgencyMessage).toString()));
  }
}
