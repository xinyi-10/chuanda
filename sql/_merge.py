# -*- coding: utf-8 -*-
"""把 schema.sql + data.sql 合并为一键初始化脚本 init_all.sql"""
import os
here = os.path.dirname(os.path.abspath(__file__))
s = open(os.path.join(here, "schema.sql"), encoding="utf-8").read()
d = open(os.path.join(here, "data.sql"), encoding="utf-8").read()

sep = "-- " + "=" * 69
hdr = f"{sep}\n-- 以下为初始化数据（原 data.sql 内容）\n{sep}\n\n"
out = s.rstrip() + "\n\n\n" + hdr + d
open(os.path.join(here, "init_all.sql"), "w", encoding="utf-8").write(out)
print("OK -> init_all.sql")
