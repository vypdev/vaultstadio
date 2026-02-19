-- VaultStadio Initial Database Schema
-- Version: 1
-- Description: Creates all initial tables for VaultStadio

-- Users table
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    quota_bytes BIGINT,
    avatar_url VARCHAR(500),
    preferences TEXT,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE UNIQUE INDEX idx_users_username ON users(username);

-- User sessions table
CREATE TABLE user_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_activity_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_sessions_token_hash ON user_sessions(token_hash);
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);

-- API keys table
CREATE TABLE api_keys (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    permissions TEXT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_api_keys_key_hash ON api_keys(key_hash);
CREATE INDEX idx_api_keys_user_id ON api_keys(user_id);

-- Storage items table
CREATE TABLE storage_items (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    path VARCHAR(4096) NOT NULL,
    type VARCHAR(10) NOT NULL,
    parent_id VARCHAR(36),
    owner_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(255),
    checksum VARCHAR(64),
    storage_key VARCHAR(500),
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    is_trashed BOOLEAN NOT NULL DEFAULT FALSE,
    is_starred BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    trashed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 1
);

-- Self-referential foreign key for parent
ALTER TABLE storage_items ADD CONSTRAINT fk_storage_items_parent 
    FOREIGN KEY (parent_id) REFERENCES storage_items(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_storage_items_owner_path ON storage_items(owner_id, path);
CREATE INDEX idx_storage_items_parent_id ON storage_items(parent_id);
CREATE INDEX idx_storage_items_owner_trashed ON storage_items(owner_id, is_trashed);
CREATE INDEX idx_storage_items_owner_starred ON storage_items(owner_id, is_starred);
CREATE INDEX idx_storage_items_owner_type ON storage_items(owner_id, type);

-- Storage item metadata table
CREATE TABLE storage_item_metadata (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL REFERENCES storage_items(id) ON DELETE CASCADE,
    plugin_id VARCHAR(100) NOT NULL,
    key VARCHAR(100) NOT NULL,
    value TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_storage_item_metadata_unique ON storage_item_metadata(item_id, plugin_id, key);
CREATE INDEX idx_storage_item_metadata_plugin ON storage_item_metadata(plugin_id);
CREATE INDEX idx_storage_item_metadata_item ON storage_item_metadata(item_id);

-- Share links table
CREATE TABLE share_links (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL REFERENCES storage_items(id) ON DELETE CASCADE,
    token VARCHAR(64) NOT NULL,
    created_by VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE,
    password VARCHAR(255),
    max_downloads INTEGER,
    download_count INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_share_links_token ON share_links(token);
CREATE INDEX idx_share_links_item_id ON share_links(item_id);
CREATE INDEX idx_share_links_created_by ON share_links(created_by);

-- Activities table
CREATE TABLE activities (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    item_id VARCHAR(36),
    item_path VARCHAR(4096),
    details TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activities_user_id ON activities(user_id);
CREATE INDEX idx_activities_item_id ON activities(item_id);
CREATE INDEX idx_activities_created_at ON activities(created_at);
CREATE INDEX idx_activities_type ON activities(type);
