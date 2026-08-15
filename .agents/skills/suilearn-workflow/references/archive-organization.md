# 归档组织

采用与 OpenSpec / Spec Kit 一致的扁平日期目录：

```text
openspec/changes/archive/YYYY-MM-DD-<change-name>/
```

归档前：

1. 运行 `python3 scripts/check_suilearn_workflow.py --closing-change <change-name>`。
2. 确认当前事实已同步或记录 `not affected`。
3. 运行：

```bash
python3 scripts/archive_openspec_change.py --change-name <change-name>
```

历史主域信息只记录在 `archive.md` 的 metadata 中，不再作为目录层级。不要使用 `.codex/skills/openspec-archive-change` 的旧平铺路径，也不要手工移动目录。
