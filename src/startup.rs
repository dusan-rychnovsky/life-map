use crate::routes::{health_check, tasks};
use actix_web::dev::Server;
use actix_web::{App, HttpServer, web};
use std::net::TcpListener;

pub fn run(listener: TcpListener) -> Result<Server, std::io::Error> {
    let server = HttpServer::new(|| {
        App::new()
            .route("/health_check", web::get().to(health_check::health_check))
            .route("/tasks", web::post().to(tasks::create_task))
    })
    .listen(listener)?
    .run();
    Ok(server)
}
