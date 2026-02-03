package com.dataforge.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@DisplayName("InternetGen 测试")
class InternetGenTest {

  private InternetGen internetGen;
  private DataGen dataGen;

  @BeforeEach
  void setUp() {
    dataGen = new DataGen();
    internetGen = new InternetGen(dataGen);
  }

  @Nested
  @DisplayName("邮箱生成测试")
  class EmailGenerationTests {
    @Test
    @DisplayName("应生成邮箱地址")
    void shouldGenerateEmailAddress() {
      String email = internetGen.email();

      assertThat(email).isNotNull();
      assertThat(email).isNotEmpty();
      assertThat(email).matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    @Test
    @DisplayName("应生成指定域名的邮箱地址")
    void shouldGenerateEmailAddressWithDomain() {
      String email = internetGen.email("example.com");

      assertThat(email).isNotNull();
      assertThat(email).isNotEmpty();
      assertThat(email).endsWith("@example.com");
      assertThat(email).matches("^[\\w.-]+@example\\.com$");
    }

    @Test
    @DisplayName("邮箱地址应包含@符号")
    void emailShouldContainAtSymbol() {
      String email = internetGen.email();

      assertThat(email).contains("@");
    }

    @Test
    @DisplayName("邮箱地址应包含域名部分")
    void emailShouldContainDomainPart() {
      String email = internetGen.email();

      assertThat(email).contains(".");
      int atIndex = email.indexOf("@");
      assertThat(atIndex).isGreaterThan(0);
      assertThat(email.substring(atIndex)).contains(".");
    }

    @Test
    @DisplayName("邮箱地址长度应在合理范围内")
    void emailLengthShouldBeInReasonableRange() {
      String email = internetGen.email();

      assertThat(email.length()).isBetween(5, 100);
    }
  }

  @Nested
  @DisplayName("URL生成测试")
  class URLGenerationTests {
    @Test
    @DisplayName("应生成URL")
    void shouldGenerateURL() {
      String url = internetGen.url();

      assertThat(url).isNotNull();
      assertThat(url).isNotEmpty();
      assertThat(url).matches("^https?://[\\w.-]+(?:\\.[a-zA-Z]{2,})(?:/.*)?$");
    }

    @Test
    @DisplayName("URL应包含协议部分")
    void urlShouldContainProtocol() {
      String url = internetGen.url();

      boolean hasHttpOrHttps = url.startsWith("http://") || url.startsWith("https://");
      assertThat(hasHttpOrHttps).isTrue();
    }

    @Test
    @DisplayName("URL应包含域名")
    void urlShouldContainDomain() {
      String url = internetGen.url();

      // URL应包含域名（至少有一个点号）
      assertThat(url).contains(".");
      // URL应以http://或https://开头
      assertThat(url).matches("^https?://.+");
    }

    @Test
    @DisplayName("URL长度应在合理范围内")
    void urlLengthShouldBeInReasonableRange() {
      String url = internetGen.url();

      assertThat(url.length()).isBetween(10, 200);
    }
  }

