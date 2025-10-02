use life_map::run;

#[tokio::main]
async fn main() -> std::io::Result<()> {
    run()?.await
}
