use life_map::configuration::get_configuration;
use life_map::startup::run;
use sqlx::PgPool;
use std::net::TcpListener;

#[tokio::test]
async fn health_check_returns_200() {
    let app = spawn_app().await;
    let client = reqwest::Client::new();

    let response = client
        .get(&format!("{}/health_check", app.address))
        .send()
        .await
        .expect("Failed to execute request.");

    assert!(response.status().is_success());
    assert_eq!(Some(0), response.content_length());
}

#[tokio::test]
async fn create_task_returns_200_for_valid_form_data() {
    let app = spawn_app().await;
    let client = reqwest::Client::new();

    let body = "title=Test+Task&description=Test+Description";
    let response = client
        .post(&format!("{}/tasks", app.address))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(body)
        .send()
        .await
        .expect("Failed to execute request.");

    assert!(response.status().is_success());

    let saved = sqlx::query!("SELECT title, description FROM tasks")
        .fetch_one(&app.db_pool)
        .await
        .expect("Failed to fetch saved task.");

    assert_eq!(saved.title, "Test Task");
    assert_eq!(saved.description, Some("Test Description".to_string()));
}

#[tokio::test]
async fn create_task_returns_400_when_data_is_missing() {
    let app = spawn_app().await;
    let client = reqwest::Client::new();

    let test_cases = vec![
        ("title=Test+Task", "missing the description"),
        ("description=Test+Description", "missing the title"),
        ("", "missing both title and description"),
    ];
    for (invalid_body, error_message) in test_cases {
        let response = client
            .post(&format!("{}/tasks", app.address))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(invalid_body)
            .send()
            .await
            .expect("Failed to execute request.");
        assert_eq!(
            400,
            response.status().as_u16(),
            "The API did not fail with 400 Bad Request when the payload was {}.",
            error_message
        );
    }
}

pub struct TestApp {
    pub address: String,
    pub db_pool: PgPool,
}

async fn spawn_app() -> TestApp {
    let listener = TcpListener::bind("127.0.0.1:0").expect("Failed to bind a random port.");
    let port = listener.local_addr().unwrap().port();
    let address = format!("http://127.0.0.1:{}", port);

    let config = get_configuration().expect("Failed to read configuration.");
    let connection_pool = PgPool::connect(&config.database.connection_string())
        .await
        .expect("Failed to connect to Postgres.");

    let server = run(listener, connection_pool.clone()).expect("Failed to bind address.");
    let _ = tokio::spawn(server);
    TestApp {
        address,
        db_pool: connection_pool,
    }
}
