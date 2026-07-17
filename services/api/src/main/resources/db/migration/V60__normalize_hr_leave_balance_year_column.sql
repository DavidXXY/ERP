ALTER TABLE hr_leave_balances ADD COLUMN IF NOT EXISTS leave_year INTEGER;

UPDATE hr_leave_balances
SET leave_year = COALESCE(leave_year, year, EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER)
WHERE leave_year IS NULL;

ALTER TABLE hr_leave_balances ALTER COLUMN leave_year SET NOT NULL;

ALTER TABLE hr_leave_balances DROP COLUMN IF EXISTS year;
