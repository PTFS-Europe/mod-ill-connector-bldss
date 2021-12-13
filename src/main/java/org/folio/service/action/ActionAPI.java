package org.folio.service.action;

import io.vertx.core.Context;
import org.folio.rest.jaxrs.model.ActionMetadata;
import org.folio.rest.jaxrs.model.ActionResponse;
import org.folio.rest.jaxrs.model.ConfirmationHeader;
import org.folio.rest.jaxrs.model.Header;
import org.folio.util.BLDSSActionResponse;
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

  @Override
  // Perform the action and return a CompletableAction that completes to
  // a BLDSSActionResponse object
  // We do this because we may need to initiate a side-effect API call that
  // requires properties from the original request and the response
  public CompletableFuture<BLDSSActionResponse> performAction(String actionName, ActionMetadata payload, Context context, Map<String, String> headers) {
    String path =  "/orders";
    HashMap<String, Object> toReturn = new HashMap<>();
    HashMap<String, String> params = new HashMap<>();
    CompletableFuture<BLDSSActionResponse> future = new CompletableFuture<>();
    BLDSSRequest req = new BLDSSRequest("POST", path, params);
    req.setPayload(payload);
    req.makeRequest().thenApply(respObj -> {
      BLDSSActionResponse actionResponse = new BLDSSActionResponse(
        respObj.body(),
        prepareResponse(respObj, req),
        req
      );
      future.complete(actionResponse);
      return actionResponse;
    });
    // We received a "result" element in the response, we use that to send a
    // message containing the request result
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
