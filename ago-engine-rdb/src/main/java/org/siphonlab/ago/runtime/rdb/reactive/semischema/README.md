JSON 半结构化 + reactive 模式

本 reactive 模式底层采用 json/jsonb 字段存储 slots, 这样所有 ago_instance 采用同一个表即可.

对于 generate_ddl 来说,固定生成几张表即可.


