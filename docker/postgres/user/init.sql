-- noinspection SqlNoDataSourceInspectionForFile
CREATE TABLE external_user(
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id UUID NOT NULL UNIQUE,
      username varchar(255) NOT NULL UNIQUE,
      last_activity_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_follow(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    followed_user_id UUID NOT NULL,
    following_user_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    FOREIGN KEY (followed_user_id) REFERENCES external_user(id) ON DELETE CASCADE,
    FOREIGN KEY (following_user_id) REFERENCES external_user(id) ON DELETE CASCADE,
    CONSTRAINT uc_user_follow UNIQUE (followed_user_id, following_user_id),
    CONSTRAINT self_follow CHECK (followed_user_id != following_user_id)
);

CREATE INDEX idx_user_followed_user_id ON user_follow(followed_user_id);
CREATE INDEX idx_user_following_user_id ON user_follow(following_user_id);