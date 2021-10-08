package org.folio.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.folio.config.Constants.BLDSS_TEST_API_URL;

public class BLDSSRequest {

  private String httpMethod;
  private String path;
  private String payload;
  private HashMap<String, String> parameters;

  public BLDSSRequest(String httpMethod, String path, HashMap<String, String> parameters, String payload) {
    this.httpMethod = httpMethod;
    this.path = "/api" + path;
    this.parameters = parameters;
    this.payload = payload;
  }

  public CompletableFuture<BLDSSResponse> makeRequest() {
    CompletableFuture<BLDSSResponse> future = new CompletableFuture<>();
    HttpClient client = HttpClient.newHttpClient();
    BLDSSAuth auth = new BLDSSAuth(this.httpMethod, this.path, this.parameters, this.payload);
    String authHeader = auth.getHeaderString();
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(BLDSS_TEST_API_URL + this.path))
      .POST(HttpRequest.BodyPublishers.ofString(this.payload))
      .header("BLDSS-API-Authentication", authHeader)
      .header("Content-type", "application/xml")
      .build();
    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(apiResponse -> {
          BLDSSResponse response = new BLDSSResponse(apiResponse.body());
          future.complete(response);
          return response;
        });
    return future;
  }

  public HashMap<String, String> getParameters() {
    return parameters;
  }

  public void setParameter(String key, String value) {
    this.parameters.put(key, value);
  }

}
