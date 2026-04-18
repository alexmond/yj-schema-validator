#!/usr/bin/env python3
"""
Fix fully qualified class names (FQN) used inline in Java source files.
Replaces inline FQN usages with simple class names and adds missing imports.

Usage: python3 fix_fqn.py [root_dir]
Default root_dir: current working directory

Saved at: .claude/scripts/fix_fqn.py
"""

import os
import re
import sys

# FQN patterns to fix: (fqn_pattern, simple_name, import_line)
FQN_RULES = [
    # java.util
    ("java.util.Map", "Map", "import java.util.Map;"),
    ("java.util.List", "List", "import java.util.List;"),
    ("java.util.Set", "Set", "import java.util.Set;"),
    ("java.util.Collection", "Collection", "import java.util.Collection;"),
    ("java.util.ArrayList", "ArrayList", "import java.util.ArrayList;"),
    ("java.util.HashMap", "HashMap", "import java.util.HashMap;"),
    ("java.util.LinkedHashMap", "LinkedHashMap", "import java.util.LinkedHashMap;"),
    ("java.util.HashSet", "HashSet", "import java.util.HashSet;"),
    ("java.util.Arrays", "Arrays", "import java.util.Arrays;"),
    ("java.util.Collections", "Collections", "import java.util.Collections;"),
    ("java.util.Optional", "Optional", "import java.util.Optional;"),
    ("java.util.Objects", "Objects", "import java.util.Objects;"),
    ("java.util.Iterator", "Iterator", "import java.util.Iterator;"),
    ("java.util.Locale.ROOT", "Locale.ROOT", "import java.util.Locale;"),
    ("java.util.Locale", "Locale", "import java.util.Locale;"),
    ("java.util.Base64", "Base64", "import java.util.Base64;"),
    ("java.util.regex.Pattern", "Pattern", "import java.util.regex.Pattern;"),
    ("java.util.concurrent.TimeUnit", "TimeUnit", "import java.util.concurrent.TimeUnit;"),
    # java.nio
    ("java.nio.charset.StandardCharsets", "StandardCharsets", "import java.nio.charset.StandardCharsets;"),
    ("java.nio.file.Path", "Path", "import java.nio.file.Path;"),
    ("java.nio.file.Files", "Files", "import java.nio.file.Files;"),
    ("java.nio.file.Paths", "Paths", "import java.nio.file.Paths;"),
    # java.io
    ("java.io.ByteArrayInputStream", "ByteArrayInputStream", "import java.io.ByteArrayInputStream;"),
    ("java.io.InputStream", "InputStream", "import java.io.InputStream;"),
    ("java.io.IOException", "IOException", "import java.io.IOException;"),
    ("java.io.File", "File", "import java.io.File;"),
    # java.net
    ("java.net.URL", "URL", "import java.net.URL;"),
    ("java.net.URI", "URI", "import java.net.URI;"),
    # java.time
    ("java.time.Duration", "Duration", "import java.time.Duration;"),
    # java.security
    ("java.security.SecureRandom", "SecureRandom", "import java.security.SecureRandom;"),
    # com.networknt
    ("com.networknt.schema.JsonSchema", "JsonSchema", "import com.networknt.schema.JsonSchema;"),
    ("com.networknt.schema.JsonSchemaFactory", "JsonSchemaFactory", "import com.networknt.schema.JsonSchemaFactory;"),
    ("com.networknt.schema.ValidationMessage", "ValidationMessage", "import com.networknt.schema.ValidationMessage;"),
    ("com.networknt.schema.SpecVersion", "SpecVersion", "import com.networknt.schema.SpecVersion;"),
    # tools.jackson (Jackson 3.x)
    ("tools.jackson.databind.ObjectMapper", "ObjectMapper", "import tools.jackson.databind.ObjectMapper;"),
    ("tools.jackson.databind.JsonNode", "JsonNode", "import tools.jackson.databind.JsonNode;"),
    ("tools.jackson.dataformat.yaml.YAMLMapper", "YAMLMapper", "import tools.jackson.dataformat.yaml.YAMLMapper;"),
]


def find_java_files(root_dir):
    """Find all .java files under src/ directories."""
    java_files = []
    for dirpath, _, filenames in os.walk(root_dir):
        for f in filenames:
            if f.endswith(".java") and "/src/" in os.path.join(dirpath, f):
                java_files.append(os.path.join(dirpath, f))
    return java_files


def get_import_section_end(lines):
    """Find the line index after the last import statement."""
    last_import = -1
    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith("import "):
            last_import = i
    return last_import


def has_import(lines, import_stmt):
    """Check if an import already exists."""
    clean = import_stmt.strip().rstrip(";")
    for line in lines:
        if line.strip().rstrip(";") == clean:
            return True
    return False


def is_in_import_line(line):
    """Check if a line is an import statement."""
    return line.strip().startswith("import ")


def is_in_string_literal(line, pos):
    """Rough check if position is inside a string literal."""
    in_string = False
    i = 0
    while i < pos and i < len(line):
        if line[i] == '"' and (i == 0 or line[i-1] != '\\'):
            in_string = not in_string
        i += 1
    return in_string


def is_in_comment(line, pos):
    """Rough check if position is inside a line comment."""
    comment_start = line.find("//")
    if comment_start >= 0 and pos > comment_start:
        return True
    return False


def process_file(filepath):
    """Process a single Java file, replacing FQN with imports."""
    with open(filepath, "r") as f:
        content = f.read()

    lines = content.split("\n")
    imports_to_add = set()
    changes_made = False

    for fqn, simple, import_line in FQN_RULES:
        if fqn not in content:
            continue

        new_lines = []
        for line in lines:
            if is_in_import_line(line):
                new_lines.append(line)
                continue

            if fqn not in line:
                new_lines.append(line)
                continue

            new_line = line
            idx = 0
            while True:
                pos = new_line.find(fqn, idx)
                if pos < 0:
                    break
                if is_in_string_literal(new_line, pos) or is_in_comment(new_line, pos):
                    idx = pos + len(fqn)
                    continue

                new_line = new_line[:pos] + simple + new_line[pos + len(fqn):]
                idx = pos + len(simple)
                imports_to_add.add(import_line)
                changes_made = True

            new_lines.append(new_line)

        lines = new_lines

    if not changes_made:
        return False

    imports_actually_needed = set()
    for imp in imports_to_add:
        if not has_import(lines, imp):
            imports_actually_needed.add(imp)

    if imports_actually_needed:
        last_import_idx = get_import_section_end(lines)
        if last_import_idx >= 0:
            for imp in sorted(imports_actually_needed):
                last_import_idx += 1
                lines.insert(last_import_idx, imp)
        else:
            for i, line in enumerate(lines):
                if line.strip().startswith("package "):
                    lines.insert(i + 1, "")
                    for j, imp in enumerate(sorted(imports_actually_needed)):
                        lines.insert(i + 2 + j, imp)
                    break

    with open(filepath, "w") as f:
        f.write("\n".join(lines))

    return True


def main():
    root_dir = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
    java_files = find_java_files(root_dir)
    total_fixed = 0

    for filepath in java_files:
        if process_file(filepath):
            rel = os.path.relpath(filepath, root_dir)
            print(f"  Fixed: {rel}")
            total_fixed += 1

    print(f"\nDone. Fixed {total_fixed} files.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
