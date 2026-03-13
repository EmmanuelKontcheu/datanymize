@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM
@REM Required ENV vars:
@REM JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars
@REM M2_HOME - location of maven's installed home (optional)
@REM MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the batch commands
@REM MAVEN_BATCH_PAUSE - set to 'on' to wait for a keystroke before ending
@REM MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM     e.g. to debug Maven itself, use
@REM set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@setlocal

set ERROR_CODE=0

@REM To isolate internal variables from possible post scripts, we use another setlocal
@setlocal

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%" == "" goto OkJHome

echo.
echo Error: JAVA_HOME not found in your environment. >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
echo.
goto error

:OkJHome
if exist "%JAVA_HOME%\bin\java.exe" goto init

echo.
echo Error: JAVA_HOME is set to an invalid directory. >&2
echo JAVA_HOME = "%JAVA_HOME%" >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
echo.
goto error

@REM ==== END VALIDATION ====

:init

@REM Find the project base dir, i.e. the directory that contains the folder ".mvn".
@REM Fallback to current working directory if not found.

set MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
IF "%MAVEN_PROJECTBASEDIR%"=="" (
set MAVEN_PROJECTBASEDIR=%CD%
)

set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

set DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") DO (
    if "%%A"=="distributionUrl" set DOWNLOAD_URL=%%B
)

@REM This goes in the command line:
@REM for /f "usebackq tokens=1,2 delims==" %%A in ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") do (
@REM     if "%%A"=="distributionUrl" (
@REM         set DOWNLOAD_URL=%%B
@REM     )
@REM )

@REM if not "%DOWNLOAD_URL%"=="" (
@REM     powershell -Command "&{"^
@REM         "$webclient = new-object System.Net.WebClient;"^
@REM         "if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%') -and [string]::IsNullOrEmpty('%MVNW_PASSWORD%'))) {"^
@REM         "$webclient.Credentials = new-object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');"^
@REM         "}"^
@REM         "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%DOWNLOAD_URL%', '%WRAPPER_JAR%')"^
@REM         "}"
@REM     echo Finished downloading %WRAPPER_JAR%
@REM )
if exist "%WRAPPER_JAR%" (
    if "%MVNW_VERBOSE%" == "true" (
        echo Found %WRAPPER_JAR%
    )
) else (
    if not "%MVNW_REPOURL%" == "" (
        set DOWNLOAD_URL="%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    )
    if "%MVNW_VERBOSE%" == "true" (
        echo Couldn't find %WRAPPER_JAR%, downloading it ...
        echo Downloading from: %DOWNLOAD_URL%
    )

    powershell -Command "&{"^
		"$webclient = new-object System.Net.WebClient;"^
		"if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%') -and [string]::IsNullOrEmpty('%MVNW_PASSWORD%'))) {"^
		"$webclient.Credentials = new-object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');"^
		"}"^
		"[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%DOWNLOAD_URL%', '%WRAPPER_JAR%')"^
		"}"
    if "%ERRORLEVEL%" == "0" (
        if "%MVNW_VERBOSE%" == "true" (
            echo Finished downloading %WRAPPER_JAR%
        )
    ) else (
        echo Error downloading %WRAPPER_JAR%
        exit /b 1
    )
)
@REM End of extension

@REM If specified, validate the SHA-256 sum of the Maven wrapper jar
set WRAPPER_SHA_256_SUM=""
FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") DO (
    if "%%A"=="wrapperSha256Sum" set WRAPPER_SHA_256_SUM=%%B
)
if not %WRAPPER_SHA_256_SUM%=="" (
    powershell -Command "&{"^
       "$hash = (Get-FileHash '%WRAPPER_JAR%' -Algorithm SHA256).Hash.ToLower();"^
       "if ('%WRAPPER_SHA_256_SUM%' -ne $hash) {"^
           "Write-Output 'Error: Failed to validate Maven wrapper SHA-256, your Maven wrapper might be compromised.'"^
           "Write-Output 'Investigate or delete %WRAPPER_JAR% to attempt a clean download.'"^
           "Write-Output 'If you updated your Maven version, please update the specified wrapperSha256Sum property.'"^
           "exit 1"^
       "}"^
       "}"
    if "%ERRORLEVEL%" == "0" (
        if "%MVNW_VERBOSE%" == "true" (
            echo Validated Maven wrapper SHA-256
        )
    ) else (
        exit /b 1
    )
)

@REM Provide a "standardized" way to retrieve the CLI args that will
@REM work with both Windows and non-Windows executions.
if "%OS%"=="Windows_NT" (
  set "APP_HOME=%~dp0"
) else (
  set "APP_HOME=`pwd`"
)

@REM Add default JVM options here. You can also use MAVEN_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx1024m" "-Xms1024m"

@REM Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@REM Increase the default stack size if needed
if "%MAVEN_OPTS%" == "" set MAVEN_OPTS=%DEFAULT_JVM_OPTS%

@setlocal enabledelayedexpansion
for /F "usebackq delims=" %%a in ("%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config") do set "JVM_CONFIG_MAVEN_PROPS=!JVM_CONFIG_MAVEN_PROPS! %%a"
@endlocal & set JVM_CONFIG_MAVEN_PROPS=%JVM_CONFIG_MAVEN_PROPS%

"%JAVA_EXE%" %JVM_CONFIG_MAVEN_PROPS% %MAVEN_OPTS% %MAVEN_DEBUG_OPTS% -classpath %WRAPPER_JAR% "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable MAVEN_EXIT_ERROR_CODE if you want to exit with a different code than 1
if not "%MAVEN_EXIT_ERROR_CODE%"=="" (
  set "EXIT_CODE=%MAVEN_EXIT_ERROR_CODE%"
) else (
  set "EXIT_CODE=1"
)

if not "%MAVEN_SKIP_RC%"=="" goto skipRcPost
@REM check for post script, e.g. mavenrc_post.bat
if exist "%MAVEN_PROJECTBASEDIR%\mavenrc_post.bat" call "%MAVEN_PROJECTBASEDIR%\mavenrc_post.bat"
:skipRcPost

@endlocal & exit /b %EXIT_CODE%
