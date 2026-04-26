package com.dataforge.generators.spi;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.FieldConfig;

/**
 * 所有数据生成器的统一接口。
 *
 * <p>该接口定义了数据生成器的核心契约，支持泛型以确保类型安全。 所有具体的数据生成器实现都必须实现此接口。
 *
 * @param <T> 生成的数据类型 (例如: String, Integer, LocalDate)
 * @param <C> 该生成器特定的配置类，必须继承自 FieldConfig
 * @author DataForge Team
 * @since 1.0.0
 */
public interface DataGenerator<T, C extends FieldConfig> {

  /**
   * 返回该生成器能处理的类型名称，用于CLI或配置文件中的匹配。
   *
   * <p>类型名称应该是唯一的，且易于理解的字符串标识符。 例如: "idcard", "name", "phone", "email" 等。
   *
   * @return 数据类型标识符，不能为null或空字符串
   */
  String getType();

  /**
   * 根据配置和上下文生成数据。
   *
   * <p>这是数据生成的核心方法。实现时应该：
   *
   * <ul>
   *   <li>根据配置参数生成符合要求的数据
   *   <li>利用上下文实现字段间的关联性
   *   <li>确保生成的数据符合业务规则和校验要求
   *   <li>处理异常情况并提供有意义的错误信息
   * </ul>
   *
   * @param config 特定于此生成器的配置对象，包含生成参数
   * @param context 生成上下文，用于处理字段间的关联和共享数据
   * @return 生成的数据，类型为T
   * @throws IllegalArgumentException 当配置参数无效时
   * @throws RuntimeException 当生成过程中发生不可恢复的错误时
   */
  T generate(C config, DataForgeContext context);

  /**
   * 返回此生成器对应的配置类类型。
   *
   * <p>用于配置反序列化和类型检查。框架会使用此信息 将通用的配置映射转换为具体的配置对象。
   *
   * @return 配置类的Class对象，不能为null
   */
  Class<C> getConfigClass();

  /**
   * 验证配置参数的有效性。
   *
   * <p>默认实现返回true，子类可以重写此方法提供具体的验证逻辑。 建议在generate方法执行前调用此方法进行预检查。
   *
   * @param config 要验证的配置对象
   * @return 如果配置有效返回true，否则返回false
   */
  default boolean isValidConfig(C config) {
    return config != null;
  }

  /**
   * 获取生成器的描述信息。
   *
   * <p>用于CLI帮助信息和文档生成。默认返回类型名称， 子类可以重写提供更详细的描述。
   *
   * @return 生成器的描述信息
   */
  default String getDescription() {
    return "Generator for " + getType() + " data type";
  }

  /**
   * 检查生成器是否为无状态生成器。
   *
   * <p>无状态生成器可以安全地实现为单例，并在多个线程间共享。 有状态生成器每次调用都应该创建新实例或使用对象池。
   *
   * <p>默认返回true，表示大多数生成器是无状态的。 有状态的生成器应该重写此方法返回false。
   *
   * @return 如果为无状态生成器返回true，否则返回false
   */
  default boolean isStateless() {
    return true;
  }
}
