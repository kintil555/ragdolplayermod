#!/bin/sh

##############################################################################
# Gradle start up script for UN*X
##############################################################################
APP_HOME=$(cd "$(dirname "$0")" && pwd)
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_PROPERTIES="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

# Find Java
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=java
fi

exec "$JAVACMD" -jar "$WRAPPER_JAR" "$@"
