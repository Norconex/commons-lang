#!/bin/sh
# Copyright 2017 Norconex Inc.
# 
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
cd $(dirname $0)

argerror=false
if [ "$#" -eq 3 ] && [ "$1" != "-k" ] && [ "$1" != "-f" ] && [ "$1" != "-e" ] && [ "$1" != "-p" ]; then
    echo "Invalid argument: $1"
    argerror=true
fi
if [ "$#" -ne 3 ]; then
    argerror=true
fi
if [ "$argerror" = true ]; then
    echo ""
    echo "Usage: $0 -k|-f|-e|-p key text"
    echo ""
    echo "Decrypt a string using a custom key."
    echo ""
    echo "Arguments:"
    echo "  -k       key is the encryption key"
    echo "  -f       key is the file containing the encryption key"
    echo "  -e       key is the environment variable holding the encryption key"
    echo "  -p       key is the system property holding the encryption key"
    echo "  key      the encryption key (or file, or env. variable, etc.)"
    echo "  text     text to decrypt"
    exit 1
fi

java -Dfile.encoding=UTF8 -cp "./lib/*:../lib/*" com.norconex.commons.lang.encrypt.EncryptionUtil decrypt "$@"

