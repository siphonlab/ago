# ago — Agent Instructions

## Project overview

- **Language**: Java 22, Maven multi-module monorepo (version `0.7.0-EA`).
- **Purpose**: Implementation of the "ago" programming language — a custom compiler + runtime where functions are classes and call frames are heap objects.

### Module boundaries

| Module | Role | Key dependency on other modules |
|---|---|---|
| `ago-engine` | Core runtime (CallFrame, RunSpace, bytecode interpreter) | none |
| `ago-compiler` | ANTLR-based compiler; produces `.agoc` bytecode classes | depends on `ago-engine` |
| `ago-engine-rdb` | RDB persistence layer (PostgreSQL/H2, Ebean ORM) | depends on `ago-engine`; uses Groovy-Eclipse compiler |
| `test-cases` | JUnit 5 test suite; compiles + runs `.ago` files at runtime | depends on all three modules above |

## Build & test commands

```bash
# Full build (skip tests)
mvn clean package -DskipTests

# Run default test suite (many suites excluded by surefire config, see below)
mvn test -pl test-cases

# Run a single test class or method
mvn test -pl test-cases -Dtest=ExprTest
mvn test -pl test-cases -Dtest=ExprTest#someMethod
```

### Test suite exclusions (default surefire config)

The following are **excluded** from `mvn test` by default in `test-cases/pom.xml`:

- `*WorkflowTest.java` — requires running PostgreSQL
- `ConcurrentTests.java`
- `JsonTest.java`
- `RdbDdlTest.java` — requires running PostgreSQL
- `RestfulTest.java`
- `CompilerTest.java`
- `HttpServerTest.java`

To run excluded suites, override with `-Dtest=...`.

### Runtime SDK dependency for tests

Tests load the ago standard library at runtime from one of two locations (checked in order):

1. `../ago-sdk/compiled/lang/` — directory of compiled `.class` files
2. `../ago-sdk/lang.agopkg` — pre-packaged ZIP archive

Both paths are **relative to the test working directory** (`test-cases/`). If neither exists, tests will fail with class-loading errors. To rebuild the SDK from source, run:

```bash
mvn test -pl test-cases -Dtest=CompileSdk
```

(`CompilerTest.langCompile` is also available but annotated `@Disabled`.)

### PostgreSQL for RDB / workflow tests

RDB and workflow tests need a live PostgreSQL instance. Configure via `database.properties` on the test classpath (e.g., place in `test-cases/src/test/resources/`). If absent, these defaults are used:

```properties
host=127.0.0.1
port=5432
database=ago
user=ago
password=ago
```

### Engine selection env var

The `engine` environment variable controls which runtime engine tests use:

| Value | Engine | Notes |
|---|---|---|
| *(empty)* or `netty` | Netty-based RunSpace (default) | — |
| `vertx` | Vert.x-based RunSpace | — |
| `workflow` | WorkflowEngine with PostgreSQL JSON persistence | requires DB |

## Codegen: ANTLR grammar

- Grammar files live in `ago-compiler/src/main/antlr/` (`AgoLexer.g4`, `AgoParser.g4`).
- Generated Java sources are written to `ago-compiler/gen/org/siphonlab/ago/compiler/parser/`.
- The `gen/` directory is gitignored and added as a source root via `build-helper-maven-plugin`.
- Running `mvn generate-sources -pl ago-compiler` regenerates parser code.

## Important paths & conventions

- `.ago` source files for tests live under `test-cases/examples/`, organized by feature area (`bootstrap/`, `expr/`, `control/`, etc.).
- Compiled test output goes to `test-cases/output/` (gitignored).
- The root `pom.xml` uses `maven.compiler.source/target=22`. No preview features are enabled.
- `ago-engine-rdb` uses the Groovy-Eclipse compiler (`groovy-eclipse-compiler`) instead of the default JDK compiler — required for its build configuration.

## Gitignored paths

The following directories and files are gitignored and should not be committed:

| Path | Contents |
|---|---|
| `**/gen/` | ANTLR-generated parser sources |
| `**/target/` | Maven build output |
| `**/output/` | Compiled `.ago` test results |
| `*.agoc` | Compiled ago bytecode classes |
| `ago-sdk/compiled/` | SDK compilation artifacts |
