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

echo.
echo PLEASE READ CAREFULLY
echo.
echo To install this component and its dependencies into another product, 
echo please specify the target product directory where libraries (.jar files)
echo can be found.
echo.
echo This is often a "lib" directory. For example, to install this component  
echo into the Norconex HTTP Collector, specify the full path to the Collector
echo "lib" directory, which may look somewhat like this:
echo. 
echo    C:\MyProject\norconex-collector-http-x.x.x\lib
echo.
echo If .jar duplicates are found, you will be asked how you wish to deal with
echo them. It is recommended to try keep most recent versions upon encountering
echo version conflicts. When in doubt, simply choose the default option.
echo.

java -Dfile.encoding=UTF8 -cp "./lib/*;../lib/*" com.norconex.commons.lang.jar.JarCopier ./lib

pause