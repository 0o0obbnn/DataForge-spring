package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.util.DataLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 职业/职位生成器
 *
 * <p>支持的参数： - industry: 行业类型 (IT|FINANCE|EDUCATION|HEALTHCARE|MANUFACTURING|RETAIL|ANY) - level:
 * 职位层级 (JUNIOR|SENIOR|MANAGER|DIRECTOR|EXECUTIVE|ANY) - file: 自定义职业列表文件路径 - weights: 职业权重配置 (如
 * "软件工程师:10,产品经理:5")
 *
 * @author DataForge
 */
public class OccupationGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(OccupationGenerator.class);
  private static final Random random = new Random();

  // 按行业分类的职业数据
  private static final Map<String, Map<String, List<String>>> OCCUPATIONS_BY_INDUSTRY =
      new HashMap<>();

  static {
    initializeOccupations();
  }

  private static void initializeOccupations() {
    // IT行业
    Map<String, List<String>> itOccupations = new HashMap<>();
    itOccupations.put(
        "JUNIOR",
        Arrays.asList(
            "初级软件工程师", "前端开发工程师", "后端开发工程师", "测试工程师", "运维工程师", "UI设计师", "产品助理", "数据分析师", "技术支持工程师",
            "系统管理员"));
    itOccupations.put(
        "SENIOR",
        Arrays.asList(
            "高级软件工程师",
            "架构师",
            "高级前端工程师",
            "高级后端工程师",
            "高级测试工程师",
            "DevOps工程师",
            "高级UI设计师",
            "产品经理",
            "高级数据分析师",
            "技术专家"));
    itOccupations.put(
        "MANAGER",
        Arrays.asList(
            "技术经理", "项目经理", "产品总监", "研发经理", "测试经理", "运维经理", "设计总监", "数据总监", "技术总监", "部门经理"));
    itOccupations.put("DIRECTOR", Arrays.asList("技术总监", "研发总监", "产品副总裁", "工程总监", "创新总监"));
    itOccupations.put("EXECUTIVE", Arrays.asList("首席技术官", "首席产品官", "首席信息官", "副总裁", "总经理"));
    OCCUPATIONS_BY_INDUSTRY.put("IT", itOccupations);

    // 金融行业
    Map<String, List<String>> financeOccupations = new HashMap<>();
    financeOccupations.put(
        "JUNIOR",
        Arrays.asList(
            "银行柜员", "客户经理", "信贷员", "风控专员", "财务分析师", "投资顾问助理", "保险代理人", "证券经纪人", "会计", "出纳"));
    financeOccupations.put(
        "SENIOR",
        Arrays.asList(
            "高级客户经理", "高级信贷经理", "风控经理", "高级财务分析师", "投资顾问", "保险经理", "证券分析师", "高级会计师", "财务经理",
            "审计师"));
    financeOccupations.put(
        "MANAGER",
        Arrays.asList(
            "支行行长", "信贷部经理", "风控总监", "财务总监", "投资经理", "保险部经理", "证券部经理", "会计主管", "审计经理", "合规经理"));
    financeOccupations.put("DIRECTOR", Arrays.asList("分行行长", "风控总监", "财务总监", "投资总监", "运营总监"));
    financeOccupations.put("EXECUTIVE", Arrays.asList("总行行长", "首席风险官", "首席财务官", "首席投资官", "副总裁"));
    OCCUPATIONS_BY_INDUSTRY.put("FINANCE", financeOccupations);

    // 教育行业
    Map<String, List<String>> educationOccupations = new HashMap<>();
    educationOccupations.put(
        "JUNIOR",
        Arrays.asList(
            "小学教师", "中学教师", "幼儿园教师", "培训师", "教学助理", "辅导员", "班主任", "体育教师", "音乐教师", "美术教师"));
    educationOccupations.put(
        "SENIOR",
        Arrays.asList(
            "高级教师", "学科带头人", "教研员", "高级培训师", "教务主任", "年级主任", "教学主管", "课程设计师", "教育顾问", "学术研究员"));
    educationOccupations.put(
        "MANAGER",
        Arrays.asList(
            "教务处长", "学生处长", "系主任", "培训部经理", "教学总监", "学术主任", "研究所所长", "教育项目经理", "校区负责人", "部门主管"));
    educationOccupations.put("DIRECTOR", Arrays.asList("副校长", "教学副校长", "学术副校长", "教育总监", "研究院院长"));
    educationOccupations.put("EXECUTIVE", Arrays.asList("校长", "院长", "教育集团总裁", "首席教育官", "董事长"));
    OCCUPATIONS_BY_INDUSTRY.put("EDUCATION", educationOccupations);

    // 医疗行业
    Map<String, List<String>> healthcareOccupations = new HashMap<>();
    healthcareOccupations.put(
        "JUNIOR",
        Arrays.asList("住院医师", "护士", "药师", "医技师", "康复师", "营养师", "心理咨询师", "医学检验师", "放射技师", "麻醉师"));
    healthcareOccupations.put(
        "SENIOR",
        Arrays.asList(
            "主治医师", "主管护师", "主管药师", "高级医技师", "高级康复师", "高级营养师", "心理治疗师", "高级检验师", "高级放射师", "主管麻醉师"));
    healthcareOccupations.put(
        "MANAGER",
        Arrays.asList(
            "科室主任", "护士长", "药剂科主任", "医技科主任", "康复科主任", "营养科主任", "心理科主任", "检验科主任", "放射科主任", "麻醉科主任"));
    healthcareOccupations.put("DIRECTOR", Arrays.asList("医务处长", "护理部主任", "医院副院长", "医疗总监", "学科带头人"));
    healthcareOccupations.put("EXECUTIVE", Arrays.asList("院长", "医疗集团总裁", "首席医疗官", "医院董事长", "卫生局长"));
    OCCUPATIONS_BY_INDUSTRY.put("HEALTHCARE", healthcareOccupations);

    // 制造业
    Map<String, List<String>> manufacturingOccupations = new HashMap<>();
    manufacturingOccupations.put(
        "JUNIOR",
        Arrays.asList("生产工人", "质检员", "设备操作员", "仓库管理员", "采购员", "销售代表", "技术员", "维修工", "包装工", "物流专员"));
    manufacturingOccupations.put(
        "SENIOR",
        Arrays.asList(
            "生产主管", "质量工程师", "设备工程师", "仓储主管", "采购主管", "销售经理", "工艺工程师", "维修主管", "物流经理", "生产计划员"));
    manufacturingOccupations.put(
        "MANAGER",
        Arrays.asList(
            "生产经理", "质量经理", "设备经理", "仓储经理", "采购经理", "销售总监", "技术经理", "维修经理", "物流总监", "车间主任"));
    manufacturingOccupations.put(
        "DIRECTOR", Arrays.asList("生产总监", "质量总监", "技术总监", "供应链总监", "运营总监"));
    manufacturingOccupations.put("EXECUTIVE", Arrays.asList("总经理", "副总裁", "首席运营官", "工厂厂长", "集团总裁"));
    OCCUPATIONS_BY_INDUSTRY.put("MANUFACTURING", manufacturingOccupations);

    // 零售行业
    Map<String, List<String>> retailOccupations = new HashMap<>();
    retailOccupations.put(
        "JUNIOR",
        Arrays.asList("销售员", "收银员", "导购员", "客服专员", "理货员", "店员", "促销员", "配送员", "仓库管理员", "美工"));
    retailOccupations.put(
        "SENIOR",
        Arrays.asList(
            "销售主管", "客服主管", "店长", "区域销售", "采购专员", "商品经理", "营销专员", "运营专员", "视觉陈列师", "培训师"));
    retailOccupations.put(
        "MANAGER",
        Arrays.asList(
            "销售经理", "客服经理", "区域经理", "采购经理", "商品总监", "营销经理", "运营经理", "门店经理", "品牌经理", "渠道经理"));
    retailOccupations.put("DIRECTOR", Arrays.asList("销售总监", "运营总监", "商品总监", "营销总监", "品牌总监"));
    retailOccupations.put("EXECUTIVE", Arrays.asList("总经理", "副总裁", "首席营销官", "首席运营官", "董事长"));
    OCCUPATIONS_BY_INDUSTRY.put("RETAIL", retailOccupations);
  }

  @Override
  public String getType() {
    return "occupation";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String industry = config.getParam("industry", String.class, "ANY");
      String level = config.getParam("level", String.class, "ANY");
      String weightsParam = config.getParam("weights", String.class, null);

      // 加载职业数据
      List<String> occupations = loadOccupations(config, industry, level);

      // 应用权重选择
      String occupation;
      if (weightsParam != null && !weightsParam.isEmpty()) {
        occupation = selectWithWeights(occupations, weightsParam);
      } else {
        occupation = occupations.get(random.nextInt(occupations.size()));
      }

      logger.debug("Generated occupation: {}", occupation);
      return occupation;

    } catch (Exception e) {
      logger.error("Error generating occupation", e);
      return "软件工程师";
    }
  }

  private List<String> loadOccupations(FieldConfig config, String industry, String level) {
    String customFile = config.getParam("file", String.class, null);
    if (customFile != null) {
      try {
        return DataLoader.loadDataFromFile(customFile);
      } catch (Exception e) {
        logger.warn("Failed to load custom occupation file: {}", customFile, e);
      }
    }

    // 使用内置职业数据
    List<String> occupations = new ArrayList<>();

    if ("ANY".equals(industry)) {
      // 混合所有行业
      for (Map<String, List<String>> industryOccupations : OCCUPATIONS_BY_INDUSTRY.values()) {
        if ("ANY".equals(level)) {
          // 混合所有级别
          industryOccupations.values().forEach(occupations::addAll);
        } else {
          // 指定级别
          List<String> levelOccupations = industryOccupations.get(level);
          if (levelOccupations != null) {
            occupations.addAll(levelOccupations);
          }
        }
      }
    } else {
      // 指定行业
      Map<String, List<String>> industryOccupations = OCCUPATIONS_BY_INDUSTRY.get(industry);
      if (industryOccupations != null) {
        if ("ANY".equals(level)) {
          // 混合所有级别
          industryOccupations.values().forEach(occupations::addAll);
        } else {
          // 指定级别
          List<String> levelOccupations = industryOccupations.get(level);
          if (levelOccupations != null) {
            occupations.addAll(levelOccupations);
          }
        }
      }
    }

    // 如果没有找到合适的职业，使用默认列表
    if (occupations.isEmpty()) {
      occupations.addAll(OCCUPATIONS_BY_INDUSTRY.get("IT").get("JUNIOR"));
    }

    return occupations;
  }

  private String selectWithWeights(List<String> occupations, String weightsParam) {
    Map<String, Integer> weights = parseWeights(weightsParam);

    // 计算总权重
    int totalWeight = 0;
    for (String occupation : occupations) {
      totalWeight += weights.getOrDefault(occupation, 1);
    }

    // 随机选择
    int randomValue = random.nextInt(totalWeight);
    int currentWeight = 0;

    for (String occupation : occupations) {
      currentWeight += weights.getOrDefault(occupation, 1);
      if (randomValue < currentWeight) {
        return occupation;
      }
    }

    // 默认返回第一个
    return occupations.get(0);
  }

  private Map<String, Integer> parseWeights(String weightsParam) {
    Map<String, Integer> weights = new HashMap<>();

    try {
      String[] pairs = weightsParam.split(",");
      for (String pair : pairs) {
        String[] parts = pair.split(":");
        if (parts.length == 2) {
          String occupation = parts[0].trim();
          int weight = Integer.parseInt(parts[1].trim());
          weights.put(occupation, weight);
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to parse weights: {}", weightsParam, e);
    }

    return weights;
  }
}
