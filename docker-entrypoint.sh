#!/bin/sh
# Render injects a PORT env var and routes external traffic to it.
# Tomcat's server.xml hardcodes port 8080, so patch it at startup.
set -e

PORT="${PORT:-8080}"
sed -i "s/port=\"8080\"/port=\"${PORT}\"/" /usr/local/tomcat/conf/server.xml
sed -i 's/port="8005" shutdown="SHUTDOWN"/port="-1" shutdown="SHUTDOWN"/' /usr/local/tomcat/conf/server.xml

exec catalina.sh run
