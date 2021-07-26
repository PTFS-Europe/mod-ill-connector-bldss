package org.folio.service.search;

import io.vertx.core.Context;
import org.folio.rest.jaxrs.model.SearchResponse;
import org.w3c.dom.Document;

import java.net.http.HttpRequest;
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

    /**
    * This method accepts an XCQL org.w3c.dom.Document and returns a search string ready
    * to be passed to the supplier's search API
    *
    * @param search An XCQL document representing the search node tree
    * @param baseUrl A String representing the supplier's API base URL
    * @param offset offset
    * @param limit limit
    * @return An HttpRequest instance that is ready to be used by the client
    */
    HttpRequest prepareRequest(Document search, String baseUrl, int offset, int limit);

    /**
    * This method receives an HttpResponse containing the supplier's API response
    * and transforms it into a SearchResponse object
    *
    * @param response The supplier's response body
    * @return A SearchResponse instance
    */
    SearchResponse prepareResponse(String response);

}
