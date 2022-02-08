package org.folio.service.getter;

import org.folio.rest.jaxrs.model.GetterResponse;
import org.folio.service.BaseService;
import org.folio.util.*;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class ConnectorGetter extends BaseService implements GetterService {

  @Override
  public CompletableFuture<GetterResponse> getFromConnector(String toGet) {
    // Get the endpoint and auth requirements for the requested resource
    GetterEndpointMap endpointMap = new GetterEndpointMap();
    String endpoint = endpointMap.getEndpoint(toGet);
    Boolean needsAuth = endpointMap.getNeedsAuth(toGet);

    CompletableFuture<GetterResponse> future = new CompletableFuture<>();
    HashMap<String, String> params = new HashMap<>();
    BLDSSRequest req = new BLDSSRequest(
      "GET",
      "/" + endpoint,
      params
    );
    req.makeRequest(needsAuth).thenApply(respObj -> {
      String body = respObj.body();

      BLDSSResponse bldssResponse = new BLDSSResponse(body);

      GetterResponse response = new GetterResponse();
      response.setTimestamp(bldssResponse.getTimestamp());
      response.setStatus(bldssResponse.getStatus());
      response.setMessage(bldssResponse.getMessage());

      XMLUtil xmlUtil = new XMLUtil();
      String json = xmlUtil.getJson(body, "apiResponse", "apiResponse");
      response.setGetterResult(json);
      future.complete(response);
      return response;
    });
    return future;
  }

}
