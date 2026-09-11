#!/bin/bash
"$(dirname -- "${BASH_SOURCE[0]}")/../gradlew" runClient --console=plain 2>&1 | less -RS +F