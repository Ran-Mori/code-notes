# Rust Hello World 语言特性演示

本项目旨在演示Rust语言的核心特性，并通过丰富的注释帮助你快速理解Rust的语法和用法。项目还集成了第三方库（如serde、serde_json），便于你了解Rust的依赖管理。

## 目录结构

```
├── Cargo.toml      # 项目配置及依赖声明
├── src/
│   └── main.rs     # 主程序，涵盖所有核心特性
```

## 如何拉取依赖

首次运行或添加依赖后，使用如下命令自动拉取并编译所有依赖库：

```bash
cargo build
```

或直接运行时自动拉取：

```bash
cargo run
```

## 如何运行

在项目根目录下执行：

```bash
cargo run
```

即可编译并运行项目，控制台会输出各类Rust特性的演示结果。

## 主要演示特性

- 变量与可变性
- 基本数据类型
- 复合类型（元组、数组）
- 函数与表达式
- 控制流（if、for、while、loop）
- 所有权、借用与切片
- 结构体、方法与关联函数
- 枚举与模式匹配
- 泛型、trait与生命周期
- 错误处理（Result、Option）
- 集合与迭代器
- 闭包与函数指针
- 模块与可见性
- 宏
- 第三方库serde/serde_json的使用

## 依赖管理说明

所有依赖均在 `Cargo.toml` 文件中声明。以本项目为例：

```
[dependencies]
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
```

Rust会自动下载和管理依赖，无需手动处理。

---

如需了解更多Rust相关内容，建议查阅 [Rust官方文档](https://www.rust-lang.org/zh-CN/learn)。 