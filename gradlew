#!/bin/sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || exit 1

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=java
fi

if ! "$JAVACMD" -version >/dev/null 2>&1; then
    echo "A Java 17 runtime is required. Set JAVA_HOME to Android Studio's bundled JDK." >&2
    exit 1
fi

exec "$JAVACMD" \
    -Xmx64m \
    -Xms64m \
    -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
