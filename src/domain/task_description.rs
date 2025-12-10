use unicode_segmentation::UnicodeSegmentation;

#[derive(Debug)]
pub struct TaskDescription(String);

impl TaskDescription {
    pub fn parse(value: String) -> Result<TaskDescription, String> {
        let is_empty_or_whitespace = value.trim().is_empty();
        let is_too_long = value.graphemes(true).count() > 10000;
        if is_empty_or_whitespace || is_too_long {
            Err(format!("Invalid task description: {}", value))
        } else {
            Ok(Self(value))
        }
    }
}

impl AsRef<str> for TaskDescription {
    fn as_ref(&self) -> &str {
        &self.0
    }
}

#[cfg(test)]
mod tests {
    use crate::domain::task_description::TaskDescription;
    use claim::{assert_err, assert_ok};

    #[test]
    fn task_description_parse_valid_description_succeeds() {
        assert_ok!(TaskDescription::parse("A Task Description".to_string()));
    }

    #[test]
    fn task_description_parse_empty_description_fails() {
        assert_err!(TaskDescription::parse("".to_string()));
    }

    #[test]
    fn task_description_parse_whitespace_only_description_fails() {
        assert_err!(TaskDescription::parse(" ".to_string()));
    }

    #[test]
    fn task_description_parse_too_long_description_fails() {
        assert_err!(TaskDescription::parse("a".repeat(10001)));
    }

    #[test]
    fn task_description_parse_max_length_description_succeeds() {
        assert_ok!(TaskDescription::parse("a".repeat(10000)));
    }
}
