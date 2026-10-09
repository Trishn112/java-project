@echo off
setlocal
if exist "%USERPROFILE%\.m2\apache-maven-3.9.6\bin\mvn.cmd" (
    "%USERPROFILE%\.m2\apache-maven-3.9.6\bin\mvn.cmd" %*
) else (
    mvn %*
)
