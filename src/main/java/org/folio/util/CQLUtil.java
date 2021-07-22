package org.folio.util;

import org.folio.exception.ConnectorQueryException;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;

import java.io.IOException;

public class CQLUtil {

  /**
   * Parse a CQL string into a CQLNode
   *
   * @param cql The string containing the CQL query
   * @return CQLNode representing the passed CQL
   */
  public CQLNode cqlToNode (String cql) {
    try {
      CQLParser parser = new CQLParser();
      return parser.parse(cql);
    } catch (CQLParseException | IOException e) {
      throw new ConnectorQueryException(e.getMessage());
    }
  }

  /**
   * Parse a CQLNode to a XCQL string
   *
   * @param node The CQLNode to be returned
   * @return String An XCQL representation of the passed node
   */
  public String nodeToXCQL (CQLNode node) {
    try {
      return node.toXCQL();
    } catch(NullPointerException e) {
      throw new ConnectorQueryException(e.getMessage());
    }
  }

  /**
   * Parse a CQL String to an XCQL String
   *
   * @param cql The String representing the CQL query
   * @return String The String representing the parsed XCQL
   */
  public String parseToXCQL (String cql) {
    CQLNode node = cqlToNode(cql);
    return nodeToXCQL(node);
  }
}
