CREATE TABLE health_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pet_id BIGINT NOT NULL,
    record_date DATE NOT NULL,
    appetite VARCHAR(16) NOT NULL,
    water_intake VARCHAR(16) NOT NULL,
    energy_level VARCHAR(16) NOT NULL,
    activity_level VARCHAR(16) NOT NULL,
    sleep_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    stool_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    urination_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    abnormal BOOLEAN NOT NULL DEFAULT FALSE,
    abnormal_description VARCHAR(500) NULL,
    remark VARCHAR(500) NULL,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted TINYINT NOT NULL DEFAULT 0,
    active_record_date DATE GENERATED ALWAYS AS (
        CASE WHEN deleted = 0 THEN record_date ELSE NULL END
    ),
    PRIMARY KEY (id),
    UNIQUE KEY uk_health_record_pet_active_date (pet_id, active_record_date),
    KEY idx_health_record_pet_date (pet_id, deleted, record_date, id),
    CONSTRAINT fk_health_record_pet FOREIGN KEY (pet_id) REFERENCES pet (id),
    CONSTRAINT chk_health_record_appetite CHECK (appetite IN ('EXCELLENT', 'NORMAL', 'REDUCED', 'POOR')),
    CONSTRAINT chk_health_record_water CHECK (water_intake IN ('INCREASED', 'NORMAL', 'REDUCED', 'UNKNOWN')),
    CONSTRAINT chk_health_record_energy CHECK (energy_level IN ('ACTIVE', 'NORMAL', 'LOW', 'VERY_LOW')),
    CONSTRAINT chk_health_record_activity CHECK (activity_level IN ('HIGH', 'NORMAL', 'LOW', 'VERY_LOW')),
    CONSTRAINT chk_health_record_sleep CHECK (sleep_status IN ('LESS', 'NORMAL', 'MORE', 'UNKNOWN')),
    CONSTRAINT chk_health_record_stool CHECK (stool_status IN ('NORMAL', 'SOFT', 'DIARRHEA', 'CONSTIPATED', 'UNKNOWN')),
    CONSTRAINT chk_health_record_urination CHECK (urination_status IN ('NORMAL', 'INCREASED', 'REDUCED', 'ABNORMAL', 'UNKNOWN')),
    CONSTRAINT chk_health_record_abnormal_description CHECK (
        abnormal = FALSE OR (abnormal_description IS NOT NULL AND LENGTH(TRIM(abnormal_description)) > 0)
    ),
    CONSTRAINT chk_health_record_deleted CHECK (deleted IN (0, 1))
);

CREATE TABLE weight_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pet_id BIGINT NOT NULL,
    weight DECIMAL(7, 3) NOT NULL,
    record_date DATE NOT NULL,
    remark VARCHAR(500) NULL,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_weight_record_pet_date (pet_id, deleted, record_date, update_time, id),
    CONSTRAINT fk_weight_record_pet FOREIGN KEY (pet_id) REFERENCES pet (id),
    CONSTRAINT chk_weight_record_weight CHECK (weight BETWEEN 0.001 AND 500.000),
    CONSTRAINT chk_weight_record_deleted CHECK (deleted IN (0, 1))
);

INSERT INTO weight_record (pet_id, weight, record_date, remark, deleted, create_time, update_time)
SELECT id, current_weight, DATE(create_time), NULL, 0, create_time, create_time
FROM pet
WHERE current_weight IS NOT NULL;
