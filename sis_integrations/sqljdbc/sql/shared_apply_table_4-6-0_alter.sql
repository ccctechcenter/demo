ALTER TABLE SZRSAPP ADD acceptedTerms NVARCHAR(1);
ALTER TABLE SZRSAPP ADD acceptedTermsTimestamp DATETIME2;
ALTER TABLE SZRSAPP ADD ipAddressAtAccountCreation NVARCHAR(40);
ALTER TABLE SZRSAPP ADD ipAddressAtAppCreation NVARCHAR(40);
ALTER TABLE SZRSAPP ADD phoneType NVARCHAR(10);
ALTER TABLE SZRSAPP ADD mailingAddressValidationOverride NVARCHAR(1);
ALTER TABLE SZRSAPP ADD fraudScore NUMERIC(18,5);
ALTER TABLE SZRSAPP ADD fraudStatus INT;
ALTER TABLE SZRSAPP ADD studentParent NVARCHAR(1);