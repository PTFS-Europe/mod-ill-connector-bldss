package org.folio.config;

import java.util.ArrayList;
import java.util.Arrays;

public class Constants {

  private Constants() {
  }

  public static final String CONNECTOR_NAME = "British Library Document Supply";
  public static final ArrayList<String> CONNECTOR_ABILITIES = new ArrayList<>(
    Arrays.asList("ill-connector-search", "ill-connector-action", "ill-connector-sa-update")
  );

  public static final String ID = "id";
  public static final String OKAPI_URL = "x-okapi-url";
  public static final String BLDSS_TEST_API_URL = "https://apitest.bldss.bl.uk";
  public static final String CALLBACK_URL = "http://195.166.150.188:9130/ill-connector/sa-update";
  public static final String BLDSS_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS z";
  public static final String BLDSS_REQUEST_RESPONSE_DATE_FORMAT = "dd/MM/yyyy";
  public static final String ISO18626_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

  public static final String EMPTY_ARRAY = "[]";
}
