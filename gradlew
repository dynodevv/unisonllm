#!/bin/bash

# Gradle wrapper script for Unix-like systems
# This downloads and runs the Gradle distribution specified in gradle-wrapper.properties

GRADLE_WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"

# Download gradle wrapper jar if needed
if [ ! -f "$GRADLE_WRAPPER_JAR" ]; then
    echo "Downloading Gradle Wrapper..."
    mkdir -p gradle/wrapper
    curl -sL "https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar" -o "$GRADLE_WRAPPER_JAR"
fi

# Execute gradle
exec java -jar "$GRADLE_WRAPPER_JAR" "$@"
