#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

# Resolve links - $0 may be a link
PRG="$0"
while [ -h "$PRG" ]; do
  LS=$(ls -ld "$PRG")
  LINK=$(expr "$LS" : '.*-> \(.*\)$')
  if expr "$LINK" : '/.*' > /dev/null; then
    PRG="$LINK"
  else
    PRG=$(dirname "$PRG")/"$LINK"
  fi
done

PRG_DIR=$(dirname "$PRG")
EXECUTABLE="$PRG_DIR/gradle"

CLASSPATH=$PRG_DIR/gradle-wrapper.jar

# Determine Java command to use
if [ -n "$JAVA_HOME" ]; then
  JAVA_HOME="$JAVA_HOME"
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD=$(which java)
fi

if [ ! -x "$JAVACMD" ]; then
  echo "ERROR: JAVA_HOME is not defined correctly."
  echo "  We cannot execute $JAVACMD"
  exit 1
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
