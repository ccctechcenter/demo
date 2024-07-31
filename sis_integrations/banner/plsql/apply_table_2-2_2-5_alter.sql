ACCEPT owner CHAR PROMPT 'Adaptor user: ';
declare
  column_exists exception;
  pragma exception_init (column_exists , -01430);
begin
  execute immediate 'ALTER TABLE "&owner".SZRAPLY ADD (  RACE_ETHNIC_FULL VARCHAR2(1000 BYTE) )';
  exception when column_exists then null;
end;
/