CREATE TABLE tasks (
    id          UUID PRIMARY KEY,
    title       TEXT NOT NULL,
    description TEXT NOT NULL,
    status      TEXT NOT NULL CHECK (status IN ('new', 'active', 'completed', 'removed'))
);
