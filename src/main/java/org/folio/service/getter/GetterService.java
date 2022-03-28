package org.folio.service.getter;

import org.folio.rest.jaxrs.model.GetterResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface GetterService {
  CompletableFuture<GetterResponse> getFromConnector(String toGet, Map<String, String> headers);
}
