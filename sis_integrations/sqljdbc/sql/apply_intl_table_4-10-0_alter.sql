-- Fixing issue found in 4.9.0 with international applications
ALTER TABLE SZRINTL ALTER COLUMN majorDescription NVARCHAR(100);