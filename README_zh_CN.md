![Project Logo](./logo.svg)

# ago 编程语言

> **函数即类，调用帧即实例**

## 概述

ago 是一种面向对象的静态编程语言，其设计核心理念是 *“函数即类，调用帧即实例”*。这使得函数及其调用——编程语言的两大核心且无处不在的元素——能够在语义上对现实世界中的“动作（Actions）”进行建模。

受过程哲学启发，ago 认为现实世界中的“动作”应当具备完整的生命周期状态——既包含起点和终点，也包含中间过程。传统的编程模型将调用帧（CallFrame）绑定到底层栈结构上，使其对程序员不可见且难以直接操作。ago 将其抽象为面向对象的概念：
*   **函数即类**：函数演变为类。它们是一等公民，拥有生命周期属性和方法字段。
*   **调用帧即实例**：当函数被创建和执行时，调用帧会具象化为基于堆的对象。

该设计为你带来：

* **异步动作**——函数调用可以像协程一样挂起和恢复，因为它们本质上是普通对象。
* **完整持久化**——每个调用帧都是堆对象；其状态可存储于任意数据库或键值存储中，并在崩溃后恢复。
* **分布式执行**——由于帧是对象，它们可以在节点间传输。
* **无处不在的面向对象语义**——闭包、泛型、特质（traits）、元类（meta-classes）和参数化类共存于同一类型系统中。
* **基于堆而非栈的执行环境**——ago 设计了基于迭代机制的 RunSpace 来运行调用帧而避免使用堆栈机制的线程。作为逻辑对象 RunSpace 可以在不同的环境迁移，并可子类化。

ago 基于 Java 22 实现。你可以将其嵌入任何 Java 应用程序，或作为独立解释器运行。

> **为什么使用 ago？**  
> 当你的业务逻辑涉及长时间运行、事件驱动的工作流（例如审批流程、支付处理、游戏动画）时，传统语言迫使你混合使用回调、状态机或外部工作流引擎。ago 让你能够将这些流程*直接*编写为普通函数，并内置挂起、恢复和持久化能力。

---

## 特性

| 特性                                  | 为你带来什么                                                                                                       |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| **函数即类**                     | 函数即为类；每个函数都派生自 `Function<R>`，你可以实现接口/特质，并添加字段。       |
| **调用帧即实例**                 | 每次调用都是堆上的普通对象——可挂起、可序列化、可中断。                           |
| **元类与参数化类** | 将常量值嵌入类型中（例如 `VarChar::(200)`）。                                                                |
| **标量化类引用（`classref`）**    | 将类视为一等值；可用作泛型类型参数或运行时检查。                             |
| **扩展装箱机制**                      | 装箱机制适用于 `Boxer<T>` 的子类，允许使用类似 `name as VarChar::(200) = 'John'` 的语法。                                      |
| **调用时重载解析**            | 使用 `f#1`、`f#2` 等语法——编译器在调用时根据参数类型进行解析。                                           |
| **属性（Getter/Setter）**           | 声明为共享名称的两个重载版本（`name#get`、`name#set`）。                                                    |
| **结构化并发原语**    | `fork`、`await`、`race()`、`awaitMany()`——全部基于调用帧构建。                                                     |
| **运行空间（RunSpace）抽象**                 | 每个执行“线程”对应一个 RunSpace；可由线程池、事件循环或虚拟线程作为底层支撑。                 |
| **持久化钩子**                    | 实现 `Slots` 接口即可将状态存储至 PostgreSQL JSON 列、NoSQL 数据库，甚至区块链中。                               |

---

## 架构

```
+-------------------+
|  AgoSource (.ago) |
+--------+----------+
         |
     compile → Bytecode
         |
+--------v----------+
|  AgoEngine        |   
|  - CallFrame      |  (在 AgoFrame 实例内执行 ago 字节码)
|  - RunSpace       |
|  - Slots          |
+--------+----------+
         |
  execute on JVM
```

* **`Instance`**——所有 ago 对象的基类。持有对其所属类（`agoClass`）的引用以及一个 `Slots` 实现。
* **`AgoClass` / `MetaClass`**——普通类与元类。函数是 `AgoClass` 的子类。
* **`CallFrame`**——运行时帧；存储局部变量、程序计数器（`pc`）、调用者引用等。所有帧均为堆对象。
* **`RunSpace`**——拥有*当前*调用帧。当函数让出控制权（通过 `await`）时，其状态会变为 *WAITING_RESULT*，从而释放线程以处理其他任务。

