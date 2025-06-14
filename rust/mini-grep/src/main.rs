use mini_grep::Config;

fn main() {
    let args: Vec<String> = std::env::args().collect();

    let config: Config = Config::build(&args).unwrap_or_else(|err: &str| {
        eprintln!("{err}");
        std::process::exit(1);
    });

    mini_grep::run(config).unwrap();
}
