package org.folio.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Arrays;

/*
  Allow easy access to the components of a BLDSS response document
*/

public class BLDSSResponse {

  private final XMLUtil xmlUtil;
  private final Element result;
  private final Document doc;
  private final String status;

  public BLDSSResponse(String responseText) {
    this.xmlUtil = new XMLUtil();
    // Parse the response document into something we can use
    this.doc = xmlUtil.parse(responseText);
    // A response should contain a "result" element
    this.result = (Element) xmlUtil.getNode(doc, "result");
    Element statusEl = (Element) xmlUtil.getNode(doc, "status");
    this.status = statusEl.getTextContent();
  }

  public String getResponseType() {

    // The various types of response we could receive
    ArrayList<String> potentials = new ArrayList<>(Arrays.asList(
      "newOrder"
        /*
        TODO: Add handling for these other responses
        "availability",
        "orderline",
        "orderlines",
        "services",
        "scheme"
        */
    ));

    // Get all the children of "result" and see if any of our
    // potentials are there
    NodeList children = this.result.getChildNodes();
    Node found = null;
    for (int i=0; i < children.getLength(); i++) {
      if (potentials.contains(children.item(i).getNodeName())) {
        found = children.item(i);
      }
    }
    return found != null ? found.getNodeName() : null;
  }

  public String getCustomerReference() {
    Element el = (Element) this.xmlUtil.getNode(this.doc, "customerReference");
    return el.getTextContent();
  }

  public String getOrderline() {
    Element el = (Element) this.xmlUtil.getNode(this.doc, "orderline");
    return el.getTextContent();
  }

  public String getTimestamp() {
    Element el = (Element) this.xmlUtil.getNode(this.doc, "timestamp");
    return el.getTextContent();
  }

  public String getMessage() {
    Element el = (Element) this.xmlUtil.getNode(this.doc, "message");
    return el.getTextContent();
  }

  public String getStatus() {
    return this.status;
  }

  public String getEstimatedDespatchDate() {
    Element el = (Element) this.xmlUtil.getNode(this.doc, "estimatedDespatchDate");
    return el.getTextContent();
  }

  public String getNote() {
    Element el = (Element) this.xmlUtil.getNode(this.doc, "note");
    return el.getTextContent();
  }

}
