ACCEPT owner CHAR PROMPT 'Adaptor user: ';
-- Placement View table.
Alter table "&owner".SZRPLMT add SZRPLMT_STATUS VARCHAR2(20)
/
Alter table "&owner".SZRPLMT modify SZRPLMT_DATA_SOURCE null
/
Alter table "&owner".SZRPLMT modify SZRPLMT_ENGLISH null
/
Alter table "&owner".SZRPLMT modify SZRPLMT_DATA_SOURCE null
/
