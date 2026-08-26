@ECHO OFF

SET DIRNAME=%~dp0
IF "%DIRNAME%" == "" SET DIRNAME=.
SET APP_BASE_NAME=%~n0
SET APP_HOME=%DIRNAME%

@rem Use the Java runtime from JAVA_HOME when available.
IF DEFINED JAVA_HOME GOTO findJavaFromJavaHome

SET JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
IF %ERRORLEVEL% EQU 0 GOTO execute
GOTO fail

:findJavaFromJavaHome
SET JAVA_HOME=%JAVA_HOME:"=%
SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
IF EXIST "%JAVA_EXE%" GOTO execute
GOTO fail

:execute
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
GOTO end

:fail
ECHO ERROR: JAVA_HOME is not set and no java.exe could be found.
EXIT /B 1

:end