由于一切皆对象：
1. 你可以通过序列化其 Slots 来持久化一个调用帧。
2. 使用序列化形式在进程间传输调用帧。
3. 在任何知道如何解释该字节码的 JVM 中恢复执行。

---

## 快速开始

### 环境要求

* JDK 22 或更高版本。
* Maven 3.6+（用于从源码构建）。

### 构建项目

```bash
git clone https://github.com/inshua/ago.git
cd ago
mvn clean package -DskipTests 
```

### 运行示例程序

创建 `hello.ago`：

```ago
fun main(){
    Trace.print("Hello, ago!")
}
```

编译并运行：

```bash
# 编译
java -cp path/to/ago-compiler/target/ago-compiler-<ver>.jar -agocp ago-sdk/lang.agopkg -i hello.ago

# 运行
java -jar path/to/ago-engine/target/ago-engine-1.0-SNAPSHOT.jar -agocp ago-sdk/lang.agopkg ./
```

你应该会看到：
```
Hello, ago!
```

> **提示：** 你也可以将 ago 引擎作为库嵌入到你的 Java 应用程序中。

---

## 语言基础

以下是展示核心概念的简短代码片段。

### 函数即类

```ago
fun add(a as int, b as int) as int{
    return a + b
}
fun main(){
    var f = new add(3, 4)
    Trace.print(f())   // 输出 7
}
```

### 结构化并发原语

```ago
fun f(){
    Trace.print('wait notify')
    await                // 让出控制权
    Trace.print('resume f')
    sleep(2000)
}

fun main(){
    var c = fork f();   // 启动子运行空间，获取其调用帧
    sleep(2000);        // 内置原生函数（异步）
    c.notify();         // 恢复子任务执行
    // 自动等待所有子运行空间完成    
}
```

`fork` 会创建一个新的 `RunSpace`。  
`notify()` 用于恢复其执行。

标准库还提供了：

| 函数 | 说明 |
|----------|-------------|
| `race<R>(functions as Function<R>...) as R` | 并行运行函数，返回首个结果并终止其余任务。 |
| `awaitMany(count as int)` | 挂起直至指定数量（*count*）的子任务完成。 |


### 运行空间（RunSpaces）

ago 支持对 `RunSpace` 进行子类化，你可以在对应的引擎环境中使用一种或多种类型的 `RunSpace`。目前除默认的 `RunSpace` 外，`WorkflowEngine` 还提供了几种专用空间，如 `EntityRunSpace`、`WorkflowRunSpace` 和 `EntityWorkflowRunSpace`。

- **EntityRunSpace** – 处理带有 *Entity* 注解的类的 ORM 操作。当 `EntityRunSpace` 启动时会自动开启事务；结束时会自动提交所有对 *Entity* 类型对象的修改。

例如：

```ago
fun createUser() as User {
    var u = new User() with {
        .name     = "Tom";
        .address  = "liberation street 222";
        .age      = 20;
    }
    return u;
}

fun main(){
    // 在 EntityRunSpace 中运行该函数
    var u = await createUser() via EntitySpace.new#();
    
    // after the forked EntityRunSpace ends, the User object saved

    Trace.print(u.name)
    Trace.print(u.age)
}
```

- **WorkflowRunSpace** – 持久化实现了 *Task* 接口的函数。当任务执行到带有 *Task* 注解的函数时，其执行点会被自动保存，从而使调用真正挂起——完全释放内存。此外，`Task` 提供了如 `RunAt::(node)` 等子类；当遇到 `RunAt` 任务时，`WorkflowRunSpace` 会以分布式方式迁移至其他节点。

以下是一个用 ago 编写的简单爬虫工作流示例。当执行 `downloadImages` 时，任务会被转移到 *downloader* 节点：

```ago
fun downloadImages(images as ArrayList<string>) with RunAt::("downloader") {
    for (var i = 0; i < images.size(); ++i) {
        var url = images[i];
        Trace.print("downloading " + url);
        downloadImage(url);
    }

    Trace.print("download finished");
}

fun runPage(page as int) as int with RunAt::("page") {
    var result = extractImages(page);
    downloadImages(result.images);

    Trace.print("fetch finished");

    return result.curPage;
}
```

