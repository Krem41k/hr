ALTER TABLE template_field
    ADD COLUMN IF NOT EXISTS field_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS label VARCHAR(255),
    ADD COLUMN IF NOT EXISTS value_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS value_source VARCHAR(32);

UPDATE template_field
SET field_code = CASE type
                     WHEN 'FULL_NAME' THEN 'fullName'
                     WHEN 'BIRTH_DATE' THEN 'birthDate'
                     WHEN 'CURRENT_DATE' THEN 'currentDate'
                     ELSE lower(type)
    END,
    label = CASE type
                WHEN 'FULL_NAME' THEN 'ФИО'
                WHEN 'BIRTH_DATE' THEN 'Дата рождения'
                WHEN 'CURRENT_DATE' THEN 'Текущая дата'
                ELSE type
        END,
    value_type = CASE type
                     WHEN 'FULL_NAME' THEN 'TEXT'
                     ELSE 'DATE'
        END,
    value_source = CASE type
                       WHEN 'CURRENT_DATE' THEN 'SYSTEM'
                       ELSE 'USER'
        END
WHERE field_code IS NULL;

ALTER TABLE template_field
    ALTER COLUMN field_code SET NOT NULL,
    ALTER COLUMN value_type SET NOT NULL,
    ALTER COLUMN value_source SET NOT NULL;

ALTER TABLE template_field
    DROP COLUMN IF EXISTS type;
