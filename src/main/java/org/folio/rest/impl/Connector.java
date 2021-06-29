package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.jaxrs.model.Request;
import org.folio.rest.jaxrs.resource.IllConnector;

import javax.ws.rs.core.Response;
import java.util.Map;

public class Connector implements IllConnector {
  @Override
  public void getIllConnectorSearch(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void getIllConnectorServices(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void getIllConnectorPrices(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void getIllConnectorRequestByRequestId(String requestId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void postIllConnectorRequestByRequestId(String requestId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void putIllConnectorRequestByRequestId(String requestId, String lang, Request entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void deleteIllConnectorRequestByRequestId(String requestId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void putIllConnectorRequestCancelByRequestId(String requestId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void putIllConnectorRequestRetryByRequestId(String requestId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void putIllConnectorRequestReminderByRequestId(String requestId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void putIllConnectorRequestRenewByRequestId(String requestId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void putIllConnectorRequestNotifyByRequestId(String requestId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }
}
