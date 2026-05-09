CREATE TABLE moment_visible_users (
    id BIGSERIAL PRIMARY KEY,
    moment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (moment_id) REFERENCES moments(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE(moment_id, user_id)
);

CREATE INDEX idx_moment_visible_users_moment_id ON moment_visible_users(moment_id);
CREATE INDEX idx_moment_visible_users_user_id ON moment_visible_users(user_id);
