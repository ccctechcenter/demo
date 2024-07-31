CREATE GLOBAL TEMPORARY TABLE sisTermStudentObjExt   (
    sisPersonId            VARCHAR2(9),
    SPRIDEN_PIDM           NUMBER,
    sisTermId              VARCHAR2(6),
    Field_Name            VARCHAR2(50),
    Field_ID              VARCHAR2(50),
    Field_Value           VARCHAR2(50)
) ON COMMIT PRESERVE ROWS;
