-- liquibase formatted sql

-- changeset kmozze:4 splitStatements:false
-- comment: generic function to update updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- changeset kmozze:5
-- comment: trigger for category table
CREATE TRIGGER set_updated_at_category
    BEFORE UPDATE ON category
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- changeset kmozze:6
-- comment: trigger for expense table
CREATE TRIGGER set_updated_at_expense
    BEFORE UPDATE ON expense
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
