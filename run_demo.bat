@echo off
cd /d "%~dp0"
echo Compiling FastShip...
if not exist "target\classes" mkdir "target\classes"
dir /s /b src\main\java\*.java > target\sources.txt
javac -encoding UTF-8 -d target\classes @target\sources.txt
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
echo Launching FastShip GUI...
set "FASTSHIP_DB_URL=jdbc:mysql://localhost:3307/fastship?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&connectTimeout=1000"
set MYSQL_JAR=%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar
if exist "%MYSQL_JAR%" (
    java -cp "target\classes;%MYSQL_JAR%" fastship.ui.FastShipApp
) else (
    java -cp "target\classes" fastship.ui.FastShipApp
)
