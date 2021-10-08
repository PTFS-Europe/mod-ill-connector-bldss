package org.folio.util;

import org.folio.exception.ConnectorQueryException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BLDSSResponse {
  private String xml;
  private String timestamp;
  private String status;
  private String message;
  private String result;

  public BLDSSResponse(String responseXML) {
    this.xml = responseXML;
    this.prepareResponse();
  }

  public void prepareResponse() {

    XMLUtil parser = new XMLUtil();
    Document responseDoc = parser.parse(this.xml);

    this.timestamp = this.getNode(responseDoc, "timestamp").getTextContent();
    this.status = this.getNode(responseDoc, "status").getTextContent();
    this.message = this.getNode(responseDoc, "message").getTextContent();

    Node result = this.getNode(responseDoc, "result");
    JSONObject xmlJsonObj = XML.toJSONObject(result.toString());
    this.result = xmlJsonObj.toString();
  }

  private Node getNode(Document doc, String nodeName) {
    NodeList nodes = doc.getElementsByTagName(nodeName);
    if (nodes.getLength() != 1) {
      throw(new ConnectorQueryException("Unexpected number of response " + nodeName + " elements: " + nodes.getLength()));
    }
    return nodes.item(0);
  }

  public String getTimestamp() {
    return timestamp;
  }

  public String getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public String getResult() {
    return result;
  }
}
