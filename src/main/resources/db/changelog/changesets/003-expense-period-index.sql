-- liquibase formatted sql

-- changeset kmozze:7
-- comment: replace user expense index with period query index
DROP INDEX IF EXISTS idx_expense_user_id;
CREATE INDEX idx_expense_user_id_created_at ON expense(user_id, created_at);