宿主语言开发者（目前为 Java）可将 ago 视为 DSL，并编写自定义的 `RunSpace` 以将调用权限、定时触发及其他活动纳入自身控制。

对于 *Entity* 类型，ago 支持基于 Schema 谱系技术构建的 SQL 查询：

```ago
// 自动创建类 `userByName.Result` 作为其返回类型
query userByName(name as string?, minAge as int?, maxAge as int?, sort as Sort[]? = [Sort[] | new Sort('u.name', 'asc')]) {
    sql{.
        SELECT u.id, u.name, u.age FROM User u WHERE name = :name AND age >= :minAge AND age <= :maxAge
        ORDER BY :sort ASC
    .}
}

fun main(){
    var it2 = await userByName('Tom', null, 30, null) via EntitySpace.new#()
    for(var item in it){
        Trace.print(item.name)      // name 来自类型化的查询结果
        Trace.print(item.age)
    }
}
```

宿主语言开发者（目前为 Java）可以创建自定义的 `RunSpace`，在其自身生态系统中进行权限控制、定时触发等等随意操纵函数运行。

### 3. 元类与参数化类

```ago
class VarChar from String{
    metaclass{
        fun new(maxLength as int){}
    }
}
class Person{
    name as VarChar::(200)
}
fun main(){
    var p = new Person()
    p.name = 'John'         // 扩展装箱机制
    Trace.print(p.name)    
}
```

`VarChar::(200)` 会创建一个带有字段 `maxLength = 200` 的 `参数化类`。

### 基于 ClassRef 的泛型

```ago
class Animal{
    metaclass{
        fun foo(){
            Trace.print("Animal.foo")
        }
    }
}
class Cat from Animal{
    metaclass{
        override foo(){
            Trace.print("Cat.foo")
        }
    }
}
class Dog from Animal{
    override bark(){
        Trace.print("woof")
    }
}

fun main(){
    var t as classref = Cat
    Trace.print(t)
    
    var T as [Animal to _]      // ScopedClassInterval::(Animal, any)，classref 的装箱类
    T = Cat     
    T.foo()     

    var U like Animal
    U = Dog
    U.foo()
}
```

## 通过函数类实现回调

```ago
fun add(a as int, b as int) as int  { return a + b }

fun add2(a as int, b as int) as int { return a + b + a + b }
fun test(op like add, a as int, b as int){      // 关于 `like add`，见下文说明
    Trace.print(op(a, b))
}

class C{
    i as int
    fun new(i as int){
        this.i = i;
    }
    fun add(a as int, b as int) as int{ return this.i + a + b }

    fun add2(a as int, b as int) as int{ return i + a + b + a + b }
}

fun main(){
    test(add, 1, 2)     // 3
    test(add2, 1, 2)    // 6

    var c = new C(100);
    test(c.add, 1, 2)   // 103
    test(c.add2, 1, 2)  // 106
}
```
`like Class` 是 `as [Class to any]` 的语法糖。  
对于函数，ago 编译器会根据参数列表和返回类型生成对应的接口 `FunctionN<Arg1,Arg2,...>`。`like` 关键字始终会获取一个从该接口开始的 ScopedClassInterval（作用域类区间）。  
因此 `like add` 等价于 `as [Function2<int,int,int> to _]`，它可以匹配上述的 `add`、`add2` 等函数，且支持作用域约束。

### 重载与属性

```ago
fun f#1(i as int){}
fun f#2(i as int, j as int){}

class Person{
    name as string
    fun name#get as string{ return this.name }
    fun name#set(name as string){ this.name = name }
}
```

编译器会根据参数数量/类型解析调用；若需消除歧义，可显式使用 `f#2(1,2)`。

---

## 示例代码

> 所有示例 `.ago` 文件均位于仓库的 `test-cases/examples/` 目录中。

---

## 贡献指南

ago 语言开辟了一个全新的领域；有太多有趣的事物值得探索，我们非常欢迎你的参与。目前项目急需在编译器、游戏引擎、工作流引擎、低代码平台等领域的帮助；大语言模型（LLM）应用也将是 ago 的重要发展方向之一。
项目当前处于贡献者招募阶段——欢迎通过 GitHub Issue 进行登记报名。

---

## 许可证

ago 采用 **Apache-2.0** 许可证发布 – 详见 [LICENSE](LICENSE)。

如有问题或需要支持，请在 GitHub 上提交 Issue。