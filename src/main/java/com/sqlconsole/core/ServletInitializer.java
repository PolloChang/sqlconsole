package com.sqlconsole.core;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class ServletInitializer extends SpringBootServletInitializer {

  /**
   * Configures the application when running in a servlet container.
   *
   * @param application the application builder
   * @return the configured application builder
   */
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(SqlConsoleApplication.class);
  }
}
