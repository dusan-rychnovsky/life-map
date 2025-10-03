use actix_web::dev::Server;
use actix_web::{App, HttpResponse, HttpServer, web};
use serde::Deserialize;
use std::net::TcpListener;

#[derive(Deserialize)]
struct FormData {
    title: String,
    description: String,
}

async fn health_check() -> HttpResponse {
    HttpResponse::Ok().finish()
}

async fn create_task(form: web::Form<FormData>) -> HttpResponse {
    let _task = form.into_inner();
    HttpResponse::Ok().finish()
}

pub fn run(listener: TcpListener) -> Result<Server, std::io::Error> {
    let server = HttpServer::new(|| {
        App::new()
            .route("/health_check", web::get().to(health_check))
            .route("/tasks", web::post().to(create_task))
    })
    .listen(listener)?
    .run();
    Ok(server)
}
