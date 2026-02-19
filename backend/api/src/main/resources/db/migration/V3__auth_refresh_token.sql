-- VaultStadio Auth Refresh Token Migration
-- Version: 3
-- Description: Adds refresh token support to user sessions

-- Add refresh token hash column to user_sessions
ALTER TABLE user_sessions ADD COLUMN refresh_token_hash VARCHAR(64);

-- Create index for refresh token lookups
CREATE INDEX idx_user_sessions_refresh_token ON user_sessions(refresh_token_hash);

-- Add comment for documentation
COMMENT ON COLUMN user_sessions.refresh_token_hash IS 'SHA-256 hash of the refresh token for token rotation';
