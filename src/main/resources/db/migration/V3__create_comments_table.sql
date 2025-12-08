-- Migration script to create comments table for task comments feature

CREATE TABLE comments (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id NUMBER NOT NULL,
    user_id NUMBER NOT NULL,
    content VARCHAR2(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_comment_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_task_id ON comments(task_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);

-- Commit the changes
COMMIT;

