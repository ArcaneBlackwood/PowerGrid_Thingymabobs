#!/bin/bash
killtree() {
	local pid=$1
	for child in $(ps -o pid= --ppid "$pid"); do
		killtree "$child"
	done
	kill -TERM "$pid" 2>/dev/null
}

coproc GRADLE { "$(dirname -- "${BASH_SOURCE[0]}")/../gradlew" runClient2 --console=plain 2>&1; }
less -RS +F <&"${GRADLE[0]}"
killtree "$GRADLE_PID"