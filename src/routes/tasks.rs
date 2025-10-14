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

#[tracing::instrument(
  name = "Creating a new task.",
  skip(form, pool),
  fields(
    task_title= %form.title
  )
)]
pub async fn create_task(form: web::Form<FormData>, pool: web::Data<PgPool>) -> HttpResponse {
    match insert_task(&pool, &form).await {
        Ok(_) => HttpResponse::Ok().finish(),
        Err(_) => HttpResponse::InternalServerError().finish(),
    }
}

#[tracing::instrument(name = "Saving a new task in the database.", skip(form, pool))]
pub async fn insert_task(pool: &PgPool, form: &FormData) -> Result<(), sqlx::Error> {
    sqlx::query!(
        r#"
      INSERT INTO tasks(id, title, description, created_at)
      VALUES($1,$2,$3,$4)
    "#,
        Uuid::new_v4(),
        form.title,
        form.description,
        Utc::now()
    )
    .execute(pool)
    .await
    .map_err(|e| {
        tracing::error!("Failed to execute query: {:?}", e);
        e
    })?;
    Ok(())
}
