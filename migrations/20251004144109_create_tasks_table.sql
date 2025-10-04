CREATE TABLE tasks(
  id uuid NOT NULL,
  title TEXT NOT NULL,
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL
);
