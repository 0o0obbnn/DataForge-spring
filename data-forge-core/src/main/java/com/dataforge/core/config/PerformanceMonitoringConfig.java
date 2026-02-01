package com.dataforge.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 性能监控配置类。
 *
 * <p>启用AspectJ自动代理，确保性能监控切面能够正常工作。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Configuration
@EnableAspectJAutoProxy
public class PerformanceMonitoringConfig {

  // 配置类主要用于启用AspectJ自动代理
  // 具体的Bean配置在各自的组件类中通过@Service、@Component等注解完成

}
