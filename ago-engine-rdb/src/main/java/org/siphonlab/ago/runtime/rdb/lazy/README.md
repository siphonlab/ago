lazy mode

平时采用 ObjectRefInstance。当需要获取 slots 以及获取属性，执行 run 时展开。

当设置到另一个对象上(another.slots.setObject(it)) 时，折叠为 ObjectRefInstance。

目的是让所有 instance 之间不建立真正的连接，这样当 RunSpace 运行结束时，所有相关的对象都已归还。

对于 AgoFrame 来说，仅当 run 时，相关属性、slots 加载。

对于普通对象来说，仅当调用它的 getSlots 时，加载到内存。

