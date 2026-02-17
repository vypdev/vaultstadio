-- Add shared_with_users column to share_links table for direct user sharing

ALTER TABLE share_links ADD COLUMN shared_with_users TEXT DEFAULT '';

-- Create index for efficient lookups of shares by recipient
CREATE INDEX idx_share_links_shared_with ON share_links(shared_with_users);
