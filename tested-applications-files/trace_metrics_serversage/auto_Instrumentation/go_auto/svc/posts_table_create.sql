CREATE TABLE posts (
    id SERIAL PRIMARY KEY,     -- Auto-incrementing ID
    title VARCHAR(255) NOT NULL, -- Post title, max 255 characters
    content TEXT NOT NULL        -- Post content, unlimited length
);

INSERT INTO posts (title, content) VALUES
('First Post', 'This is the content of the first post.'),
('Second Post', 'Content for the second post goes here.'),
('Learning Golang', 'Golang is a fantastic language for building APIs.'),
('Database Integration', 'Using PostgreSQL with Go is simple and efficient.');

