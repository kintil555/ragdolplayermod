#!/bin/sh
#
# Gradle start up script for POSIX

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=java
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVACMD" \
    -Xmx64m -Xms64m \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
