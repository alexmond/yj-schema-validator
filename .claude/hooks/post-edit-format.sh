#!/bin/bash
# PostToolUse hook: auto-format Java files after Edit/Write

set -euo pipefail

INPUT=$(cat)
TOOL_NAME=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_name',''))" 2>/dev/null || echo "")
FILE_PATH=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('file_path',''))" 2>/dev/null || echo "")

# Only run for Edit/Write on Java files
if [[ "$TOOL_NAME" != "Edit" && "$TOOL_NAME" != "Write" ]]; then
    exit 0
fi

if [[ "$FILE_PATH" != *.java ]]; then
    exit 0
fi

cd /Users/alex.mondshain/IdeaProjects/yj-schema-validator

# Run formatter on the project (single module, fast)
./mvnw spring-javaformat:apply -q 2>/dev/null || true

# Fix fully qualified names
python3 .claude/scripts/fix_fqn.py . 2>/dev/null || true
