ACCEPT owner CHAR PROMPT 'Adaptor user: ';

drop table "&owner".SZRPLMT
/
drop sequence "&owner".szrplmt_seq_no
/
drop public synonym SZRPLMT
/
drop public synonym SZRPLMT_SEQ_NO
/