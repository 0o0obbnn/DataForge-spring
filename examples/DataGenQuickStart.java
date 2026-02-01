package com.dataforge.examples;

import com.dataforge.facade.DataGen;
import com.dataforge.facade.PersonGen;
import com.dataforge.facade.InternetGen;
import com.dataforge.facade.FinanceGen;

/**
 * DataGen 快速开始示例
 * 
 * <p>演示如何使用 gen.name() 这种简洁的方式生成测试数据
 * 
 * @author DataForge Team
 * @since 1.0.0
 */
public class DataGenQuickStart {
    
    public static void main(String[] args) {
        // 创建 DataGen 实例
        DataGen gen = new DataGen();
        
        System.out.println("=== DataGen 快速开始示例 ===\n");
        
        // 1. 基础使用 - 直接调用方法
        System.out.println("1. 基础使用：");
        System.out.println("   UUID: " + gen.uuid());
        System.out.println("   姓名: " + gen.name());
        System.out.println("   邮箱: " + gen.email());
        System.out.println("   电话: " + gen.phone());
        System.out.println("   银行卡: " + gen.bankCard());
        System.out.println();
        
        // 2. 使用分类门面 - 更多选项
        System.out.println("2. 使用分类门面：");
        
        // 个人信息
        PersonGen person = gen.person();
        System.out.println("   中文姓名: " + person.chineseName());
        System.out.println("   英文姓名: " + person.englishName());
        System.out.println("   年龄: " + person.age(18, 65));
        System.out.println("   手机: " + person.mobile());
        System.out.println();
        
        // 互联网数据
        InternetGen internet = gen.internet();
        System.out.println("   邮箱: " + internet.email());
        System.out.println("   URL: " + internet.url());
        System.out.println("   IPv4: " + internet.ipv4());
        System.out.println("   用户名: " + internet.username());
        System.out.println();
        
        // 金融数据
        FinanceGen finance = gen.finance();
        System.out.println("   银行卡: " + finance.bankCard());
        System.out.println("   信用卡: " + finance.creditCard());
        System.out.println("   金额: " + finance.amount(100.0, 10000.0));
        System.out.println();
        
        // 3. 批量生成
        System.out.println("3. 批量生成 5 个姓名：");
        gen.names(5).forEach(name -> System.out.println("   - " + name));
        System.out.println();
        
        // 4. 生成完整用户数据示例
        System.out.println("4. 生成完整用户数据：");
        for (int i = 1; i <= 3; i++) {
            System.out.println("   用户 " + i + ":");
            System.out.println("     ID: " + gen.uuid());
            System.out.println("     姓名: " + person.chineseName());
            System.out.println("     年龄: " + person.age(18, 65));
            System.out.println("     邮箱: " + internet.email());
            System.out.println("     电话: " + person.mobile());
            System.out.println("     银行卡: " + finance.bankCard());
            System.out.println();
        }
        
        System.out.println("=== 示例完成 ===");
        System.out.println("\n提示：使用 gen.name() 这种简洁的方式，就像使用 Faker 一样简单！");
    }
}

