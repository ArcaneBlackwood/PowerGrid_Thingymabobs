#!/bin/bash
killtree() {
	local pid=$1
	for child in $(ps -o pid= --ppid "$pid"); do
		killtree "$child"
	done
  echo "Sending TERM to #$pid"
	kill -TERM "$pid" 2>/dev/null
}

TEMP_DIR="/tmp/gradlelogs"
LOG_FILE="$TEMP_DIR/$1$(printf '%x' "$$")"

cd "$(dirname -- "${BASH_SOURCE[0]}")/../"
mkdir -p "$TEMP_DIR/"
rm -f "$LOG_FILE"
touch "$LOG_FILE"
GRADLE_PID=

{ sleep 0.1; ./gradlew "$1" --console=plain > "$LOG_FILE" 2>&1; } &
GRADLE_PID=$!
echo "SERVER STARTED #$GRADLE_PID"
echo "SERVER STARTED #$GRADLE_PID" > "$LOG_FILE"
echo "LOG FILE AT $LOG_FILE" > "$LOG_FILE"

less --exit-follow-on-close -RS +F "$LOG_FILE"
killtree "$GRADLE_PID"
rm -f "$LOG_FILE"