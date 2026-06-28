@echo off
REM Copyright 2017 Norconex Inc.
REM 
REM Licensed under the Apache License, Version 2.0 (the "License");
REM you may not use this file except in compliance with the License.
REM You may obtain a copy of the License at
REM 
REM    http://www.apache.org/licenses/LICENSE-2.0
REM 
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
cd %~dp0

if [%1]==[]      goto usage

java -Dfile.encoding=UTF8 -cp "./lib/*;../lib/*" com.norconex.commons.lang.jar.JarDuplicateFinder %*

goto :eof

:usage
echo.
echo Usage: %~nx0 path [path] [path] [...]
echo.
echo Detect duplicate jars by comparing specified locations.
echo.
echo Arguments:
echo   path     one or more directories or jar files to check for duplicates
echo.

exit /B 1


