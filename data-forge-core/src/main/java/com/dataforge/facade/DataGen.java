package com.dataforge.facade;

import com.dataforge.config.SimpleFieldConfig;
import com.dataforge.core.DataForgeContext;
import com.dataforge.core.GeneratorFactory;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * DataGen 主门面类
 *
 * <p>提供简洁的 API 来生成各种测试数据，使用方式：gen.name(), gen.email() 等
 *
 * <p>示例：
 *
 * <pre>
 * DataGen gen = new DataGen();
 * String name = gen.name();
 * String email = gen.email();
 * List&lt;String&gt; names = gen.names(10);
 * </pre>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class DataGen {

  private final GeneratorFactory generatorFactory;
  private final DataForgeContext context;

  // 分类门面（延迟初始化）
  private PersonGen personGen;
  private InternetGen internetGen;
  private AddressGen addressGen;
  private FinanceGen financeGen;
  private DateTimeGen dateTimeGen;

  /** 构造函数（支持 Spring 依赖注入） */
  public DataGen(GeneratorFactory generatorFactory) {
    this.generatorFactory = generatorFactory;
    this.context = new DataForgeContext();
  }

  /** 构造函数（无参，用于非 Spring 环境） */
  public DataGen() {
    this.generatorFactory = new GeneratorFactory();
    this.context = new DataForgeContext();
  }

  // ==================== 分类门面访问器 ====================

  /** 获取个人信息生成器 */
  public PersonGen person() {
    if (personGen == null) {
      personGen = new PersonGen(this);
    }
    return personGen;
  }

  /** 获取互联网数据生成器 */
  public InternetGen internet() {
    if (internetGen == null) {
      internetGen = new InternetGen(this);
    }
    return internetGen;
  }

  /** 获取地址信息生成器 */
  public AddressGen address() {
    if (addressGen == null) {
      addressGen = new AddressGen(this);
    }
    return addressGen;
  }

  /** 获取金融数据生成器 */
  public FinanceGen finance() {
    if (financeGen == null) {
      financeGen = new FinanceGen(this);
    }
    return financeGen;
  }

  /** 获取日期时间生成器 */
  public DateTimeGen dateTime() {
    if (dateTimeGen == null) {
      dateTimeGen = new DateTimeGen(this);
    }
    return dateTimeGen;
  }

  // ==================== 基础生成方法 ====================

  /** 生成 UUID */
  public String uuid() {
    return generate("uuid");
  }

  /** 生成姓名 */
  public String name() {
    return generate("name");
  }

  /** 生成姓名（指定类型和性别） */
  public String name(String type, String gender) {
    return generate("name", Map.of("type", type, "gender", gender));
  }

  /** 生成电话号码 */
  public String phone() {
    return generate("phone");
  }

  /** 生成电话号码（指定国家） */
  public String phone(String country) {
    return generate("phone", Map.of("country", country));
  }

  /** 生成邮箱 */
  public String email() {
    return generate("email");
  }

  /** 生成邮箱（指定域名） */
  public String email(String domain) {
    return generate("email", Map.of("domain", domain));
  }

  /** 生成银行卡号 */
  public String bankCard() {
    return generate("bankcard");
  }

  /** 生成完整地址 */
  public String fullAddress() {
    return generate("address");
  }

  /** 生成整数 */
  public Integer integer() {
    return Integer.parseInt(generate("random_number"));
  }

  /** 生成整数（指定范围） */
  public Integer integer(int min, int max) {
    return Integer.parseInt(generate("random_number", Map.of("min", min, "max", max)));
  }

  /** 生成小数 */
  public Double decimal() {
    return Double.parseDouble(generate("decimal"));
  }

  /** 生成小数（指定范围和精度） */
  public Double decimal(double min, double max, int scale) {
    return Double.parseDouble(generate("decimal", Map.of("min", min, "max", max, "scale", scale)));
  }

  // ==================== 批量生成方法 ====================

  /** 批量生成 UUID */
  public List<String> uuids(int count) {
    return generateList("uuid", count);
  }

  /** 批量生成姓名 */
  public List<String> names(int count) {
    return generateList("name", count);
  }

  /** 批量生成电话 */
  public List<String> phones(int count) {
    return generateList("phone", count);
  }

  /** 批量生成邮箱 */
  public List<String> emails(int count) {
    return generateList("email", count);
  }

  /** 批量生成银行卡 */
  public List<String> bankCards(int count) {
    return generateList("bankcard", count);
  }

  // ==================== 核心生成方法 ====================

  /** 生成数据（无参数） */
  protected String generate(String type) {
    return generate(type, new HashMap<>());
  }

  /** 生成数据（带参数） */
  @SuppressWarnings("unchecked")
  protected String generate(String type, Map<String, Object> params) {
    DataGenerator<Object, FieldConfig> generator =
        (DataGenerator<Object, FieldConfig>) generatorFactory.getGenerator(type);

    if (generator == null) {
      throw new IllegalArgumentException("No generator found for type: " + type);
    }

    SimpleFieldConfig config = new SimpleFieldConfig("temp", type);
    if (params != null && !params.isEmpty()) {
      config.setParams(params);
    }

    Object result = generator.generate(config, context);
    return result != null ? result.toString() : null;
  }

  /** 批量生成数据 */
  protected List<String> generateList(String type, int count) {
    return generateList(type, count, new HashMap<>());
  }

  /** 批量生成数据（带参数） */
  protected List<String> generateList(String type, int count, Map<String, Object> params) {
    List<String> results = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      results.add(generate(type, params));
    }
    return results;
  }
}
