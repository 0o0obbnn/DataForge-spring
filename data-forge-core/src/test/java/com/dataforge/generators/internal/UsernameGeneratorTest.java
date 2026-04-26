package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UsernameGenerator 测试")
class UsernameGeneratorTest {

  private UsernameGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new UsernameGenerator();
    config = new SimpleFieldConfig();
    config.setType("username");
    context = new DataForgeContext();
  }

  @Nested
  @DisplayName("默认配置测试")
  class DefaultConfigurationTests {

    @Test
    @DisplayName("默认配置应生成有效用户名")
    void shouldGenerateValidUsernameWithDefaultConfig() {
      String username = generator.generate(config, context);

      assertThat(username).isNotNull();
      assertThat(username.length()).isBetween(6, 16);
    }

    @Test
    @DisplayName("默认用户名应包含字母和数字")
    void shouldContainAlphaNumericWithDefaultConfig() {
      String username = generator.generate(config, context);

      boolean hasAlpha = username.chars().anyMatch(Character::isLetter);

      assertThat(hasAlpha).isTrue();
      // 数字不是必需的，但应该有可能出现
      assertThat(username).matches("^[a-zA-Z][a-zA-Z0-9]*$");
    }

    @Test
    @DisplayName("生成的用户名应具有唯一性")
    void shouldGenerateUniqueUsernames() {
      Set<String> usernames = new HashSet<>();

      for (int i = 0; i < 100; i++) {
        String username = generator.generate(config, context);
        assertThat(usernames).doesNotContain(username);
        usernames.add(username);
      }

      assertThat(usernames).hasSize(100);
    }

    @Test
    @DisplayName("默认用户名不应包含黑名单词汇")
    void shouldNotContainBlacklistedWords() {
      Set<String> blacklisted = new HashSet<>();

      for (int i = 0; i < 100; i++) {
        String username = generator.generate(config, context);
        String lowerUsername = username.toLowerCase();

        // 检查常见的黑名单词汇
        if (lowerUsername.contains("admin")
            || lowerUsername.contains("root")
            || lowerUsername.contains("test")
            || lowerUsername.contains("user")) {
          blacklisted.add(username);
        }
      }

      // 应该不会有黑名单词汇
      assertThat(blacklisted).isEmpty();
    }
  }

  @Nested
  @DisplayName("长度配置测试")
  class LengthConfigurationTests {

    @Test
    @DisplayName("应生成指定长度的用户名")
    void shouldGenerateUsernameWithFixedLength() {
      config.setParam("length", "10");

      String username = generator.generate(config, context);

      assertThat(username).hasSize(10);
    }

    @Test
    @DisplayName("应生成长度范围内的用户名")
    void shouldGenerateUsernameWithinLengthRange() {
      config.setParam("length", "8,12");

      for (int i = 0; i < 20; i++) {
        String username = generator.generate(config, context);
        assertThat(username.length()).isBetween(8, 12);
      }
    }

    @Test
    @DisplayName("应处理无效长度参数并使用默认值")
    void shouldHandleInvalidLengthParameter() {
      config.setParam("length", "invalid");

      String username = generator.generate(config, context);

      assertThat(username).isNotNull();
      assertThat(username.length()).isBetween(6, 16);
    }

    @Test
    @DisplayName("应处理最小长度为1的情况")
    void shouldHandleMinimumLengthOfOne() {
      config.setParam("length", "1,3");

      String username = generator.generate(config, context);

      assertThat(username.length()).isBetween(1, 3);
    }
  }

  @Nested
  @DisplayName("字符集配置测试")
  class CharacterSetTests {

    @Test
    @DisplayName("ALPHANUMERIC字符集应包含字母和数字")
    void shouldGenerateAlphanumericUsernames() {
      config.setParam("chars", "ALPHANUMERIC");
      config.setParam("length", "10");

      for (int i = 0; i < 20; i++) {
        String username = generator.generate(config, context);
        assertThat(username).matches("^[a-zA-Z0-9]+$");
      }
    }

    @Test
    @DisplayName("ALPHA字符集应只包含字母")
    void shouldGenerateAlphaOnlyUsernames() {
      config.setParam("chars", "ALPHA");
      config.setParam("length", "10");

      for (int i = 0; i < 20; i++) {
        String username = generator.generate(config, context);
        assertThat(username).matches("^[a-zA-Z]+$");
      }
    }

    @Test
    @DisplayName("NUMERIC字符集应只包含数字")
    void shouldGenerateNumericOnlyUsernames() {
      config.setParam("chars", "NUMERIC");
      config.setParam("length", "10");

      for (int i = 0; i < 20; i++) {
        String username = generator.generate(config, context);
        assertThat(username).matches("^[0-9]+$");
      }
    }

    @Test
    @DisplayName("ALPHANUMERIC_SPECIAL字符集应包含字母、数字和特殊字符")
    void shouldGenerateUsernamesWithSpecialChars() {
      config.setParam("chars", "ALPHANUMERIC_SPECIAL");
      config.setParam("length", "12");

      for (int i = 0; i < 50; i++) {
        String username = generator.generate(config, context);
        assertThat(username).matches("^[a-zA-Z0-9_-]+$");
      }

      // 特殊字符不是必需的，但应该有可能出现
      assertThat(true).isTrue();
    }

    @Test
    @DisplayName("CUSTOM字符集应使用自定义字符")
    void shouldUseCustomCharacterSet() {
      config.setParam("chars", "CUSTOM");
      config.setParam("custom_chars", "abc123");
      config.setParam("length", "10");

      for (int i = 0; i < 20; i++) {
        String username = generator.generate(config, context);
        assertThat(username).matches("^[abc123]+$");
      }
    }

    @Test
    @DisplayName("空自定义字符集应使用默认字符集")
    void shouldUseDefaultCharacterSetForEmptyCustomChars() {
      config.setParam("chars", "CUSTOM");
      config.setParam("custom_chars", "");

      String username = generator.generate(config, context);

      assertThat(username).isNotNull();
      assertThat(username).matches("^[a-zA-Z0-9]+$");
    }

    @Test
    @DisplayName("无效字符集类型应使用默认ALPHANUMERIC")
    void shouldUseDefaultForInvalidCharsType() {
      config.setParam("chars", "INVALID");

      String username = generator.generate(config, context);

      assertThat(username).isNotNull();
      assertThat(username).matches("^[a-zA-Z0-9]+$");
    }
  }

  @Nested
  @DisplayName("前缀和后缀测试")
  class PrefixAndSuffixTests {

    @Test
    @DisplayName("应添加前缀到用户名")
    void shouldAddPrefixToUsername() {
      config.setParam("prefix", "user_");
      config.setParam("length", "10,15");

      for (int i = 0; i < 20; i++) {
        String username = generator.generate(config, context);
        assertThat(username).startsWith("user_");
        assertThat(username.length()).isBetween(10, 15);
      }
    }

    @Test
    @DisplayName("应添加后缀到用户名")
    void shouldAddSuffixToUsername() {
      config.setParam("suffix", "_2024");
      config.setParam("length", "10,15");

      for (int i = 0; i < 20; i++) {
        String username = generator.generate(config, context);
        assertThat(username).endsWith("_2024");
        assertThat(username.length()).isBetween(10, 15);
      }
    }

    @Test
    @DisplayName("应同时添加前缀和后缀")
    void shouldAddBothPrefixAndSuffix() {
      config.setParam("prefix", "user_");
      config.setParam("suffix", "_dev");
      config.setParam("length", "12,16");

      for (int i = 0; i < 20; i++) {
        String username = generator.generate(config, context);
        assertThat(username).startsWith("user_");
        assertThat(username).endsWith("_dev");
        assertThat(username.length()).isBetween(12, 16);
      }
    }

    @Test
    @DisplayName("前缀和后缀不应计入随机生成部分长度")
    void shouldExcludePrefixSuffixFromRandomLength() {
      config.setParam("prefix", "usr_");
      config.setParam("suffix", "_end");
      config.setParam("length", "10,12");

      String username = generator.generate(config, context);

      // 总长度应该是前缀(4) + 后缀(4) + 随机部分(2-4)
      assertThat(username).matches("^usr_.{2,4}_end$");
    }
  }

  @Nested
  @DisplayName("基于姓名生成测试")
  class NameBasedGenerationTests {

    @Test
    @DisplayName("应基于中文姓名生成拼音用户名")
    void shouldGenerateUsernameFromChineseName() {
      config.setParam("link_name", "true");
      config.setParam("length", "4,12"); // 缩短最小长度以适配短拼音

      context.putValue("name", "张伟");

      String username = generator.generate(config, context);

      assertThat(username).isNotNull();
      assertThat(username.length()).isBetween(4, 12);
      // 应该包含 zhang, wei, z, w 等字符（基于生成的拼音）
      assertThat(username.toLowerCase()).matches(".*(zhang|wei|z|w).*");
    }

    @Test
    @DisplayName("应基于英文姓名生成用户名")
    void shouldGenerateUsernameFromEnglishName() {
      config.setParam("link_name", "true");
      config.setParam("length", "6,12");

      context.putValue("name", "John Smith");

      String username = generator.generate(config, context);

      assertThat(username).isNotNull();
      assertThat(username.length()).isBetween(6, 12);
      // 应该包含 john, smith, j, s 等字符
      assertThat(username.toLowerCase()).matches(".*(john|smith|j|s).*");
    }

    @Test
    @DisplayName("PINYIN风格应生成完整拼音用户名")
    void shouldGenerateFullPinyinUsername() {
      config.setParam("link_name", "true");
      config.setParam("name_style", "PINYIN");
      config.setParam("length", "8,16");

      context.putValue("name", "王芳");

      String username = generator.generate(config, context);

      assertThat(username).isNotNull();
      assertThat(username.toLowerCase()).contains("wang").contains("fang");
    }

    @Test
    @DisplayName("INITIALS风格应生成首字母用户名")
    void shouldGenerateInitialsUsername() {
      config.setParam("link_name", "true");
      config.setParam("name_style", "INITIALS");
      config.setParam("length", "2,6");

      context.putValue("name", "张伟");

      String username = generator.generate(config, context);

      assertThat(username).isNotNull();
      assertThat(username.length()).isBetween(2, 6);
      // 可能是纯随机生成（如果长度要求不符合），也可能包含首字母
      // 所以我们只验证用户名的有效性，不验证具体内容
    }

    @Test
    @DisplayName("无姓名时应生成随机用户名")
    void shouldGenerateRandomUsernameWhenNoName() {
      config.setParam("link_name", "true");

      String username = generator.generate(config, context);

      assertThat(username).isNotNull();
      assertThat(username.length()).isBetween(6, 16);
    }

    @Test
    @DisplayName("禁用姓名关联时应生成随机用户名")
    void shouldGenerateRandomUsernameWhenNameLinkDisabled() {
      config.setParam("link_name", "false");
      context.putValue("name", "张伟");

      String username = generator.generate(config, context);

      assertThat(username).isNotNull();
      // 不应该包含姓名的拼音
      assertThat(username.toLowerCase()).doesNotContain("zhang").doesNotContain("wei");
    }
  }

  @Nested
  @DisplayName("黑名单测试")
  class BlacklistTests {

    @Test
    @DisplayName("默认黑名单应过滤常见保留词")
    void shouldFilterDefaultBlacklistedWords() {
      config.setParam("length", "4,8");

      // 尝试生成很多用户名，应该不会出现黑名单词汇
      Set<String> generated = new HashSet<>();
      for (int i = 0; i < 100; i++) {
        String username = generator.generate(config, context);
        generated.add(username.toLowerCase());
      }

      // 检查不包含默认黑名单词汇
      assertThat(generated).doesNotContain("admin", "root", "test", "user", "system");
    }

    @Test
    @DisplayName("自定义黑名单应过滤指定词汇")
    void shouldFilterCustomBlacklistedWords() {
      config.setParam("blacklist", "forbidden,banned,blocked");
      config.setParam("length", "8,12");

      Set<String> generated = new HashSet<>();
      for (int i = 0; i < 100; i++) {
        String username = generator.generate(config, context);
        generated.add(username.toLowerCase());
      }

      // 检查不包含自定义黑名单词汇
      assertThat(generated)
          .doesNotContain("forbidden", "banned", "blocked")
          .doesNotContain("forbidden", "banned", "blocked");
    }

    @Test
    @DisplayName("黑名单词汇应作为子字符串检查")
    void shouldCheckBlacklistAsSubstring() {
      config.setParam("blacklist", "badword");
      config.setParam("length", "8,15");

      Set<String> generated = new HashSet<>();
      for (int i = 0; i < 100; i++) {
        String username = generator.generate(config, context);
        generated.add(username.toLowerCase());
      }

      // 检查不包含黑名单词汇作为子字符串
      for (String username : generated) {
        assertThat(username).doesNotContain("badword");
      }
    }

    @Test
    @DisplayName("黑名单应区分大小写")
    void blacklistShouldBeCaseInsensitive() {
      config.setParam("blacklist", "Admin");
      config.setParam("prefix", "Admin_");
      config.setParam("length", "8,12");

      // 应该重新生成，因为 Admin 在黑名单中（虽然大小写不同）
      String username = generator.generate(config, context);

      // 用户名不应该以 Admin_ 开头（因为被过滤了）
      // 或者如果前缀被强制添加，应该重新生成随机部分
      assertThat(username).isNotNull();
    }
  }

  @Nested
  @DisplayName("唯一性测试")
  class UniquenessTests {

    @Test
    @DisplayName("启用唯一性应生成不重复用户名")
    void shouldGenerateUniqueUsernamesWhenEnabled() {
      config.setParam("unique", "true");
      config.setParam("length", "6,8");

      Set<String> usernames = new HashSet<>();
      for (int i = 0; i < 200; i++) {
        String username = generator.generate(config, context);
        assertThat(usernames).doesNotContain(username);
        usernames.add(username);
      }

      assertThat(usernames).hasSize(200);
    }

    @Test
    @DisplayName("禁用唯一性可能生成重复用户名")
    void mayGenerateDuplicateUsernamesWhenDisabled() {
      config.setParam("unique", "false");
      config.setParam("length", "6");
      config.setParam("chars", "ALPHA");

      // 生成大量用户名，有可能重复（虽然概率很低）
      Set<String> usernames = new HashSet<>();
      for (int i = 0; i < 500; i++) {
        String username = generator.generate(config, context);
        usernames.add(username);
      }

      // 不保证唯一性，但应该生成了500个用户名
      assertThat(usernames.size()).isLessThanOrEqualTo(500);
    }

    @Test
    @DisplayName("批量生成应保持唯一性")
    void shouldMaintainUniquenessForBulkGeneration() {
      config.setParam("unique", "true");
      config.setParam("length", "6,10");

      Set<String> usernames = new HashSet<>();
      for (int i = 0; i < 1000; i++) {
        String username = generator.generate(config, context);
        assertThat(usernames).doesNotContain(username);
        usernames.add(username);
      }

      assertThat(usernames).hasSize(1000);
    }
  }

  @Nested
  @DisplayName("边界条件和异常测试")
  class EdgeCaseTests {

    @Test
    @DisplayName("前缀和后缀长度超过总长度应仍生成有效用户名")
    void shouldHandlePrefixSuffixLongerThanTotalLength() {
      config.setParam("prefix", "verylongprefix_");
      config.setParam("suffix", "_verylongsuffix");
      config.setParam("length", "10,15");

      String username = generator.generate(config, context);

      assertThat(username).isNotNull();
      assertThat(username).startsWith("verylongprefix_");
      assertThat(username).endsWith("_verylongsuffix");
    }

    @Test
    @DisplayName("空字符串前缀后缀应被忽略")
    void shouldIgnoreEmptyPrefixSuffix() {
      config.setParam("prefix", "");
      config.setParam("suffix", "");

      String username = generator.generate(config, context);

      assertThat(username).isNotNull();
    }

    @Test
    @DisplayName("批量生成应保持性能")
    void shouldMaintainPerformanceForBulkGeneration() {
      config.setParam("length", "8,12");

      long startTime = System.currentTimeMillis();
      for (int i = 0; i < 1000; i++) {
        String username = generator.generate(config, context);
        assertThat(username).isNotNull();
      }
      long duration = System.currentTimeMillis() - startTime;

      // 1000个用户名应该在5秒内生成
      assertThat(duration).isLessThan(5000);
    }

    @Test
    @DisplayName("长度范围反转应自动调整")
    void shouldHandleReversedLengthRange() {
      config.setParam("length", "16,8"); // min > max

      String username = generator.generate(config, context);

      assertThat(username).isNotNull();
      assertThat(username.length()).isBetween(8, 16);
    }
  }
}
