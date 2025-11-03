reactive 模式

一种更激进的模式, 原则上所有操作都在数据库进行, 例如 add 指令, 转化为
```
update ago_object set slot1 = slot2 + slot3 where ...
```  

对于无法在数据库完成的, 先通过重载的 slots.getXX() 加载到内存, 在解释器运算完成后再 update 入库.

但在实现上本模式目前并不实用，这是因为目前仅将 ObjectRef 附着于 Slots，slots 的数据是影子，但 Instance 自身仍是真实的。

```java
org.siphonlab.ago.runtime.rdb.reactive.semischema.PGJsonSlotsAdapter.getSlotValue(){
    case TypeCode.OBJECT_VALUE -> {
        PGobject obj = (PGobject) resultSet.getObject(1);
        if (obj == null) yield null;
        String json = obj.getValue();
        Map<String, Object> r = (Map<String, Object>) new JsonSlurper().parseText(json);
        ObjectRef ref = new ObjectRef((String) r.get("@type"), (Long) r.get("@id"));
        yield adapter.restoreInstance(connection, ref, callFrame);
    }
}
```

而 Instance 持有的例如 creator, caller 等均无法释放，导致内存实际上会一直占用。

如需要更低的内存占用，可在此基础上继续改进，和 lazy 模式结合。
