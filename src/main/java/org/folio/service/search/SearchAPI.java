package org.folio.service.search;

import io.vertx.core.Context;
import org.folio.rest.jaxrs.model.SearchResponse;
import org.folio.service.BaseService;
import org.w3c.dom.Document;

import java.net.http.HttpRequest;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SearchAPI extends BaseService implements SearchService {

  @Override
  public CompletableFuture<SearchResponse> performSearch(Document xcqlDoc, int offset, int limit, Context context, Map<String, String> headers) {
    CompletableFuture<SearchResponse> future = new CompletableFuture<>();
    future.complete(new SearchResponse());
    return future;
  }

  @Override
  public HttpRequest prepareRequest(Document xcqlDoc, String url, int offset, int limit) {
    return null;
  }

  @Override
  public SearchResponse prepareResponse(String response) {
    return null;
  }
}
