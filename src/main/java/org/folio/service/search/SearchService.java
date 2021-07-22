package org.folio.service.search;

import io.vertx.core.Context;
import org.folio.rest.jaxrs.model.SearchResponse;
import org.w3c.dom.Document;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface SearchService {

  /**
   * This method creates {@link SearchResponse}
   *
   * @param search A CQL string representing the search terms
   * @param offset  offset
   * @param limit   limit
   * @param context Vert.X context
   * @param headers OKAPI headers
   * @return collection of search results{@link SearchResponse}
   */
  CompletableFuture<SearchResponse> performSearch(Document search, int offset, int limit, Context context, Map<String, String> headers);
}
