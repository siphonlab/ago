# MyHashMap 实现笔记

## 文件位置
- 实现+测试: `test-cases/examples/collection/MyHashMapTest.ago`
- Java 测试: `test-cases/src/test/java/org/siphonlab/ago/test/CollectionTest.java`

## 当前状态
✅ 基本功能全部通过：int/string key 的 put/get/containsKey/remove/clear/putAll/keys/values/iterator

## ago 泛型类的限制（已验证）

### 1. Object[] 不能和 null 比较
```ago
// ❌ TypeMismatchError: cannot cast 'null' to 'lang.Object'
if(this.objKeys[i] != null){ ... }

// ✅ 用 boolean[] 跟踪占用状态
occupied as boolean[];
if(this.occupied[i]){ ... }
```

### 2. 泛型数组字段导致 NPE（已修复）
`Key[]`, `Value[]` 等泛型数组字段在类加载时触发 NPE。使用 `Object[]` + 运行时类型转换替代。

### 3. 所有字段必须在类顶部声明
ago 不允许在方法之间插入字段声明。

### 4. 辅助方法限制严重
- 不能接受基本类型参数（int, boolean 等）
- 不能有返回类型（`as int` 会导致语法错误）
- while 循环和复杂控制流在某些情况下不被解析
- **解决方案**：将所有逻辑内联到 override 方法中，使用 for 循环

### 5. 泛型返回值不能为 null
```ago
// ❌ cannot cast 'null' to generic type parameter
override get#key(index as Key) as Value{ return null; }

// ✅ 用实例字段存储结果
resultValue as Value;
override get#key(index as Key) as Value{
    // ... populate resultValue ...
    return this.resultValue;
}
```

### 6. Object key 的 equals() 支持（运行时限制）
**核心问题**：在 `Object[]` slot 中存储的对象调用 `.equals()` 时，没有正确分派到重写的方法。

调试发现：
- `instanceof Object` 对自定义类有效 ✓
- 存储的对象 class name 显示为正确的类型（如 "Person"）✓
- 但 `.equals()` 返回 false，即使对象应该相等 ✗

这是 ago 运行时的限制——在泛型 slot 中存储的对象的 `.equals()` 调用没有正确分派到重写的方法。

**临时解决方案**：当前实现使用 `k == index` 做比较，对基本类型和 string 有效。对于自定义 Object key，需要等待运行时修复。

## 实现细节

### 存储结构
- `objKeys as Object[]` — 存储 key（运行时通过 `| Key` 转换）
- `objValues as Object[]` — 存储 value（运行时通过 `| Value` 转换）
- `occupied as boolean[]` — 跟踪槽位占用状态

### 查找逻辑
线性搜索 + for 循环：
```ago
for(var i = 0; i < this.capacity; i++){
    if(this.occupied[i]){
        var k = this.objKeys[i] | Key;
        if(k == index){ ... }
    }
}
```

## hashCode extension methods（已添加到 SDK）

为以下 primitive types 添加了 `hashCode` extension method：
- int: `fun hashCode(this as int) as int{ return this; }`
- long: `fun hashCode(this as long) as int{ return (this bxor (this >>> 32)) | int; }`
- float: native helper `Float_floatToIntBits` + extension method
- double: native helper `Double_doubleToLongBits` + extension method
- byte: `fun hashCode(this as byte) as int{ return this | int; }`
- short: `fun hashCode(this as short) as int{ return this | int; }`
- char: `fun hashCode(this as char) as int{ return this | int; }`
- boolean: `fun hashCode(this as boolean) as int{ return 1231 if this else 1237; }`
- string: delegates to String.hashCode()
- decimal: native helper `Decimal_hashCode` + extension method

## 后续改进方向
1. **修复运行时 bug**：Object[] slot 中对象的 `.equals()` 调用分派问题
2. **实现真正的哈希表**：当前是线性搜索，需要添加 hash function + linear probing
3. **支持 resize**：容量不足时自动扩容
4. **Object key equals() 支持**：核心目标，需要运行时配合修复

## ago 语法要点回顾
- `{}` 不产生作用域，只有类和函数才有作用域
- 函数名必须包含 `#`（编译器自动追加）
- override 方法不需要 `#` 后缀
- 基本类型不是对象，需要装箱才能调用 .hashCode() 等方法
- for 循环可用，while 循环在泛型类中有问题
- XOR 运算符是 `bxor`，不是 `^`
