#!/usr/bin/env bash
set -euo pipefail

# SuiLearn 质量门禁 — 每次提交、每次 Agent 会话结束后运行
# 支持 WSL / Git Bash / Linux。工具缺失时跳过并提示。

PASS=0
FAIL=0
SKIP=0
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

JAVA_AVAILABLE=false
if command -v java &>/dev/null; then
    JAVA_AVAILABLE=true
fi

check() {
    local name="$1" cmd="$2"
    shift 2
    echo -n "  [$name] "
    if ! command -v "$cmd" &>/dev/null; then
        echo -e "${YELLOW}SKIP${NC} (未安装 $cmd)"
        ((SKIP+=1))
        return
    fi
    if "$@" > /tmp/suilearn-check.log 2>&1; then
        echo -e "${GREEN}PASS${NC}"
        ((PASS+=1))
    else
        echo -e "${RED}FAIL${NC}"
        tail -15 /tmp/suilearn-check.log | sed 's/^/  │ /'
        ((FAIL+=1))
    fi
}

echo "=== SuiLearn Quality Gate ==="
echo ""

echo "── Android ──"
if [ "$JAVA_AVAILABLE" = true ] && [ -f gradlew ]; then
    check "Unit tests"       java      ./gradlew :app:test --no-daemon
else
    echo "  [Unit tests] ${YELLOW}SKIP${NC} (需要 Java 环境 + gradlew)"
    ((SKIP+=1))
fi

echo ""
echo "── Backend ──"
check "Service tests"    mvn       mvn -f services/api/pom.xml test -q

echo ""
echo "── Contracts ──"
PYTHON_CMD=""
if command -v python3 &>/dev/null; then
    PYTHON_CMD="python3"
elif command -v python &>/dev/null; then
    PYTHON_CMD="python"
fi
if [ -n "$PYTHON_CMD" ]; then
    check "OpenAPI valid"    "$PYTHON_CMD"   "$PYTHON_CMD" -c "
import yaml, sys
with open('contracts/openapi/suilearn-v2.yaml') as f:
    yaml.safe_load(f)
print('OK')
"
else
    echo "  [OpenAPI valid] ${YELLOW}SKIP${NC} (未安装 python)"
    ((SKIP+=1))
fi

echo ""
echo "═══════════════════════════════"
echo -e "结果: ${GREEN}$PASS 通过${NC} / ${RED}$FAIL 失败${NC} / ${YELLOW}$SKIP 跳过${NC}"
if [ "$FAIL" -gt 0 ]; then
    echo "❌ 质量门禁未通过 — 请修复后再提交"
    exit 1
elif [ "$SKIP" -gt 0 ] && [ "$PASS" -eq 0 ]; then
    echo "⚠️  无工具可运行 — 请在 Android Studio / IntelliJ 中运行测试"
else
    echo "✅ 通过 (部分跳过)"
fi
