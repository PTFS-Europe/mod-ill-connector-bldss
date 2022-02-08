package org.folio.util;

import java.util.HashMap;

class GetterEndpoint {
  private String endpoint;
  private Boolean needsAuth;

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public void setNeedsAuth(Boolean needsAuth) {
    this.needsAuth = needsAuth;
  }

  public GetterEndpoint(String endpoint, Boolean needsAuth) {
    this.endpoint = endpoint;
    this.needsAuth = needsAuth;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public Boolean getNeedsAuth() {
    return needsAuth;
  }
}

public class GetterEndpointMap {
  private final HashMap<String, GetterEndpoint> endpoints = new HashMap<>();

  public GetterEndpointMap() {
    endpoints.put(
      "prices",
      new GetterEndpoint(
        "prices",
        true
      )
    );

    endpoints.put(
      "services",
      new GetterEndpoint(
        "reference/services",
        false
      )
    );

    endpoints.put(
      "formats",
      new GetterEndpoint(
        "reference/formats",
        false
      )
    );

    endpoints.put(
      "speeds",
      new GetterEndpoint(
        "reference/speeds",
        false
      )
    );

    endpoints.put(
      "quality",
      new GetterEndpoint(
        "reference/quality",
        false
      )
    );

  }

  public String getEndpoint(String key) {
    return endpoints.get(key).getEndpoint();
  }

  public Boolean getNeedsAuth(String key) {
    return endpoints.get(key).getNeedsAuth();
  }
}
