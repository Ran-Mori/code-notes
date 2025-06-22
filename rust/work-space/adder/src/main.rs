use rand::Rng;

fn main() {
    let num = 10;
    println!("Hello, world! {num} plus one is {}!", add_one::add_one_fn(num));
}

fn test_use_rand() {
    rand::rng().random()
}
