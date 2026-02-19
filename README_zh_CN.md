# ago: Function is Class

**Function is Class, and CallFrame is its Instance**

## Overview

ago 是一种旨在统一面向对象编程（OOP）与函数式范式的新型语言，实现了语义级别的对现实世界“动作”的描述。

受 **Process Philosophy** 的启发，`ago` 认为现实世界的“动作”应当具有完整的生命周期状态——既包含起点也保留终点和中间过程。
传统的编程模型将 `CallFrame（调用帧）` 绑定在底层栈结构上且不可见、不支持持久化。而 ago 将其抽象为面向对象的概念：
*   **Function is Class**：函数是类。函数被提升为一等公民，具有生命周期属性和方法字段；
*   **Call Frame is its Instance**：CallFrame 是函数的实例。每一个函数调用，产生一个 Call Frame 实例。

This design gives you:

* **Asynchronous actions** – functions can suspend and resume like coroutines, but are just normal objects.
* **Full persistence** – every call frame is a heap object; its state can be stored in any database or key‑value store and recovered after a crash.
* **Distributed execution** – because frames are objects they can be shipped across nodes or serialized to a blockchain.
* **Object‑oriented semantics everywhere** – closures, generics, traits, meta‑classes, and parameterized classes all live in the same type system.

> **Why use ago?**  
> When your business logic involves long‑running, event‑driven workflows (e.g. approvals, payment processing, game animation), traditional languages force you to mix callbacks, state machines, or external workflow engines. ago lets you write those flows *directly* as ordinary functions, with built‑in suspension, resumption, and persistence.

## 核心设计理念 / Core Philosophy

### 1. 统一函数闭包
在 ago 中，无论是类还是普通方法都被视为对象。内部定义的子函数与方法语义等价；且允许向静态代码体注入字段和方法。
*   **Scope as Object**：作用域被统一为 `Slots` 接口实现的对象。

### 2. CallFrame 的生命周期化
传统的调用帧是栈上的临时状态，而 ago 将其视为堆内存中的对象。这使得函数不仅是一个执行路径的描述符（数学映射），更是一种具有 **Suspend (挂起)** 和 **Resume (恢复)** 能力的“计算实体”。
*   支持显式的 `await` 语句进行控制权让渡；
*   支持 Fork 创建子进程，实现结构化并发。

### 3. 类型系统的增强
ago 引入了独特的类型系统设计：
*   **Parameterized Classes（参数类）**：允许将常量值直接嵌入到类型的元数据中。
    *   示例: `var p = new Person(); name as VarChar::(200)`
        *(注意这里的 200 是一个编译期或运行期的属性，而非模板泛型)*
*   **Type Scalarization (类型标量化)**：
    通过特殊的 Boxing 类（如 ScopedClassInterval, ClassRef），实现了对类引用的丰富语义封装。
*   **Function Overloading**：通过 `#` 符号区分重载签名。

## 语言特性示例 / Features Examples

### 1. 函数定义与调用
在 ago 中，函数声明支持显式类型注解（类似 Go 或 Kotlin），且编译后会生成特定的 VM 指令序列。代码结构看起来非常直观：

```ago
// 定义一个简单的加法操作符作为类实例化过程 (伪语法)
fun add(a as int, b as int) {
    return a + b;
}

// 主程序入口，展示 CallFrame 的创建与执行分离思想

var t = new f(arguments);   // 1. 创建对象
t();                         // 2. 执行状态机逻辑（类似协程 Resume）
```

### 2. 结构化并发 与 持久性 Action (Structured Concurrency & Persistence)
ago 通过 `fork` 和自定义的运行空间管理异步任务，且由于 CallFrame 本质是普通对象：

```ago
// 定义一个简单的 Worker 动作

fun f() {
    await; // 显式挂起：进入休眠状态（类似 Coroutines 的 yield）
}

class Employee {    
    fun requestLeave(){
        var leave = this.submitForm();  // 提交表单，CallFrame 挂起并持久化到 DB
        if (leave.approved) {
            await toManager(approve);   // 等待外部事件触发恢复（类似 Workflow Node 触发）
            
            notifyScheduler('Leave');
        }
    }    
}
```

### 3. 分布式事务支持**
ago 的 CallFrame 可以直接映射到数据库中的行或 JSON 文档，从而实现语义级的分布式执行：

```java
// 运行时概念：Slots 实现（伪代码示意）

public interface Slots {
   // ... get/set ...
}

class TransactionRunSpace extends AgoRunSpace { 
    List<CallFrames> children; 
    
    fun commit() {
        for(child in this.children) child.notify();  // 提交子事务
     }
}
```

## 技术架构 / Architecture

ago 目前是一个基于 Java 运行时的解释器框架，而非传统的编译型语言：

1.  **Runtime (Java Implementation)**:
    *   基于 `AgoClass` 和自定义的 VM 指令集。
    *   使用了特殊的接口实现来管理对象状态（例如直接写入 PostgreSQL JSON 的 Slots 实现）。

2.  **VM Instructions** (`ago code -> .vC files`)

    编译器将 ago 脚本编译为特定的字节码指令。以下是一个简单的加法函数在 VM 中对应的执行流：

```
// 假设: var t = add(5,4)

0   new_vC      2                     // 创建 Class `add` 的实例 (CallFrame)
3   const_fld_i_ovc    ...             // 设置参数
11  invoke_v        ...
13  accept_i_v      
```

## 应用场景 / Use Cases

*   **游戏开发**: 摒弃传统的回调地狱和状态机，通过函数组合表达复杂的动画序列（类比 Cocos2d 的 Action 系统）。
*   **工作流引擎 (Workflow Engines)**: 将业务流程图直接映射为 ago 代码。由于 CallFrame 可以被持久化到数据库或消息队列中恢复执行, 它可以替代传统的 BPMN 引擎，支持跨服务的断点续传和容错处理（如 Saga 模式）。
*   **领域建模**: 利用 Parameterized Classes 和 Scope 特性进行更精细的类型约束。

## License

MIT