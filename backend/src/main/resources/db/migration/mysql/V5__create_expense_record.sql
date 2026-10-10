CREATE TABLE expense_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    pet_id BIGINT NULL,
    category VARCHAR(24) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    expense_date DATE NOT NULL,
    merchant VARCHAR(160) NULL,
    description VARCHAR(500) NULL,
    remark VARCHAR(500) NULL,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_expense_user_date (user_id, deleted, expense_date, id),
    KEY idx_expense_user_category_date (user_id, deleted, category, expense_date),
    KEY idx_expense_user_pet_date (user_id, deleted, pet_id, expense_date),
    CONSTRAINT fk_expense_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_expense_pet FOREIGN KEY (pet_id) REFERENCES pet (id),
    CONSTRAINT chk_expense_category CHECK (
        category IN ('FOOD', 'MEDICAL', 'VACCINE', 'DEWORMING', 'CLEANING', 'TOY', 'DAILY_SUPPLIES', 'OTHER')
    ),
    CONSTRAINT chk_expense_amount CHECK (amount > 0.00 AND amount <= 999999.99),
    CONSTRAINT chk_expense_deleted CHECK (deleted IN (0, 1))
);
