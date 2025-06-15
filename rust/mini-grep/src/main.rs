use mini_grep::Config;

fn main() {
    let args = std::env::args();

    let config: Config = Config::build(args).unwrap_or_else(|err: &str| {
        eprintln!("{err}");
        std::process::exit(1);
    });

    mini_grep::run(config).unwrap();
}
