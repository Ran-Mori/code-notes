use std::cmp::Ordering;
use rand::Rng;

pub fn guess_number() {
    let secret_number: i32 = rand::rng().random_range(1..101);

    println!("Guess the number! the number is between 1 and 100");
    println!("Please input your guess.");

    loop {
        let mut guess: String = String::new();

        std::io::stdin().read_line(&mut guess).unwrap();
        println!("your guess is {guess}");

        let guess: i32 = match guess.trim().parse() {
            Ok(value) => value,
            _ => continue,
        };

        match guess.cmp(&secret_number) {
            Ordering::Less => {
                println!("too small");
            }
            Ordering::Greater => {
                println!("too large");
            }
            Ordering::Equal => {
                println!("your guess is right");
                break;
            }
        }
    }
}