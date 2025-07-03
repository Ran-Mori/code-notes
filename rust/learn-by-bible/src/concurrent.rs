use std::sync::mpsc;
use std::thread;
use std::time::Duration;

pub fn chanel() {
    println!("main thread start");
    let (s, r) = mpsc::channel::<i32>();
    thread::spawn(move || {
        println!("new thread start");
        s.send(1).unwrap();
        println!("new thread end");
    });
    println!("main thread start receive value");
    let r_value = r.recv().unwrap();
    println!("receive value :{}", r_value);
    println!("main thread end")
}