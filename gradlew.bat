@echo off
REM -----------------------------------------------------------------------------
REM Gradle start up script for Windows
REM -----------------------------------------------------------------------------

SET DIRNAME=%~dp0
SET APP_BASE_NAME=%~n0
SET GRADLE_HOME=%DIRNAME%

REM Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
SET DEFAULT_JVM_OPTS=

REM Find java.exe
IF NOT DEFINED JAVA_HOME (
    SET JAVA_EXE=java
) ELSE (
    SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -classpath "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
