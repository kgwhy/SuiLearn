#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/check_workflow_skill.py
BASE_REF="$(git merge-base HEAD origin/dev 2>/dev/null || git merge-base HEAD origin/main 2>/dev/null || git rev-parse HEAD)"
python3 scripts/check_suilearn_workflow.py --base-ref "$BASE_REF"
