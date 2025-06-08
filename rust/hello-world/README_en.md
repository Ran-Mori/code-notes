# Rust Hello World Language Features Demo

This project demonstrates the core features of the Rust language, with rich comments to help you quickly understand Rust syntax and usage. It also integrates third-party crates (such as serde and serde_json) to help you learn about Rust dependency management.

## Project Structure

```
├── Cargo.toml      # Project configuration and dependencies
├── src/
│   └── main.rs     # Main program, covers all core features
```

## How to Fetch Dependencies

On first run or after adding dependencies, use the following command to automatically fetch and build all dependencies:

```bash
cargo build
```

Or simply run the project, which will also fetch dependencies automatically:

```bash
cargo run
```

## How to Run

In the project root directory, execute:

```bash
cargo run
```

This will compile and run the project, and the console will output demonstrations of various Rust features.

## Main Features Demonstrated

- Variables and mutability
- Basic data types
- Compound types (tuple, array)
- Functions and expressions
- Control flow (if, for, while, loop)
- Ownership, borrowing, and slices
- Structs, methods, and associated functions
- Enums and pattern matching
- Generics, traits, and lifetimes
- Error handling (Result, Option)
- Collections and iterators
- Closures and function pointers
- Modules and visibility
- Macros
- Usage of third-party crates serde/serde_json

## Dependency Management

All dependencies are declared in the `Cargo.toml` file. For example:

```
[dependencies]
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
```

Rust will automatically download and manage dependencies for you.

---

For more information about Rust, please refer to the [official Rust documentation](https://www.rust-lang.org/learn). 