ACCEPT owner CHAR PROMPT 'Adaptor user: ';
-- Placement View table.
Alter table "&owner".SZRPLMT ADD SZRPLMT_tstmp_ERP_Transmit DATE
/
Alter table "&owner".SZRPLMT ADD SZRPLMT_tstmp_SIS_Transmit  DATE
/
Alter table "&owner".SZRPLMT ADD SZRPLMT_sis_Processed_Flag VARCHAR2(1)
/
Alter table "&owner".SZRPLMT ADD SZRPLMT_tstmp_SIS_Processed DATE
/
Alter table "&owner".SZRPLMT ADD SZRPLMT_sis_Processed_Notes VARCHAR2(256)
/