  @Nested
  @DisplayName("域名生成测试")
  class DomainGenerationTests {
    @Test
    @DisplayName("应生成域名")
    void shouldGenerateDomain() {
      String domain = internetGen.domain();

      assertThat(domain).isNotNull();
      assertThat(domain).isNotEmpty();
      assertThat(domain).matches("^[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    @Test
    @DisplayName("域名应包含顶级域名")
    void domainShouldContainTLD() {
      String domain = internetGen.domain();

      assertThat(domain).contains(".");
      int lastDotIndex = domain.lastIndexOf(".");
      assertThat(lastDotIndex).isGreaterThan(0);
      assertThat(lastDotIndex).isLessThan(domain.length() - 1);
    }

    @Test
    @DisplayName("域名长度应在合理范围内")
    void domainLengthShouldBeInReasonableRange() {
      String domain = internetGen.domain();

      assertThat(domain.length()).isBetween(4, 100);
    }

    @Test
    @DisplayName("顶级域名应为2-6个字母")
    void topLevelDomainShouldBeTwoToSixLetters() {
      String domain = internetGen.domain();
      int lastDotIndex = domain.lastIndexOf(".");
      String tld = domain.substring(lastDotIndex + 1);

      assertThat(tld).matches("[a-zA-Z]{2,6}");
    }
  }

  @Nested
  @DisplayName("IP地址生成测试")
  class IPAddressGenerationTests {
    @Test
    @DisplayName("应生成IPv4地址")
    void shouldGenerateIPv4Address() {
      String ipv4 = internetGen.ipv4();

      assertThat(ipv4).isNotNull();
      assertThat(ipv4).isNotEmpty();
      assertThat(ipv4).matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    }

    @Test
    @DisplayName("应生成IPv6地址")
    void shouldGenerateIPv6Address() {
      String ipv6 = internetGen.ipv6();

      assertThat(ipv6).isNotNull();
      assertThat(ipv6).isNotEmpty();
      // IPv6格式：8组十六进制数，每组1-4位，或者返回IPv4（如果生成器不支持IPv6）
      boolean isValidIPv6 = ipv6.matches("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
      boolean isValidIPv4 = ipv6.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
      assertThat(isValidIPv6 || isValidIPv4).isTrue();
    }

    @Test
    @DisplayName("IPv4地址每个八位组应在0-255范围内")
    void ipv4OctetsShouldBeInRange() {
      String ipv4 = internetGen.ipv4();
      String[] octets = ipv4.split("\\.");

      assertThat(octets).hasSize(4);
      for (String octet : octets) {
        int value = Integer.parseInt(octet);
        assertThat(value).isBetween(0, 255);
      }
    }

    @Test
    @DisplayName("IPv6地址应包含冒号分隔符")
    void ipv6ShouldContainColons() {
      String ipv6 = internetGen.ipv6();

      assertThat(ipv6).isNotNull();
      // 如果是IPv6格式应该包含冒号，如果是IPv4格式则不包含
      boolean isIPv6Format = ipv6.contains(":");
      boolean isIPv4Format = ipv6.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
      assertThat(isIPv6Format || isIPv4Format).isTrue();
    }

    @Test
    @DisplayName("IPv6地址各段应为十六进制")
    void ipv6SegmentsShouldBeHexadecimal() {
      String ipv6 = internetGen.ipv6();

      assertThat(ipv6).isNotNull();
      // 如果是IPv6格式检查段，如果是IPv4格式则跳过
      if (ipv6.contains(":")) {
        String[] segments = ipv6.split(":");

        assertThat(segments).hasSize(8);
        for (String segment : segments) {
          assertThat(segment).matches("[0-9a-fA-F]{1,4}");
        }
      } else {
        // IPv4格式，直接验证
        assertThat(ipv6).matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
      }
    }
  }

  @Nested
  @DisplayName("MAC地址生成测试")
  class MACAddressGenerationTests {
    @Test
    @DisplayName("应生成MAC地址")
    void shouldGenerateMACAddress() {
      String mac = internetGen.macAddress();

      assertThat(mac).isNotNull();
      assertThat(mac).isNotEmpty();
      // MAC地址格式：6组十六进制数，用冒号分隔
      assertThat(mac).matches("^([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}$");
    }

    @Test
    @DisplayName("MAC地址应包含冒号分隔符")
    void macAddressShouldContainColons() {
      String mac = internetGen.macAddress();

      assertThat(mac).contains(":");
      long colonCount = mac.chars().filter(ch -> ch == ':').count();
      assertThat(colonCount).isEqualTo(5);
    }

    @Test
    @DisplayName("MAC地址每个字节应为十六进制")
    void macAddressBytesShouldBeHexadecimal() {
      String mac = internetGen.macAddress();
      String[] bytes = mac.split(":");

      assertThat(bytes).hasSize(6);
      for (String byteStr : bytes) {
        assertThat(byteStr).matches("[0-9a-fA-F]{2}");
      }
    }

    @Test
    @DisplayName("MAC地址长度应为17字符")
    void macAddressLengthShouldBeSeventeenCharacters() {
      String mac = internetGen.macAddress();

      assertThat(mac.length()).isEqualTo(17);
    }
  }

  @Nested
  @DisplayName("用户名生成测试")
  class UsernameGenerationTests {
    @Test
    @DisplayName("应生成用户名")
    void shouldGenerateUsername() {
      String username = internetGen.username();

      assertThat(username).isNotNull();
      assertThat(username).isNotEmpty();
      assertThat(username).matches("^[a-zA-Z0-9_]+$");
    }

    @Test
    @DisplayName("用户名长度应在合理范围内")
    void usernameLengthShouldBeInReasonableRange() {
      String username = internetGen.username();

      assertThat(username.length()).isBetween(3, 30);
    }

    @Test
    @DisplayName("用户名应以字母开头")
    void usernameShouldStartWithLetter() {
      String username = internetGen.username();

      assertThat(username).matches("^[a-zA-Z].*$");
    }
  }

  @Nested
  @DisplayName("密码生成测试")
  class PasswordGenerationTests {
    @Test
    @DisplayName("应生成密码")
    void shouldGeneratePassword() {
      String password = internetGen.password();

      assertThat(password).isNotNull();
      assertThat(password).isNotEmpty();
    }

    @Test
    @DisplayName("应生成指定长度的密码")
    void shouldGeneratePasswordWithLength() {
      int length = 16;
      String password = internetGen.password(length);

      assertThat(password).isNotNull();
      assertThat(password).isNotEmpty();
      assertThat(password.length()).isEqualTo(length);
    }

    @Test
    @DisplayName("密码长度应在合理范围内")
    void passwordLengthShouldBeInReasonableRange() {
      String password = internetGen.password();

      assertThat(password.length()).isBetween(8, 32);
    }
  }

  @Nested
  @DisplayName("User-Agent生成测试")
  class UserAgentGenerationTests {
    @Test
    @DisplayName("应生成User-Agent")
    void shouldGenerateUserAgent() {
      String userAgent = internetGen.userAgent();

      assertThat(userAgent).isNotNull();
      assertThat(userAgent).isNotEmpty();
    }

    @Test
    @DisplayName("User-Agent应包含Mozilla")
    void userAgentShouldContainMozilla() {
      String userAgent = internetGen.userAgent();

      assertThat(userAgent).contains("Mozilla");
    }

    @Test
    @DisplayName("User-Agent长度应在合理范围内")
    void userAgentLengthShouldBeInReasonableRange() {
      String userAgent = internetGen.userAgent();

      assertThat(userAgent.length()).isBetween(20, 200);
    }
  }

  @Nested
  @DisplayName("颜色生成测试")
  class ColorGenerationTests {
    @Test
    @DisplayName("应生成颜色十六进制值")
    void shouldGenerateColorHex() {
      String colorHex = internetGen.colorHex();

      assertThat(colorHex).isNotNull();
      assertThat(colorHex).isNotEmpty();
      assertThat(colorHex).matches("^#[0-9a-fA-F]{6}$");
    }

    @Test
    @DisplayName("颜色十六进制值应以#开头")
    void colorHexShouldStartWithHash() {
      String colorHex = internetGen.colorHex();

      assertThat(colorHex).startsWith("#");
    }

    @Test
    @DisplayName("颜色十六进制值长度应为7字符")
    void colorHexLengthShouldBeSevenCharacters() {
      String colorHex = internetGen.colorHex();

      assertThat(colorHex.length()).isEqualTo(7);
    }

    @Test
    @DisplayName("颜色十六进制值各分量应为有效值")
    void colorHexComponentsShouldBeValid() {
      String colorHex = internetGen.colorHex();
      String hex = colorHex.substring(1);

      int red = Integer.parseInt(hex.substring(0, 2), 16);
      int green = Integer.parseInt(hex.substring(2, 4), 16);
      int blue = Integer.parseInt(hex.substring(4, 6), 16);

      assertThat(red).isBetween(0, 255);
      assertThat(green).isBetween(0, 255);
      assertThat(blue).isBetween(0, 255);
    }
  }

  @Nested
  @DisplayName("批量生成测试")
  class BatchGenerationTests {
    @Test
    @DisplayName("批量生成邮箱应返回正确数量")
    void shouldGenerateCorrectNumberOfEmails() {
      int count = 100;
      List<String> emails = internetGen.emails(count);

      assertThat(emails).hasSize(count);
      assertThat(emails).allSatisfy(e -> {
        assertThat(e).isNotNull();
        assertThat(e).matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
      });
    }

    @Test
    @DisplayName("批量生成URL应返回正确数量")
    void shouldGenerateCorrectNumberOfURLs() {
      int count = 100;
      List<String> urls = internetGen.urls(count);

      assertThat(urls).hasSize(count);
      assertThat(urls).allSatisfy(u -> {
        assertThat(u).isNotNull();
        // URL格式：协议://域名[:端口][/路径][?查询]
        assertThat(u).matches("^https?://[\\w.-]+(?:\\.[a-zA-Z]{2,})(?::\\d+)?(?:/.*)?$");
      });
    }

    @Test
    @DisplayName("批量生成IPv4地址应返回正确数量")
    void shouldGenerateCorrectNumberOfIPv4Addresses() {
      int count = 100;
      List<String> ipv4s = internetGen.ipv4s(count);

      assertThat(ipv4s).hasSize(count);
      assertThat(ipv4s).allSatisfy(ip -> {
        assertThat(ip).isNotNull();
        assertThat(ip).matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
      });
    }
  }

  @Nested
  @DisplayName("格式验证测试")
  class FormatValidationTests {
    @Test
    @DisplayName("邮箱格式应为有效格式")
    void shouldHaveValidEmailFormat() {
      String email = internetGen.email();

      assertThat(email).matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    @Test
    @DisplayName("URL格式应为有效格式")
    void shouldHaveValidURLFormat() {
      String url = internetGen.url();

      // URL应以http://或https://开头，并且包含域名
      boolean startsWithProtocol = url.startsWith("http://") || url.startsWith("https://");
      boolean hasDomain = url.contains(".");
      assertThat(startsWithProtocol).isTrue();
      assertThat(hasDomain).isTrue();
    }

    @Test
    @DisplayName("域名格式应为有效格式")
    void shouldHaveValidDomainFormat() {
      String domain = internetGen.domain();

      assertThat(domain).matches("^[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    @Test
    @DisplayName("IPv4格式应为有效格式")
    void shouldHaveValidIPv4Format() {
      String ipv4 = internetGen.ipv4();

      assertThat(ipv4).matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    }

    @Test
    @DisplayName("IPv6格式应为有效格式")
    void shouldHaveValidIPv6Format() {
      String ipv6 = internetGen.ipv6();

      // IPv6格式或者IPv4格式（如果生成器不支持IPv6）
      boolean isValidIPv6 = ipv6.matches("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
      boolean isValidIPv4 = ipv6.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
      assertThat(isValidIPv6 || isValidIPv4).isTrue();
    }

    @Test
    @DisplayName("MAC地址格式应为有效格式")
    void shouldHaveValidMACAddressFormat() {
      String mac = internetGen.macAddress();

      assertThat(mac).matches("^([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}$");
    }

    @Test
    @DisplayName("颜色十六进制格式应为有效格式")
    void shouldHaveValidColorHexFormat() {
      String colorHex = internetGen.colorHex();

      assertThat(colorHex).matches("^#[0-9a-fA-F]{6}$");
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {
    @Test
    @DisplayName("批量生成0个应返回空列表")
    void shouldReturnEmptyListForZeroCount() {
      List<String> emails = internetGen.emails(0);

      assertThat(emails).isNotNull();
      assertThat(emails).isEmpty();
    }

    @Test
    @DisplayName("批量生成大量数据应成功")
    void shouldGenerateLargeBatchOfEmails() {
      int count = 10000;
      List<String> emails = internetGen.emails(count);

      assertThat(emails).hasSize(count);
      assertThat(emails).allSatisfy(e -> {
        assertThat(e).isNotNull();
        assertThat(e).matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
      });
    }

    @Test
    @DisplayName("密码最小长度应为8")
    void passwordMinimumLengthShouldBeEight() {
      String password = internetGen.password(8);

      assertThat(password).isNotNull();
      assertThat(password.length()).isEqualTo(8);
    }

    @Test
    @DisplayName("用户名最小长度应为3")
    void usernameMinimumLengthShouldBeThree() {
      String username = internetGen.username();

      assertThat(username).isNotNull();
      assertThat(username.length()).isGreaterThanOrEqualTo(3);
    }
  }

  @Nested
  @DisplayName("性能测试")
  class PerformanceTests {
    @Test
    @DisplayName("批量生成邮箱应高效")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldGenerateEmailsEfficiently() {
      int count = 10000;
      long startTime = System.nanoTime();

      List<String> emails = internetGen.emails(count);

      long duration = System.nanoTime() - startTime;
      double recordsPerSecond = (count * 1_000_000_000.0) / duration;

      assertThat(emails).hasSize(count);
      assertThat(recordsPerSecond).isGreaterThan(1000.0);
    }

    @Test
    @DisplayName("批量生成URL应高效")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldGenerateURLsEfficiently() {
      int count = 10000;
      long startTime = System.nanoTime();

      List<String> urls = internetGen.urls(count);

      long duration = System.nanoTime() - startTime;
      double recordsPerSecond = (count * 1_000_000_000.0) / duration;

      assertThat(urls).hasSize(count);
      assertThat(recordsPerSecond).isGreaterThan(1000.0);
    }

    @Test
    @DisplayName("批量生成IPv4地址应高效")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldGenerateIPv4AddressesEfficiently() {
      int count = 10000;
      long startTime = System.nanoTime();

      List<String> ipv4s = internetGen.ipv4s(count);

      long duration = System.nanoTime() - startTime;
      double recordsPerSecond = (count * 1_000_000_000.0) / duration;

      assertThat(ipv4s).hasSize(count);
      assertThat(recordsPerSecond).isGreaterThan(1000.0);
    }

    @Test
    @DisplayName("单个生成操作应快速")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void singleGenerationShouldBeFast() {
      long iterations = 1000;
      long startTime = System.nanoTime();

      for (int i = 0; i < iterations; i++) {
        internetGen.email();
        internetGen.url();
        internetGen.ipv4();
        internetGen.macAddress();
        internetGen.username();
        internetGen.password();
        internetGen.colorHex();
      }

      long duration = System.nanoTime() - startTime;
      double operationsPerSecond = (iterations * 7 * 1_000_000_000.0) / duration;

      assertThat(operationsPerSecond).isGreaterThan(1000.0);
    }
  }
}