-- noinspection SqlNoDataSourceInspectionForFile

CREATE TABLE external_user(
    id UUID PRIMARY KEY,
    username varchar(255) NOT NULL UNIQUE
);

CREATE TABLE content(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id UUID NOT NULL,
    parent_id UUID default NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    text VARCHAR(255) NOT NULL,
    media_urls JSONB default NULL,
    deleted_at TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES content(id) ON DELETE CASCADE ,
    FOREIGN KEY (creator_id) REFERENCES external_user(id) ON DELETE CASCADE
);

CREATE TABLE user_content_like(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    content_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES external_user(id) ON DELETE CASCADE,
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    CONSTRAINT uc_user_content_like UNIQUE (user_id, content_id)
);

CREATE TABLE user_content_favorite(
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  content_id UUID NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (user_id) REFERENCES external_user(id) ON DELETE CASCADE,
  FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
  CONSTRAINT uc_user_content_favorite UNIQUE (user_id, content_id)
);

CREATE INDEX idx_content_creator ON content(creator_id);
CREATE INDEX idx_comments ON content(parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_posts ON content(parent_id) WHERE parent_id IS NULL;
CREATE INDEX idx_user_content_like ON user_content_like(user_id, content_id);
CREATE INDEX idx_user_content_favorite ON user_content_favorite(user_id, content_id);
