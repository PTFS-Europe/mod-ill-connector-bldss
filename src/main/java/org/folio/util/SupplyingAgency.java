package org.folio.util;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.AgencyId;
import org.folio.rest.jaxrs.model.Costs;
import org.folio.rest.jaxrs.model.ISO18626.SamDeliveryInfo;
import org.folio.rest.jaxrs.model.ISO18626.SamStatusInfo;
import org.folio.rest.jaxrs.model.ISO18626.SupplyingAgencyMessage;
import org.folio.rest.jaxrs.model.SupplyingAgencyMessageHeader;
import org.folio.rest.jaxrs.model.SupplyingAgencyMessageInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.folio.config.Constants.ISO18626_DATE_FORMAT;

public class SupplyingAgency {

  static final Logger logger = LogManager.getLogger("SupplyingAgency");

  Map<String, SupplyingAgencyMessageInfo.AnswerYesNo> answerYesNoMap;
  Map<String, SupplyingAgencyMessageInfo.ReasonForMessage> reasonForMessageMap;
  Map<String, SupplyingAgencyMessageInfo.ReasonUnfilled> reasonUnfilledMap;
  Map<String, SupplyingAgencyMessageInfo.ReasonRetry> reasonRetryMap;
  Map<String, SamStatusInfo.Status> statusMap;
  XMLUtil xmlUtil;

  public SupplyingAgency() {
    this.xmlUtil = new XMLUtil();
    this.answerYesNoMap = bldssStatusToAnswerYesNoMap();
    this.reasonForMessageMap = bldssCodeToReasonForMessage();
    this.reasonUnfilledMap = bldssCodeToReasonUnfulfilled();
    this.reasonRetryMap = bldssCodeToReasonRetry();
    this.statusMap = bldssCodeToStatus();
  }

  // Receive a document either from a BLDSS orderline update or a response
  // to a request to the BLDSS API and build an ISO18626 Supplying Agency
  // Message from it
  public SupplyingAgencyMessage buildMessageFromOrderlineUpdate(String orderlineUpdate) {

    // Parse what we've received into something we can use
    Document doc = this.xmlUtil.parse(orderlineUpdate);

    AgencyId supplierId = new AgencyId()
      .withAgencyIdType(AgencyId.AgencyIdType.ISIL)
      .withAgencyIdValue("MY_SUPPLIER_ID");

    AgencyId requesterId = new AgencyId()
      .withAgencyIdType(AgencyId.AgencyIdType.ISIL)
      .withAgencyIdValue("MY_REQUESTER_ID");

    Element orderlineEl = (Element) this.xmlUtil.getNode(doc, "orderline");
    String supplierRequestId = orderlineEl.getAttribute("id");

    Element event = (Element) this.xmlUtil.getNode(doc,"event");
    String time = event.getAttribute("time");

    // Header
    SupplyingAgencyMessageHeader header = buildMessageHeader(
      supplierId,
      requesterId,
      "TEST",
      supplierRequestId,
      time
    );

    // MessageInfo
    SupplyingAgencyMessageInfo messageInfo = buildMessageInfo(doc);

    // StatusInfo
    SamStatusInfo statusInfo = buildMessageStatusInfo(doc);

    // DeliveryInfo
    SamDeliveryInfo deliveryInfo = buildMessageDeliveryInfo(doc);

    SupplyingAgencyMessage sam = new SupplyingAgencyMessage();

    if (header != null) {
      sam.setHeader(header);
    }

   sam.setMessageInfo(messageInfo);

    if (statusInfo != null) {
      sam.setStatusInfo(statusInfo);
    }

    if (deliveryInfo != null) {
      sam.setDeliveryInfo(deliveryInfo);
    }

    return sam;
  }

