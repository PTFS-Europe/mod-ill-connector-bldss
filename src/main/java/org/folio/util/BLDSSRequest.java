package org.folio.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.folio.config.Constants.BLDSS_TEST_API_URL;

public class BLDSSRequest {

  public final String type;
  public final String httpMethod;
  public final String path;
  public final HashMap<String, String> parameters;
  public final Boolean needsAuth;
  public String reqPayload;
  public URI uri;

  public BLDSSRequest(String type, String httpMethod, String path, HashMap<String, String> parameters, Boolean needsAuth) {
    this.type = type;
    this.httpMethod = httpMethod;
    this.path = "/api" + path;
    this.parameters = parameters;
    this.needsAuth = needsAuth;
    this.uri = URI.create(BLDSS_TEST_API_URL + this.path);
  }

  public CompletableFuture<HttpResponse<String>> makeRequest() {
    CompletableFuture<HttpResponse<String>> future = new CompletableFuture<>();
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest.Builder builder = HttpRequest.newBuilder()
      .uri(this.uri)
      .header("Content-type", "application/xml");

    if (this.needsAuth) {
      BLDSSAuth auth = new BLDSSAuth(this.httpMethod, this.path, this.parameters, this.reqPayload);
      String authHeader = auth.getHeaderString();
      builder.header("BLDSS-API-Authentication", authHeader);
    }

    switch(this.httpMethod) {
      case "GET":
        builder.GET();
        break;
      case "POST":
        builder.POST(HttpRequest.BodyPublishers.ofString(this.reqPayload));
        break;
      case "PUT":
        builder.PUT(HttpRequest.BodyPublishers.ofString(this.reqPayload));
        break;
      case "DELETE":
        builder.DELETE();
        break;
    }

    HttpRequest request = builder.build();
    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
      .thenApply(apiResponse -> {
        future.complete(apiResponse);
        return apiResponse;
      });

    return future;
  }

  public String getReqType() {
    return this.type;
  }

  public void setReqPayload(String reqPayload) {
    this.reqPayload = reqPayload;
  }
}
