
CREATE SEQUENCE fraudSequence as BIGINT START WITH 1 INCREMENT BY 1;
CREATE TABLE SZRFRAD (
         sisFraudReportId BIGINT,
         misCode NVARCHAR(4),
         appId BIGINT,
         cccId NVARCHAR(12),
         fraudType NVARCHAR(20),
         reportedByMisCode NVARCHAR(4),
         tstmpSubmit DATETIME2,
         sisProcessedFlag NVARCHAR(1),
         sisTimestamp DATETIME2,
         sisProcessedNote NVARCHAR(100)
);
