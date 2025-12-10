use unicode_segmentation::UnicodeSegmentation;

#[derive(Debug)]
pub struct TaskTitle(String);

impl TaskTitle {
    pub fn parse(value: String) -> Result<TaskTitle, String> {
        let is_empty_or_whitespace = value.trim().is_empty();
        let is_too_long = value.graphemes(true).count() > 256;
        if is_empty_or_whitespace || is_too_long {
            Err(format!("Invalid task title: {}", value))
        } else {
            Ok(Self(value))
        }
    }
}

impl AsRef<str> for TaskTitle {
    fn as_ref(&self) -> &str {
        &self.0
    }
}

#[cfg(test)]
mod tests {
    use crate::domain::task_title::TaskTitle;
    use claim::{assert_err, assert_ok};

    #[test]
    fn parse_valid_name_succeeds() {
        assert_ok!(TaskTitle::parse("A Task Title".to_string()));
    }

    #[test]
    fn parse_empty_title_fails() {
        assert_err!(TaskTitle::parse("".to_string()));
    }
    #[test]
    fn parse_whitespace_only_title_fails() {
        assert_err!(TaskTitle::parse(" ".to_string()));
    }

    #[test]
    fn parse_too_long_title_fails() {
        assert_err!(TaskTitle::parse("a".repeat(257)));
    }

    #[test]
    fn parse_max_length_title_succeeds() {
        assert_ok!(TaskTitle::parse("a".repeat(256)));
    }
}
