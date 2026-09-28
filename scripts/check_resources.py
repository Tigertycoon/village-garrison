"""Validate mod resource JSON; reject duplicate keys rather than silently dropping data."""
from pathlib import Path
import json

def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"Duplicate JSON key: {key}")
        result[key] = value
    return result

root = Path(__file__).resolve().parents[1]
resources = sorted(root.glob("**/src/main/resources/**/*.json"))
for path in resources:
    try:
        json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_object)
    except (ValueError, OSError) as error:
        raise SystemExit(f"{path.relative_to(root)}: {error}") from error
print(f"Validated {len(resources)} resource JSON files.")
