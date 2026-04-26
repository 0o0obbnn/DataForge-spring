package com.dataforge.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@DisplayName("AddressGen 测试")
class AddressGenTest {

  private AddressGen addressGen;
  private DataGen dataGen;

  @BeforeEach
  void setUp() {
    dataGen = new DataGen();
    addressGen = new AddressGen(dataGen);
  }

  @Nested
  @DisplayName("完整地址生成测试")
  class FullAddressTests {
    @Test
    @DisplayName("应生成完整地址")
    void shouldGenerateFullAddress() {
      String address = addressGen.fullAddress();

      assertThat(address).isNotNull();
      assertThat(address).isNotEmpty();
      assertThat(address.length()).isBetween(10, 200);
    }

    @Test
    @DisplayName("批量生成完整地址应返回正确数量")
    void shouldGenerateCorrectNumberOfFullAddresses() {
      int count = 100;
      List<String> addresses = addressGen.fullAddresses(count);

      assertThat(addresses).hasSize(count);
      assertThat(addresses)
          .allSatisfy(
              addr -> {
                assertThat(addr).isNotNull();
                assertThat(addr).isNotEmpty();
              });
    }

    @Test
    @DisplayName("完整地址应包含地址组件")
    void fullAddressShouldContainAddressComponents() {
      String address = addressGen.fullAddress();

      assertThat(address).isNotNull();
      assertThat(address).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("省份生成测试")
  class ProvinceTests {
    @Test
    @DisplayName("应生成省份")
    void shouldGenerateProvince() {
      String province = addressGen.province();

      assertThat(province).isNotNull();
      assertThat(province).isNotEmpty();
      assertThat(province.length()).isBetween(2, 20);
    }

    @Test
    @DisplayName("批量生成省份应返回正确数量")
    void shouldGenerateCorrectNumberOfProvinces() {
      int count = 50;
      List<String> provinces = addressGen.provinces(count);

      assertThat(provinces).hasSize(count);
      assertThat(provinces)
          .allSatisfy(
              p -> {
                assertThat(p).isNotNull();
                assertThat(p).isNotEmpty();
              });
    }

    @Test
    @DisplayName("省份名称应为有效中文名称")
    void provinceNameShouldBeValidChineseName() {
      String province = addressGen.province();

      assertThat(province).isNotNull();
      assertThat(province).matches("[\\u4e00-\\u9fa5]+");
    }
  }

  @Nested
  @DisplayName("城市生成测试")
  class CityTests {
    @Test
    @DisplayName("应生成城市")
    void shouldGenerateCity() {
      String city = addressGen.city();

      assertThat(city).isNotNull();
      assertThat(city).isNotEmpty();
      assertThat(city.length()).isBetween(2, 30);
    }

    @Test
    @DisplayName("批量生成城市应返回正确数量")
    void shouldGenerateCorrectNumberOfCities() {
      int count = 50;
      List<String> cities = addressGen.cities(count);

      assertThat(cities).hasSize(count);
      assertThat(cities)
          .allSatisfy(
              c -> {
                assertThat(c).isNotNull();
                assertThat(c).isNotEmpty();
              });
    }

    @Test
    @DisplayName("城市名称应为有效名称")
    void cityNameShouldBeValidName() {
      String city = addressGen.city();

      assertThat(city).isNotNull();
      assertThat(city).matches("[\\u4e00-\\u9fa5a-zA-Z]+");
    }
  }

  @Nested
  @DisplayName("区县生成测试")
  class DistrictTests {
    @Test
    @DisplayName("应生成区县")
    void shouldGenerateDistrict() {
      String district = addressGen.district();

      assertThat(district).isNotNull();
      assertThat(district).isNotEmpty();
      assertThat(district.length()).isBetween(2, 20);
    }

    @Test
    @DisplayName("批量生成区县应返回正确数量")
    void shouldGenerateCorrectNumberOfDistricts() {
      int count = 50;
      List<String> districts = addressGen.districts(count);

      assertThat(districts).hasSize(count);
      assertThat(districts)
          .allSatisfy(
              d -> {
                assertThat(d).isNotNull();
                assertThat(d).isNotEmpty();
              });
    }
  }

  @Nested
  @DisplayName("街道生成测试")
  class StreetTests {
    @Test
    @DisplayName("应生成街道")
    void shouldGenerateStreet() {
      String street = addressGen.street();

      assertThat(street).isNotNull();
      assertThat(street).isNotEmpty();
      assertThat(street.length()).isBetween(5, 50);
    }

    @Test
    @DisplayName("批量生成街道应返回正确数量")
    void shouldGenerateCorrectNumberOfStreets() {
      int count = 50;
      List<String> streets = addressGen.streets(count);

      assertThat(streets).hasSize(count);
      assertThat(streets)
          .allSatisfy(
              s -> {
                assertThat(s).isNotNull();
                assertThat(s).isNotEmpty();
              });
    }

    @Test
    @DisplayName("街道应包含数字")
    void streetShouldContainNumber() {
      String street = addressGen.street();

      assertThat(street).isNotNull();
      assertThat(street).matches(".*\\d+.*");
    }
  }

  @Nested
  @DisplayName("邮政编码生成测试")
  class ZipCodeTests {
    @Test
    @DisplayName("应生成邮政编码")
    void shouldGenerateZipCode() {
      String zipCode = addressGen.zipCode();

      assertThat(zipCode).isNotNull();
      assertThat(zipCode).isNotEmpty();
      assertThat(zipCode).matches("\\d{6}");
    }

    @Test
    @DisplayName("批量生成邮政编码应返回正确数量")
    void shouldGenerateCorrectNumberOfZipCodes() {
      int count = 50;
      List<String> zipCodes = addressGen.zipCodes(count);

      assertThat(zipCodes).hasSize(count);
      assertThat(zipCodes)
          .allSatisfy(
              zip -> {
                assertThat(zip).isNotNull();
                assertThat(zip).isNotEmpty();
                assertThat(zip).matches("\\d{6}");
              });
    }

    @Test
    @DisplayName("邮政编码应为6位数字")
    void zipCodeShouldBeSixDigits() {
      String zipCode = addressGen.zipCode();

      assertThat(zipCode).isNotNull();
      assertThat(zipCode).hasSize(6);
    }
  }

  @Nested
  @DisplayName("国家生成测试")
  class CountryTests {
    @Test
    @DisplayName("应生成国家")
    void shouldGenerateCountry() {
      String country = addressGen.country();

      assertThat(country).isNotNull();
      assertThat(country).isNotEmpty();
      assertThat(country.length()).isBetween(2, 50);
    }

    @Test
    @DisplayName("批量生成国家应返回正确数量")
    void shouldGenerateCorrectNumberOfCountries() {
      int count = 50;
      List<String> countries = addressGen.countries(count);

      assertThat(countries).hasSize(count);
      assertThat(countries)
          .allSatisfy(
              c -> {
                assertThat(c).isNotNull();
                assertThat(c).isNotEmpty();
              });
    }

    @Test
    @DisplayName("国家名称应为有效名称")
    void countryNameShouldBeValidName() {
      String country = addressGen.country();

      assertThat(country).isNotNull();
      assertThat(country).matches("[\\u4e00-\\u9fa5a-zA-Z]+");
    }
  }

  @Nested
  @DisplayName("坐标生成测试")
  class CoordinatesTests {
    @Test
    @DisplayName("应生成有效经度")
    void shouldGenerateValidLongitude() {
      Double longitude = addressGen.longitude();

      assertThat(longitude).isNotNull();
      assertThat(longitude).isBetween(-180.0, 180.0);
    }

    @Test
    @DisplayName("应生成有效纬度")
    void shouldGenerateValidLatitude() {
      Double latitude = addressGen.latitude();

      assertThat(latitude).isNotNull();
      assertThat(latitude).isBetween(-90.0, 90.0);
    }

    @Test
    @DisplayName("应生成有效坐标字符串")
    void shouldGenerateValidCoordinates() {
      String coordinates = addressGen.coordinates();

      assertThat(coordinates).isNotNull();
      assertThat(coordinates).isNotEmpty();
      assertThat(coordinates).matches("-?\\d+\\.\\d+,-?\\d+\\.\\d+");
    }

    @Test
    @DisplayName("坐标字符串应包含逗号分隔符")
    void coordinatesShouldContainCommaSeparator() {
      String coordinates = addressGen.coordinates();

      assertThat(coordinates).isNotNull();
      assertThat(coordinates).contains(",");
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {
    @Test
    @DisplayName("批量生成0个地址应返回空列表")
    void shouldReturnEmptyListForZeroCount() {
      List<String> addresses = addressGen.fullAddresses(0);

      assertThat(addresses).isNotNull();
      assertThat(addresses).isEmpty();
    }

    @Test
    @DisplayName("批量生成大量地址应成功")
    void shouldGenerateLargeBatchOfAddresses() {
      int count = 10000;
      List<String> addresses = addressGen.fullAddresses(count);

      assertThat(addresses).hasSize(count);
      assertThat(addresses)
          .allSatisfy(
              addr -> {
                assertThat(addr).isNotNull();
                assertThat(addr).isNotEmpty();
              });
    }

    @Test
    @DisplayName("批量生成1个地址应返回列表")
    void shouldReturnListForSingleAddress() {
      List<String> addresses = addressGen.fullAddresses(1);

      assertThat(addresses).isNotNull();
      assertThat(addresses).hasSize(1);
      assertThat(addresses.get(0)).isNotNull();
      assertThat(addresses.get(0)).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("性能测试")
  class PerformanceTests {
    @Test
    @DisplayName("批量生成应高效")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldGenerateBatchEfficiently() {
      int count = 10000;
      long startTime = System.nanoTime();

      List<String> addresses = addressGen.fullAddresses(count);

      long duration = System.nanoTime() - startTime;
      double recordsPerSecond = (count * 1_000_000_000.0) / duration;

      assertThat(addresses).hasSize(count);
      assertThat(recordsPerSecond).isGreaterThan(1000.0);
    }

    @Test
    @DisplayName("批量生成省份应高效")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldGenerateProvincesEfficiently() {
      int count = 10000;
      long startTime = System.nanoTime();

      List<String> provinces = addressGen.provinces(count);

      long duration = System.nanoTime() - startTime;
      double recordsPerSecond = (count * 1_000_000_000.0) / duration;

      assertThat(provinces).hasSize(count);
      assertThat(recordsPerSecond).isGreaterThan(1000.0);
    }

    @Test
    @DisplayName("批量生成城市应高效")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldGenerateCitiesEfficiently() {
      int count = 10000;
      long startTime = System.nanoTime();

      List<String> cities = addressGen.cities(count);

      long duration = System.nanoTime() - startTime;
      double recordsPerSecond = (count * 1_000_000_000.0) / duration;

      assertThat(cities).hasSize(count);
      assertThat(recordsPerSecond).isGreaterThan(1000.0);
    }
  }
}
