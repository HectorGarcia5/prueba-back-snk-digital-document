ALTER TABLE digital_document
    DROP COLUMN IF EXISTS emp_employee_id,
    DROP COLUMN IF EXISTS emp_managed_group_id,
    ADD COLUMN IF NOT EXISTS employee_data TEXT;
