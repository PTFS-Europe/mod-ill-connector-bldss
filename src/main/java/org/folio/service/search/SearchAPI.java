package org.folio.service.search;

import io.vertx.core.Context;
import org.folio.rest.jaxrs.model.*;
import org.folio.service.BaseService;
import org.folio.util.ISO18626Util;
import org.folio.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.folio.config.Constants.BLDSS_TEST_API_URL;

class IndexMapper {
  String issn;
  String isbn;
  String title;
  String author;
  String type;
  String general;

  public IndexMapper() {
    this.issn = null;
    this.isbn = null;
    this.title = null;
    this.author = null;
    this.type = null;
    this.general = null;
  }

  private String prepParam(String paramName, String value) {
    String encoded = null;
    try {
      encoded = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return "SearchRequest.Advanced." + paramName + "=" + encoded;
  }

  public ArrayList<String> escape() {
    ArrayList<String> params = new ArrayList<>();

    if (this.issn != null) {
      params.add(prepParam("issn", this.issn));
    }

    if (this.isbn != null) {
      params.add(prepParam("isbn", this.isbn));
    }

    if (this.title != null) {
      params.add(prepParam("title", this.title));
    }

    if (this.author != null) {
      params.add(prepParam("author", this.author));
    }

    if (this.type != null) {
      params.add(prepParam("type", this.type));
    }

    if (this.general != null) {
      params.add(prepParam("general", this.general));
    }

    return params;
  }

  public void parse(NodeList nodes) {
    int length = nodes.getLength();
    for (int i = 0; i < length; i++) {
      if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
        Element el = (Element) nodes.item(i);
        String indexName = el.getElementsByTagName("index").item(0).getTextContent();
        switch (indexName) {
          case "Title":
          case "TitleOfComponent":
            this.getTitle(indexName, el);
            break;
          case "Author":
          case "AuthorOfComponent":
            this.getAuthor(indexName, el);
            break;
          case "PublicationType":
            this.getType(indexName, el);
            break;
          case "ISBN":
            this.getIsbn(indexName, el);
            break;
          case "ISSN":
            this.getIssn(indexName, el);
            break;
          case "Volume":
            this.getGeneral(indexName, el);
            break;
        }
      }
    }
  }

  private void getGeneral(String indexName, Element el) {
    this.general = el.getElementsByTagName("term").item(0).getTextContent();
  }

  private void getIssn(String indexName, Element el) {
    this.issn = el.getElementsByTagName("term").item(0).getTextContent();
  }

  private void getIsbn(String indexName, Element el) {
    this.isbn = el.getElementsByTagName("term").item(0).getTextContent();
  }

  private void getType(String indexName, Element el) {
    this.type = el.getElementsByTagName("term").item(0).getTextContent().toLowerCase();
  }

  private void getAuthor(String indexName, Element el) {
    // We can only send one author, so
    // AuthorOfComponent is prioritised over
    // Author
    if (indexName.equals("AuthorOfComponent") || (this.author == null && indexName.equals("Author"))) {
      this.author = el.getElementsByTagName("term").item(0).getTextContent();
    }
  }

