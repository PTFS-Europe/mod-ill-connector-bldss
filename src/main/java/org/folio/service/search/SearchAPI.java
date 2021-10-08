package org.folio.service.search;

import io.vertx.core.Context;
import org.folio.exception.ConnectorQueryException;
import org.folio.rest.jaxrs.model.*;
import org.folio.service.BaseService;
import org.folio.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.folio.config.Constants.BLDSS_TEST_API_URL;

public class SearchAPI extends BaseService implements SearchService {

  public static String baseUrl = BLDSS_TEST_API_URL + "/api/search/";

  @Override
  public CompletableFuture<SearchResponse> performSearch(Document xcqlDoc, int offset, int limit, Context context, Map<String, String> headers) {
    CompletableFuture<SearchResponse> future = new CompletableFuture<>();
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest preparedRequest = prepareRequest(xcqlDoc, baseUrl, offset, limit);
    client.sendAsync(preparedRequest, HttpResponse.BodyHandlers.ofString())
      .thenApply(apiResponse -> {
        SearchResponse response = prepareResponse(apiResponse.body());
        response.setOffset(offset);
        response.setLimit(limit);
        future.complete(response);
        return response;
      });
    return future;
  }

  @Override
  public HttpRequest prepareRequest(Document xcqlDoc, String url, int offset, int limit) {
    NodeList nodes = xcqlDoc.getElementsByTagName("searchClause");
    if (nodes.getLength() > 1) {
      throw(new ConnectorQueryException("Unexpected number of searchClause elements: " + nodes.getLength() + "(Expected 1)"));
    }
    Node firstNode = nodes.item(0);
    String indexName = "";
    String indexValue = "";
    if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
      Element el = (Element) firstNode;
      indexName = el.getElementsByTagName("index").item(0).getTextContent();
      indexValue = el.getElementsByTagName("term").item(0).getTextContent();
    }
    if (!indexName.equals("title")) {
      throw(new ConnectorQueryException("Unknown search index: " + indexName));
    }
    url += indexValue;

    ArrayList<String> params = new ArrayList<>();
    params.add("SearchRequest.fullDetails=true");
    if (offset > 0) {
      params.add("SearchRequest.start=" + offset);
    }
    if (limit > 0) {
      params.add("SearchRequest.maxResults=" + limit);
    }
    url += "?" + String.join("&", params);

    return HttpRequest.newBuilder()
      .uri(URI.create(url)).build();
  }

  @Override
  public SearchResponse prepareResponse(String response) {
    XMLUtil parser = new XMLUtil();
    Document doc = parser.parse(response);
    SearchResponse searchResponse = new SearchResponse();
    NodeList results = doc.getElementsByTagName("record");
    ArrayList<Result> returnResults = new ArrayList<>();
    for (int temp = 0; temp < results.getLength(); temp++) {
      Node node = results.item(temp);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Result result = new Result();
        Element element = (Element) node;

        result.setId(getDescendant(element, "uin", 0));
        result.setType(getDescendant(element, "type", 0));
        result.setAvailable(getDescendant(element, "isAvailableImmediateley", 0).equals("true"));

        Element metadata = (Element) element.getElementsByTagName("metadata").item(0);
        Element titleLevel = (Element) metadata.getElementsByTagName("titleLevel").item(0);
        Element itemLevel = (Element) metadata.getElementsByTagName("itemLevel").item(0);

        TitleLevel resultTitleLevel = new TitleLevel();
        resultTitleLevel.setTitle(getDescendant(titleLevel, "title", 0));
        resultTitleLevel.setAuthor(getDescendant(titleLevel, "author", 0));
        resultTitleLevel.setIdentifier(getDescendant(titleLevel, "identifier", 0));
        resultTitleLevel.setShelfmark(getDescendant(titleLevel, "shelfmark", 0));
        resultTitleLevel.setPublisher(getDescendant(titleLevel, "publisher", 0));

        ItemLevel resultItemLevel = new ItemLevel();
        resultItemLevel.setYear(getDescendant(itemLevel, "year", 0));
        resultItemLevel.setType(getDescendant(itemLevel, "type", 0));

        Metadata resultMetadata = new Metadata();
        resultMetadata.setTitleLevel(resultTitleLevel);
        resultMetadata.setItemLevel(resultItemLevel);

        result.setMetadata(resultMetadata);

        returnResults.add(result);
      }
    }
    searchResponse.setResults(returnResults);
    return searchResponse;
  }

  private String getDescendant(Node node, String target, Integer idx) {
    Element element = (Element) node;
    NodeList nodeList = element.getElementsByTagName(target);
    if (idx < 0 || nodeList.getLength() < idx + 1) {
      return null;
    }
    return nodeList.item(idx).getTextContent();
  }

}
