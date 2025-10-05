use actix_web::{HttpResponse, web};
use serde::Deserialize;

#[derive(Deserialize)]
pub struct FormData {
    title: String,
    description: String,
}

pub async fn create_task(form: web::Form<FormData>) -> HttpResponse {
    let _task = form.into_inner();
    HttpResponse::Ok().finish()
}
