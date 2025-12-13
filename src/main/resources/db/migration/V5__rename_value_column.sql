-- Rename 'value' column to 'field_value' in custom_field_values table
-- 'value' is a reserved word in Oracle and causes issues with Hibernate

-- Check if the old column exists and rename it
DECLARE
    v_count NUMBER;
BEGIN
    -- Check if 'value' column exists (the old name)
    SELECT COUNT(*) INTO v_count 
    FROM user_tab_columns 
    WHERE table_name = 'CUSTOM_FIELD_VALUES' 
    AND column_name = 'VALUE';
    
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE custom_field_values RENAME COLUMN "VALUE" TO field_value';
    END IF;
    
    -- Check if column doesn't exist at all (new installation)
    SELECT COUNT(*) INTO v_count 
    FROM user_tab_columns 
    WHERE table_name = 'CUSTOM_FIELD_VALUES' 
    AND column_name = 'FIELD_VALUE';
    
    IF v_count = 0 THEN
        -- Column doesn't exist, add it
        EXECUTE IMMEDIATE 'ALTER TABLE custom_field_values ADD field_value VARCHAR2(1000)';
    END IF;
END;
/

