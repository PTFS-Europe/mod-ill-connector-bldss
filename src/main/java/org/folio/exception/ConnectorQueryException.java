package org.folio.exception;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.Error;

public class ConnectorQueryException extends RuntimeException {

  private final transient Error error;

  public ConnectorQueryException(String message) {
    super(StringUtils.isNotEmpty(message) ? message : "No message");
    this.error = new Error().withMessage(message);
  }

  public Error getError() {
    return error;
  }
}
