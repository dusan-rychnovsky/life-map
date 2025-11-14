use secrecy::{ExposeSecret, SecretBox};
use sqlx::postgres::PgConnectOptions;
use sqlx::postgres::PgSslMode;

#[derive(serde::Deserialize)]
pub struct Settings {
    pub database: DatabaseSettings,
    pub application: ApplicationSettings,
}

#[derive(serde::Deserialize)]
pub struct ApplicationSettings {
    pub port: u16,
    pub host: String,
}

#[derive(serde::Deserialize)]
pub struct DatabaseSettings {
    pub username: String,
    pub password: SecretBox<String>,
    pub port: u16,
    pub host: String,
    pub database_name: String,
    pub require_ssl: bool,
}

impl DatabaseSettings {
    pub fn with_db(&self) -> PgConnectOptions {
        self.without_db().database(&self.database_name)
    }

    pub fn without_db(&self) -> PgConnectOptions {
        let ssl_mode = if self.require_ssl {
            PgSslMode::Require
        } else {
            PgSslMode::Prefer
        };
        PgConnectOptions::new()
            .host(&self.host)
            .username(&self.username)
            .password(self.password.expose_secret())
            .port(self.port)
            .ssl_mode(ssl_mode)
    }
}

pub fn get_configuration() -> Result<Settings, config::ConfigError> {
    let environment: Environment = std::env::var("APP_ENVIRONMENT")
        .unwrap_or_else(|_| "local".into())
        .try_into()
        .expect("Failed to parse APP_ENVIRONMENT.");

    config::Config::builder()
        .add_source(config::File::with_name("config/base"))
        .add_source(config::File::with_name(&format!(
            "config/{}",
            environment.as_str()
        )))
        .add_source(config::Environment::with_prefix("app").separator("__"))
        .build()?
        .try_deserialize()
}

pub enum Environment {
    Local,
    Prod,
}

impl Environment {
    pub fn as_str(&self) -> &'static str {
        match self {
            Environment::Local => "local",
            Environment::Prod => "prod",
        }
    }
}

impl TryFrom<String> for Environment {
    type Error = String;
    fn try_from(s: String) -> Result<Self, Self::Error> {
        match s.to_lowercase().as_str() {
            "local" => Ok(Self::Local),
            "prod" => Ok(Self::Prod),
            other => Err(format!(
                "{} is not a supported environment. Use either `local` or `prod`.",
                other
            )),
        }
    }
}
