
pub struct User {
    username: String,
    age: u32
}

impl User {
    
    pub fn new(username: &str, age: u32) -> Self {
        Self {
            username: String::from(username),
            age
        }
    }
    
    pub fn say_hello(&self) {
        println!("Hello, I'm {}, and I'm {} years old.", self.username.as_str(), self.age);
    }
    
    pub fn user_name(&self) -> &str {
        self.username.as_ref()
    }
}