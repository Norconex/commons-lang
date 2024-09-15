#!/bin/sh
# Copyright 2017-2024 Norconex Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#    http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
cd $(dirname $0)

echo ""
echo "PLEASE READ CAREFULLY"
echo ""
echo "To manually install this component and its dependencies into an"
echo "existing installation, please specify the target directory where" 
echo "Java libraries are stored (.jar files). This is often a \"lib\" directory."
echo ""
echo "If .jar duplicates are found, you will be asked how you wish to deal with"
echo "them. It is recommended to try keep most recent versions upon encountering"
echo "version conflicts. When in doubt, simply choose the default option."
echo ""

java -Dfile.encoding=UTF8 -cp "./lib/*:../lib/*" com.norconex.commons.lang.jar.JarCopier "./lib"