@ECHO OFF
SETLOCAL
SET "MVNW_VERSION=3.9.11"
SET "BASE_DIR=%~dp0"
SET "MAVEN_HOME=%BASE_DIR%.mvn\apache-maven-%MVNW_VERSION%"
IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
  IF NOT EXIST "%BASE_DIR%.mvn" MKDIR "%BASE_DIR%.mvn"
  SET "ARCHIVE=%BASE_DIR%.mvn\apache-maven-%MVNW_VERSION%-bin.zip"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVNW_VERSION%/apache-maven-%MVNW_VERSION%-bin.zip' -OutFile '%BASE_DIR%.mvn\apache-maven-%MVNW_VERSION%-bin.zip'; Expand-Archive -Force '%BASE_DIR%.mvn\apache-maven-%MVNW_VERSION%-bin.zip' '%BASE_DIR%.mvn'; Remove-Item '%BASE_DIR%.mvn\apache-maven-%MVNW_VERSION%-bin.zip'"
)
CALL "%MAVEN_HOME%\bin\mvn.cmd" %*
ENDLOCAL

