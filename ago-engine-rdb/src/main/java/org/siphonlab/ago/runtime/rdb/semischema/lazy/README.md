JSON 半结构化 + lazy 模式

本模式为完全实现的 ObjectRefInstance 模式.

slots 按需完全加载. 采用 json/jsonb 存储 slots.

本模式特别适合 workflow.

技术上存在各种排列组合, 但根据应用场景 workflow 就应当选用该模式.