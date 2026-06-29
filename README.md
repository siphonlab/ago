![Project Logo](./logo.svg)

# ago Programming Language

[中文](README_zh_CN.md)

> **FUNCTION IS CLASS, and CALLFRAME IS INSTANCE**

## Overview

**ago**(derived from the Esperanto term `ago` meaning `action.`) is an object-oriented static programming language designed around the idea that *"FUNCTION IS CLASS, and CALLFRAME IS INSTANCE"*, enabling functions and their invocations—the two core, ubiquitous elements of programming languages—to semantically model real‑world Actions.

Inspired by Process Philosophy, ago believes that real‑world “actions” should have a complete lifecycle state—containing both start and end points as well as intermediate processes. Traditional programming models bind the CallFrame to an underlying stack structure, making it invisible and far from programmer. ago abstracts it as an object‑oriented concept:
*   **FUNCTION IS CLASS**: Functions become classes. They are first-class citizens with lifecycle properties and method fields.
*   **CALLFRAME IS INSTANCE**: CallFrames materialize as heap-based objects when functions creator and execute.

This design gives you:

* **Asynchronous actions** – Function invocations can suspend and resume like coroutines, since they are ordinary objects.
* **Full persistence** – Every call frame is a heap object; its state can be stored in any database or key-value store and recovered after a crash.
* **Distributed execution** – Because frames are objects they can be shipped across nodes.
* **Object-oriented semantics everywhere** – Closures, generics, traits, meta-classes, and parameterized classes all live in the same type system.
* **Heap‑based rather than stack‑based execution environment** – Ago designed a **RunSpace** based on iterative mechanism to execute call frames, thereby avoiding the use of thread stacks. As a kind of logical object, `RunSpace` can be migrated across different environments and can be subclassed.

ago is implemented with Java 22. You can embed it into any Java application or run it as a standalone interpreter.

> **Why use ago?**  
> When your business logic involves long-running, event-driven workflows (e.g., approvals, payment processing, game animation), traditional languages force you to mix callbacks, state machines, or external workflow engines. ago lets you write those flows *directly* as ordinary functions with built-in suspension, resumption, and persistence.

For a deeper understanding, please see the paper at https://doi.org/10.5281/zenodo.20919493. 

> At present, ago is still in its early beta‑testing phase and requires substantial development work before it’s ready for production use. We therefore do not recommend deploying it in formal projects at this time.
> We warmly invite you to join us in refining it! If you’re willing to contribute a pilot project, we are glad to grow together with you.
---

## Features

| Feature                                  | What it gives you                                                                                                       |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| **Function = Class**                     | Functions are classes; every function derives from `Function<R>`, you can implement interfaces/traits, add fields.       |
| **CallFrame = Instance**                 | Every invocation is an ordinary object on the heap – suspendable, serializable, interruptible.                           |
| **Meta-classes & Parameterized Classes** | Embed constant values in types (e.g., `VarChar::(200)`).                                                                |
| **Scalarized ClassRefs (`classref`)**    | Treat classes as first-class values; use them as generic type parameters or runtime checks.                             |
| **Extended Boxing**                      | Boxing works on subclasses of `Boxer<T>`, syntax like `name as VarChar::(200) = 'John'` is allowed.                                      |
| **Overloading at Invocation**            | Use `f#1`, `f#2` etc. – the compiler resolves at call time by argument types.                                           |
| **Attributes (getter/setter)**           | Declared as two overloads of a shared name (`name#get`, `name#set`).                                                    |
| **Structured concurrency primitives**    | `fork`, `await`, `race()`, `awaitMany()` – all built on CallFrames.                                                     |
| **RunSpace abstraction**                 | One RunSpace per "thread" of execution; can be backed by thread pools, event loops, or virtual threads.                 |
| **Persistence hooks**                    | Implement `Slots` to store state in PostgreSQL JSON columns, NoSQL, or even a blockchain.                               |

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

* **`Instance`** – Base for all ago objects. Holds a reference to its class (`agoClass`) and a `Slots` implementation.
* **`AgoClass` / `MetaClass`** – Ordinary classes and meta-classes. Functions are subclasses of `AgoClass`.
* **`CallFrame`** – Runtime frame; stores local variables, program counter (`pc`), caller reference, etc. All frames are heap objects.
* **`RunSpace`** – Owns the *current* call frame. When a function yields (via `await`) it changes its state to *WAITING_RESULT*, freeing the thread for another task.

Because everything is an object:
1. You can persist a frame by serializing its slots.
2. Transfer a frame across processes using serialized form.
3. Resume execution in any JVM that knows how to interpret the bytecode.

---

## Getting Started

### Prerequisites

* JDK 22 or newer.
* Maven 3.6+ (for building from source).

