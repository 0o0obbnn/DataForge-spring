# DataGen 使用指南

## 📖 简介

DataGen 是 DataForge 项目的简洁门面接口，提供类似 Faker 库的使用体验。通过 `gen.name()` 这种简单的方式即可生成各种测试数据。

## 🚀 快速开始

### 基础使用

```java
// 创建 DataGen 实例
DataGen gen = new DataGen();

// 生成基础数据
String uuid = gen.uuid();
String name = gen.name();
String email = gen.email();
String phone = gen.phone();
String bankCard = gen.bankCard();
```

### 批量生成

```java
// 批量生成数据
List<String> names = gen.names(10);
List<String> emails = gen.emails(10);
List<String> phones = gen.phones(10);
```

## 📚 分类门面

### 1. PersonGen - 个人信息

```java
PersonGen person = gen.person();

// 姓名生成
String fullName = person.fullName();
String chineseName = person.chineseName();
String maleName = person.chineseName("MALE");
String englishName = person.englishName();
String lastName = person.lastName();
String firstName = person.firstName();

// 联系方式
String phone = person.phone();
String mobile = person.mobile();
String idCard = person.idCard();

// 年龄和性别
Integer age = person.age();
Integer teenAge = person.age(13, 19);
String gender = person.gender();

// 批量生成
List<String> names = person.fullNames(10);
List<String> phones = person.phones(10);
```

### 2. InternetGen - 互联网数据

```java
InternetGen internet = gen.internet();

// 网络信息
String email = internet.email();
String customEmail = internet.email("example.com");
String url = internet.url();
String domain = internet.domain();
String username = internet.username();

// IP 地址
String ipv4 = internet.ipv4();
String ipv6 = internet.ipv6();
String mac = internet.macAddress();

// 安全相关
String password = internet.password();
String strongPassword = internet.password(16);

// 其他
String colorHex = internet.colorHex();

// 批量生成
List<String> emails = internet.emails(10);
List<String> urls = internet.urls(10);
```

### 3. AddressGen - 地址信息

```java
AddressGen address = gen.address();

// 地址组件
String fullAddress = address.fullAddress();
String province = address.province();
String city = address.city();
String district = address.district();
String street = address.street();
String zipCode = address.zipCode();
String country = address.country();

// 地理坐标
Double longitude = address.longitude();
Double latitude = address.latitude();
String coordinates = address.coordinates();

// 批量生成
List<String> addresses = address.fullAddresses(10);
List<String> provinces = address.provinces(10);
```

### 4. FinanceGen - 金融数据

```java
FinanceGen finance = gen.finance();

// 银行卡
String bankCard = finance.bankCard();
String creditCard = finance.creditCard();

// 金额
Double amount = finance.amount();
Double price = finance.amount(100.0, 10000.0);

// 货币
String currencyCode = finance.currencyCode();
String currencyName = finance.currencyName();
String currencySymbol = finance.currencySymbol();

// 国际标准
String iban = finance.iban();
String bic = finance.bic();

// 批量生成
List<String> bankCards = finance.bankCards(10);
```

### 5. DateTimeGen - 日期时间

```java
DateTimeGen dateTime = gen.dateTime();

// 基础生成
String date = dateTime.date();
String customDate = dateTime.date("yyyy-MM-dd");
String time = dateTime.time();
String dateTimeStr = dateTime.dateTime();

// 时间戳
String timestamp = dateTime.timestamp();
String timestampSec = dateTime.timestampSeconds();

// 相对日期
String pastDate = dateTime.pastDate();
String past30Days = dateTime.pastDate(30);
String futureDate = dateTime.futureDate();
String future7Days = dateTime.futureDate(7);

// 日期组件
Integer year = dateTime.year();
Integer month = dateTime.month();
String weekday = dateTime.weekday();

// 批量生成
List<String> dates = dateTime.dates(10);
List<String> timestamps = dateTime.timestamps(10);
```

## 💡 完整示例

### 生成用户数据

```java
DataGen gen = new DataGen();
PersonGen person = gen.person();
InternetGen internet = gen.internet();
FinanceGen finance = gen.finance();

// 生成 10 个用户
for (int i = 0; i < 10; i++) {
    System.out.println("用户 " + (i + 1) + ":");
    System.out.println("  ID: " + gen.uuid());
    System.out.println("  姓名: " + person.chineseName());
    System.out.println("  年龄: " + person.age(18, 65));
    System.out.println("  邮箱: " + internet.email());
    System.out.println("  电话: " + person.mobile());
    System.out.println("  银行卡: " + finance.bankCard());
    System.out.println();
}
```

## 🎯 最佳实践

### 1. 在 Spring 项目中使用

```java
@Service
public class UserService {
    
    @Autowired
    private DataGen gen;
    
    public User createTestUser() {
        PersonGen person = gen.person();
        InternetGen internet = gen.internet();
        
        User user = new User();
        user.setId(gen.uuid());
        user.setName(person.chineseName());
        user.setAge(person.age(18, 65));
        user.setEmail(internet.email());
        user.setPhone(person.mobile());
        
        return user;
    }
}
```

### 2. 在测试中使用

```java
@Test
public void testUserCreation() {
    DataGen gen = new DataGen();
    PersonGen person = gen.person();
    
    // 生成测试数据
    String name = person.chineseName();
    String email = gen.internet().email();
    
    // 执行测试
    User user = userService.create(name, email);
    
    assertNotNull(user);
    assertEquals(name, user.getName());
}
```

## 📊 与原 API 对比

| 功能 | 原有方式 | DataGen 方式 |
|------|----------|--------------|
| 生成姓名 | 配置文件 + CLI | `gen.name()` |
| 生成邮箱 | 配置文件 + CLI | `gen.email()` |
| 批量生成 | 循环 + 配置 | `gen.names(10)` |
| 参数化 | YAML 配置 | `gen.name("CN", "MALE")` |
| IDE 支持 | 无 | 完整代码提示 |
| 类型安全 | 运行时 | 编译时 |

## 🔧 高级用法

### 自定义参数

```java
// 使用 Map 传递自定义参数
Map<String, Object> params = Map.of(
    "type", "CN",
    "gender", "MALE",
    "use_weight", true
);
String name = gen.generate("name", params);
```

### 扩展新的门面

```java
public class CompanyGen {
    private final DataGen gen;
    
    public CompanyGen(DataGen gen) {
        this.gen = gen;
    }
    
    public String companyName() {
        return gen.generate("company");
    }
}
```

## 📝 运行示例

```bash
# 编译项目
mvn clean compile -DskipTests

# 运行快速开始示例
java -cp data-forge-core/target/classes com.dataforge.examples.DataGenQuickStart
```

## ✅ 总结

DataGen 提供了：

- ✅ **简洁的 API** - `gen.name()` 一行代码搞定
- ✅ **分类门面** - 按功能分组，易于查找
- ✅ **批量支持** - 一次生成多条数据
- ✅ **类型安全** - 编译时检查，减少错误
- ✅ **IDE 友好** - 完整的代码提示和文档
- ✅ **向后兼容** - 不影响现有配置文件方式

---

**版本**: 1.0.0-SNAPSHOT  
**更新日期**: 2026-01-17  
**作者**: DataForge Team

