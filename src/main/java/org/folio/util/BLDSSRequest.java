package org.folio.util;

import org.folio.rest.jaxrs.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.folio.config.Constants.BLDSS_TEST_API_URL;

public class BLDSSRequest {

  private final String httpMethod;
  private final String path;
  private String payload;
  private final HashMap<String, String> parameters;
  private ActionMetadata actionPayload;

  public BLDSSRequest(String httpMethod, String path, HashMap<String, String> parameters) {
    this.httpMethod = httpMethod;
    this.path = "/api" + path;
    this.parameters = parameters;
  }

  public CompletableFuture<BLDSSResponse> makeRequest() {
    CompletableFuture<BLDSSResponse> future = new CompletableFuture<>();
    HttpClient client = HttpClient.newHttpClient();
    BLDSSAuth auth = new BLDSSAuth(this.httpMethod, this.path, this.parameters, this.payload);
    String authHeader = auth.getHeaderString();
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(BLDSS_TEST_API_URL + this.path))
      .POST(HttpRequest.BodyPublishers.ofString(this.payload))
      .header("BLDSS-API-Authentication", authHeader)
      .header("Content-type", "application/xml")
      .build();
    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(apiResponse -> {
          BLDSSResponse response = new BLDSSResponse(apiResponse.body());
          future.complete(response);
          return response;
        });
    return future;
  }


  public HashMap<String, String> getParameters() {
    return parameters;
  }

  public void setParameter(String key, String value) {
    this.parameters.put(key, value);
  }

  // Take an ISO18626 payload and mung it into a BLDSS compliant one
  public void setPayload(ActionMetadata payload) {
    this.actionPayload = payload;
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.newDocument();
      Element rootEl = doc.createElement("NewOrderRequest");
      doc.appendChild(rootEl);
      Element type = doc.createElement("type");
      type.setTextContent("S");
      rootEl.appendChild(type);
      Element payCopyright = doc.createElement("payCopyright");
      payCopyright.setTextContent("true");
      rootEl.appendChild(payCopyright);
      rootEl.appendChild(getItem(doc));
      XMLUtil xmlUtil = new XMLUtil();
      System.out.println(xmlUtil.docAsString(doc));
      this.payload = xmlUtil.docAsString(doc);
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }
  }

  private Node getItem(Document doc) {
    Element item = doc.createElement("Item");
    Node titleLevel = getTitleLevel(doc);
    Node itemLevel = getItemLevel(doc);
    Node itemOfInterestLevel = getItemOfInterestLevel(doc);

    if (titleLevel.getChildNodes().getLength() > 0) {
      item.appendChild(getTitleLevel(doc));
    }
    if (itemLevel.getChildNodes().getLength() > 0) {
      item.appendChild(getItemLevel(doc));
    }
    if (itemOfInterestLevel.getChildNodes().getLength() > 0) {
      item.appendChild(getItemOfInterestLevel(doc));
    }
    return item;
  }

  private Node getTitleLevel(Document doc)  {
    Element titleLevel = doc.createElement("titleLevel");

    BibliographicInfo bibInfo = this.actionPayload.getBibliographicInfo();
    String title = bibInfo.getTitle();
    String author = bibInfo.getAuthor();
    BibliographicItemId itemId = bibInfo.getBibliographicItemId();

    return titleLevel;
  }

  private Node getItemLevel(Document doc)  {
    Element itemLevel = doc.createElement("itemLevel");

    PublicationInfo pubInfo = this.actionPayload.getPublicationInfo();
    BibliographicInfo bibInfo = this.actionPayload.getBibliographicInfo();

    String year = pubInfo.getPublicationDate();
    String volume = bibInfo.getVolume();
    String part = bibInfo.getIssue();
    String issue = bibInfo.getIssue();
    String edition = bibInfo.getEdition();

    appendElement(doc, year, "year", itemLevel);
    appendElement(doc, volume, "volume", itemLevel);
    appendElement(doc, part, "part", itemLevel);
    appendElement(doc, issue, "issue", itemLevel);
    appendElement(doc, edition, "edition", itemLevel);

    return itemLevel;
  }

  private Node getItemOfInterestLevel(Document doc)  {
    Element itemOfInterestLevel = doc.createElement("itemOfInterestLevel");

    BibliographicInfo bibInfo = this.actionPayload.getBibliographicInfo();

    String articleTitle = bibInfo.getTitleOfComponent();
    String pages = bibInfo.getPagesRequested();
    String author = bibInfo.getAuthorOfComponent();

    appendElement(doc, articleTitle, "title", itemOfInterestLevel);
    appendElement(doc, pages, "pages", itemOfInterestLevel);
    appendElement(doc, author, "author", itemOfInterestLevel);
    System.out.println(itemOfInterestLevel.getChildNodes().getLength());
    return itemOfInterestLevel;
  }

  private void appendElement(Document doc, String value, String elementName, Element appendTo) {
    if (value == null) return;
    Element element = doc.createElement(elementName);
    element.setTextContent(value);
    appendTo.appendChild(element);
  }
}
