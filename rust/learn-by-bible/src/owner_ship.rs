

pub fn test_all() {
    let s1: String = String::from("hello world");
    let s2: String = s1;
    // value used after moved
    // println!("s1 = {s1}, s2 = {s2}");


    let s1: String = String::from("hello");
    take_ownership(s1); // s1 is not valid in owner_ship_and_copy
    // println!(s1) // compile error

    let i: i32 = 10;
    make_copy(i); // use copy
    println!("i is {i}"); // not compile error

    let s2: String = give_ownership(); // take owner_ship from a function
    println!("s2 is {s2}");

    let s3: String = String::from("hello");
    reference(&s3);
    println!("sill have the owner ship of s3, and s3 is {s3}");

    let mut s4: String = String::from("hello");
    let slice = slice(&s4);
    // s4.push('C'); // compile error, two borrow
    println!("slice is {slice}");
    
}

fn take_ownership(s: String) {
    println!("take the ownership of {s}");
}

fn make_copy(i: i32) {
    println!("make the copy of {i}");
}

fn give_ownership() -> String {
    String::from("hello")
}

fn reference(s: &String) -> usize {
    s.len()
}

// 不需要所有权，因此只用传引用
fn slice(s: &String) -> &str {
    let length = s.len();
    if length >= 3 { 
        &s[..3]
    } else {
        &s[..length]
    }
}