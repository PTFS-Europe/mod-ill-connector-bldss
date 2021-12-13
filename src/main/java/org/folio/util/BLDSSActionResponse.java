package org.folio.util;

import org.folio.rest.jaxrs.model.ActionResponse;

public class BLDSSActionResponse {
  private final String actionResponseString;
  private final ActionResponse actionResponse;
  private final BLDSSRequest bldssRequest;

  public BLDSSActionResponse(String actionResponseString, ActionResponse actionResponse, BLDSSRequest bldssRequest) {
    this.actionResponseString = actionResponseString;
    this.actionResponse = actionResponse;
    this.bldssRequest = bldssRequest;
  }

  public String getActionResponseString() {
    return actionResponseString;
  }

  public ActionResponse getActionResponse() {
    return actionResponse;
  }

  public BLDSSRequest getBldssRequest() {
    return bldssRequest;
  }
}
