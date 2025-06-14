use std::env;
use std::error::Error;

pub struct Config {
    query: String,
    file_path: String,
    ignore_case: bool,
}

impl Config {
    pub fn build(args: &Vec<String>) -> Result<Config, &str> {
        if args.len() != 3 {
            Err("please input right file path and query word")
        } else {
            let config = Config {
                query: args[1].clone(),
                file_path: args[2].clone(),
                ignore_case: env::var("IGNORE_CASE").is_ok(),
            };
            Ok(config)
        }
    }
}

pub fn run(config: Config) -> Result<(), Box<dyn Error>> {
    let contents: String = std::fs::read_to_string(config.file_path)?;

    let results = if config.ignore_case {
        search_case_insensitive(&config.query, &contents)
    } else {
        search(&config.query, &contents)
    };

    for result in results {
        println!("{}", result);
    }
    Ok(())
}

fn search<'a>(query: & str, contents: &'a str) -> Vec<&'a str> {
    let mut result: Vec<&str> = Vec::new();
    for line in contents.lines() {
        if line.contains(query) {
            result.push(line)
        }
    }
    result
}

pub fn search_case_insensitive<'a>(
    query: &str,
    contents: &'a str,
) -> Vec<&'a str> {
    let query: String = query.to_lowercase();
    let mut results: Vec<&str> = Vec::new();

    for line in contents.lines() {
        if line.to_lowercase().contains(&query) {
            results.push(line);
        }
    }

    results
}
