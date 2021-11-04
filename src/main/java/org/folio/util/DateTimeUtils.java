package org.folio.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import static org.folio.config.Constants.BLDSS_DATE_FORMAT;
import static org.folio.config.Constants.ISO18626_DATE_FORMAT;

public class DateTimeUtils {

  public static ZonedDateTime stringToDt(String input) {
    return ZonedDateTime.parse(input);
  }

  public static ZonedDateTime stringToDt(String input, String pattern) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
    return ZonedDateTime.parse(input, formatter);
  }

  public static String dtToString(ZonedDateTime input, String pattern) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
    return input.format(formatter);
  }

  public static String bldssToIso(String input) {
    ZonedDateTime bldss = stringToDt(input, BLDSS_DATE_FORMAT);
    return dtToString(bldss, ISO18626_DATE_FORMAT);
  }

  public static String isoToBldss(String input) {
    ZonedDateTime iso = stringToDt(input, ISO18626_DATE_FORMAT);
    return dtToString(iso, BLDSS_DATE_FORMAT);
  }

}
