package org.folio.util;

import org.folio.exception.ConnectorQueryException;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class BLDSSResponse {
  private final XMLUtil parser;
  private final String xml;
  private String timestamp;
  private String status;
  private String message;
  private String result;

  public BLDSSResponse(String responseXML) {
    this.parser = new XMLUtil();
    this.xml = responseXML;
    this.prepareResponse();
  }

  public void prepareResponse() {
    Document responseDoc = this.parser.parse(this.xml);

    this.timestamp = this.getNode(responseDoc, "timestamp").getTextContent();
    this.status = this.getNode(responseDoc, "status").getTextContent();
    this.message = this.getNode(responseDoc, "message").getTextContent();
    this.result = this.getJsonResult();
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
    return this.result;
  }

  public String getJsonResult() {
    // We need to take the entire "result" node and convert it to JSON
    String output = "";
    StringWriter sw = new StringWriter();
    Document responseDoc = this.parser.parse(this.xml);
    Node result = this.getNode(responseDoc, "result");
    try {
      Transformer t = TransformerFactory.newInstance().newTransformer();
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      t.transform(new DOMSource(result), new StreamResult(sw));
      output = sw.toString();
    } catch (TransformerException e) {
      e.printStackTrace();
    }
    return XML.toJSONObject(output).get("result").toString();
  }
}
