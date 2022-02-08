package org.folio.config;

import org.folio.service.action.ActionAPI;
import org.folio.service.action.ActionService;
import org.folio.service.getter.ConnectorGetter;
import org.folio.service.getter.GetterService;
import org.folio.service.search.SearchAPI;
import org.folio.service.search.SearchService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
  @Bean
  public SearchService illSearchService() {
    return new SearchAPI();
  }
  @Bean
  public ActionService illActionService() {
    return new ActionAPI();
  }
  @Bean
  public GetterService illGetterService() {
    return new ConnectorGetter();
  }
}