### Build

```bash
git clone https://github.com/siphonlab/ago.git
cd ago
mvn clean package -DskipTests 
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
# compile
java -cp path/to/ago-compiler/target/ago-compiler-<ver>.jar -agocp ago-sdk/lang.agopkg -i hello.ago

# run
java -jar path/to/ago-engine/target/ago-engine-<ver>.jar -agocp ago-sdk/lang.agopkg ./
```

You should see:
```
Hello, ago!
```

> **Tip:** You can also embed ago engine as libraries inside your Java application.

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
    sleep(2000);        // built-in native function (async)
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


### RunSpaces

ago supports subclassing `RunSpace`, and you can use one or more kinds of `RunSpace` within the corresponding engine environment. Currently, exclude the default `RunSpace`, `WorkflowEngine` also provides several specialized spaces such as `EntityRunSpace`, `WorkflowRunSpace`, and `EntityWorkflowRunSpace`.

- **EntityRunSpace** – Handles ORM operations for classes marked with the *Entity* annotation. When an `EntityRunSpace` starts, it automatically opens a transaction; when it ends, it commits all changes made to objects of type *Entity*.

For example:

```ago
class User with Entity<User>{
    public name as string;
    public address as string;
    public age as int;
}

fun createUser() as User {
    var u = new User() with {
        .name     = "Tom";
        .address  = "liberation street 222";
        .age      = 20;
    }
    return u;
}

fun main(){
    // Run the function inside an EntityRunSpace
    var u = await createUser() via EntitySpace.new#();

    Trace.print(u.name)
    Trace.print(u.age)
}
```

- **WorkflowRunSpace** – Persists functions implemented *Task* interface. When a task reaches a *Task*-annotated function, its execution point is automatically saved so that the call can truly suspend—completely releasing memory. And `Task` provides subclasses such as `RunAt::(node)`, when encountering a `RunAt` task, the `WorkflowRunSpace` will migrate to another node in a distributed fashion.

Below is an example of a simple crawler workflow written in ago. When `downloadImages` runs, the task is transferred to the *downloader* node:

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

Host language developers (currently Java) can treat ago as a DSL and write their own `RunSpace` to bring call permissions, scheduled triggers, and other activities under their control.

For *Entity* types, ago supports SQL queries built on schema‑lineage technology:

```ago
// Automatically creates the class `userByName.Result` as its result type
query userByName(name as string?, minAge as int?, maxAge as int?, sort as Sort[]? = [Sort[] | new Sort('u.name', 'asc')]) {
    sql{.
        SELECT u.id, u.name, u.age FROM User u WHERE name = :name AND age >= :minAge AND age <= :maxAge
        ORDER BY :sort ASC
    .}
}

fun main(){
    var it2 = await userByName('Tom', null, 30, null) via EntitySpace.new#()
    for(var item in it){
        Trace.print(item.name)      // name comes with a typed query result
        Trace.print(item.age)
    }
}
```

Host language developers (currently Java) can create their own `RunSpace` to manage permissions, timing triggers, and other concerns within their own ecosystem.

### 3. Meta-class & Parameterized Class

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

`VarChar::(200)` creates a `parameterized class` with the field `maxLength = 200`.

### 4. Generics via ClassRef

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
    
    var T as [Animal to _]      // ScopedClassInterval::(Animal, any), a boxing class of classref
    T = Cat     
    T.foo()     

    var U like Animal
    U = Dog
    U.foo()
}
```

## Callback via Function Class

```ago
fun add(a as int, b as int) as int  { return a + b }

fun add2(a as int, b as int) as int { return a + b + a + b }
fun test(op like add, a as int, b as int){      // what's `like add`, see the explanation below
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
`like Class` is syntactic sugar for `as [Class to any]`.  
For functions, the ago compiler generates an interface `FunctionN<Arg1,Arg2,...>` corresponding to parameter lists and result types. The `like` always gets a ScopedClassInterval starting from this interface.  
Thus `like add` is equivalent to `as [Function2<int,int,int> to _]`, which can match the above `add`, `add2`, etc., even with scope.

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
---

## Examples

> All example `.ago` files live in the `examples/` directory of the repository.

---

## Contributing

The ago language opens a new domain; there are too many interesting things to explore, and your participation is very welcome. We urgently need help with compilers, game engines, workflow engines, low‑code platforms, and other areas; LLM applications will also be a direction for ago. 
The project is currently in the contributor sign‑up phase—feel free to register via an issue.

## Support this project

If this project helps you, consider supporting it:

- PayPal: https://paypal.me/inshua

---

## License

ago is released under the **Apache-2.0** license – see [LICENSE](LICENSE).

For questions or support, open an issue on GitHub.