use actix_web::{HttpResponse, web};
use chrono::Utc;
use serde::Deserialize;
use sqlx::PgPool;
use uuid::Uuid;

#[derive(Deserialize)]
pub struct FormData {
    title: String,
    description: String,
}

pub async fn create_task(form: web::Form<FormData>, pool: web::Data<PgPool>) -> HttpResponse {
    match sqlx::query!(
        r#"
        INSERT INTO tasks(id, title, description, created_at)
        VALUES($1,$2,$3,$4)
        "#,
        Uuid::new_v4(),
        form.title,
        form.description,
        Utc::now()
    )
    .execute(pool.as_ref())
    .await
    {
        Err(e) => {
            eprintln!("Failed to execute query: {}", e);
            HttpResponse::InternalServerError().finish()
        }
        Ok(_) => HttpResponse::Ok().finish(),
    }
}
