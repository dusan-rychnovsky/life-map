use crate::domain::{new_task::NewTask, task_description::TaskDescription, task_title::TaskTitle};
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
    let new_task = match form.0.try_into() {
        Ok(task) => task,
        Err(e) => {
            tracing::warn!("Failed to parse new task: {}", e);
            return HttpResponse::BadRequest().finish();
        }
    };
    match insert_task(&pool, &new_task).await {
        Ok(_) => HttpResponse::Ok().finish(),
        Err(_) => HttpResponse::InternalServerError().finish(),
    }
}

impl TryFrom<FormData> for NewTask {
    type Error = String;

    fn try_from(value: FormData) -> Result<Self, Self::Error> {
        let title = TaskTitle::parse(value.title)?;
        let description = TaskDescription::parse(value.description)?;
        Ok(NewTask { title, description })
    }
}

#[tracing::instrument(name = "Saving a new task in the database.", skip(new_task, pool))]
pub async fn insert_task(pool: &PgPool, new_task: &NewTask) -> Result<(), sqlx::Error> {
    sqlx::query!(
        r#"
      INSERT INTO tasks(id, title, description, created_at)
      VALUES($1,$2,$3,$4)
    "#,
        Uuid::new_v4(),
        new_task.title.as_ref(),
        new_task.description.as_ref(),
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
