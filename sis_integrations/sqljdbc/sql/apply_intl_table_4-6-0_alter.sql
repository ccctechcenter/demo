ALTER TABLE SZRINTL ADD acceptedTerms NVARCHAR(1);
ALTER TABLE SZRINTL ADD acceptedTermsTimestamp DATETIME2;
ALTER TABLE SZRINTL ADD ipAddressAtAccountCreation NVARCHAR(40);
ALTER TABLE SZRINTL ADD ipAddressAtAppCreation NVARCHAR(40);
ALTER TABLE SZRINTL ADD phoneType NVARCHAR(10);