package org.folio.util;

import io.vertx.core.json.JsonObject;
import org.folio.common.OkapiParams;
import org.folio.rest.jaxrs.model.BibliographicInfo;
import org.folio.rest.jaxrs.model.Config;
import org.folio.rest.jaxrs.model.PublicationInfo;
import org.folio.service.configuration.ConfigurationService;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.folio.config.Constants.CALLBACK_UUID;
import static org.folio.config.Constants.OUR_BASE_API;

public class BLDSSOrderRequest extends BLDSSRequest {

  private BibliographicInfo searchResultBibInfo;
  private BibliographicInfo submissionBibInfo;
  private PublicationInfo searchResultPubInfo;
  private PublicationInfo submissionPubInfo;


  public BLDSSOrderRequest(String httpMethod, String path, HashMap<String, String> parameters, Boolean needsAuth) {
    super("order", httpMethod, path, parameters, needsAuth);
  }

  public String preparePayload(String payload, Map<String, String> okapiHeaders) {
    ISO18626Util iso18626Util = new ISO18626Util();
    OkapiParams okapiParams = new OkapiParams(okapiHeaders);

    JsonObject json = new JsonObject(payload);

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db;
    try {
      db = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      return null;
    }

    Element rootEl;

    Document doc = db.newDocument();

    // What we do depends on the action we're processing
    String localRequestId = json.getString("localRequestId");
    JsonObject requestMetadata = json.getJsonObject("requestMetadata");
    JsonObject submission = json.getJsonObject("submission");
    JsonObject selectedResult = json.getJsonObject("selectedResult");

    // Set the properties that we will use to obtain metadata
    setInfos(selectedResult, submission);

    rootEl = doc.createElement("NewOrderRequest");
    doc.appendChild(rootEl);

    // Top level elements
    addValueToEl(doc, "S", "type", rootEl);
    addValueToEl(doc, localRequestId, "customerReference", rootEl);
    addValueToEl(doc, okapiParams.getUrl() + OUR_BASE_API + "/" + CALLBACK_UUID + "/sa-update", "callbackUrl", rootEl);

    // Service
    JsonObject services = requestMetadata.getJsonObject("services");
    String format = services.getString("format");
    String speed = services.getString("speed");
    String quality = services.getString("quality");
    Element service = doc.createElement("Service");
    addValueToEl(doc, "1", "service", service);
    addValueToEl(doc, format, "format", service);
    addValueToEl(doc, speed, "speed", service);
    addValueToEl(doc, quality, "quality", service);
    rootEl.appendChild(service);

    // Item
    Element item = doc.createElement("Item");

    // uin
    String supplierUniqueRecordId = this.searchResultBibInfo.getSupplierUniqueRecordId();
    addValueToEl(doc, supplierUniqueRecordId, "uin", item);

    // type
    String publicationType = this.searchResultPubInfo.getPublicationType().toString();
    String bldssType = iso18626Util.isoTypeToBldss(publicationType);
    addValueToEl(doc, bldssType, "type", item);

    // titleLevel
    Element titleLevel = doc.createElement("titleLevel");
    // title
    String title = getPrioritisedValue(
      this.searchResultBibInfo.getTitle(),
      this.submissionBibInfo.getTitle()
    );
    addValueToEl(doc, title, "title", titleLevel);
    // author
    String author = getPrioritisedValue(
      this.searchResultBibInfo.getAuthor(),
      this.submissionBibInfo.getAuthor()
    );
    addValueToEl(doc, author, "author", titleLevel);
    // ISBN
    String searchIsbn = iso18626Util.getIdentifierFromBibInfo(this.searchResultBibInfo, "ISBN");
    String subIsbn = iso18626Util.getIdentifierFromBibInfo(this.submissionBibInfo, "ISBN");
    String isbn = getPrioritisedValue(searchIsbn, subIsbn);
    addValueToEl(doc, isbn, "ISBN", titleLevel);
    // ISSN
    String searchIssn = iso18626Util.getIdentifierFromBibInfo(this.searchResultBibInfo, "ISSN");
    String subIssn = iso18626Util.getIdentifierFromBibInfo(this.submissionBibInfo, "ISSN");
    String issn = getPrioritisedValue(searchIssn, subIssn);
    addValueToEl(doc, issn, "ISSN", titleLevel);
    // ISMN
    String searchIsmn = iso18626Util.getIdentifierFromBibInfo(this.searchResultBibInfo, "ISMN");
    String subIsmn = iso18626Util.getIdentifierFromBibInfo(this.submissionBibInfo, "ISMN");
    String ismn = getPrioritisedValue(searchIsmn, subIsmn);
    addValueToEl(doc, ismn, "ISMN", titleLevel);
    // publisher
    String publisher = getPrioritisedValue(
      this.searchResultPubInfo.getPublisher(),
      this.submissionPubInfo.getPublisher()
    );
    addValueToEl(doc, publisher, "publisher", titleLevel);
    if (titleLevel.getChildNodes().getLength() > 0) {
      item.appendChild(titleLevel);
    }

    // itemLevel
    Element itemLevel = doc.createElement("itemLevel");
    // year
    String publicationDate = getPrioritisedValue(
      this.searchResultPubInfo.getPublicationDate(),
      this.submissionPubInfo.getPublicationDate()
    );
    if (publicationDate != null) {
      ZonedDateTime zdt = DateTimeUtils.stringToDt(publicationDate);
      int year = zdt.getYear();
      addValueToEl(doc, Integer.toString(year), "year", itemLevel);
    }
    // volume
    String volume = getPrioritisedValue(
      this.searchResultBibInfo.getVolume(),
      this.submissionBibInfo.getVolume()
    );
    addValueToEl(doc, volume, "volume", itemLevel);
    // issue
    String issue = getPrioritisedValue(
      this.searchResultBibInfo.getIssue(),
      this.submissionBibInfo.getIssue()
    );
    addValueToEl(doc, issue, "issue", itemLevel);
    // edition
    String edition = getPrioritisedValue(
      this.searchResultBibInfo.getEdition(),
      this.submissionBibInfo.getEdition()
    );
    addValueToEl(doc, edition, "edition", itemLevel);
    if (itemLevel.getChildNodes().getLength() > 0) {
      item.appendChild(itemLevel);
    }

    // itemOfInterestLevel
    Element itemOfInterestLevel = doc.createElement("itemOfInterestLevel");
    // title
    String titleOfComponent = getPrioritisedValue(
      this.searchResultBibInfo.getTitleOfComponent(),
      this.submissionBibInfo.getTitleOfComponent()
    );
    addValueToEl(doc, titleOfComponent, "title", itemOfInterestLevel);
    // author
    String authorOfComponent = getPrioritisedValue(
      this.searchResultBibInfo.getAuthorOfComponent(),
      this.submissionBibInfo.getAuthorOfComponent()
    );
    addValueToEl(doc, authorOfComponent, "author", itemOfInterestLevel);
    // pages
    String pagesRequested = getPrioritisedValue(
      this.searchResultBibInfo.getPagesRequested(),
      this.submissionBibInfo.getPagesRequested()
    );
    addValueToEl(doc, pagesRequested, "pages", itemOfInterestLevel);
    if (itemOfInterestLevel.getChildNodes().getLength() > 0) {
      item.appendChild(itemOfInterestLevel);
    }

    // We need to get the libraryPrivilege and outsideUk settings from the config
    String libPriv = getLibraryPrivilege(doc, okapiHeaders);
    addValueToEl(doc, libPriv, "LibraryPrivilege", rootEl);
    rootEl.appendChild(item);
    Boolean outsideUk = getIsOutsideUk(okapiHeaders);
    if (outsideUk) {
      addValueToEl(doc, "true", "payCopyright", rootEl);
    }
    XMLUtil xmlUtil = new XMLUtil();
    System.out.println(xmlUtil.docAsString(doc, false));
    return xmlUtil.docAsString(doc, false);
  }

