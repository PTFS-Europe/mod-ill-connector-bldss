package org.folio.util;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ISO18626Util {

  private static Map<String, String> typeMap;

  public ISO18626Util() {
    typeMap = Stream.of(new String[][] {
      { "Article", "article" },
      { "Book", "book" },
      { "Journal", "journal" },
      { "Newspaper", "newspaper" },
      { "ConferenceProc", "conference"},
      { "Thesis", "thesis" },
      { "MusicScore", "score" }
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
  }

  // Translate an ISO18626 "PublicationType" into a BLDSS "type"
  public String isoTypeToBldss(String isoType) {
    return typeMap.get(isoType);
  }

  // Translate a BLDSS "type" into an ISO18626 "PublicationType"
  public String bldssToIsoType(String bldss) {
    for (Map.Entry<String, String> entry: typeMap.entrySet()) {
      if (entry.getValue().equals(bldss)) {
        return entry.getKey();
      }
    }
    return null;
  }
}
