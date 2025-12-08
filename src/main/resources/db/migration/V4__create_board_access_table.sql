-- Migration script to create board_access table for board sharing feature

CREATE TABLE board_access (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    board_id NUMBER NOT NULL,
    user_id NUMBER NOT NULL,
    access_level VARCHAR2(10) NOT NULL CHECK (access_level IN ('READ', 'WRITE', 'ADMIN')),
    granted_by NUMBER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_board_access_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    CONSTRAINT fk_board_access_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_board_access_granted_by FOREIGN KEY (granted_by) REFERENCES users(id),
    CONSTRAINT uk_board_access UNIQUE (board_id, user_id)
);

CREATE INDEX idx_board_access_board_id ON board_access(board_id);
CREATE INDEX idx_board_access_user_id ON board_access(user_id);

-- Commit the changes
COMMIT;

