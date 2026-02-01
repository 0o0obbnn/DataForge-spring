package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 传真号码生成器
 *
 * <p>支持的参数： - region: 地区 (CN|US|UK|JP|ANY) - area_code: 区号 (指定区号或随机) - format: 输出格式
 * (STANDARD|INTERNATIONAL|COMPACT) - include_extension: 是否包含分机号 (true|false) - extension_length:
 * 分机号长度 (2-6) - prefix: 自定义前缀
 *
 * @author DataForge
 */
public class FaxGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(FaxGenerator.class);
  private static final Random random = new Random();

  // 输出格式枚举
  private enum FaxFormat {
    STANDARD, // 标准格式：区号-号码
    INTERNATIONAL, // 国际格式：+国家码-区号-号码
    COMPACT // 紧凑格式：区号号码（无分隔符）
  }

  // 国家代码和区号映射
  private static final Map<String, CountryInfo> COUNTRY_INFO = new HashMap<>();

  // 中国常用区号
  private static final List<String> CHINA_AREA_CODES =
      Arrays.asList(
          "010", "021", "022", "023", "024", "025", "027", "028", "029", "0311", "0312", "0313",
          "0314", "0315", "0316", "0317", "0318", "0319", "0335", "0349", "0351", "0352", "0353",
          "0354", "0355", "0356", "0357", "0358", "0359", "0371", "0372", "0373", "0374", "0375",
          "0376", "0377", "0378", "0379", "0391", "0392", "0393", "0394", "0395", "0396", "0398",
          "0411", "0412", "0413", "0414", "0415", "0416", "0417", "0418", "0419", "0421", "0427",
          "0429", "0431", "0432", "0433", "0434", "0435", "0436", "0437", "0438", "0439", "0451",
          "0452", "0453", "0454", "0455", "0456", "0457", "0458", "0459", "0464", "0467", "0468",
          "0469", "0471", "0472", "0473", "0474", "0475", "0476", "0477", "0478", "0479", "0482",
          "0483", "0511", "0512", "0513", "0514", "0515", "0516", "0517", "0518", "0519", "0523",
          "0527", "0528", "0531", "0532", "0533", "0534", "0535", "0536", "0537", "0538", "0539",
          "0543", "0546", "0547", "0548", "0549", "0551", "0552", "0553", "0554", "0555", "0556",
          "0557", "0558", "0559", "0561", "0562", "0563", "0564", "0565", "0566");

  // 国家信息类
  private static class CountryInfo {
    final String countryCode;
    final List<String> areaCodes;
    final int numberLength;

    CountryInfo(String countryCode, List<String> areaCodes, int numberLength) {
      this.countryCode = countryCode;
      this.areaCodes = areaCodes;
      this.numberLength = numberLength;
    }
  }

  static {
    initializeCountryInfo();
  }

  private static void initializeCountryInfo() {
    // 中国
    COUNTRY_INFO.put("CN", new CountryInfo("+86", CHINA_AREA_CODES, 8));

    // 美国
    COUNTRY_INFO.put(
        "US",
        new CountryInfo(
            "+1",
            Arrays.asList(
                "212", "213", "214", "215", "216", "217", "218", "219", "301", "302", "303", "304",
                "305", "307", "308", "309", "310", "312", "313", "314", "315", "316", "317", "318",
                "319", "320", "321", "323", "330", "334", "336", "337", "339", "347", "351", "352",
                "360", "361", "386", "401", "402", "404", "405", "406", "407", "408", "409", "410",
                "412", "413", "414", "415", "417", "419", "423", "424", "425", "430", "432", "434",
                "435", "440", "443", "469", "470", "475", "478", "479", "480", "484", "501", "502",
                "503", "504", "505", "507", "508", "509", "510", "512", "513", "515", "516", "517",
                "518", "520", "530", "540", "541", "551", "559", "561", "562", "563", "564", "567",
                "570", "571", "573", "574", "575", "580", "585", "586", "601", "602", "603", "605",
                "606", "607", "608", "609", "610", "612", "614", "615", "616", "617", "618", "619",
                "620", "623", "626", "630", "631", "636", "641", "646", "650", "651", "660", "661",
                "662", "667", "678", "682", "701", "702", "703", "704", "706", "707", "708", "712",
                "713", "714", "715", "716", "717", "718", "719", "720", "724", "727", "731", "732",
                "734", "737", "740", "754", "757", "760", "763", "765", "770", "772", "773", "774",
                "775", "781", "785", "786", "801", "802", "803", "804", "805", "806", "808", "810",
                "812", "813", "814", "815", "816", "817", "818", "828", "830", "831", "832", "843",
                "845", "847", "848", "850", "856", "857", "858", "859", "860", "862", "863", "864",
                "865", "870", "878", "901", "903", "904", "906", "907", "908", "909", "910", "912",
                "913", "914", "915", "916", "917", "918", "919", "920", "925", "928", "929", "931",
                "936", "937", "940", "941", "947", "949", "951", "952", "954", "956", "970", "971",
                "972", "973", "978", "979", "980", "985", "989"),
            7));

    // 英国
    COUNTRY_INFO.put(
        "UK",
        new CountryInfo(
            "+44",
            Arrays.asList(
                "20", "121", "131", "141", "151", "161", "113", "114", "115", "116", "117", "118",
                "1204", "1223", "1224", "1225", "1226", "1227", "1228", "1229", "1233", "1234",
                "1235", "1236", "1237", "1239", "1241", "1242", "1243", "1244", "1245", "1246",
                "1248", "1249", "1250", "1252", "1253", "1254", "1255", "1256", "1257", "1258",
                "1259", "1260", "1261", "1262", "1263", "1264", "1267", "1268", "1269", "1270",
                "1271", "1272", "1273", "1274", "1275", "1276", "1277", "1278", "1279", "1280",
                "1282", "1283", "1284", "1285", "1286", "1287", "1288", "1289", "1290", "1291",
                "1292", "1293", "1294", "1295", "1296", "1297", "1298", "1299"),
            6));

    // 日本
    COUNTRY_INFO.put(
        "JP",
        new CountryInfo(
            "+81",
            Arrays.asList(
                "3", "6", "11", "43", "44", "45", "48", "52", "53", "54", "55", "58", "72", "75",
                "76", "78", "82", "86", "92", "95", "96", "98", "99", "11", "123", "124", "125",
                "126", "133", "134", "135", "136", "137", "138", "139", "142", "143", "144", "145",
                "146", "152", "153", "154", "155", "156", "157", "158", "162", "163", "164", "165",
                "166", "167", "172", "173", "174", "175", "176", "178", "179", "182", "183", "184",
                "185", "186", "187", "191", "192", "193", "194", "195", "220", "223", "224", "225",
                "226", "228", "229", "233", "234", "235", "237", "238", "240", "241", "242", "243",
                "244", "246", "247", "248", "250", "254", "255", "256", "258", "260", "261", "263",
                "264", "265", "266", "267", "268", "269", "270", "274", "276", "277", "278", "279",
                "280", "282", "283", "284", "285", "287", "288", "289", "291", "293", "294", "295",
                "296", "297", "299"),
            8));
  }

  @Override
  public String getType() {
    return "fax";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String region = config.getParam("region", String.class, "CN");
      String areaCode = config.getParam("area_code", String.class, null);
      String format = config.getParam("format", String.class, "STANDARD");
      boolean includeExtension =
          Boolean.parseBoolean(config.getParam("include_extension", String.class, "false"));
      int extensionLength =
          Integer.parseInt(config.getParam("extension_length", String.class, "3"));
      String prefix = config.getParam("prefix", String.class, null);

      // 生成传真号码
      String faxNumber =
          generateFaxNumber(region, areaCode, format, includeExtension, extensionLength, prefix);

      // 将传真号码信息存入上下文
      context.put("fax_number", faxNumber);
      context.put("fax_region", region);
      context.put("fax_area_code", extractAreaCode(faxNumber, region));

      logger.debug("Generated fax number: {}", faxNumber);
      return faxNumber;

    } catch (Exception e) {
      logger.error("Error generating fax number", e);
      return "010-12345678";
    }
  }

  private String generateFaxNumber(
      String region,
      String areaCode,
      String format,
      boolean includeExtension,
      int extensionLength,
      String prefix) {

    // 获取国家信息
    CountryInfo countryInfo = COUNTRY_INFO.get(region.toUpperCase());
    if (countryInfo == null) {
      countryInfo = COUNTRY_INFO.get("CN"); // 默认使用中国
    }

    // 确定区号
    String finalAreaCode = areaCode != null ? areaCode : selectRandomAreaCode(countryInfo);

    // 生成号码主体
    String mainNumber = generateMainNumber(countryInfo);

    // 生成分机号
    String extension = includeExtension ? generateExtension(extensionLength) : null;

    // 应用格式
    return formatFaxNumber(countryInfo, finalAreaCode, mainNumber, extension, format, prefix);
  }

  private String selectRandomAreaCode(CountryInfo countryInfo) {
    List<String> areaCodes = countryInfo.areaCodes;
    return areaCodes.get(random.nextInt(areaCodes.size()));
  }

  private String generateMainNumber(CountryInfo countryInfo) {
    StringBuilder number = new StringBuilder();

    for (int i = 0; i < countryInfo.numberLength; i++) {
      number.append(random.nextInt(10));
    }

    return number.toString();
  }

  private String generateExtension(int length) {
    StringBuilder extension = new StringBuilder();

    for (int i = 0; i < length; i++) {
      extension.append(random.nextInt(10));
    }

    return extension.toString();
  }

  private String formatFaxNumber(
      CountryInfo countryInfo,
      String areaCode,
      String mainNumber,
      String extension,
      String format,
      String prefix) {

    StringBuilder faxNumber = new StringBuilder();

    // 添加自定义前缀
    if (prefix != null && !prefix.isEmpty()) {
      faxNumber.append(prefix);
    }

    FaxFormat faxFormat = parseFaxFormat(format);

    switch (faxFormat) {
      case INTERNATIONAL:
        faxNumber.append(countryInfo.countryCode).append("-");
        faxNumber.append(areaCode).append("-");
        faxNumber.append(mainNumber);
        break;

      case COMPACT:
        faxNumber.append(areaCode).append(mainNumber);
        break;

      case STANDARD:
      default:
        faxNumber.append(areaCode).append("-").append(mainNumber);
        break;
    }

    // 添加分机号
    if (extension != null) {
      faxNumber.append(" ext.").append(extension);
    }

    return faxNumber.toString();
  }

  private FaxFormat parseFaxFormat(String format) {
    try {
      return FaxFormat.valueOf(format.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Unknown fax format: {}. Using STANDARD.", format);
      return FaxFormat.STANDARD;
    }
  }

  private String extractAreaCode(String faxNumber, String region) {
    try {
      CountryInfo countryInfo = COUNTRY_INFO.get(region.toUpperCase());
      if (countryInfo == null) {
        return "unknown";
      }

      // 移除国家代码和前缀
      String cleanNumber = faxNumber;
      if (cleanNumber.startsWith(countryInfo.countryCode)) {
        cleanNumber = cleanNumber.substring(countryInfo.countryCode.length());
      }

      // 移除分隔符
      cleanNumber = cleanNumber.replaceAll("[^0-9]", "");

      // 根据区号长度提取
      for (String code : countryInfo.areaCodes) {
        if (cleanNumber.startsWith(code)) {
          return code;
        }
      }

      return "unknown";
    } catch (Exception e) {
      return "unknown";
    }
  }

  /** 验证传真号码格式 */
  public boolean validateFaxNumber(String faxNumber, String region) {
    if (faxNumber == null || faxNumber.isEmpty()) {
      return false;
    }

    CountryInfo countryInfo = COUNTRY_INFO.get(region.toUpperCase());
    if (countryInfo == null) {
      return false;
    }

    // 移除所有非数字字符（除了+号）
    String cleanNumber = faxNumber.replaceAll("[^0-9+]", "");

    // 检查是否包含有效的区号
    for (String areaCode : countryInfo.areaCodes) {
      if (cleanNumber.contains(areaCode)) {
        return true;
      }
    }

    return false;
  }

  /** 生成企业传真号码（通常比个人传真号码更正式） */
  public String generateCorporateFax(String region) {
    CountryInfo countryInfo = COUNTRY_INFO.get(region.toUpperCase());
    if (countryInfo == null) {
      countryInfo = COUNTRY_INFO.get("CN");
    }

    // 企业传真通常使用主要城市的区号
    String areaCode;
    if ("CN".equals(region.toUpperCase())) {
      String[] majorCities = {"010", "021", "022", "020", "0755", "0571", "025", "027", "028"};
      areaCode = majorCities[random.nextInt(majorCities.length)];
    } else {
      areaCode = selectRandomAreaCode(countryInfo);
    }

    String mainNumber = generateMainNumber(countryInfo);

    return formatFaxNumber(countryInfo, areaCode, mainNumber, null, "INTERNATIONAL", "Fax: ");
  }
}
