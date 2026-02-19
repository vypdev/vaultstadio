-- VaultStadio Phase 6: Advanced Features Database Schema
-- Version: 2
-- Description: Creates tables for file versioning, sync, collaboration, and federation

-- ============================================================================
-- Phase 6: File Versioning Tables
-- ============================================================================

-- File versions table
CREATE TABLE file_versions (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL REFERENCES storage_items(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    size BIGINT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    created_by VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    comment TEXT,
    is_latest BOOLEAN NOT NULL DEFAULT FALSE,
    restored_from INTEGER
);

CREATE UNIQUE INDEX idx_file_versions_item_version ON file_versions(item_id, version_number);
CREATE INDEX idx_file_versions_item_latest ON file_versions(item_id, is_latest);
CREATE INDEX idx_file_versions_storage_key ON file_versions(storage_key);

-- ============================================================================
-- Phase 6: Sync Tables
-- ============================================================================

-- Sync devices table
CREATE TABLE sync_devices (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(255) NOT NULL,
    device_type VARCHAR(50) NOT NULL,
    last_sync_at TIMESTAMP WITH TIME ZONE,
    last_sync_cursor VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_sync_devices_user_device ON sync_devices(user_id, device_id);
CREATE INDEX idx_sync_devices_user_active ON sync_devices(user_id, is_active);

-- Sync changes table
CREATE TABLE sync_changes (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(36),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    cursor BIGINT NOT NULL,
    old_path VARCHAR(4096),
    new_path VARCHAR(4096),
    checksum VARCHAR(64),
    metadata TEXT
);

CREATE INDEX idx_sync_changes_user_cursor ON sync_changes(user_id, cursor);
CREATE INDEX idx_sync_changes_item ON sync_changes(item_id);
CREATE INDEX idx_sync_changes_timestamp ON sync_changes(timestamp);

-- Sync conflicts table
CREATE TABLE sync_conflicts (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL,
    local_change_id VARCHAR(36) NOT NULL REFERENCES sync_changes(id) ON DELETE CASCADE,
    remote_change_id VARCHAR(36) NOT NULL REFERENCES sync_changes(id) ON DELETE CASCADE,
    conflict_type VARCHAR(30) NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolution VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sync_conflicts_item ON sync_conflicts(item_id);
CREATE INDEX idx_sync_conflicts_resolved ON sync_conflicts(resolved_at);

-- ============================================================================
-- Phase 6: Collaboration Tables
-- ============================================================================

-- Collaboration sessions table
CREATE TABLE collaboration_sessions (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL REFERENCES storage_items(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_collaboration_sessions_item ON collaboration_sessions(item_id);
CREATE INDEX idx_collaboration_sessions_expires ON collaboration_sessions(expires_at);

-- Collaboration participants table
CREATE TABLE collaboration_participants (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL REFERENCES collaboration_sessions(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_name VARCHAR(100) NOT NULL,
    color VARCHAR(7) NOT NULL,
    cursor_line INTEGER,
    cursor_column INTEGER,
    cursor_offset INTEGER,
    selection_start_line INTEGER,
    selection_start_column INTEGER,
    selection_start_offset INTEGER,
    selection_end_line INTEGER,
    selection_end_column INTEGER,
    selection_end_offset INTEGER,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_editing BOOLEAN NOT NULL DEFAULT FALSE,
    left_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_collaboration_participants_session ON collaboration_participants(session_id);
CREATE INDEX idx_collaboration_participants_user ON collaboration_participants(user_id);

-- Document states table (for OT)
CREATE TABLE document_states (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL UNIQUE REFERENCES storage_items(id) ON DELETE CASCADE,
    version BIGINT NOT NULL DEFAULT 0,
    content TEXT NOT NULL DEFAULT '',
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Collaboration operations table (OT history)
CREATE TABLE collaboration_operations (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL REFERENCES storage_items(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation_type VARCHAR(20) NOT NULL,
    position INTEGER NOT NULL,
    text TEXT,
    length INTEGER,
    base_version BIGINT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_collaboration_operations_item_version ON collaboration_operations(item_id, base_version);

-- User presence table
CREATE TABLE user_presence (
    user_id VARCHAR(36) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    last_seen TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    active_session VARCHAR(36),
    active_document VARCHAR(36)
);

-- Document comments table
CREATE TABLE document_comments (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL REFERENCES storage_items(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    anchor_start_line INTEGER NOT NULL,
    anchor_start_column INTEGER NOT NULL,
    anchor_end_line INTEGER NOT NULL,
    anchor_end_column INTEGER NOT NULL,
    quoted_text TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_document_comments_item ON document_comments(item_id);
CREATE INDEX idx_document_comments_user ON document_comments(user_id);

-- Comment replies table
CREATE TABLE comment_replies (
    id VARCHAR(36) PRIMARY KEY,
    comment_id VARCHAR(36) NOT NULL REFERENCES document_comments(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comment_replies_comment ON comment_replies(comment_id);

-- ============================================================================
-- Phase 6: Federation Tables
-- ============================================================================

-- Federated instances table
CREATE TABLE federated_instances (
    id VARCHAR(36) PRIMARY KEY,
    domain VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    version VARCHAR(50) NOT NULL,
    public_key TEXT NOT NULL,
    capabilities TEXT NOT NULL, -- JSON array
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    last_seen_at TIMESTAMP WITH TIME ZONE,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    metadata TEXT -- JSON object
);

CREATE INDEX idx_federated_instances_status ON federated_instances(status);

-- Federated shares table
CREATE TABLE federated_shares (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL REFERENCES storage_items(id) ON DELETE CASCADE,
    source_instance VARCHAR(255) NOT NULL,
    target_instance VARCHAR(255) NOT NULL,
    target_user_id VARCHAR(100),
    permissions TEXT NOT NULL, -- JSON array
    expires_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    accepted_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_federated_shares_item ON federated_shares(item_id);
CREATE INDEX idx_federated_shares_created_by ON federated_shares(created_by);
CREATE INDEX idx_federated_shares_target ON federated_shares(target_instance);
CREATE INDEX idx_federated_shares_status ON federated_shares(status);

-- Federated identities table
CREATE TABLE federated_identities (
    id VARCHAR(36) PRIMARY KEY,
    local_user_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE,
    remote_user_id VARCHAR(100) NOT NULL,
    remote_instance VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    avatar_url VARCHAR(500),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_federated_identities_remote ON federated_identities(remote_user_id, remote_instance);
CREATE INDEX idx_federated_identities_local ON federated_identities(local_user_id);

-- Federated activities table
CREATE TABLE federated_activities (
    id VARCHAR(36) PRIMARY KEY,
    instance_domain VARCHAR(255) NOT NULL,
    activity_type VARCHAR(30) NOT NULL,
    actor_id VARCHAR(255) NOT NULL,
    object_id VARCHAR(255) NOT NULL,
    object_type VARCHAR(50) NOT NULL,
    summary TEXT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    metadata TEXT
);

CREATE INDEX idx_federated_activities_instance ON federated_activities(instance_domain);
CREATE INDEX idx_federated_activities_timestamp ON federated_activities(timestamp);
