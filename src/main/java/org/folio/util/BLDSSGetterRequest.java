package org.folio.util;

import java.util.HashMap;

public class BLDSSGetterRequest extends BLDSSRequest{

  public BLDSSGetterRequest(String path, Boolean needsAuth) {
    super("getter", "GET", "/" + path, new HashMap<>(), needsAuth);
  }
}