  public SupplyingAgencyMessage buildMessageFromBLResponse(
    String blResponseString,
    BLDSSRequest bldssRequest
  ) {
    BLDSSResponse bldssResponse = new BLDSSResponse(blResponseString);

    logger.info("Received response from BL:");
    logger.info(blResponseString);

    // If we don't have a response type we can't proceed
    if (bldssResponse.getResponseType() == null) {
      return null;
    }

    String customerReference = bldssResponse.getCustomerReference();
    String orderline = bldssResponse.getOrderline();
    String timestamp = bldssResponse.getTimestamp();
    AgencyId requestingAgencyId = bldssRequest.getRequestingAgencyId();
    AgencyId supplyingAgencyId = bldssRequest.getSupplyingAgencyId();

    SupplyingAgencyMessageHeader header = buildMessageHeader(
      requestingAgencyId,
      supplyingAgencyId,
      customerReference,
      orderline,
      timestamp
    );

    // MessageInfo
    String type = bldssResponse.getResponseType();
    String status = bldssResponse.getStatus();

    SupplyingAgencyMessageInfo.AnswerYesNo answerYesNo = status.equals("0") ?
      SupplyingAgencyMessageInfo.AnswerYesNo.Y :
      SupplyingAgencyMessageInfo.AnswerYesNo.N;

    // Note should comprise the BL response's <message> element +
    // anything in the BL response's <note> element
    String sendNote = bldssResponse.getMessage();
    String respNote = bldssResponse.getNote();
    if(respNote.length() > 0) {
      sendNote = sendNote + ". " + respNote;
    }

    SupplyingAgencyMessageInfo messageInfo = new SupplyingAgencyMessageInfo()
      .withReasonForMessage(this.reasonForMessageMap.get(type))
      .withAnswerYesNo(answerYesNo)
      .withNote(sendNote);

    if (!status.equals("0")) {
      SupplyingAgencyMessageInfo.ReasonUnfilled reasonUnfilled = this.reasonUnfilledMap.get(status);
      if (reasonUnfilled != null) {
        messageInfo.setReasonUnfilled(reasonUnfilled);
      }
      SupplyingAgencyMessageInfo.ReasonRetry reasonRetry = this.reasonRetryMap.get(status);
      if (reasonRetry != null) {
        messageInfo.setReasonRetry(reasonRetry);
      }
    }

    // StatusInfo
    SamStatusInfo statusInfo = new SamStatusInfo()
      .withStatus(this.statusMap.get(status))
      .withExpectedDeliveryDate(DateTimeUtils.bldssRequestResponseToIso(bldssResponse.getEstimatedDespatchDate()))
      .withLastChange(timestamp);

    return new SupplyingAgencyMessage()
      .withHeader(header)
      .withMessageInfo(messageInfo)
      .withStatusInfo(statusInfo);
  }

  private SupplyingAgencyMessageHeader buildMessageHeader(
    AgencyId supplierId,
    AgencyId requesterId,
    String requesterRequestId,
    String supplierRequestId,
    String time
  ) {
    return new SupplyingAgencyMessageHeader()
      .withSupplyingAgencyId(supplierId)
      .withRequestingAgencyId(requesterId)
      .withTimestamp(DateTimeUtils.bldssToIso(time))
      .withRequestingAgencyRequestId(requesterRequestId)
      .withSupplyingAgencyRequestId(supplierRequestId);
  }

  private SupplyingAgencyMessageInfo buildMessageInfo(Document doc) {

    String code = getEventCode(doc);

    SupplyingAgencyMessageInfo messageInfo = new SupplyingAgencyMessageInfo()
      .withReasonForMessage(this.reasonForMessageMap.get(code));

    // In case our mapping falls short, add everything into the note field
    Element eventType = (Element) this.xmlUtil.getNode(doc, "eventType");
    Element additionalInfo = (Element) this.xmlUtil.getNode(doc, "additionalInfo");
    String note = code + ": " + eventType.getTextContent() + " " + additionalInfo.getTextContent();
    messageInfo.setNote(note);

    // Add additional properties as necessary
    if (this.reasonUnfilledMap.containsKey(code)) {
      messageInfo.setReasonUnfilled(this.reasonUnfilledMap.get(code));
    }
    if (this.reasonRetryMap.containsKey(code)) {
      messageInfo.setReasonRetry(this.reasonRetryMap.get(code));
      // If we've received an "Exceeds max cost" message, extract the actual
      // cost and populate it
      if (code.equals("18f") && additionalInfo.getTextContent() != null) {
        String actualCost = additionalInfo.getTextContent().replaceAll("Exceeds max cost ","");
        Costs costs = new Costs()
          // TODO: Populate currency code correctly
          .withCurrencyCode("GBP")
          .withMonetaryValue(actualCost);
        messageInfo.setOfferedCosts(costs);
      }
    }

    return messageInfo;
  }

