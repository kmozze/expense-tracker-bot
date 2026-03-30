-- liquibase formatted sql

-- changeset kmozze:1
-- comment: creating an expense category table

CREATE TABLE category (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    chat_id    BIGINT       NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_category_per_user UNIQUE (chat_id, name)
);

-- changeset kmozze:2
-- comment: creating an expense table
CREATE TABLE expense (
    id          UUID PRIMARY KEY,
    amount      DECIMAL(19, 2) NOT NULL,
    category_id UUID           NOT NULL,
    chat_id     BIGINT         NOT NULL,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_category FOREIGN KEY (category_id)
        REFERENCES category (id) ON DELETE RESTRICT
);

-- changeset kmozze:3
-- comment: indexes
CREATE INDEX idx_category_chat_id ON category(chat_id);
CREATE INDEX idx_expense_chat_id ON expense(chat_id);
CREATE INDEX idx_expense_category_id ON expense(category_id);
