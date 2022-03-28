package org.folio.util;

import org.folio.rest.jaxrs.model.Config;
import org.folio.service.configuration.ConfigurationService;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
  }

  public CompletableFuture<HttpResponse<String>> makeRequest(Map<String, String> headers) {
    CompletableFuture<HttpResponse<String>> future = new CompletableFuture<>();
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest.Builder builder = HttpRequest.newBuilder()
      .header("Content-type", "application/xml");

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
    ConfigurationService configurationService = new ConfigurationService();
    Config config = configurationService.getConfigurationEntry("apiSettings", headers, "UI-PLUGIN-ILL-CONNECTOR-BLDSS");
    JSONObject conf = new JSONObject(config.getValue());

    if (this.needsAuth) {
      BLDSSAuth auth = new BLDSSAuth(this.httpMethod, this.path, this.parameters, this.reqPayload, conf);
      String authHeader = auth.getHeaderString();
      builder.header("BLDSS-API-Authentication", authHeader);
    }

    builder.uri(URI.create(conf.getString("apiUrl") + this.path));
    HttpRequest request = builder.build();
    return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
      .thenApply(apiResponse -> {
        future.complete(apiResponse);
        return apiResponse;
      });
  }

  public String getReqType() {
    return this.type;
  }

  public void setReqPayload(String reqPayload) {
    this.reqPayload = reqPayload;
  }
}
