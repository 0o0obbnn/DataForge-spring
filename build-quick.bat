@echo off
chcp 65001 >nul

echo === DataForge 快速构建脚本 ===

REM 检查Java环境
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到Java环境，请确保已安装Java 17+
    pause
    exit /b 1
)

REM 检查Maven环境
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到Maven环境，请确保已安装Maven 3.8+
    pause
    exit /b 1
)

REM 快速构建项目（跳过代码质量检查）
echo 正在快速构建项目...
mvn clean compile -DskipTests -Dpmd.skip=true -Dcheckstyle.skip=true -q

if %errorlevel% neq 0 (
    echo 错误: 项目构建失败
    pause
    exit /b 1
)

echo 项目构建成功！

REM 安装core模块到本地仓库
echo 正在安装core模块...
cd data-forge-core
mvn install -DskipTests -Dpmd.skip=true -Dcheckstyle.skip=true -q
cd ..

if %errorlevel% neq 0 (
    echo 错误: core模块安装失败
    pause
    exit /b 1
)

echo core模块安装成功！

REM 构建Web模块
echo 正在构建Web模块...
cd data-forge-web
mvn package -DskipTests -q
cd ..

if %errorlevel% neq 0 (
    echo 错误: Web模块构建失败
    pause
    exit /b 1
)

echo Web模块构建成功！

echo.
echo === 构建完成 ===
echo 可以使用以下命令启动Web应用:
echo   java -jar data-forge-web\target\data-forge-web-1.0.0-SNAPSHOT.jar
echo.
echo 或者运行: run-web.bat

pause