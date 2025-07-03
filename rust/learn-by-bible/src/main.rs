use crate::user::User;

mod guess_number;
mod owner_ship;
mod user;
mod concurrent;

fn main() {
    // guess_number::guess_number();
    owner_ship::test_all();

    let user: User = User::new("Alice", 18);
    user.say_hello();
    println!("user name is {}", user.user_name());
    
    concurrent::chanel()
}