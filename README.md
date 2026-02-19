# ago Programming Language

> **Function is Class, and CallFrame is its Instance**

## Overview

ago 是基于“函数是类，CallFrame是函数的实例”思想设计的面向对象静态编程语言，赋予了函数和函数调用这两个核心而常见的程序语言要素对现实世界的“动作”的语义建模能力。

受 **Process Philosophy** 的启发，`ago` 认为现实世界的“动作”应当具有完整的生命周期状态——既包含起点也保留终点和中间过程。
传统的编程模型将 `CallFrame（调用帧）` 绑定在底层栈结构上且不可见、不支持持久化。而 ago 将其抽象为面向对象的概念：
*   **Function is Class**：函数是类。函数被提升为一等公民，具有生命周期属性和方法字段；
*   **Call Frame is its Instance**：CallFrame 是函数的实例。每一个函数调用，产生一个 Call Frame 实例。

This design gives you:

* **Asynchronous actions** – function invocations can suspend and resume like coroutines, but are just normal objects.
* **Full persistence** – every call frame is a heap object; its state can be stored in any database or key‑value store and recovered after a crash.
* **Distributed execution** – because frames are objects they can be shipped across nodes.
* **Object‑oriented semantics everywhere** – closures, generics, traits, meta‑classes, and parameterized classes all live in the same type system.

ago is developed with Java 22, you can embed it into any Java application or run it as a standalone interpreter.

> **Why use ago?**  
> When your business logic involves long‑running, event‑driven workflows (e.g. approvals, payment processing, game animation), traditional languages force you to mix callbacks, state machines, or external workflow engines. ago lets you write those flows *directly* as ordinary functions, with built‑in suspension, resumption, and persistence.

---

## Features

| Feature                                  | What it gives you                                                                                                       |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| **Function = Class**                     | Functions are classes; every function derive from `Function<R>`, you can implement interfaces/traits, add fields.       |
| **CallFrame = Instance**                 | Every invocation is an ordinal object on the heap – suspendable, serializable, interruptible.                           |
| **Meta‑classes & Parameterized Classes** | Embed constant values in types (e.g., `VarChar::(200)`).                                                                |
| **Scalarized ClassRefs (`classref`)**    | Treat classes as first‑class values; use them as generic type parameters or runtime checks.                             |
| **Extended Boxing**                      | Boxing works on subclass of `Boxer<T>`, `name as VarChar::(200) = 'John'` allowed.                                      |
| **Overloading at Invocation**            | Use `f#1`, `f#2` etc. – the compiler resolves at call time by argument types.                                           |
| **Attributes (getter/setter)**           | Declared as two overloads of a shared name (`name#get`, `name#set`).                                                    |
| **Structured concurrency primitives**    | `fork`, `await`, `race()`, `awaitMany()` – all built on CallFrames.                                                     |
| **RunSpace abstraction**                 | One RunSpace per "thread" of execution; can be backed by thread pools, event loops, or virtual threads.                 |
| **Persistence hooks**                    | Implement `Slots` to store state in PostgreSQL JSON columns, NoSQL, or even a blockchain.                               |
| **Extensible VM**                        | Add native functions, transform bytecode into Java bytecode or LLVM IR, or run on a stack‑based engine for performance. |

---

## Architecture

```
+-------------------+
|  AgoSource (.ago) |
+--------+----------+
         |
     compile → Bytecode
         |
+--------v----------+
|  AgoEngine        |   
|  - CallFrame      |  (ago bytecode evaluating within AgoFrame instance)
|  - RunSpace       |
|  - Slots          |
+--------+----------+
         |
  execute on JVM
```

* **`Instance`** – base for all objects. Holds a reference to its class (`agoClass`) and a `Slots` implementation.
* **`AgoClass` / `MetaClass`** – ordinary classes and meta‑classes. Functions are subclasses of `AgoClass`.
* **`CallFrame`** – runtime frame; stores local variables, program counter (`pc`), caller reference, etc. All frames are heap objects.
* **`RunSpace`** – owns the *current* call frame. When a function yields (via `await`) it changes its state to *WAITING_RESULT*, freeing the thread for another task.

Because everything is an object, you can:

1. Persist a frame by serializing its slots.
2. Transfer a frame across processes by sending its serialized form.
3. Resume execution in any JVM that knows how to interpret the bytecode.

---

## Getting Started

### Prerequisites

* JDK 22 or newer.
* Maven 3.6+ (for building from source) – optional if you just run the pre‑built jar.

### Build

```bash
git clone https://github.com/inshua/ago.git
cd ago
mvn clean package -DskipTests   # produces target/ago-<ver>.jar
```

### Run a Sample Program

Create `hello.ago`:

```ago
fun main(){
    Trace.print("Hello, ago!")
}
```

Compile & run:

```bash
java -cp target/ago-<ver>.jar org.inshua.ago.compiler.Compiler hello.ago
# The compiler writes bytecode to hello.bin

java -cp target/ago-<ver>.jar org.inshua.ago.runtime.AgoRuntime hello.bin
```

You should see:

```
Hello, ago!
```

