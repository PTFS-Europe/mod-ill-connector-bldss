package org.folio.util;

import org.folio.rest.jaxrs.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SupplyingAgency {

  Map<String, SamMessageInfo.ReasonForMessage> reasonForMessageMap;
  Map<String, SamMessageInfo.ReasonUnfilled> reasonUnfilledMap;
  Map<String, SamMessageInfo.ReasonRetry> reasonRetryMap;
  Map<String, SamStatusInfo.Status> statusMap;
  XMLUtil xmlUtil;

  public SupplyingAgency() {
    this.xmlUtil = new XMLUtil();
    this.reasonForMessageMap = bldssCodeToReasonForMessage();
    this.reasonUnfilledMap = bldssCodeToReasonUnfulfilled();
    this.reasonRetryMap = bldssCodeToReasonRetry();
    this.statusMap = bldssCodeToStatus();
  }

  public SupplyingAgencyMessage buildMessage(String inboundMessage) {

    // Parse what we've received into something we can use
    Document doc = this.xmlUtil.parse(inboundMessage);

    // Header
    SamHeader header = buildHeader(doc);

    // MessageInfo
    SamMessageInfo messageInfo = buildMessageInfo(doc);

    // StatusInfo
    SamStatusInfo statusInfo = buildStatusInfo(doc);

    // DeliveryInfo
    SamDeliveryInfo deliveryInfo = buildDeliveryInfo(doc);

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

  private SamHeader buildHeader(Document doc) {
    Element orderlineEl = (Element) this.xmlUtil.getNode(doc, "orderline");
    String requestId = orderlineEl.getAttribute("id");

    AgencyId supplierId = new AgencyId()
      .withAgencyIdType(AgencyId.AgencyIdType.ISIL)
      .withAgencyIdValue("MY_SUPPLIER_ID");

    AgencyId requesterId = new AgencyId()
      .withAgencyIdType(AgencyId.AgencyIdType.ISIL)
      .withAgencyIdValue("MY_REQUESTER_ID");

    Date now = new Date(System.currentTimeMillis());

    return new SamHeader()
      .withSupplyingAgencyId(supplierId)
      .withRequestingAgencyId(requesterId)
      .withTimestamp(now)
      .withRequestingAgencyRequestId(requestId);
  }

  private SamMessageInfo buildMessageInfo(Document doc) {

    String code = getEventCode(doc);

    SamMessageInfo messageInfo = new SamMessageInfo()
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

  private SamStatusInfo buildStatusInfo(Document doc) {

    String code = getEventCode(doc);

    SamStatusInfo.Status status = this.statusMap.get(code);

    Date now = new Date(System.currentTimeMillis());

    return new SamStatusInfo()
      .withStatus(status)
      .withLastChange(now);

  }

  private SamDeliveryInfo buildDeliveryInfo(Document doc) {
    String code = getEventCode(doc);

    // If we're receiving a message containing dispatch info
    Element additionalInfo = (Element) this.xmlUtil.getNode(doc, "additionalInfo");
    if (code.equals("12")) {
      if (additionalInfo.getTextContent().length() > 0) {
        SimpleDateFormat bldssFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        try {
          Date date = bldssFormat.parse(additionalInfo.getTextContent());
          return new SamDeliveryInfo()
            .withDateSent(date);
        } catch(ParseException e) {
          e.printStackTrace();
        }
      }
    } else if (code.equals("11")) {
      SamDeliveryInfo.SentVia sentVia = SamDeliveryInfo.SentVia.URL;
      return new SamDeliveryInfo()
        .withSentVia(sentVia);
    }
    return null;
  }

  private Map<String, SamMessageInfo.ReasonRetry> bldssCodeToReasonRetry() {
    return new HashMap<String, SamMessageInfo.ReasonRetry>() {{
      put("18a", SamMessageInfo.ReasonRetry.NOT_FOUND_AS_CITED);
      put("18b", SamMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18c", SamMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18d", SamMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18e", SamMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18f", SamMessageInfo.ReasonRetry.COST_EXCEEDS_MAX_COST);
      put("18g", SamMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18h", SamMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18i", SamMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18j", SamMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("18k", SamMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
      put("21b", SamMessageInfo.ReasonRetry.NOT_CURRENT_AVAILABLE_FOR_ILL);
    }};
  }

  private Map<String, SamMessageInfo.ReasonUnfilled> bldssCodeToReasonUnfulfilled() {
    return new HashMap<String, SamMessageInfo.ReasonUnfilled>() {{
      put("18a", SamMessageInfo.ReasonUnfilled.NOT_AVAILABLE_FOR_ILL);
      put("18b", SamMessageInfo.ReasonUnfilled.NOT_HELD);
      put("18c", SamMessageInfo.ReasonUnfilled.NOT_ON_SHELF);
      put("18d", SamMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("18e", SamMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("18f", SamMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("18g", SamMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("18h", SamMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("18i", SamMessageInfo.ReasonUnfilled.POLICY_PROBLEM);
      put("18j", SamMessageInfo.ReasonUnfilled.NOT_AVAILABLE_FOR_ILL);
      put("18k", SamMessageInfo.ReasonUnfilled.NOT_AVAILABLE_FOR_ILL);
      put("21b", SamMessageInfo.ReasonUnfilled.NOT_AVAILABLE_FOR_ILL);
    }};
  }

  // Map from BLDSS event codes to ISO18626 ReasonForMessage
  // We could create this in a more concise way, but this is clearer (IMHO)
  // and maps directly onto the table 9.2 here:
  // https://apitest.bldss.bl.uk/docs/guide/appendix.html#orderlineUpdates
  // It's not clear from the BL docs what initiates some of these events, so
  // there's some best guesses here
  private Map<String, SamMessageInfo.ReasonForMessage> bldssCodeToReasonForMessage() {
    return new HashMap<String, SamMessageInfo.ReasonForMessage>() {{
      put("1", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("10", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("11", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("12", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("12a", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("13", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("14", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("15", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("16", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("17", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18a", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18b", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18c", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18d", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18e", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18f", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18g", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18h", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18i", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18j", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("18k", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("19", SamMessageInfo.ReasonForMessage.CANCEL_RESPONSE);
      put("1a", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("20b", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("20c", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("20d", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("21", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("21a", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("21b", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("22a", SamMessageInfo.ReasonForMessage.RENEW_RESPONSE);
      put("22b", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("23", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("24", SamMessageInfo.ReasonForMessage.RENEW_RESPONSE);
      put("25", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("25a", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("26", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("27", SamMessageInfo.ReasonForMessage.RENEW_RESPONSE);
      put("28", SamMessageInfo.ReasonForMessage.CANCEL_RESPONSE);
      put("29", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("2a", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("2b", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("2c", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("2d", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("2e", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("2f", SamMessageInfo.ReasonForMessage.CANCEL_RESPONSE);
      put("3a", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("4", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("47", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("48", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("49", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("4a", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("6", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("7a", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("7b", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("8", SamMessageInfo.ReasonForMessage.NOTIFICATION);
      put("9a", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("9b", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("9c", SamMessageInfo.ReasonForMessage.REQUEST_RESPONSE);
      put("9d", SamMessageInfo.ReasonForMessage.NOTIFICATION);
    }};
  }

  private Map<String, SamStatusInfo.Status> bldssCodeToStatus() {
    return new HashMap<String, SamStatusInfo.Status>() {{
      put("1", SamStatusInfo.Status.REQUEST_RECEIVED);
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
    }};
  }

  private String getEventCode(Document doc) {
    // Get the BLDSS event code
    Element eventType = (Element) this.xmlUtil.getNode(doc, "eventType");
    return eventType.getAttribute("id");
  }
}
