#!/bin/sh
cd $(dirname $0)

echo ""
echo "PLEASE READ CAREFULLY"
echo ""
echo "To install this component and its dependencies into another product,"
echo "please specify the target product directory where libraries (.jar files)"
echo "can be found."
echo ""
echo "This is often a "lib" directory. For example, to install this component"
echo "into the Norconex HTTP Collector, specify the full path to the Collector"
echo "\"lib\" directory, which may look somewhat like this:"
echo ""
echo "   /myProject/norconex-collector-http-x.x.x/lib"
echo ""
echo "If .jar duplicates are found, you will be asked how you wish to deal with"
echo "them. It is recommended to try keep most recent versions upon encountering"
echo "version conflicts. When in doubt, simply choose the default option."
echo ""

java -Dfile.encoding=UTF8 -cp "./lib/*:../lib/*" com.norconex.commons.lang.jar.JarCopier "./lib"