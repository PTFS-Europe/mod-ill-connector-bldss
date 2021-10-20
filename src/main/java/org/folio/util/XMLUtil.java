package org.folio.util;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class XMLUtil {

  private DocumentBuilder db;
  private Document doc;

  public void setDb(DocumentBuilder db) {
    this.db = db;
  }

  public void setDoc(Document doc) {
    this.doc = doc;
  }

  public Document getDoc() {
    return doc;
  }

  public XMLUtil() {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      this.setDb(dbf.newDocumentBuilder());
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }

  }

  public Document parse(String xml) {
    try {
      Document parsed = this.db.parse(new InputSource(new StringReader(xml)));
      parsed.getDocumentElement().normalize();
      this.setDoc(parsed);
    } catch(IOException | SAXException e) {
      e.printStackTrace();
    }
    return this.getDoc();
  }

  public String docAsString(Document doc) {
    StringWriter writer = new StringWriter();
    try {
      DOMSource domSource = new DOMSource(doc);
      StreamResult result = new StreamResult(writer);
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.transform(domSource, result);
    } catch(TransformerException e) {
      e.printStackTrace();
    }
    return writer.toString();
  }
}
