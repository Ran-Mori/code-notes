mod main_en;

// Rust核心语言特性示例
// 1. 变量与可变性
fn variable_demo() {
    let x = 5; // 不可变变量
    let mut y = 10; // 可变变量
    println!("x = {}, y = {}", x, y);
    y += 5;
    println!("y after += 5: {}", y);
}

// 2. 基本数据类型
fn types_demo() {
    let a: i32 = 100;
    let b: f64 = 3.14;
    let c: bool = true;
    let d: char = '中';
    let e: &str = "字符串切片";
    println!("a={}, b={}, c={}, d={}, e={}", a, b, c, d, e);
}

// 3. 复合类型（元组与数组）
fn compound_types_demo() {
    let tup: (i32, f64, u8) = (500, 6.4, 1);
    let (x, y, z) = tup; // 解构
    println!("tup: x={}, y={}, z={}", x, y, z);
    let arr = [1, 2, 3, 4, 5];
    println!("arr[0]={}", arr[0]);
}

// 4. 函数与表达式
fn add(x: i32, y: i32) -> i32 {
    x + y // 表达式作为返回值
}

// 5. 控制流
fn control_flow_demo() {
    let n = 3;
    if n < 5 {
        println!("n < 5");
    } else {
        println!("n >= 5");
    }
    for i in 0..3 {
        println!("for循环: {}", i);
    }
    let mut count = 0;
    while count < 3 {
        println!("while循环: {}", count);
        count += 1;
    }
    let mut num = 0;
    loop {
        if num == 2 { break; }
        println!("loop循环: {}", num);
        num += 1;
    }
}

// 6. 所有权、借用与切片
fn ownership_demo() {
    let s = String::from("hello");
    takes_ownership(s);
    // println!("{}", s); // 这里会报错，s的所有权已被转移
    let x = 5;
    makes_copy(x); // i32实现了Copy trait
    let s1 = String::from("world");
    let len = calculate_length(&s1); // 借用
    println!("s1={}, len={}", s1, len);
    let slice = &s1[0..2]; // 字符串切片
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

// 7. 结构体、方法与关联函数
struct User {
    name: String,
    age: u8,
}
impl User {
    fn new(name: &str, age: u8) -> Self {
        Self { name: name.to_string(), age }
    }
    fn greet(&self) {
        println!("你好, {}! 年龄:{}", self.name, self.age);
    }
}

// 8. 枚举与模式匹配
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

// 9. 泛型、trait与生命周期
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

// 10. 错误处理
fn error_handling_demo() {
    let result: Result<i32, &str> = divide(10, 0);
    match result {
        Ok(val) => println!("结果: {}", val),
        Err(e) => println!("错误: {}", e),
    }
}
fn divide(x: i32, y: i32) -> Result<i32, &'static str> {
    if y == 0 {
        Err("除数不能为0")
    } else {
        Ok(x / y)
    }
}

// 11. 集合与迭代器
fn collection_demo() {
    let mut v = vec![1, 2, 3];
    v.push(4);
    for i in &v {
        println!("vec元素: {}", i);
    }
    let sum: i32 = v.iter().sum();
    println!("vec求和: {}", sum);
}

// 12. 闭包与函数指针
fn closure_demo() {
    let add = |a, b| a + b;
    println!("闭包add: {}", add(2, 3));
    fn apply<F: Fn(i32, i32) -> i32>(f: F, x: i32, y: i32) -> i32 {
        f(x, y)
    }
    println!("函数指针: {}", apply(add, 5, 6));
}

// 13. 模块与可见性
mod mymod {
    pub fn public_fn() {
        println!("这是一个公有函数");
    }
    fn private_fn() {
        println!("这是一个私有函数");
    }
}

// 14. 宏
macro_rules! say_hello {
    () => {
        println!("Hello, 宏!");
    };
}

// 15. 使用第三方库serde
use serde::{Serialize, Deserialize};
#[derive(Serialize, Deserialize, Debug)]
struct SerdeDemo {
    id: u32,
    name: String,
}
fn serde_demo() {
    let s = SerdeDemo { id: 1, name: "测试".to_string() };
    let json = serde_json::to_string(&s).unwrap();
    println!("序列化: {}", json);
    let de: SerdeDemo = serde_json::from_str(&json).unwrap();
    println!("反序列化: {:?}", de);
}

fn main() {
    variable_demo();
    types_demo();
    compound_types_demo();
    println!("add(2,3)={}", add(2, 3));
    control_flow_demo();
    ownership_demo();
    let user = User::new("小明", 18);
    user.greet();
    println!("User summary: {}", user.summarize());
    enum_demo(Message::Move { x: 10, y: 20 });
    let arr = [3, 5, 1, 9];
    println!("最大值: {}", largest(&arr));
    error_handling_demo();
    collection_demo();
    closure_demo();
    mymod::public_fn();
    say_hello!();
    serde_demo();
} 