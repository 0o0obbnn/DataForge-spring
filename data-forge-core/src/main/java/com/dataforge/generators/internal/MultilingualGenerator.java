package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 多语言示例生成器 生成包含指定语言的文本片段
 *
 * <p>支持的参数： - languages: 要包含的语言列表 (CN|EN|AR|EL|JA|KO|RU|FR|DE|ES|ANY，默认ANY) - minLength: 最小长度
 * (默认20) - maxLength: 最大长度 (默认100) - mixRatio: 不同语言混排的比例 (0-1，默认0.5) - script: 文字方向
 * (LTR|RTL|MIXED，默认LTR) - includeNumbers: 是否包含数字 (默认true) - includePunctuation: 是否包含标点符号 (默认true)
 */
public class MultilingualGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Random RANDOM = new Random();

  // 各语言示例文本
  private static final Map<String, List<String>> LANGUAGE_SAMPLES = new HashMap<>();

  static {
    // 中文 (Chinese)
    LANGUAGE_SAMPLES.put(
        "CN",
        Arrays.asList(
            "你好世界", "欢迎使用", "数据生成", "测试文本", "中文示例", "系统测试", "用户界面", "功能模块", "这是一个测试", "多语言支持",
            "国际化应用", "本地化处理", "字符编码", "文本显示", "输入法测试"));

    // 英文 (English)
    LANGUAGE_SAMPLES.put(
        "EN",
        Arrays.asList(
            "Hello World",
            "Welcome to",
            "Data Generation",
            "Test Text",
            "Sample Content",
            "System Test",
            "User Interface",
            "Feature Module",
            "This is a test",
            "Multilingual Support",
            "International App",
            "Localization Process",
            "Character Encoding",
            "Text Display",
            "Input Method Test"));

    // 阿拉伯文 (Arabic) - RTL
    LANGUAGE_SAMPLES.put(
        "AR",
        Arrays.asList(
            "مرحبا بالعالم",
            "أهلا وسهلا",
            "توليد البيانات",
            "نص تجريبي",
            "محتوى عينة",
            "اختبار النظام",
            "واجهة المستخدم",
            "وحدة الميزة",
            "هذا اختبار",
            "دعم متعدد اللغات",
            "تطبيق دولي",
            "عملية التوطين",
            "ترميز الأحرف",
            "عرض النص",
            "اختبار طريقة الإدخال"));

    // 希腊文 (Greek)
    LANGUAGE_SAMPLES.put(
        "EL",
        Arrays.asList(
            "Γεια σας κόσμε",
            "Καλώς ήρθατε",
            "Παραγωγή δεδομένων",
            "Κείμενο δοκιμής",
            "Δείγμα περιεχομένου",
            "Δοκιμή συστήματος",
            "Διεπαφή χρήστη",
            "Μονάδα χαρακτηριστικών",
            "Αυτό είναι μια δοκιμή",
            "Πολυγλωσσική υποστήριξη",
            "Διεθνής εφαρμογή",
            "Διαδικασία εντοπισμού",
            "Κωδικοποίηση χαρακτήρων",
            "Εμφάνιση κειμένου",
            "Δοκιμή μεθόδου εισαγωγής"));

    // 日文 (Japanese)
    LANGUAGE_SAMPLES.put(
        "JA",
        Arrays.asList(
            "こんにちは世界",
            "ようこそ",
            "データ生成",
            "テストテキスト",
            "サンプルコンテンツ",
            "システムテスト",
            "ユーザーインターフェース",
            "機能モジュール",
            "これはテストです",
            "多言語サポート",
            "国際アプリ",
            "ローカライゼーションプロセス",
            "文字エンコーディング",
            "テキスト表示",
            "入力方法テスト"));

    // 韩文 (Korean)
    LANGUAGE_SAMPLES.put(
        "KO",
        Arrays.asList(
            "안녕하세요 세계",
            "환영합니다",
            "데이터 생성",
            "테스트 텍스트",
            "샘플 콘텐츠",
            "시스템 테스트",
            "사용자 인터페이스",
            "기능 모듈",
            "이것은 테스트입니다",
            "다국어 지원",
            "국제 앱",
            "현지화 프로세스",
            "문자 인코딩",
            "텍스트 표시",
            "입력 방법 테스트"));

    // 俄文 (Russian)
    LANGUAGE_SAMPLES.put(
        "RU",
        Arrays.asList(
            "Привет мир",
            "Добро пожаловать",
            "Генерация данных",
            "Тестовый текст",
            "Образец контента",
            "Системный тест",
            "Пользовательский интерфейс",
            "Функциональный модуль",
            "Это тест",
            "Многоязычная поддержка",
            "Международное приложение",
            "Процесс локализации",
            "Кодировка символов",
            "Отображение текста",
            "Тест метода ввода"));

    // 法文 (French)
    LANGUAGE_SAMPLES.put(
        "FR",
        Arrays.asList(
            "Bonjour le monde",
            "Bienvenue à",
            "Génération de données",
            "Texte de test",
            "Contenu d'échantillon",
            "Test système",
            "Interface utilisateur",
            "Module de fonctionnalité",
            "Ceci est un test",
            "Support multilingue",
            "Application internationale",
            "Processus de localisation",
            "Encodage de caractères",
            "Affichage de texte",
            "Test de méthode d'entrée"));

    // 德文 (German)
    LANGUAGE_SAMPLES.put(
        "DE",
        Arrays.asList(
            "Hallo Welt",
            "Willkommen zu",
            "Datengenerierung",
            "Testtext",
            "Beispielinhalt",
            "Systemtest",
            "Benutzeroberfläche",
            "Funktionsmodul",
            "Dies ist ein Test",
            "Mehrsprachige Unterstützung",
            "Internationale App",
            "Lokalisierungsprozess",
            "Zeichenkodierung",
            "Textanzeige",
            "Eingabemethodentest"));

    // 西班牙文 (Spanish)
    LANGUAGE_SAMPLES.put(
        "ES",
        Arrays.asList(
            "Hola mundo",
            "Bienvenido a",
            "Generación de datos",
            "Texto de prueba",
            "Contenido de muestra",
            "Prueba del sistema",
            "Interfaz de usuario",
            "Módulo de características",
            "Esta es una prueba",
            "Soporte multilingüe",
            "Aplicación internacional",
            "Proceso de localización",
            "Codificación de caracteres",
            "Visualización de texto",
            "Prueba de método de entrada"));
  }

  // 数字在不同语言中的表示
  private static final Map<String, String> LANGUAGE_NUMBERS = new HashMap<>();

  static {
    LANGUAGE_NUMBERS.put("CN", "一二三四五六七八九十");
    LANGUAGE_NUMBERS.put("AR", "٠١٢٣٤٥٦٧٨٩");
    LANGUAGE_NUMBERS.put("JA", "一二三四五六七八九十");
    LANGUAGE_NUMBERS.put("KO", "일이삼사오육칠팔구십");
    LANGUAGE_NUMBERS.put("EN", "0123456789");
  }

  // 标点符号
  private static final Map<String, String> LANGUAGE_PUNCTUATION = new HashMap<>();

  static {
    LANGUAGE_PUNCTUATION.put("CN", "，。！？；：\u201c\u201d\u2018\u2019（）【】");
    LANGUAGE_PUNCTUATION.put("EN", ",.!?;:\"'()[]");
    LANGUAGE_PUNCTUATION.put("AR", "،؟؛：\u201c\u201d\u2018\u2019()[]");
    LANGUAGE_PUNCTUATION.put("JA", "、。！？；：「」（）【】");
    LANGUAGE_PUNCTUATION.put("KO", "，。！？；：\u201c\u201d\u2018\u2019（）【】");
    LANGUAGE_PUNCTUATION.put("RU", ",.!?;:\"'()[]");
    LANGUAGE_PUNCTUATION.put("FR", ",.!?;:«»()[]");
    LANGUAGE_PUNCTUATION.put("DE", ",.!?;:\u201e\u201c()[]");
    LANGUAGE_PUNCTUATION.put("ES", ",.!?;:«»()[]¿¡");
  }

  @Override
  public String getType() {
    return "multilingual";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    String languagesStr = getStringParam(config, "languages", "ANY");
    int minLength = getIntParam(config, "minLength", 20);
    int maxLength = getIntParam(config, "maxLength", 100);
    double mixRatio = getDoubleParam(config, "mixRatio", 0.5);
    String script = getStringParam(config, "script", "LTR");
    boolean includeNumbers = getBooleanParam(config, "includeNumbers", true);
    boolean includePunctuation = getBooleanParam(config, "includePunctuation", true);

    List<String> languages = parseLanguages(languagesStr);
    int targetLength = RANDOM.nextInt(maxLength - minLength + 1) + minLength;

    StringBuilder result = new StringBuilder();
    String currentLanguage = languages.get(RANDOM.nextInt(languages.size()));

    while (result.length() < targetLength) {
      // 决定是否切换语言
      if (RANDOM.nextDouble() < mixRatio && languages.size() > 1) {
        currentLanguage = languages.get(RANDOM.nextInt(languages.size()));
      }

      // 生成文本片段
      String fragment = generateFragment(currentLanguage, includeNumbers, includePunctuation);

      if (result.length() + fragment.length() <= targetLength) {
        if (result.length() > 0) {
          result.append(" ");
        }
        result.append(fragment);
      } else {
        break;
      }
    }

    return formatByScript(result.toString(), script);
  }

  /** 解析语言列表 */
  private List<String> parseLanguages(String languagesStr) {
    if ("ANY".equals(languagesStr.toUpperCase())) {
      return Arrays.asList("CN", "EN", "AR", "JA", "KO");
    }

    return Arrays.asList(languagesStr.split(","));
  }

  /** 生成文本片段 */
  private String generateFragment(
      String language, boolean includeNumbers, boolean includePunctuation) {
    List<String> samples = LANGUAGE_SAMPLES.get(language.toUpperCase());
    if (samples == null) {
      samples = LANGUAGE_SAMPLES.get("EN"); // 默认使用英文
    }

    StringBuilder fragment = new StringBuilder();
    String sample = samples.get(RANDOM.nextInt(samples.size()));
    fragment.append(sample);

    // 添加数字
    if (includeNumbers && RANDOM.nextDouble() < 0.3) {
      fragment.append(" ");
      fragment.append(generateNumber(language));
    }

    // 添加标点符号
    if (includePunctuation && RANDOM.nextDouble() < 0.4) {
      String punctuation = LANGUAGE_PUNCTUATION.get(language.toUpperCase());
      if (punctuation != null && !punctuation.isEmpty()) {
        char punct = punctuation.charAt(RANDOM.nextInt(punctuation.length()));
        fragment.append(punct);
      }
    }

    return fragment.toString();
  }

  /** 生成数字 */
  private String generateNumber(String language) {
    String numbers = LANGUAGE_NUMBERS.get(language.toUpperCase());
    if (numbers == null) {
      numbers = LANGUAGE_NUMBERS.get("EN");
    }

    StringBuilder number = new StringBuilder();
    int digitCount = RANDOM.nextInt(3) + 1; // 1-3位数字

    for (int i = 0; i < digitCount; i++) {
      if (language.equals("CN") || language.equals("JA")) {
        // 中文/日文数字
        char digit = numbers.charAt(RANDOM.nextInt(Math.min(10, numbers.length())));
        number.append(digit);
      } else if (language.equals("AR")) {
        // 阿拉伯数字
        char digit = numbers.charAt(RANDOM.nextInt(Math.min(10, numbers.length())));
        number.append(digit);
      } else if (language.equals("KO")) {
        // 韩文数字
        char digit = numbers.charAt(RANDOM.nextInt(Math.min(10, numbers.length())));
        number.append(digit);
      } else {
        // 阿拉伯数字 (0-9)
        number.append(RANDOM.nextInt(10));
      }
    }

    return number.toString();
  }

  /** 根据文字方向格式化 */
  private String formatByScript(String text, String script) {
    switch (script.toUpperCase()) {
      case "RTL":
        return "\u202E" + text + "\u202C"; // 添加RTL覆盖标记
      case "MIXED":
        return insertBidiMarks(text);
      case "LTR":
      default:
        return text;
    }
  }

  /** 插入双向文本标记 */
  private String insertBidiMarks(String text) {
    StringBuilder result = new StringBuilder();
    String[] words = text.split(" ");

    for (int i = 0; i < words.length; i++) {
      if (i > 0) {
        result.append(" ");
      }

      String word = words[i];
      // 检测是否包含RTL字符
      if (containsRtlChars(word)) {
        result.append("\u202E").append(word).append("\u202C"); // RTL覆盖
      } else {
        result.append("\u202D").append(word).append("\u202C"); // LTR覆盖
      }
    }

    return result.toString();
  }

  /** 检测是否包含RTL字符 */
  private boolean containsRtlChars(String text) {
    for (char c : text.toCharArray()) {
      // 阿拉伯文字符范围
      if ((c >= 0x0600 && c <= 0x06FF)
          || (c >= 0x0750 && c <= 0x077F)
          || (c >= 0x08A0 && c <= 0x08FF)
          || (c >= 0xFB50 && c <= 0xFDFF)
          || (c >= 0xFE70 && c <= 0xFEFF)) {
        return true;
      }
      // 希伯来文字符范围
      if (c >= 0x0590 && c <= 0x05FF) {
        return true;
      }
    }
    return false;
  }
}
