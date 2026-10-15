#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Lapis 数据库初始化脚本

作用：
  1. 创建 lapis_admin_dev / lapis_uaa_dev / lapis_eventlog_dev 三个库
  2. 导入 SQL/ 目录下对应的初始化脚本
  3. 校验表数量与关键权限数据是否落地
  4. 可选：从旧库 hauyne_*_dev 迁移存量数据（需要 mysqldump）

前置条件：
  - 本机已安装 MySQL 8.x 并已启动
  - mysql（迁移时还需 mysqldump）可执行文件可用

用法：
  python init-lapis-databases.py
  python init-lapis-databases.py --password 123456 --host 127.0.0.1
  python init-lapis-databases.py --mysql-bin "C:/Program Files/MySQL/MySQL Server 8.0/bin"
  python init-lapis-databases.py --migrate            # 同时从旧库迁移数据
  python init-lapis-databases.py --dry-run            # 只打印将要执行的命令
"""

import argparse
import os
import subprocess
import sys

SQL_DIR = os.path.dirname(os.path.abspath(__file__))

# 数据库名 -> 对应的初始化脚本
DATABASES = {
    "lapis_admin_dev": "lapis_admin_dev.sql",
    "lapis_uaa_dev": "lapis_uaa_dev.sql",
    "lapis_eventlog_dev": "lapis_eventlog_dev.sql",
}

# 旧库名映射（用于 --migrate）
OLD_DATABASES = {
    "lapis_admin_dev": "hauyne_admin_dev",
    "lapis_uaa_dev": "hauyne_uaa_dev",
    "lapis_eventlog_dev": "hauyne_eventlog_dev",
}

# 更名+安全加固后新增的功能权限，用于校验导入结果
EXPECTED_AUTHORITIES = [
    "sys-user:add",
    "sys-user:update",
    "sys-user:delete",
    "sys-user:reset-password",
    "sys-login-history:delete",
    "sys-tool:generate-ddl",
]


def resolve_exe(bin_dir, name):
    """定位 mysql / mysqldump 可执行文件"""
    exe = name + (".exe" if os.name == "nt" else "")
    if bin_dir:
        p = os.path.join(bin_dir, exe)
        if os.path.isfile(p):
            return p
        raise SystemExit("在 %s 下找不到 %s" % (bin_dir, exe))
    return exe


def run(cmd, dry_run=False, capture=False, stdin_file=None):
    printable = " ".join(cmd)
    if stdin_file:
        printable += " < " + stdin_file
    print("    $ " + printable)
    if dry_run:
        return ""
    stdin = open(stdin_file, "rb") if stdin_file else None
    try:
        r = subprocess.run(cmd, stdin=stdin, stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE)
    finally:
        if stdin:
            stdin.close()
    if r.returncode != 0:
        err = r.stderr.decode("utf-8", "ignore").strip()
        print("    失败: " + err[:400])
        raise SystemExit(1)
    return r.stdout.decode("utf-8", "ignore").strip()


def main():
    ap = argparse.ArgumentParser(description="Lapis 数据库初始化")
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", default="3306")
    ap.add_argument("--user", default="root")
    ap.add_argument("--password", default="123456")
    ap.add_argument("--mysql-bin", default="", help="MySQL 的 bin 目录，留空则使用 PATH 中的 mysql")
    ap.add_argument("--migrate", action="store_true", help="从旧库 hauyne_*_dev 迁移数据")
    ap.add_argument("--dry-run", action="store_true", help="只打印命令，不执行")
    ap.add_argument("--only", default="", help="只处理指定库，逗号分隔")
    args = ap.parse_args()

    mysql = resolve_exe(args.mysql_bin, "mysql")
    mysqldump = resolve_exe(args.mysql_bin, "mysqldump") if args.migrate else None

    base = [mysql, "-h", args.host, "-P", str(args.port), "-u", args.user]
    if args.password:
        base += ["-p" + args.password]

    targets = DATABASES
    if args.only:
        wanted = [x.strip() for x in args.only.split(",")]
        targets = {k: v for k, v in DATABASES.items() if k in wanted}
        if not targets:
            raise SystemExit("--only 未匹配到任何数据库")

    print("")
    print("[1/5] 检查 MySQL 连接 ...")
    version = run(base + ["-N", "-e", "SELECT VERSION();"], args.dry_run, capture=True)
    print("    MySQL 版本: " + (version or "(dry-run)"))

    print("")
    print("[2/5] 创建数据库 ...")
    for db in targets:
        run(base + ["-e",
                    "CREATE DATABASE IF NOT EXISTS `%s` "
                    "DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" % db],
            args.dry_run)
    print("    已就绪: " + ", ".join(targets))

    if args.migrate:
        print("")
        print("[3/5] 从旧库迁移存量数据 ...")
        for db, old in OLD_DATABASES.items():
            if db not in targets:
                continue
            check = run(base + ["-N", "-e",
                                "SELECT SCHEMA_NAME FROM information_schema.schemata "
                                "WHERE SCHEMA_NAME='%s';" % old],
                        args.dry_run, capture=True)
            if not check:
                print("    跳过 %s（旧库 %s 不存在）" % (db, old))
                continue
            dump_cmd = [mysqldump, "-h", args.host, "-P", str(args.port),
                        "-u", args.user]
            if args.password:
                dump_cmd += ["-p" + args.password]
            dump_cmd += ["--single-transaction", "--routines", "--triggers", old]

            print("    迁移 %s -> %s" % (old, db))
            if not args.dry_run:
                p1 = subprocess.Popen(dump_cmd, stdout=subprocess.PIPE,
                                      stderr=subprocess.PIPE)
                p2 = subprocess.Popen(base + [db], stdin=p1.stdout,
                                      stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                p1.stdout.close()
                _, err2 = p2.communicate()
                p1.wait()
                if p2.returncode != 0:
                    print("    失败: " + err2.decode("utf-8", "ignore").strip()[:400])
                    raise SystemExit(1)
        print("    迁移完成")
    else:
        print("")
        print("[3/5] 导入初始化脚本（未开启 --migrate，执行全新导入）...")
        for db, sql_file in targets.items():
            path = os.path.join(SQL_DIR, sql_file)
            if not os.path.isfile(path):
                print("    跳过 %s（脚本 %s 不存在）" % (db, sql_file))
                continue
            run(base + [db], args.dry_run, stdin_file=path)
        print("    导入完成")

    print("")
    print("[4/5] 校验表数量 ...")
    ok = True
    for db in targets:
        n = run(base + ["-N", "-e",
                        "SELECT COUNT(*) FROM information_schema.tables "
                        "WHERE table_schema='%s';" % db],
                args.dry_run, capture=True)
        flag = "" if (args.dry_run or (n and int(n) > 0)) else "   <-- 异常：0 张表"
        if flag:
            ok = False
        print("    %-22s %s 张表%s" % (db, n or "-", flag))

    print("")
    print("[5/5] 校验新增功能权限 ...")
    if "lapis_admin_dev" in targets:
        in_clause = ",".join("'%s'" % a for a in EXPECTED_AUTHORITIES)
        n = run(base + ["-N", "-e",
                        "SELECT COUNT(*) FROM lapis_admin_dev.hyn_sys_authority "
                        "WHERE authority_code IN (%s);" % in_clause],
                args.dry_run, capture=True)
        if args.dry_run:
            print("    (dry-run 跳过)")
        elif n and int(n) == len(EXPECTED_AUTHORITIES):
            print("    权限记录 %s/%s 条，全部就位" % (n, len(EXPECTED_AUTHORITIES)))
        else:
            ok = False
            print("    权限记录 %s 条，预期 %s 条，请检查导入是否完整"
                  % (n, len(EXPECTED_AUTHORITIES)))
    else:
        print("    跳过（未处理 lapis_admin_dev）")

    print("")
    if args.dry_run:
        print("dry-run 完成，以上命令均未执行。")
    elif ok:
        print("全部完成。数据库已就绪：" + ", ".join(targets))
    else:
        print("完成，但存在异常项，请查看上面的标记。")
        sys.exit(1)


if __name__ == "__main__":
    main()
