#!/bin/sh
#
# Gradle start up script for UN*X
#
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Resolve links: $0 may be a link
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)'`
    if expr "$link" : '/.*' > /dev/null; then PRG="$link"
    else PRG=`dirname "$PRG"`"/$link"; fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

GRADLE_VERSION="8.8"
GRADLE_DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_HOME="${HOME}/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin"

if [ ! -d "$GRADLE_HOME" ]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    mkdir -p "$GRADLE_HOME"
    TMPFILE=$(mktemp /tmp/gradle-XXXXXX.zip)
    curl -sL "$GRADLE_DIST_URL" -o "$TMPFILE"
    unzip -q "$TMPFILE" -d "$GRADLE_HOME"
    rm "$TMPFILE"
fi

GRADLE_BIN=$(find "$GRADLE_HOME" -name "gradle" -type f | head -1)
exec "$GRADLE_BIN" "$@"
