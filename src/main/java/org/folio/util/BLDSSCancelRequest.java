package org.folio.util;

import java.util.HashMap;

public class BLDSSCancelRequest extends BLDSSRequest{

  private final String localReqId;
  private final String supplierReqId;

  public BLDSSCancelRequest(String supplierRequestId, String requesterRequestId, HashMap<String, String> params) {
    super("cancel", "DELETE", "/orders/" + supplierRequestId, params, true);
    this.localReqId = requesterRequestId;
    this.supplierReqId = supplierRequestId;
  }

  public String getLocalReqId() {
    return localReqId;
  }

  public String getSupplierReqId() {
    return supplierReqId;
  }
}
