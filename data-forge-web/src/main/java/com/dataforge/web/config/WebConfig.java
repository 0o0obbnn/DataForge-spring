package com.dataforge.web.config;

import com.dataforge.web.filter.RequestIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web配置类。
 *
 * <p>配置Web相关的组件，包括过滤器、拦截器等。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Configuration
public class WebConfig {

  /**
   * 注册请求ID追踪过滤器。
   *
   * @param requestIdFilter 请求ID过滤器
   * @return FilterRegistrationBean 过滤器注册Bean
   */
  @Bean
  public FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration(
      RequestIdFilter requestIdFilter) {
    FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(requestIdFilter);
    registration.addUrlPatterns("/*");
    registration.setName("requestIdFilter");
    registration.setOrder(1);
    return registration;
  }
}
