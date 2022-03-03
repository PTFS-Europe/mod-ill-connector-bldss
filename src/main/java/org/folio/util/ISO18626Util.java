package org.folio.util;

import org.folio.rest.jaxrs.model.BibliographicInfo;
import org.folio.rest.jaxrs.model.BibliographicRecordId;

import java.util.List;
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

  // Given a BibliographicInfo object, return the first matching identifier we find
  public String getIdentifierFromBibInfo(BibliographicInfo bibInfo, String findType) {
    List<BibliographicRecordId> identifiers = bibInfo.getBibliographicRecordId();
    for (int i = 0; i < identifiers.size(); i++) {
      BibliographicRecordId bibRecordId = identifiers.get(i);
      String idType = bibRecordId.getBibliographicRecordIdentifierCode().toString();
      if (idType.equals(findType)) {
        String idValue = bibRecordId.getBibliographicRecordIdentifier();
        if (idValue != null) {
          return idValue;
        }
      }
    }
    return null;
  }
}
