use std::net::TcpListener;

#[tokio::test]
async fn health_check_returns_200() {
    let address = spawn_app();
    let client = reqwest::Client::new();
    let response = client
        .get(&format!("{}/health_check", address))
        .send()
        .await
        .expect("Failed to execute request.");

    assert!(response.status().is_success());
    assert_eq!(Some(0), response.content_length());
}

#[tokio::test]
async fn create_task_returns_200_for_valid_form_data() {
    let address = spawn_app();
    let client = reqwest::Client::new();

    let body = "title=Test+Task&description=Test+Description";
    let response = client
        .post(&format!("{}/tasks", address))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(body)
        .send()
        .await
        .expect("Failed to execute request.");

    assert!(response.status().is_success());
}

#[tokio::test]
async fn create_task_returns_400_when_data_is_missing() {
    let address = spawn_app();
    let client = reqwest::Client::new();
    let test_cases = vec![
        ("title=Test+Task", "missing the description"),
        ("description=Test+Description", "missing the title"),
        ("", "missing both title and description"),
    ];

    for (invalid_body, error_message) in test_cases {
        let response = client
            .post(&format!("{}/tasks", address))
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

fn spawn_app() -> String {
    let listener = TcpListener::bind("127.0.0.1:0").expect("Failed to bind a random port.");
    let port = listener.local_addr().unwrap().port();
    let server = life_map::run(listener).expect("Failed to bind address.");
    let _ = tokio::spawn(server);
    format!("http://127.0.0.1:{}", port)
}