  private SamStatusInfo buildMessageStatusInfo(Document doc) {

    String code = getEventCode(doc);

    SamStatusInfo.Status status = this.statusMap.get(code);

    return new SamStatusInfo()
      .withStatus(status)
      .withLastChange(DateTimeUtils.dtToString(ZonedDateTime.now(), ISO18626_DATE_FORMAT));
  }

  private SamDeliveryInfo buildMessageDeliveryInfo(Document doc) {
    String code = getEventCode(doc);

    // If we're receiving a message containing dispatch info
    Element additionalInfo = (Element) this.xmlUtil.getNode(doc, "additionalInfo");
    if (code.equals("12")) {
      if (additionalInfo.getTextContent().length() > 0) {
        String date = DateTimeUtils.bldssToIso(additionalInfo.getTextContent());
        return new SamDeliveryInfo()
          .withDateSent(date);
      }
    } else if (code.equals("11")) {
      SamDeliveryInfo.SentVia sentVia = SamDeliveryInfo.SentVia.URL;
      return new SamDeliveryInfo()
        .withSentVia(sentVia);
    }
    return null;
  }

  public String buildConfirmation(JsonObject isoConfirmation) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.newDocument();
      Element rootEl = doc.createElement("updateResponse");
      doc.appendChild(rootEl);

      String outDt = DateTimeUtils.isoToBldss(isoConfirmation.getJsonObject("Header").getString("Timestamp"));
      Element timestamp = doc.createElement("timestamp");
      timestamp.setTextContent(outDt);
      rootEl.appendChild(timestamp);

      Element status = doc.createElement("status");
      status.setTextContent("0");
      rootEl.appendChild(status);

      Element message = doc.createElement("message");
      rootEl.appendChild(message);

