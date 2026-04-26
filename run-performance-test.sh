#!/bin/bash

# DataForge 性能测试脚本
# 用于测试不同数据量级下的生成性能

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查Java环境
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java未安装，请先安装Java 17+"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        print_error "Java版本过低，需要Java 17+，当前版本: $JAVA_VERSION"
        exit 1
    fi
    
    print_success "Java环境检查通过: Java $JAVA_VERSION"
}

# 构建项目
build_project() {
    print_info "开始构建项目..."
    mvn clean package -DskipTests -q
    
    if [ $? -eq 0 ]; then
        print_success "项目构建成功"
    else
        print_error "项目构建失败"
        exit 1
    fi
}

# 运行性能测试
run_test() {
    local test_name=$1
    local config_file=$2
    local jvm_args=$3
    
    print_info "=========================================="
    print_info "开始测试: $test_name"
    print_info "配置文件: $config_file"
    print_info "JVM参数: $jvm_args"
    print_info "=========================================="
    
    # 记录开始时间
    START_TIME=$(date +%s)
    
    # 运行测试
    java $jvm_args -jar data-forge-cli/target/data-forge-cli-1.0.0-SNAPSHOT.jar \
        --config "$config_file"
    
    # 记录结束时间
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    
    print_success "测试完成: $test_name"
    print_info "耗时: ${DURATION}秒 ($(($DURATION / 60))分钟)"
    
    # 检查输出文件
    if [ -f "output/performance_test_*.csv" ]; then
        FILE_SIZE=$(du -h output/performance_test_*.csv | cut -f1)
        print_info "输出文件大小: $FILE_SIZE"
    fi
    
    echo ""
}

# 主函数
main() {
    print_info "DataForge 性能测试开始"
    echo ""
    
    # 检查环境
    check_java
    
    # 构建项目
    build_project
    
    # 创建输出目录
    mkdir -p output
    
    # 测试场景选择
    echo ""
    print_info "请选择测试场景:"
    echo "1) 小数据量测试 (1万条, < 1秒)"
    echo "2) 中等数据量测试 (100万条, 1-2分钟)"
    echo "3) 大数据量测试 (1000万条, 10-20分钟)"
    echo "4) 超大数据量测试 (1亿条, 1-2小时)"
    echo "5) 全部测试"
    echo ""
    read -p "请输入选项 (1-5): " choice
    
    case $choice in
        1)
            run_test "小数据量测试" "examples/performance-test-config.yml" "-Xms512m -Xmx1g"
            ;;
        2)
            run_test "中等数据量测试" "examples/performance-test-config.yml" "-Xms2g -Xmx4g -XX:+UseG1GC"
            ;;
        3)
            run_test "大数据量测试" "examples/performance-test-config.yml" "-Xms4g -Xmx8g -XX:+UseG1GC"
            ;;
        4)
            print_warning "超大数据量测试需要大量时间和资源，确认继续? (y/n)"
            read -p "> " confirm
            if [ "$confirm" = "y" ]; then
                run_test "超大数据量测试" "examples/performance-test-config.yml" \
                    "-Xms32g -Xmx64g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
            fi
            ;;
        5)
            run_test "小数据量测试" "examples/performance-test-config.yml" "-Xms512m -Xmx1g"
            run_test "中等数据量测试" "examples/performance-test-config.yml" "-Xms2g -Xmx4g -XX:+UseG1GC"
            run_test "大数据量测试" "examples/performance-test-config.yml" "-Xms4g -Xmx8g -XX:+UseG1GC"
            ;;
        *)
            print_error "无效选项"
            exit 1
            ;;
    esac
    
    echo ""
    print_success "所有测试完成！"
    print_info "测试结果保存在 output/ 目录"
}

# 执行主函数
main