  // Set the BibliographicInfo & PublicationInfo properties
  private void setInfos(JsonObject searchResult, JsonObject submissionMetadata) {
    JsonObject searchBibInfo = searchResult.getJsonObject("metadata").getJsonObject("BibliographicInfo");
    JsonObject searchPubInfo = searchResult.getJsonObject("metadata").getJsonObject("PublicationInfo");
    JsonObject subBibInfo = submissionMetadata.getJsonObject("submissionMetadata").getJsonObject("BibliographicInfo");
    JsonObject subPubInfo = submissionMetadata.getJsonObject("submissionMetadata").getJsonObject("PublicationInfo");
    this.searchResultBibInfo = searchBibInfo.mapTo(BibliographicInfo.class);
    this.searchResultPubInfo = searchPubInfo.mapTo(PublicationInfo.class);
    this.submissionBibInfo = subBibInfo.mapTo(BibliographicInfo.class);
    this.submissionPubInfo = subPubInfo.mapTo(PublicationInfo.class);
  }

  // Create an element, set its text and append it to something
  private void addValueToEl(Document doc, String value, String elementName, Element appendTo) {
    if (value != null && value.length() > 0) {
      Element el = doc.createElement(elementName);
      el.setTextContent(value);
      appendTo.appendChild(el);
    }
  }

  // Obtain a value, whichever is populated
  private String getPrioritisedValue(String value1, String value2) {
    return value1 != null && value1.length() > 0 ? value1 : value2;
  }

  // For more on Library Privilege, see here:
  // https://support.talis.com/hc/en-us/articles/205864591-British-Library-integration-functional-overview
  private String getLibraryPrivilege(Document doc, Map<String, String> okapiHeaders) {
    ConfigurationService configurationService = new ConfigurationService();
    Config config = configurationService.getConfigurationEntry("generalSettings", okapiHeaders, "UI-PLUGIN-ILL-CONNECTOR-BLDSS");
    JSONObject jsonObject = new JSONObject(config.getValue());
    boolean libPriv = jsonObject.getBoolean("libraryPrivilege");
    return libPriv ? "1" : "0";
  }

  private Boolean getIsOutsideUk(Map<String, String> okapiHeaders) {
    ConfigurationService configurationService = new ConfigurationService();
    Config config = configurationService.getConfigurationEntry("generalSettings", okapiHeaders, "UI-PLUGIN-ILL-CONNECTOR-BLDSS");
    JSONObject jsonObject = new JSONObject(config.getValue());
    return jsonObject.getBoolean("outsideUk");
  }
}