  private void getTitle(String indexName, Element el) {
    // We can only send one title, so
    // TitleOfComponent is prioritised over
    // Title
    if (indexName.equals("TitleOfComponent") || (this.title == null && indexName.equals("Title"))) {
      this.title = el.getElementsByTagName("term").item(0).getTextContent();
    }
  }
}

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

    IndexMapper indexMapper = new IndexMapper();
    indexMapper.parse(nodes);
    ArrayList<String> params = indexMapper.escape();

    params.add("SearchRequest.fullDetails=true");
    if (offset > 0) {
      params.add("SearchRequest.start=" + offset);
    }
    if (limit > 0) {
      params.add("SearchRequest.maxResults=" + limit);
    }
    url += "?" + String.join("&", params);
    return HttpRequest.newBuilder(URI.create(url)).build();
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
        Element metadata = (Element) element.getElementsByTagName("metadata").item(0);
        Element titleLevel = (Element) metadata.getElementsByTagName("titleLevel").item(0);
        Element itemLevel = (Element) metadata.getElementsByTagName("itemLevel").item(0);
        Element itemOfInterestLevel = (Element) metadata.getElementsByTagName("itemOfInterestLevel").item(0);

        BibliographicInfo bibinfo = new BibliographicInfo();
        bibinfo.setSupplierUniqueRecordId(getDescendant(element, "uin", 0));

        ArrayList<Element> titles = new ArrayList<>();
        titles.add(itemOfInterestLevel);
        titles.add(titleLevel);
        bibinfo.setTitle(extractMetadata(titles, "title"));

        ArrayList<Element> authors = new ArrayList<>();
        authors.add(itemOfInterestLevel);
        authors.add(titleLevel);
        bibinfo.setAuthor(extractMetadata(authors, "author"));

        bibinfo.setSeriesTitle(getDescendant(itemLevel, "title", 0));
        bibinfo.setEdition(getDescendant(itemLevel, "edition", 0));
        bibinfo.setTitleOfComponent(getDescendant(itemOfInterestLevel, "title", 0));
        bibinfo.setAuthorOfComponent(getDescendant(itemOfInterestLevel, "author", 0));
        bibinfo.setVolume(getDescendant(itemLevel, "volume", 0));
        bibinfo.setIssue(getDescendant(itemLevel, "issue", 0));

        ArrayList<BibliographicItemId> identifiers = getIdentifiers(titleLevel);
        if (identifiers.size() > 0) {
          bibinfo.setBibliographicItemId(identifiers);
        }

        PublicationInfo pubInfo = new PublicationInfo();
        pubInfo.setPublisher(getDescendant(titleLevel, "publisher", 0));
        ISO18626Util isoUtil = new ISO18626Util();
        String blType = getDescendant(element, "type", 0);
        String isoType = isoUtil.bldssToIsoType(blType);
        pubInfo.setPublicationType(PublicationInfo.PublicationType.fromValue(isoType));

        SearchResultMetadata resultMetadata = new SearchResultMetadata();
        resultMetadata.setBibliographicInfo(bibinfo);
        resultMetadata.setPublicationInfo(pubInfo);

        result.setMetadata(resultMetadata);

        result.setAbstract(getDescendant(element, "abstractText", 0));

        returnResults.add(result);

      }
    }
    NodeList totalRecordsNodes = doc.getElementsByTagName("numberOfRecords");
    Node totalRecords = totalRecordsNodes.item(0);
    searchResponse.setTotalRecords(Integer.parseInt(totalRecords.getTextContent()));

    searchResponse.setResults(returnResults);

    return searchResponse;
  }

  private String extractMetadata(ArrayList<Element> sources, String name) {
    String toReturn = null;
    for(Element source: sources) {
      toReturn = getDescendant(source, name, 0);
      if (toReturn != null) {
        break;
      }
    }
    return toReturn;
  }

  private String getDescendant(Node node, String target, Integer idx) {
    Element element = (Element) node;
    NodeList nodeList = element.getElementsByTagName(target);
    if (idx < 0 || nodeList.getLength() < idx + 1) {
      return null;
    }
    return nodeList.item(idx).getTextContent();
  }

  private ArrayList<BibliographicItemId> getIdentifiers(Element titleLevel) {
    String delimiterRegex = "\\|";
    ArrayList<String> types = new ArrayList<>();
    ArrayList<BibliographicItemId> toReturn = new ArrayList<>();
    types.add("isbn");
    types.add("issn");
    types.add("ismn");
    for (String type : types) {
      String value = getDescendant(titleLevel, type, 0);
      if (value != null && value.length() > 0) {
        String[] valueSplut = value.split(delimiterRegex);
        for (String splut: valueSplut) {
          BibliographicItemId bibliographicItemId = new BibliographicItemId();
          bibliographicItemId.setBibliographicItemIdentifierCode(
            BibliographicItemId.BibliographicItemIdentifierCode.fromValue(type.toUpperCase(Locale.ROOT))
          );
          bibliographicItemId.setBibliographicItemIdentifier(splut);
          toReturn.add(bibliographicItemId);
        }
      }
    }
    return toReturn;
  }

}
