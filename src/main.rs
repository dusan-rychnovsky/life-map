use life_map::configuration::get_configuration;
use life_map::startup::run;
use life_map::telemetry::{get_subscriber, init_subscriber};
use sqlx::postgres::PgPoolOptions;
use std::net::TcpListener;
use std::time::Duration;

#[tokio::main]
async fn main() -> std::io::Result<()> {
    let subscriber = get_subscriber("life_map".into(), "info".into(), std::io::stdout);
    init_subscriber(subscriber);

    let config = get_configuration().expect("Failed to read configuration.");
    tracing::info!(
        "Attempting to connect to PostgreSQL at {}:{} as user {} to database {}",
        config.database.host,
        config.database.port,
        config.database.username,
        config.database.database_name
    );
    let connection_pool = PgPoolOptions::new()
        .acquire_timeout(Duration::from_secs(30))
        .connect_with(config.database.with_db())
        .await
        .expect("Failed to connect to Postgres.");
    let address = format!("{}:{}", config.application.host, config.application.port);
    let listener = TcpListener::bind(address)?;
    run(listener, connection_pool)?.await
}
