ACCEPT owner CHAR PROMPT 'Adaptor user: ';
drop sequence "&owner".szrplcd_seq_no;
drop sequence "&owner".szrplmt_seq_no;
drop table "&owner".SZRPLMC;
drop table "&owner".SZRPLMD;
drop table "&owner".SZRPLCD;
drop table "&owner".SZRPLMT;
drop public synonym SZRPLMT
/
drop public synonym SZRPLCD
/
drop public synonym SZRPLMD
/
drop public synonym SZRPLMC
/
drop public synonym SZRPLMT_SEQ_NO
/
drop public synonym SZRPLCD_SEQ_NO
/