      String out = this.xmlUtil.docAsString(doc, true);
      return out;
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }
    return null;
  }

  private Map<String, SupplyingAgencyMessageInfo.AnswerYesNo> bldssStatusToAnswerYesNoMap() {
    return new HashMap<String, SupplyingAgencyMessageInfo.AnswerYesNo>() {{
      put("0", SupplyingAgencyMessageInfo.AnswerYesNo.Y);
      put("1", SupplyingAgencyMessageInfo.AnswerYesNo.N);
    }};
  }

  private Map<String, SupplyingAgencyMessageInfo.ReasonRetry> bldssCodeToReasonRetry() {
    return new HashMap<String, SupplyingAgencyMessageInfo.ReasonRetry>() {{
      put("5", SupplyingAgencyMessageInfo.ReasonRetry.NOT_FOUND_AS_CITED);
      put("18a", SupplyingAgencyMessageInfo.ReasonRetry.NOT_FOUND_AS_CITED);
      put("18b", SupplyingAgencyMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18c", SupplyingAgencyMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18d", SupplyingAgencyMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18e", SupplyingAgencyMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18f", SupplyingAgencyMessageInfo.ReasonRetry.COST_EXCEEDS_MAX_COST);
      put("18g", SupplyingAgencyMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18h", SupplyingAgencyMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18i", SupplyingAgencyMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18j", SupplyingAgencyMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18k", SupplyingAgencyMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("21b", SupplyingAgencyMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("111", SupplyingAgencyMessageInfo.ReasonRetry.NOT_FOUND_AS_CITED);
    }};
  }

  private Map<String, SupplyingAgencyMessageInfo.ReasonUnfilled> bldssCodeToReasonUnfulfilled() {
    return new HashMap<String, SupplyingAgencyMessageInfo.ReasonUnfilled>() {{
      put("5", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("18a", SupplyingAgencyMessageInfo.ReasonUnfilled.NOT_AVAILABLE_FOR_ILL);
      put("18b", SupplyingAgencyMessageInfo.ReasonUnfilled.NOT_HELD);
      put("18c", SupplyingAgencyMessageInfo.ReasonUnfilled.NOT_ON_SHELF);
      put("18d", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("18e", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("18f", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("18g", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("18h", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("18i", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("18j", SupplyingAgencyMessageInfo.ReasonUnfilled.NOT_AVAILABLE_FOR_ILL);
      put("18k", SupplyingAgencyMessageInfo.ReasonUnfilled.NOT_AVAILABLE_FOR_ILL);
      put("21b", SupplyingAgencyMessageInfo.ReasonUnfilled.NOT_AVAILABLE_FOR_ILL);
      put("111", SupplyingAgencyMessageInfo.ReasonUnfilled.NOT_HELD);
      put("112", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("113", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("700", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("701", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("702", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("703", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("704", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("705", SupplyingAgencyMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
    }};
  }

  // Map from BLDSS event codes and response reasons to ISO18626 ReasonForMessage
  // We could create this in a more concise way, but this is clearer (IMHO)
  // and maps directly onto the table 9.2 here:
  // https://apitest.bldss.bl.uk/docs/guide/appendix.html#orderlineUpdates
  // It's not clear from the BL docs what initiates some of these events, so
  // there's some best guesses here
  private Map<String, SupplyingAgencyMessageInfo.ReasonForMessage> bldssCodeToReasonForMessage() {
    return new HashMap<String, SupplyingAgencyMessageInfo.ReasonForMessage>() {{
      put("newOrder", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("1", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("10", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("11", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("12", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("12a", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("13", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("14", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("15", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("16", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("17", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18a", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18b", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18c", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18d", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18e", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18f", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18g", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18h", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18i", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18j", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18k", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("19", SupplyingAgencyMessageInfo.ReasonForMessage.CANCEL_RESPONSE);
      put("1a", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("20b", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("20c", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("20d", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("21", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("21a", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("21b", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("22a", SupplyingAgencyMessageInfo.ReasonForMessage.RENEW_RESPONSE);
      put("22b", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("23", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("24", SupplyingAgencyMessageInfo.ReasonForMessage.RENEW_RESPONSE);
      put("25", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("25a", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("26", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("27", SupplyingAgencyMessageInfo.ReasonForMessage.RENEW_RESPONSE);
      put("28", SupplyingAgencyMessageInfo.ReasonForMessage.CANCEL_RESPONSE);
      put("29", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("2a", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("2b", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("2c", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("2d", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("2e", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("2f", SupplyingAgencyMessageInfo.ReasonForMessage.CANCEL_RESPONSE);
      put("3a", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("4", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("47", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("48", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("49", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("4a", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("6", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("7a", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("7b", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("8", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
      put("9a", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("9b", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("9c", SupplyingAgencyMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("9d", SupplyingAgencyMessageInfo.ReasonForMessage.NOTIFICATION);
    }};
  }

  private Map<String, SamStatusInfo.Status> bldssCodeToStatus() {
    return new HashMap<String, SamStatusInfo.Status>() {{
      put("0", SamStatusInfo.Status.REQUEST_RECEIVED);
      put("1", SamStatusInfo.Status.REQUEST_RECEIVED);
      put("5", SamStatusInfo.Status.UNFILLED);
      put("10", SamStatusInfo.Status.EXPECT_TO_SUPPLY);
      put("11", SamStatusInfo.Status.LOANED);
      put("12", SamStatusInfo.Status.WILL_SUPPLY);
      put("12a", SamStatusInfo.Status.LOANED);
      put("14", SamStatusInfo.Status.LOANED);
      put("15", SamStatusInfo.Status.LOANED);
      put("16", SamStatusInfo.Status.WILL_SUPPLY);
      put("17", SamStatusInfo.Status.EXPECT_TO_SUPPLY);
      put("18a", SamStatusInfo.Status.UNFILLED);
      put("18b", SamStatusInfo.Status.UNFILLED);
      put("18c", SamStatusInfo.Status.UNFILLED);
      put("18d", SamStatusInfo.Status.UNFILLED);
      put("18e", SamStatusInfo.Status.UNFILLED);
      put("18f", SamStatusInfo.Status.UNFILLED);
      put("18g", SamStatusInfo.Status.UNFILLED);
      put("18h", SamStatusInfo.Status.UNFILLED);
      put("18i", SamStatusInfo.Status.UNFILLED);
      put("18j", SamStatusInfo.Status.UNFILLED);
      put("18k", SamStatusInfo.Status.UNFILLED);
      put("19", SamStatusInfo.Status.CANCELLED);
      put("1a", SamStatusInfo.Status.UNFILLED);
      put("20b", SamStatusInfo.Status.EXPECT_TO_SUPPLY);
      put("20c", SamStatusInfo.Status.EXPECT_TO_SUPPLY);
      put("20d", SamStatusInfo.Status.WILL_SUPPLY);
      put("21", SamStatusInfo.Status.WILL_SUPPLY);
      put("21a", SamStatusInfo.Status.REQUEST_RECEIVED);
      put("21b", SamStatusInfo.Status.UNFILLED);
      put("22a", SamStatusInfo.Status.LOANED);
      put("22b", SamStatusInfo.Status.LOANED);
      put("23", SamStatusInfo.Status.OVERDUE);
      put("24", SamStatusInfo.Status.LOANED);
      put("25", SamStatusInfo.Status.OVERDUE);
      put("25a", SamStatusInfo.Status.LOAN_COMPLETED);
      put("26", SamStatusInfo.Status.LOANED);
      put("27", SamStatusInfo.Status.LOANED);
      put("28", SamStatusInfo.Status.REQUEST_RECEIVED);
      put("29", SamStatusInfo.Status.REQUEST_RECEIVED);
      put("2a", SamStatusInfo.Status.UNFILLED);
      put("2b", SamStatusInfo.Status.UNFILLED);
      put("2c", SamStatusInfo.Status.UNFILLED);
      put("2d", SamStatusInfo.Status.UNFILLED);
      put("2e", SamStatusInfo.Status.UNFILLED);
      put("2f", SamStatusInfo.Status.UNFILLED);
      put("3a", SamStatusInfo.Status.UNFILLED);
      put("4", SamStatusInfo.Status.REQUEST_RECEIVED);
      put("47", SamStatusInfo.Status.REQUEST_RECEIVED);
      put("48", SamStatusInfo.Status.LOANED);
      put("49", SamStatusInfo.Status.LOANED);
      put("4a", SamStatusInfo.Status.LOANED);
      put("6", SamStatusInfo.Status.LOANED);
      put("7a", SamStatusInfo.Status.REQUEST_RECEIVED);
      put("7b", SamStatusInfo.Status.REQUEST_RECEIVED);
      put("8", SamStatusInfo.Status.UNFILLED);
      put("9a", SamStatusInfo.Status.REQUEST_RECEIVED);
      put("9b", SamStatusInfo.Status.REQUEST_RECEIVED);
      put("9c", SamStatusInfo.Status.EXPECT_TO_SUPPLY);
      put("9d", SamStatusInfo.Status.REQUEST_RECEIVED);
      put("111", SamStatusInfo.Status.UNFILLED);
      put("112", SamStatusInfo.Status.RETRY_POSSIBLE);
      put("113", SamStatusInfo.Status.RETRY_POSSIBLE);
      put("700", SamStatusInfo.Status.RETRY_POSSIBLE);
      put("701", SamStatusInfo.Status.RETRY_POSSIBLE);
      put("702", SamStatusInfo.Status.RETRY_POSSIBLE);
      put("703", SamStatusInfo.Status.RETRY_POSSIBLE);
      put("704", SamStatusInfo.Status.RETRY_POSSIBLE);
      put("705", SamStatusInfo.Status.RETRY_POSSIBLE);
    }};
  }

  private String getEventCode(Document doc) {
    // Get the BLDSS event code
    Element eventType = (Element) this.xmlUtil.getNode(doc, "eventType");
    return eventType.getAttribute("id");
  }
}
