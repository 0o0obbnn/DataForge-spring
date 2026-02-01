@echo off
REM DataForge 性能测试脚本 (Windows版本)
REM 用于测试不同数据量级下的生成性能

setlocal enabledelayedexpansion

echo ========================================
echo DataForge 性能测试
echo ========================================
echo.

REM 检查Java环境
echo [INFO] 检查Java环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java未安装，请先安装Java 17+
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
echo [SUCCESS] Java环境检查通过: %JAVA_VERSION%
echo.

REM 构建项目
echo [INFO] 开始构建项目...
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo [ERROR] 项目构建失败
    exit /b 1
)
echo [SUCCESS] 项目构建成功
echo.

REM 创建输出目录
if not exist "output" mkdir output

REM 测试场景选择
echo [INFO] 请选择测试场景:
echo 1) 小数据量测试 (1万条, ^< 1秒)
echo 2) 中等数据量测试 (100万条, 1-2分钟)
echo 3) 大数据量测试 (1000万条, 10-20分钟)
echo 4) 超大数据量测试 (1亿条, 1-2小时)
echo 5) 全部测试
echo.
set /p choice="请输入选项 (1-5): "

if "%choice%"=="1" goto test1
if "%choice%"=="2" goto test2
if "%choice%"=="3" goto test3
if "%choice%"=="4" goto test4
if "%choice%"=="5" goto testall
echo [ERROR] 无效选项
exit /b 1

:test1
call :run_test "小数据量测试" "examples/performance-test-config.yml" "-Xms512m -Xmx1g"
goto end

:test2
call :run_test "中等数据量测试" "examples/performance-test-config.yml" "-Xms2g -Xmx4g -XX:+UseG1GC"
goto end

:test3
call :run_test "大数据量测试" "examples/performance-test-config.yml" "-Xms4g -Xmx8g -XX:+UseG1GC"
goto end

:test4
echo [WARNING] 超大数据量测试需要大量时间和资源，确认继续? (y/n)
set /p confirm="> "
if /i "%confirm%"=="y" (
    call :run_test "超大数据量测试" "examples/performance-test-config.yml" "-Xms32g -Xmx64g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
)
goto end

:testall
call :run_test "小数据量测试" "examples/performance-test-config.yml" "-Xms512m -Xmx1g"
call :run_test "中等数据量测试" "examples/performance-test-config.yml" "-Xms2g -Xmx4g -XX:+UseG1GC"
call :run_test "大数据量测试" "examples/performance-test-config.yml" "-Xms4g -Xmx8g -XX:+UseG1GC"
goto end

:run_test
set test_name=%~1
set config_file=%~2
set jvm_args=%~3

echo.
echo ==========================================
echo [INFO] 开始测试: %test_name%
echo [INFO] 配置文件: %config_file%
echo [INFO] JVM参数: %jvm_args%
echo ==========================================
echo.

REM 记录开始时间
set start_time=%time%

REM 运行测试
java %jvm_args% -jar data-forge-cli/target/data-forge-cli-1.0.0-SNAPSHOT.jar --config "%config_file%"

REM 记录结束时间
set end_time=%time%

echo.
echo [SUCCESS] 测试完成: %test_name%
echo [INFO] 开始时间: %start_time%
echo [INFO] 结束时间: %end_time%

REM 检查输出文件
if exist "output\performance_test_*.csv" (
    for %%F in (output\performance_test_*.csv) do (
        echo [INFO] 输出文件: %%F
        echo [INFO] 文件大小: %%~zF 字节
    )
)

echo.
goto :eof

:end
echo.
echo [SUCCESS] 所有测试完成！
echo [INFO] 测试结果保存在 output\ 目录
echo.
pause

