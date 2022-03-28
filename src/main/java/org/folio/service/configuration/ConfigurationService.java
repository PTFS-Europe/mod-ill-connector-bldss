package org.folio.service.configuration;

import io.vertx.core.json.JsonObject;
import org.folio.common.OkapiParams;
import org.folio.rest.impl.BaseApi;
import org.folio.rest.jaxrs.model.Config;
import org.folio.rest.jaxrs.model.Configs;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.folio.config.Constants.TENANT_CONFIGURATION_ENTRIES;

public class ConfigurationService extends BaseApi {
  private final HttpClient client;

  public ConfigurationService() {
    this.client = HttpClient.newBuilder().build();
  }

  // Retrieve all configs for this module, we don't need to get any more discerning than that
  public Configs getConfigurationsEntries(Map<String, String> headers, String module) {
    OkapiParams okapiParams = new OkapiParams(headers);
    String query = String.format("query=module=%s", module);
    String endpoint = okapiParams.getUrl() + "/" + TENANT_CONFIGURATION_ENTRIES + "?" + query;
    HttpRequest.Builder builder = HttpRequest.newBuilder()
      .uri(URI.create(endpoint))
      .GET();

    // Add our existing headers
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      builder.header(entry.getKey(), entry.getValue());
    }

    HttpRequest request = builder.build();

    try {
      HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
      JsonObject jsonObject = new JsonObject(response.body());
      return jsonObject.mapTo(Configs.class);
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      return null;
    }
  }

  public Config getConfigurationEntry(String configName, Map<String, String> headers, String module) {
    Configs configs = getConfigurationsEntries(headers, module);
    for (Config config : configs.getConfigs()) {
      String name = config.getConfigName();
      if (name.equals(configName)) {
        return config;
      }
    }
    return null;
  }
}
