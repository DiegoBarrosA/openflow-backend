-- Migration script to add new columns for OpenFlow features
-- Run this on your Oracle database if hibernate.ddl-auto=update fails to create them

-- Add is_template column to boards table (for board templates feature)
ALTER TABLE boards ADD is_template NUMBER(1) DEFAULT 0;

-- Add show_in_card column to custom_field_definitions table (for visible fields feature)
ALTER TABLE custom_field_definitions ADD show_in_card NUMBER(1) DEFAULT 0;

-- Add assigned_user_id column to tasks table (for user assignment feature)
ALTER TABLE tasks ADD assigned_user_id NUMBER(19);

-- Create index for faster lookups on assigned user
CREATE INDEX idx_tasks_assigned_user ON tasks(assigned_user_id);

-- Commit the changes
COMMIT;

