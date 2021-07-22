package org.folio.config;

import org.folio.service.search.SearchService;
import org.folio.service.search.SearchAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
  @Bean
  public SearchService illSearchService() {
    return new SearchAPI();
  }
}
