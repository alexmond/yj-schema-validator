---
name: jacoco
description: Check JaCoCo code coverage
allowed-tools: Bash(./mvnw *), Bash(python3 *)
---

## Check JaCoCo Code Coverage

### Minimum threshold: **80%** line coverage

### Step 1: Generate coverage report

```bash
./mvnw clean verify -q
```

### Step 2: Summary

```python
python3 -c "
import xml.etree.ElementTree as ET

THRESHOLD = 80
tree = ET.parse('target/site/jacoco/jacoco.xml')
root = tree.getroot()

print(f'{'Type':<12} {'Covered':>8} {'Missed':>8} {'Total':>8} {'Coverage':>10}')
print('-' * 50)
for counter in root.findall('counter'):
    ctype = counter.get('type')
    missed = int(counter.get('missed'))
    covered = int(counter.get('covered'))
    total = missed + covered
    pct = covered / total * 100 if total > 0 else 0
    print(f'{ctype:<12} {covered:>8} {missed:>8} {total:>8} {pct:>9.2f}%')
"
```

### Step 3: Class-level breakdown (lowest coverage first)

```python
python3 -c "
import xml.etree.ElementTree as ET

tree = ET.parse('target/site/jacoco/jacoco.xml')
root = tree.getroot()
results = []
for pkg in root.findall('.//package'):
    for cls in pkg.findall('class'):
        for counter in cls.findall('counter'):
            if counter.get('type') == 'LINE':
                missed = int(counter.get('missed'))
                covered = int(counter.get('covered'))
                total = missed + covered
                if total > 0:
                    pct = covered / total * 100
                    results.append((pct, missed, covered, total, cls.get('name')))
results.sort()
print(f'{'Coverage':>10} {'Missed':>8} {'Covered':>8} {'Total':>8}  Class')
print('-' * 80)
for pct, missed, covered, total, name in results:
    print(f'{pct:>9.1f}% {missed:>8} {covered:>8} {total:>8}  {name}')
"
```

### Step 4: Gap analysis

```python
python3 -c "
import xml.etree.ElementTree as ET
import math

THRESHOLD = 80
tree = ET.parse('target/site/jacoco/jacoco.xml')
root = tree.getroot()
for counter in root.findall('counter'):
    if counter.get('type') == 'LINE':
        missed = int(counter.get('missed'))
        covered = int(counter.get('covered'))
        total = missed + covered
        pct = covered / total * 100 if total > 0 else 0
        needed = math.ceil(THRESHOLD / 100 * total) - covered
        if needed > 0:
            print(f'{pct:.2f}% - need {needed} more lines covered to reach {THRESHOLD}%')
        else:
            print(f'{pct:.2f}% - ABOVE {THRESHOLD}% (surplus: {-needed} lines)')
"
```
