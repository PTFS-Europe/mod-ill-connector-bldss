package org.folio.service.getter;

import org.folio.rest.jaxrs.model.GetterResponse;
import org.folio.service.BaseService;
import org.folio.util.BLDSSGetterRequest;
import org.folio.util.BLDSSResponse;
import org.folio.util.GetterEndpointMap;
import org.folio.util.XMLUtil;

import java.util.concurrent.CompletableFuture;

public class ConnectorGetter extends BaseService implements GetterService {

  @Override
  public CompletableFuture<GetterResponse> getFromConnector(String toGet) {
    // Get the endpoint and auth requirements for the requested resource
    GetterEndpointMap endpointMap = new GetterEndpointMap();
    String endpoint = endpointMap.getEndpoint(toGet);
    Boolean needsAuth = endpointMap.getNeedsAuth(toGet);

    CompletableFuture<GetterResponse> future = new CompletableFuture<>();
    BLDSSGetterRequest req = new BLDSSGetterRequest(
      endpoint,
      needsAuth
    );
    req.makeRequest().thenApply(respObj -> {
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
