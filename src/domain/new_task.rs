use crate::domain::task_description::TaskDescription;
use crate::domain::task_title::TaskTitle;

pub struct NewTask {
    pub title: TaskTitle,
    pub description: TaskDescription,
}
