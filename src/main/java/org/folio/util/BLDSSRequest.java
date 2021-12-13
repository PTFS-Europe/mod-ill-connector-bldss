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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.folio.config.Constants.BLDSS_TEST_API_URL;

public class BLDSSRequest {

  private final String httpMethod;
  private final String path;
  private String payload;
  private final HashMap<String, String> parameters;
  private ActionMetadata actionPayload;
  private Map<String, String> typeMap;

  public BLDSSRequest(String httpMethod, String path, HashMap<String, String> parameters) {
    this.httpMethod = httpMethod;
    this.path = "/api" + path;
    this.parameters = parameters;
    this.typeMap = isoTypeToBldss();
  }

  public CompletableFuture<HttpResponse<String>> makeRequest() {
    CompletableFuture<HttpResponse<String>> future = new CompletableFuture<>();
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
          future.complete(apiResponse);
          return apiResponse;
        });
    return future;
  }

  public ActionMetadata getActionPayload() {
    return this.actionPayload;
  }

  public HashMap<String, String> getParameters() {
    return parameters;
  }

  public void setParameter(String key, String value) {
    this.parameters.put(key, value);
  }

  // Take an ISO18626 payload and translate it into a BLDSS compliant one
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
      rootEl.appendChild(getCustomerReference(doc));

      Node item = getItem(doc);
      Node delivery = getRequestedDelivery(doc);
      Node service = getService(doc);
      // TODO: Temporarily omit LibraryPrivilege
      // Node libraryPrivilege = getLibraryPrivilege(doc);
      if (item != null) {
        rootEl.appendChild(item);
      }
      if (delivery != null) {
        rootEl.appendChild(delivery);
      }
      if (service != null) {
        rootEl.appendChild(service);
      }
      // TODO: Temporarily omit library privilege
      /*
      if (libraryPrivilege != null) {
        rootEl.appendChild(libraryPrivilege);
      }
      */

      XMLUtil xmlUtil = new XMLUtil();
      this.payload = xmlUtil.docAsString(doc, false);
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }
  }

  // Return whether this account has "library privilege" enabled
  // For more on Library Privilege, see here:
  // https://support.talis.com/hc/en-us/articles/205864591-British-Library-integration-functional-overview
  // TODO: Should be derived from the module config
  // For now, we just omit it entirely
  /*
  private Node getLibraryPrivilege(Document doc) {
    Element libraryPrivilege = doc.createElement("LibraryPrivilege");
    libraryPrivilege.setTextContent("0");
    return libraryPrivilege;
  }
  */

  // Return a customerReference node containing a customer reference
  private Node getCustomerReference(Document doc) {
    Header header = this.actionPayload.getHeader();

    Element customerRef = doc.createElement("customerReference");
    customerRef.setTextContent(header.getRequestingAgencyRequestId());
    return customerRef;
  }

  private Node getItem(Document doc) {
    Element item = doc.createElement("Item");

    BibliographicInfo bibInfo = this.actionPayload.getBibliographicInfo();
    PublicationInfo publicationInfo = this.actionPayload.getPublicationInfo();

    if (bibInfo != null) {
      appendTextElement(doc, bibInfo.getSupplierUniqueRecordId(), "uin", item);
    }
    if (publicationInfo != null) {
      String isoType = publicationInfo.getPublicationType().toString();
      String blType = this.typeMap.get(isoType);
      if (blType != null) {
        appendTextElement(doc, blType, "type", item);
      }
    }

    Node titleLevel = getTitleLevel(doc);
    Node itemLevel = getItemLevel(doc);
    Node itemOfInterestLevel = getItemOfInterestLevel(doc);

    if (titleLevel.getChildNodes().getLength() > 0) {
      item.appendChild(titleLevel);
    }
    if (itemLevel.getChildNodes().getLength() > 0) {
      item.appendChild(itemLevel);
    }
    if (itemOfInterestLevel.getChildNodes().getLength() > 0) {
      item.appendChild(itemOfInterestLevel);
    }
    return item;
  }

  private Node getTitleLevel(Document doc)  {
    Element titleLevel = doc.createElement("titleLevel");

    BibliographicInfo bibInfo = this.actionPayload.getBibliographicInfo();
    List<SupplierInfo> supplierInfos = this.actionPayload.getSupplierInfo();
    PublicationInfo publicationInfo = this.actionPayload.getPublicationInfo();


    if (bibInfo != null) {
      appendTextElement(doc, bibInfo.getTitle(), "title", titleLevel);
      appendTextElement(doc, bibInfo.getAuthor(), "author", titleLevel);
      String isbn = getBibIdentifier(
        bibInfo.getBibliographicItemId(),
        "ISBN"
      );
      String issn = getBibIdentifier(
        bibInfo.getBibliographicItemId(),
        "ISSN"
      );
      String ismn = getBibIdentifier(
        bibInfo.getBibliographicItemId(),
        "ISMN"
      );
      appendTextElement(doc, isbn, "ISBN", titleLevel);
      appendTextElement(doc, issn, "ISSN", titleLevel);
    }
    if (supplierInfos != null) {
      // We're not supporting the "brokerage" aspect of "SupplierInfo" but according to the
      // spec, it can also be used "in other circumstances", and it appears to be the only
      // place to get a call number! So we'll just use the first one we find... :-/
      appendTextElement(doc, getCallNumber(supplierInfos), "shelfmark", titleLevel);
    }
    if (publicationInfo != null) {
      appendTextElement(doc, publicationInfo.getPublisher(), "publisher", titleLevel);
    }

    return titleLevel;
  }

  private Node getItemLevel(Document doc)  {
    Element itemLevel = doc.createElement("itemLevel");

    PublicationInfo pubInfo = this.actionPayload.getPublicationInfo();
    BibliographicInfo bibInfo = this.actionPayload.getBibliographicInfo();

    if (pubInfo != null) {
      appendTextElement(doc, pubInfo.getPublicationDate(), "year", itemLevel);
    }
    if (bibInfo != null) {
      appendTextElement(doc, bibInfo.getVolume(), "volume", itemLevel);
      appendTextElement(doc, bibInfo.getIssue(), "part", itemLevel);
//      appendTextElement(doc, bibInfo.getIssue(), "issue", itemLevel);
      appendTextElement(doc, bibInfo.getEdition(), "edition", itemLevel);
    }

    return itemLevel;
  }

  private Node getItemOfInterestLevel(Document doc)  {
    Element itemOfInterestLevel = doc.createElement("itemOfInterestLevel");

    BibliographicInfo bibInfo = this.actionPayload.getBibliographicInfo();

    if (bibInfo != null) {
      appendTextElement(doc, bibInfo.getTitleOfComponent(), "title", itemOfInterestLevel);
      appendTextElement(doc, bibInfo.getPagesRequested(), "pages", itemOfInterestLevel);
      appendTextElement(doc, bibInfo.getAuthorOfComponent(), "author", itemOfInterestLevel);
    }
    return itemOfInterestLevel;
  }

  // Given a list of RequestedDeliveryInfo objects, use the first of each type
  // to construct our "Delivery" element
  private Node getRequestedDelivery(Document doc) {
    List<RequestedDeliveryInfo> deliveryInfos = this.actionPayload.getRequestedDeliveryInfo();

    // Bail if we've nothing to work with
    if (deliveryInfos.size() == 0) return null;

    Element delivery = doc.createElement("Delivery");
    int physicalPopulated = 0;
    int electronicPopulated = 0;

    for (RequestedDeliveryInfo deliveryInfo : deliveryInfos) {
      // We have to infer the address type from the properties contained therein,
      // see: https://folio-project.slack.com/archives/CC0PHKEMT/p1634657867107400
      Map<String, Object> address = deliveryInfo.getAddress().getAdditionalProperties();

      // getAdditionalProperties seems to return a LinkedHashMap so we cast to that,
      // ignoring the warning. I'm sure there must be a better way of doing this.
      @SuppressWarnings("unchecked")
      LinkedHashMap<String, Object> electronic = (LinkedHashMap<String, Object>) address.get("ElectronicAddress");
      @SuppressWarnings("unchecked")
      LinkedHashMap<String, Object> physical = (LinkedHashMap<String, Object>) address.get("PhysicalAddress");

      if (physical != null && physicalPopulated == 0) {
        Node physicalNode = getPhysicalAddress(doc, physical);
        if (physicalNode != null) {
          delivery.appendChild(physicalNode);
          physicalPopulated++;
          continue;
        }
      }

      if (electronic != null && electronicPopulated == 0) {
        Node electronicNode = getElectronic(doc, electronic);
        if (electronicNode != null) {
          delivery.appendChild(electronicNode);
          electronicPopulated++;
        }
      }

    }

    return (physicalPopulated != 0 || electronicPopulated != 0) ? delivery : null;
  }

  private Node getService(Document doc) {
    // TODO: These should be derived from the user
    Element service = doc.createElement("Service");
    appendTextElement(doc, "1", "service", service);
    appendTextElement(doc, "1", "format", service);
    appendTextElement(doc, "3", "speed", service);
    appendTextElement(doc, "1", "quality", service);
    return service;
  }

  private Node getElectronic(Document doc, LinkedHashMap<String, Object> isoAddress) {
    if (isoAddress.get("ElectronicAddressType").toString().equals("Email")) {
      Element email = doc.createElement("Email");
      email.setTextContent(isoAddress.get("ElectronicAddressData").toString());
      return email;
    }
    return null;
  }

  // ISO18626 has a bizarre set of properties to represent a physical address,
  // this is an attempt to maximise the chances of gettting an address that
  // BLDSS can use (taking into account BLDSS' required fields)
  private Node getPhysicalAddress(Document doc, LinkedHashMap<String, Object> isoAddress) {
    Element blAddress = doc.createElement("Address");

    // Map a BL's physical address property to an ISO one
    Map<String, String> blToIso = Stream.of(new String[][] {
      { "AddressLine1", "Line1" },
      { "AddressLine2", "Line2" },
      { "TownOrCity", "Locality" },
      { "CountyOrState", "Locality" },
      { "ProvinceOrRegion", "Region" },
      { "PostOrZipCode", "PostalCode" },
      { "Country", "Country" }
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    // Iterate each BL property and get the ISO equivalent value
    blToIso.forEach((k, v) -> {
      String isoValue = isoAddress.get(v).toString();
      if (isoValue != null && isoValue.length() > 0) {
        Element el = doc.createElement(k);
        el.setTextContent(isoValue);
        blAddress.appendChild(el);
      }
    });

    return blAddress;
  }

  // Return the RequestingAgencyId as an AgencyId
  public AgencyId getRequestingAgencyId() {
    SupplyingAgencyId reqRequestingAgencyId = this.actionPayload.getHeader().getRequestingAgencyId();
    String reqReqAgencyIdType = reqRequestingAgencyId.getAgencyIdType().toString();
    String reqReqAgencyIdValue = reqRequestingAgencyId.getAgencyIdValue();

    return new AgencyId()
      .withAgencyIdType(AgencyId.AgencyIdType.valueOf(reqReqAgencyIdType))
      .withAgencyIdValue(reqReqAgencyIdValue);
  }

  // Return the SupplyingAgencyId as an AgencyId
  public AgencyId getSupplyingAgencyId() {
    SupplyingAgencyId reqSupplyingAgencyId = this.actionPayload.getHeader().getSupplyingAgencyId();
    String reqSupAgencyIdType = reqSupplyingAgencyId.getAgencyIdType().toString();
    String reqSupAgencyIdValue = reqSupplyingAgencyId.getAgencyIdValue();

    return new AgencyId()
      .withAgencyIdType(AgencyId.AgencyIdType.valueOf(reqSupAgencyIdType))
      .withAgencyIdValue(reqSupAgencyIdValue);
  }
  private void appendTextElement(Document doc, String value, String elementName, Element appendTo) {
    if (value == null) return;
    Element element = doc.createElement(elementName);
    element.setTextContent(value);
    appendTo.appendChild(element);
  }

  private String getCallNumber(List<SupplierInfo> supplierInfos) {
    for (SupplierInfo info : supplierInfos) {
      String callNumber = info.getCallNumber();
      if (callNumber != null && callNumber.length() > 0) {
        return callNumber;
      }
    }
    return null;
  }

  // Seems the BLDSS API can only copy with one identifier, so just
  // return the first we find
  private String getBibIdentifier(List<BibliographicItemId> ids, String needle) {
    for (BibliographicItemId id : ids) {
      if (id.getBibliographicItemIdentifierCode().toString().equals(needle)) {
        return id.getBibliographicItemIdentifier();
      }
    }
    return null;
  }

  // Translate an ISO18626 "PublicationType" into a BLDSS "type"
  private Map<String, String> isoTypeToBldss() {
    return typeMap = Stream.of(new String[][] {
      { "Article", "article" },
      { "Book", "book" },
      { "Journal", "journal" },
      { "Newspaper", "newspaper" },
      { "ConferenceProc", "conference"},
      { "Thesis", "thesis" },
      { "MusicScore", "score" }
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
  }
}
