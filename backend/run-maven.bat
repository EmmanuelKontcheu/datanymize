@echo off
setlocal enabledelayedexpansion

set MAVEN_PROJECTBASEDIR=%CD%
set JAVA_HOME=C:\Program Files\Java\jdk-21

"%JAVA_HOME%\bin\java.exe" ^
  -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" ^
  -classpath ".\.mvn\wrapper\maven-wrapper.jar" ^
  org.apache.maven.wrapper.MavenWrapperMain ^
  %*

endlocal
