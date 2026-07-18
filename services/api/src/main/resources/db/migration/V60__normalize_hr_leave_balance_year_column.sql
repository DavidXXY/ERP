ALTER TABLE hr_leave_balances ADD COLUMN IF NOT EXISTS leave_year INTEGER;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'hr_leave_balances'
      AND column_name = 'year'
  ) THEN
    UPDATE hr_leave_balances
    SET leave_year = COALESCE(leave_year, year, EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER)
    WHERE leave_year IS NULL;
  ELSE
    UPDATE hr_leave_balances
    SET leave_year = COALESCE(leave_year, EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER)
    WHERE leave_year IS NULL;
  END IF;
END $$;

ALTER TABLE hr_leave_balances ALTER COLUMN leave_year SET NOT NULL;

ALTER TABLE hr_leave_balances DROP COLUMN IF EXISTS year;
