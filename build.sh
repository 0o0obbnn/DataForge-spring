#!/bin/bash

# DataForge 构建脚本
# 用于编译和打包DataForge项目

set -e

echo "========================================="
echo "DataForge Build Script"
echo "========================================="

# 检查Java版本
echo "Checking Java version..."
java -version

# 检查Maven版本
echo "Checking Maven version..."
mvn -version

# 清理项目
echo "Cleaning project..."
mvn clean

# 编译项目
echo "Compiling project..."
mvn compile

# 运行测试
echo "Running tests..."
mvn test

# 打包项目
echo "Packaging project..."
mvn package -DskipTests

echo "========================================="
echo "Build completed successfully!"
echo "========================================="

# 显示生成的JAR文件
echo "Generated JAR files:"
find . -name "*.jar" -type f | grep -v ".m2" | sort

echo ""
echo "To run DataForge CLI:"
echo "java -jar data-forge-cli/target/data-forge-cli-1.0.0-SNAPSHOT.jar --help"