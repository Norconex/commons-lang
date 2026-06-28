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

if [%3]==[]      goto usage
if [%1] NEQ [-k] if [%1] NEQ [-f] if [%1] NEQ [-e] if [%1] NEQ [-p] goto invalidArg

java -Dfile.encoding=UTF8 -cp "./lib/*;../lib/*" com.norconex.commons.lang.encrypt.EncryptionUtil decrypt %*

goto :eof

:invalidArg
echo Invalid argument: %1
goto usage

:usage
echo.
echo Usage: %~nx0 -k^|-f^|-e^|-p key text
echo.
echo Decrypt a string using a custom key.
echo.
echo Arguments:
echo   -k       key is the encryption key
echo   -f       key is the file containing the encryption key
echo   -e       key is the environment variable holding the encryption key
echo   -p       key is the system property holding the encryption key
echo   key      the encryption key (or file, or env. variable, etc.)
echo   text     text to decrypt
echo.

exit /B 1
