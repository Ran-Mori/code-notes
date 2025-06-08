// Rust core language features demo
// 1. Variables and mutability
fn variable_demo() {
    let x = 5; // Immutable variable
    let mut y = 10; // Mutable variable
    println!("x = {}, y = {}", x, y);
    y += 5;
    println!("y after += 5: {}", y);
}

// 2. Basic data types
fn types_demo() {
    let a: i32 = 100;
    let b: f64 = 3.14;
    let c: bool = true;
    let d: char = '中';
    let e: &str = "String slice";
    println!("a={}, b={}, c={}, d={}, e={}", a, b, c, d, e);
}

// 3. Compound types (tuple and array)
fn compound_types_demo() {
    let tup: (i32, f64, u8) = (500, 6.4, 1);
    let (x, y, z) = tup; // Destructuring
    println!("tup: x={}, y={}, z={}", x, y, z);
    let arr = [1, 2, 3, 4, 5];
    println!("arr[0]={}", arr[0]);
}

// 4. Functions and expressions
fn add(x: i32, y: i32) -> i32 {
    x + y // Expression as a return value
}

// 5. Control flow
fn control_flow_demo() {
    let n = 3;
    if n < 5 {
        println!("n < 5");
    } else {
        println!("n >= 5");
    }
    for i in 0..3 {
        println!("for loop: {}", i);
    }
    let mut count = 0;
    while count < 3 {
        println!("while loop: {}", count);
        count += 1;
    }
    let mut num = 0;
    loop {
        if num == 2 { break; }
        println!("loop: {}", num);
        num += 1;
    }
}

// 6. Ownership, borrowing, and slices
fn ownership_demo() {
    let s = String::from("hello");
    takes_ownership(s);
    // println!("{}", s); // This will error, ownership of s has moved
    let x = 5;
    makes_copy(x); // i32 implements Copy trait
    let s1 = String::from("world");
    let len = calculate_length(&s1); // Borrowing
    println!("s1={}, len={}", s1, len);
    let slice = &s1[0..2]; // String slice
    println!("slice={}", slice);
}
fn takes_ownership(some_string: String) {
    println!("takes_ownership: {}", some_string);
}
fn makes_copy(some_integer: i32) {
    println!("makes_copy: {}", some_integer);
}
fn calculate_length(s: &String) -> usize {
    s.len()
}

// 7. Structs, methods, and associated functions
struct User {
    name: String,
    age: u8,
}
impl User {
    fn new(name: &str, age: u8) -> Self {
        Self { name: name.to_string(), age }
    }
    fn greet(&self) {
        println!("Hello, {}! Age:{}", self.name, self.age);
    }
}

// 8. Enums and pattern matching
enum Message {
    Quit,
    Move { x: i32, y: i32 },
    Write(String),
}
fn enum_demo(msg: Message) {
    match msg {
        Message::Quit => println!("Quit"),
        Message::Move { x, y } => println!("Move to ({}, {})", x, y),
        Message::Write(text) => println!("Write: {}", text),
    }
}

// 9. Generics, traits, and lifetimes
fn largest<T: PartialOrd>(list: &[T]) -> &T {
    let mut largest = &list[0];
    for item in list {
        if item > largest {
            largest = item;
        }
    }
    largest
}
trait Summary {
    fn summarize(&self) -> String;
}
impl Summary for User {
    fn summarize(&self) -> String {
        format!("{} ({})", self.name, self.age)
    }
}

// 10. Error handling
fn error_handling_demo() {
    let result: Result<i32, &str> = divide(10, 0);
    match result {
        Ok(val) => println!("Result: {}", val),
        Err(e) => println!("Error: {}", e),
    }
}
fn divide(x: i32, y: i32) -> Result<i32, &'static str> {
    if y == 0 {
        Err("Divisor cannot be 0")
    } else {
        Ok(x / y)
    }
}

// 11. Collections and iterators
fn collection_demo() {
    let mut v = vec![1, 2, 3];
    v.push(4);
    for i in &v {
        println!("vec element: {}", i);
    }
    let sum: i32 = v.iter().sum();
    println!("vec sum: {}", sum);
}

// 12. Closures and function pointers
fn closure_demo() {
    let add = |a, b| a + b;
    println!("closure add: {}", add(2, 3));
    fn apply<F: Fn(i32, i32) -> i32>(f: F, x: i32, y: i32) -> i32 {
        f(x, y)
    }
    println!("function pointer: {}", apply(add, 5, 6));
}

// 13. Modules and visibility
mod mymod {
    pub fn public_fn() {
        println!("This is a public function");
    }
    fn private_fn() {
        println!("This is a private function");
    }
}

// 14. Macros
macro_rules! say_hello {
    () => {
        println!("Hello, macro!");
    };
}

// 15. Using third-party crate serde
use serde::{Serialize, Deserialize};
#[derive(Serialize, Deserialize, Debug)]
struct SerdeDemo {
    id: u32,
    name: String,
}
fn serde_demo() {
    let s = SerdeDemo { id: 1, name: "Test".to_string() };
    let json = serde_json::to_string(&s).unwrap();
    println!("Serialize: {}", json);
    let de: SerdeDemo = serde_json::from_str(&json).unwrap();
    println!("Deserialize: {:?}", de);
}

fn main() {
    variable_demo();
    types_demo();
    compound_types_demo();
    println!("add(2,3)={}", add(2, 3));
    control_flow_demo();
    ownership_demo();
    let user = User::new("Xiao Ming", 18);
    user.greet();
    println!("User summary: {}", user.summarize());
    enum_demo(Message::Move { x: 10, y: 20 });
    let arr = [3, 5, 1, 9];
    println!("Max value: {}", largest(&arr));
    error_handling_demo();
    collection_demo();
    closure_demo();
    mymod::public_fn();
    say_hello!();
    serde_demo();
} 