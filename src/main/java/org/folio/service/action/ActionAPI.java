package org.folio.service.action;

import io.vertx.core.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.ActionMetadata;
import org.folio.rest.jaxrs.model.ActionResponse;
import org.folio.rest.jaxrs.model.ConfirmationHeader;
import org.folio.rest.jaxrs.model.Header;
import org.folio.util.BLDSSRequest;
import org.folio.util.DateTimeUtils;
import org.folio.util.XMLUtil;
import org.w3c.dom.Document;

import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import static org.folio.config.Constants.ISO18626_DATE_FORMAT;

public class ActionAPI implements ActionService {

  protected final Logger logger = LogManager.getLogger(this.getClass());

  @Override
  public CompletableFuture<ActionResponse> performAction(String actionName, ActionMetadata payload, Context context, Map<String, String> headers) {
    String path =  "/orders";
    HashMap<String, String> params = new HashMap<>();
    CompletableFuture<ActionResponse> future = new CompletableFuture<>();
    BLDSSRequest req = new BLDSSRequest("POST", path, params);
    req.setPayload(payload);
    req.makeRequest().thenApply(respObj -> {
      ActionResponse actionResponse = prepareResponse(respObj, req);
      future.complete(actionResponse);
      return actionResponse;
    });

    return future;
  }

  @Override
  public ActionResponse prepareResponse(HttpResponse<String> response, BLDSSRequest request) {
    XMLUtil xmlUtil = new XMLUtil();

    ActionResponse actionResponse = new ActionResponse();
    Document bodyDoc = xmlUtil.parse(response.body());

    Header requestHeader = request.getActionPayload().getHeader();

    String received = xmlUtil.getNode(bodyDoc, "timestamp").getTextContent();
    String timestampReceived = DateTimeUtils.bldssToIso(received);

    String statusString = xmlUtil.getNode(bodyDoc, "status").getTextContent();

    ConfirmationHeader.MessageStatus messageStatus = statusString.equals("0") ?
      ConfirmationHeader.MessageStatus.OK :
      ConfirmationHeader.MessageStatus.ERROR;

    // Build most of our ConfirmationHeader
    ConfirmationHeader confirmationHeader = new ConfirmationHeader()
      .withSupplyingAgencyId(requestHeader.getSupplyingAgencyId())
      .withRequestingAgencyId(requestHeader.getRequestingAgencyId())
      .withTimestamp(DateTimeUtils.dtToString(ZonedDateTime.now(), ISO18626_DATE_FORMAT))
      .withRequestingAgencyRequestId(requestHeader.getRequestingAgencyRequestId())
      .withTimestampReceived(timestampReceived)
      .withMessageStatus(messageStatus);

    // We have an error
    if (!statusString.equals("0")) {
      String blError = xmlUtil.getNode(bodyDoc, "message").getTextContent();
      confirmationHeader.setErrorData(blError);
    }

    actionResponse.setHeader(confirmationHeader);
    return actionResponse;
  }

}
