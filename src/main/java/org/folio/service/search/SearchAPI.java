package org.folio.service.search;

import io.vertx.core.Context;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.SearchResponse;
import org.folio.service.BaseService;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SearchAPI extends BaseService implements SearchService {

  @Override
  @Validate
  public CompletableFuture<SearchResponse> performSearch(String query, int offset, int limit, Context context, Map<String, String> headers) {
    CompletableFuture<SearchResponse> future = new CompletableFuture<>();
    future.complete(new SearchResponse());
    return future;
  }
}
