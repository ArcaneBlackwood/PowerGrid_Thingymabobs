#!/bin/bash
"$(dirname -- "${BASH_SOURCE[0]}")/../gradlew" runClient2 --console=plain 2>&1 | less -RS +F