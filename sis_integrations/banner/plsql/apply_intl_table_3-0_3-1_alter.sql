ACCEPT owner CHAR PROMPT 'Adaptor user: ';
alter table "&owner".SZRINTL add (
                                  	"RESIDE_IN_US" VARCHAR2(1 BYTE));
  /