> **Tip:** The compiler and runtime are bundled together in the same JAR; you can also use them as libraries inside a larger Java application.

---

## Language Basics

Below are quick snippets illustrating core concepts.

### 1. Function as Class

```ago
fun add(a as int, b as int) as int{
    return a + b
}
fun main(){
    var f = new add(3, 4)
    Trace.print(f())   // prints 7
}
```

### Structured Concurrency Primitives

```ago
fun f(){
    Trace.print('wait notify')
    await                // yields control
    Trace.print('resume f')
    sleep(2000)
}

fun main(){
    var c = fork f();   // start child RunSpace, get its CallFrame
    sleep(2000);        // built‑in native function (async)
    c.notify();         // resume the child
    // auto wait all children runspaces done    
}
```

`fork` creates a new `RunSpace`.  
`notify()` resumes it.

The library also provides:

| Function | Description |
|----------|-------------|
| `race<R>(functions as Function<R>...) as R` | Runs functions in parallel, returns first result and aborts others. |
| `awaitMany(count as int)` | Suspends until *count* child tasks complete. |


### 3. Meta‑class & Parameterized Class

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
    p.name = 'John'         // extended boxing
    Trace.print(p.name)    
}
```

`VarChar::(200)` creates a concrete class with the field `maxLength = 200`.

### 4. Generics via ClassRef

```ago
class Animal{
    metaclass{
        fun foo(){
            Trace.print("Animal.foo")
        }
    }
    fun bark(){
    }
}
class Cat from Animal{
    metaclass{
        override foo(){
            Trace.print("Cat.foo")
        }
    }
    override bark(){
        Trace.print("meow")
    }
}
class Dog from Animal{
    override bark(){
        Trace.print("woof")
    }
}

fun main(){
    var T as [Animal to _]
    T = Cat     // T got ScopedClassInterval::(Animal, _)(value=Cat, scope=assign Scope)
    T.foo()     // Invoke(ClassUnder.Instance(Cast(LocalVar(T), Meta@Animal), "foo#")

    var U like Animal
    U = Dog
    U.foo()

//    var c = new T()     // exception, lBound != uBound
//    c.bark()
    var T2 as [Cat to Cat] = Cat
    var c = new T2()
    c.bark()

    Trace.print(T == Cat)
    Trace.print(Dog != T)
}
fun main(){
    var t as classref = Cat
    Trace.print(t)
}
```

`like add` selects the `FunctionN<int,int>` interface – a type‑interval that matches any function with two `int` arguments.

### 5. Overloading & Attributes

```ago
fun f#1(i as int){}
fun f#2(i as int, j as int){}

class Person{
    name as string
    fun name#get as string{ return this.name }
    fun name#set(name as string){ this.name = name }
}
```

The compiler resolves calls based on argument count/types; explicit `f#2(1,2)` can be used to disambiguate.

### Persistence 

`ago-engine-rdb` 项目提供了完整的用 PG 持久化各类 ago 对象的实现，含 ago_class, ago_function, runspace, instances, call_frames 等，并用 JSON 实现了对 Slots 的映射。

这些实现使 ago 程序无需做任何修改就可以以数据库为持久化后台运行。 见 `test-cases/src/test/java/org/siphonlab/ago/test/Util.java`。

```java
    public static void run(String filename, String entrance) throws CompilationError, IOException {
        var selectedEngine = parseEngine();
        switch (selectedEngine){
            case NettyEngine:
                runInNettySpace(filename, entrance);
                break;

            case VertxEngine:
                runInVertxSpace(filename,entrance);
                break;

            case PGJsonLazyEngine:
                runWithPGJsonLazy(filename, entrance);
                break;
        }

    }
```

## Extending the Runtime

* **Custom Engines** – subclass `AgoEngine` or implement `RunSpaceHost` for your own scheduling policy.
* **Different Back‑Ends** – swap `Slots` implementations: in‑memory map, Redis hash, Cassandra column family, etc.
* **Ahead‑of‑time Compilation** – translate the VM bytecode into Java bytecode (`org.inshua.ago.compiler.AgoToJVM`) or LLVM IR for native execution.

---

## Examples

| Example | Description |
|---------|-------------|
| **AnimationRunSpace** | Re‑implements Cocos2d `Action` hierarchy using `fork`, `awaitNextFrame()`. |
| **Leave‑Request Workflow** | Each form submission is a suspended CallFrame; persistence guarantees recovery after crash. |
| **Bank Transfer (Distributed Transaction)** | Uses `TransactionRunSpace`; child runs are committed/rolled back atomically. |
| **Saga Pattern** | Implements compensating actions via `with SagaTransaction` interface and `rollback()` method. |

> All example `.ago` files live in the `examples/` directory of the repository.

---

## Contributing

1. Fork the repo, create a branch (`feature/<name>` or `bugfix/<name>`).
2. Run `mvn clean test` to ensure existing tests pass.
3. Add new features/tests and submit a pull request.

Please adhere to the style guidelines in `docs/style.md`.

---

## License

ago is released under the **Apache‑2.0** license – see [LICENSE](LICENSE).

For questions or support, open an issue on GitHub.

--- 

Happy coding! 🚀