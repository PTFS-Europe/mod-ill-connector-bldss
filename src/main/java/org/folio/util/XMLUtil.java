package org.folio.util;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

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
}
