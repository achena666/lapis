# -*- coding: utf-8 -*-
"""
修复建表脚本：回收站表（_deleted_*）缺少主表新增的列。

背景
----
删除字典类型/字典项时，代码会把记录搬进回收站表再删除，形如
    INSERT INTO _deleted_hyn_sys_dict_type(id, ..., is_builtin, icon_type, ...)
    SELECT ... FROM hyn_sys_dict_type WHERE id IN (?);
但回收站表的建表语句里没有 is_builtin / icon_type，导致
    SQLSyntaxErrorException: Unknown column 'is_builtin' in 'field list'
删除操作直接 500。

本脚本在 lapis_admin_dev.sql 中：
  1. 给两张回收站表补上缺失的列定义
  2. 同步修正既有的 INSERT 语句（按值个数判断是否已修正，保证幂等）

注意：全程以二进制读写，保持原文件的 CRLF 换行不被改写。
"""
import io
import sys

SQL_FILE = "lapis_admin_dev.sql"

# 表 -> (锚点列定义, 要追加的列定义, 插入位置(第几个值之后), 要插入的值, 原值个数, 新值个数)
PATCH = {
    "_deleted_hyn_sys_dict_item": {
        "anchor": "  `is_enabled` tinyint NOT NULL COMMENT '启用状态（1=启用；0=禁用 无符号）',\n",
        "add": "  `is_builtin` tinyint NOT NULL DEFAULT 0 COMMENT '是否系统内置（0=否，1=是；【2026-10 补充】与主表 hyn_sys_dict_item 对齐，缺失会导致删除时报 Unknown column）',\n",
        "insert_after": 6,          # 在 is_enabled 之后
        "value": " 0,",
        "old_count": 11,
        "new_count": 12,
    },
    "_deleted_hyn_sys_dict_type": {
        "anchor": "  `is_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用（1=启用；0=禁用; 无符号）',\n",
        "add": (
            "  `is_builtin` tinyint NOT NULL DEFAULT 0 COMMENT '是否系统内置（0=否，1=是；【2026-10 补充】与主表对齐）',\n"
            "  `icon_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图标类型；【2026-10 补充】与主表对齐',\n"
        ),
        "insert_after": 4,          # 在 is_enabled 之后
        "value": " 0, NULL,",
        "old_count": 11,
        "new_count": 13,
    },
}


def split_top_level(s: str):
    """按顶层逗号切分，同时返回每个逗号在原串中的下标。忽略引号内的逗号。"""
    parts, commas, buf = [], [], []
    depth, i, n = 0, 0, len(s)
    while i < n:
        ch = s[i]
        if ch == "'":
            j = i + 1
            while j < n:
                if s[j] == "\\":
                    j += 2
                    continue
                if s[j] == "'":
                    break
                j += 1
            buf.append(s[i:j + 1])
            i = j + 1
            continue
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append("".join(buf))
            commas.append(i)
            buf = []
        else:
            buf.append(ch)
        i += 1
    parts.append("".join(buf))
    return parts, commas


def patch_insert_line(line: str, cfg) -> tuple:
    """对单条 INSERT 补值，返回 (新行, 是否修改)"""
    prefix = "INSERT INTO `%s` VALUES (" % cfg["_table"]
    if not line.startswith(prefix):
        return line, False
    start = line.find("(")
    end = line.rfind(")")
    if start == -1 or end <= start:
        return line, False
    inner = line[start + 1:end]
    parts, commas = split_top_level(inner)
    if len(parts) == cfg["new_count"]:
        return line, False                      # 已修正
    if len(parts) != cfg["old_count"]:
        return line, False                      # 值个数不符，跳过以免改坏
    cut = commas[cfg["insert_after"] - 1]       # 第 n 个逗号的下标（inner 内）
    new_inner = inner[:cut + 1] + cfg["value"] + inner[cut + 1:]
    return line[:start + 1] + new_inner + line[end:], True


def main():
    raw = io.open(SQL_FILE, "rb").read().decode("utf-8")
    print("处理 %s（保持原换行符 %s）" % (SQL_FILE, "CRLF" if "\r\n" in raw else "LF"))

    # 1) 建表语句补列（锚点匹配需兼容 CRLF：anchor 不带行尾换行，另按行尾插入）
    eol = "\r\n" if "\r\n" in raw else "\n"
    for table, cfg in PATCH.items():
        cfg["_table"] = table
        marker = "CREATE TABLE `%s`  (" % table
        idx = raw.find(marker)
        if idx == -1:
            print("  %s 未找到建表语句，跳过" % table)
            continue
        end = raw.find("ENGINE = InnoDB", idx)
        block = raw[idx:end]
        anchor = cfg["anchor"].rstrip("\r\n")
        if cfg["add"].split(",")[0].rstrip("\r\n") in block:
            print("  %s 建表已含新列，跳过" % table)
            continue
        a_pos = block.find(anchor)
        if a_pos == -1:
            print("  %s 未找到锚点列，跳过" % table)
            continue
        line_end = block.find("\n", a_pos)          # 锚点所在行的行尾
        insert_at = line_end + 1
        add = cfg["add"].replace("\n", eol)
        raw = raw[:idx] + block[:insert_at] + add + block[insert_at:] + raw[end:]
        print("  %s 建表语句已补列" % table)

    # 2) INSERT 补值
    lines = raw.split("\n")
    total = 0
    for table, cfg in PATCH.items():
        n = 0
        for i, line in enumerate(lines):
            new_line, changed = patch_insert_line(line, cfg)
            if changed:
                lines[i] = new_line
                n += 1
        if n:
            print("  %s 修正 %d 条 INSERT" % (table, n))
        total += n
    raw = "\n".join(lines)

    if total or True:
        io.open(SQL_FILE, "wb").write(raw.encode("utf-8"))
        print("已写回文件")
    return 0


if __name__ == "__main__":
    sys.exit(main())
