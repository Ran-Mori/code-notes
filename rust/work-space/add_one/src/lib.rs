use rand::Rng;

pub fn add_one_fn(x: i32) -> i32 {
    x + 1
}

fn test_use_rand() {
    rand::rng().random()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        let result = add_one_fn(3);
        assert_eq!(result, 4);
    }
}
