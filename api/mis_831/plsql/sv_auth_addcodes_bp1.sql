create or replace PACKAGE BODY SV_AUTH_ADDCODES_BP AS
-- Solution Centre Baseline
--AUDIT_TRAIL_MSGKEY_UPDATE
-- PROJECT : MSGKEY
-- MODULE  : SVKB_AUTH_ADDCODES_BP1
-- SOURCE  : enUS
-- TARGET  : I18N
-- DATE    : Tue Mar 12 12:27:46 2013
-- MSGSIGN : #d22f9fbc02d9dcbc
--TMI18N.ETR DO NOT CHANGE--
--
-- FILE NAME..: svkb_auth_addcodes_bp1.sql
-- RELEASE....: 8.7 C3SC
-- OBJECT NAME: SV_AUTH_ADDCODES_BP
-- PRODUCT....: STUDENT C3SC
-- USAGE......: Package contains support functions for stvrate
-- COPYRIGHT..: Copyright 2012 - 2013 Ellucian Company L.P. and its affiliates.
--
-- DESCRIPTION:
--
-- Standard functions and procedures for the STVRATE validation table.
-- Table Comments : Student Fee Assessment Rate Validation Table
--
--  Cursors: none
--
--  Functions:
--     F_IS_VALID_RSTS_CODE
--       Returns true if RSTS code can be used to override registration checks
--       sing Registration Authorization Add Codes
--
--     F_GET_HEADER_DATA
--       Obtain the important header information in one query
--
--     F_GET_COURSE_AUTH_STATUS
--       Return registration status for course overrides given Term & PIDM
--
--     F_IS_AUTHORIZED
--       returns true if valid authorization is found for given term, id  and crn
--       if false, includes p_reason_out to indicate why authorization failed
--
--     F_IS_AUTH_ENABLED
--       Returns true if authorization is enabled for given term
--
--     F_IS_AUTH_REQUIRED
--       Returns true if an authorization add code is required for a give baseline message
--
--     F_CRN_STARTED
--       Returns true if sysdate is greater or equal to the section start date + days required
--       value stored in gtvsdax for term.  If true authorization code is required for section
--
--     F_CENSUS_DATE_PAST
--       Returns true if CRN's census date is today or prior
--
--     F_EXISTS_ACTIVE
--       Returns true if the auth code exists and it's active
--
--     F_AUTH_REQ_MSG
--       Modify old SFAREGS style message to new SVAREGS style
--     F_CALL_AUTH_PAGE
--      Returns True if there any CRN with the need of entering Authorization Code
--
--     F_CHECK_CLOSED_SECTION
--     Returns true if closed section validation needs to be done.
--
--     F_GET_START_DATE
--     Returns first day a section meets
--
--     F_CHECK_CRN_STARTED
--     Returns true if Started class  validation needs to be done.
--
--    F_CHECK_CENSUS_DATE
--     Returns true if Census Date validation needs to be done.
--
--    F_set_capacity_value
--    Returns N for no capacity check , otherwise sobterm capacity ind
--
--  Procedures:
--     P_COURSE_AUTH_STATUS
--       Find registration status for course overrides given Term & PIDM
--
--    P_AUTH_MSG_FLAG_VALUES
--     Finds the Corresponding Auth Error  message and Flag Error values.
--
--    P_MSG_FLAG_VALUES
--    Gets auth message and flag error for closed section or class started validations
--
--   P_CALL_AUTH_PAGE
--   calls the authorization add registration page
--
--   P_REST_MESSAGE
--   Assign Null to the error message if error flag = O

--
--- DESCRIPTION END

/**------------------------------------------------------------------------------------**/


/**
*  returns true if RSTS and term code can be used to override registration checks
*  using Registration Authorization Add Codes
* @param p_rsts_code  Registration Code
* @param p_term Term Code
*/
   global_pidm             SPRIDEN.SPRIDEN_PIDM%TYPE;
   genpidm                 SPRIDEN.SPRIDEN_PIDM%TYPE;
   row_count               NUMBER;
   stvterm_rec             stvterm%ROWTYPE;
   sorrtrm_rec             sorrtrm%ROWTYPE;
   sfbetrm_rec             sfbetrm%ROWTYPE;
   sftregs_rec             sftregs%ROWTYPE;
   scbcrse_row             scbcrse%ROWTYPE;
   sfrbtch_row             sfrbtch%ROWTYPE;
   tbrcbrq_row             tbrcbrq%ROWTYPE;
   menu_name               VARCHAR2 (60);
   ssbsect_row             ssbsect%ROWTYPE;
   stvrsts_row             stvrsts%ROWTYPE;
   sobterm_row             sobterm%ROWTYPE;
   sql_error               NUMBER;
   term_desc               stvterm%ROWTYPE;
   sfbetrm_row             sfbetrm%ROWTYPE;
   sgrclsr_clas_code       stvclas.stvclas_code%TYPE;
   sgbstdn_row             sgbstdn%ROWTYPE;
   atmp_hrs                VARCHAR2 (1);
   clas_desc               stvclas.stvclas_desc%TYPE;




  FUNCTION F_process_with_auth(p_term_code svbauth.svbauth_term_code%TYPE,
                               p_rsts_code SFRSTCR.SFRSTCR_RSTS_CODE%TYPE)
  RETURN BOOLEAN IS

  BEGIN
      IF  sv_auth_addcodes_bp.f_is_auth_enabled(p_term_code  => p_term_code)
      AND  sv_auth_addcodes_bp.f_is_valid_rsts_code (p_rsts_code  => p_rsts_code)
      THEN
     RETURN TRUE;
       ELSE
         RETURN FALSE;
       END IF;
   END F_process_with_auth;

/**-----------------------------------------------------------------------------------**/

  FUNCTION f_is_valid_rsts_code(
      p_rsts_code  SFRSTCR.SFRSTCR_RSTS_CODE%TYPE)
    RETURN BOOLEAN IS
   BEGIN
      FOR gtvsdax_rec IN c_get_gtvsdax
                           (p_internal_code_group  => CALREG,
                            p_internal_code        => AUTH_RSTS)

      LOOP
      -- only return true if a external code was found gtvsdax
        IF gtvsdax_rec.gtvsdax_external_code = p_rsts_code   THEN
    RETURN TRUE;
        END IF;
      END LOOP;
      RETURN FALSE;
   END f_is_valid_rsts_code;

/**------------------------------------------------------------------------------------**/
--
  FUNCTION f_get_header_data(
    p_term_code     svbauth.svbauth_term_code%TYPE,
    p_crn           svbauth.svbauth_crn%TYPE)
    RETURN header_data_ref IS
--
    header_data_rc  header_data_ref;
    BEGIN
      OPEN header_data_rc FOR
           SELECT SSBSECT_TERM_CODE,
                  SSBSECT_CRN,
                  SSBSECT_PTRM_CODE,
                  SSBSECT_CAMP_CODE,
                  SSBSECT_CRSE_NUMB,
                  SSBSECT_SUBJ_CODE,
                  SSBSECT_SEQ_NUMB,
                  DECODE(SIRASGN_TERM_CODE, NULL, '',
                      SUBSTR(LTRIM(RTRIM(SPRIDEN_LAST_NAME)) ||
                      ', '||LTRIM(RTRIM(SPRIDEN_FIRST_NAME)) ||
                      ' ' ||LTRIM(RTRIM(SPRIDEN_MI)), 1, 45)),
                  SSBSECT_SEATS_AVAIL,
                  SSBSECT_WAIT_COUNT,
                  NVL(SSBSECT_PTRM_START_DATE,SSBSECT_LEARNER_REGSTART_FDATE),
                  SSBSECT_CENSUS_ENRL_DATE,
                  SSBSECT_REG_AUTH_ACTIVE_IND
             FROM SSBSECT
                  LEFT OUTER JOIN
                     (SELECT SIRASGN_TERM_CODE,
                             SIRASGN_CRN,
                             SIRASGN_PIDM
                        FROM SIRASGN
                       WHERE SIRASGN_PRIMARY_IND = 'Y')
               ON SSBSECT_TERM_CODE = SIRASGN_TERM_CODE
              AND SSBSECT_CRN = SIRASGN_CRN
                  LEFT OUTER JOIN SPRIDEN
               ON SIRASGN_PIDM = SPRIDEN_PIDM
            WHERE SSBSECT_TERM_CODE = p_term_code
              AND SSBSECT_CRN       = p_crn
              AND SPRIDEN_CHANGE_IND IS NULL;             -- C3SC JL ADDED Defect 1-O43S1I
--
      RETURN header_data_rc;
    END f_get_header_data;

/**------------------------------------------------------------------------------------**/

  FUNCTION f_get_course_auth_status(
      p_term_code  svbauth.svbauth_term_code%TYPE,
      p_crn        svbauth.svbauth_crn%TYPE DEFAULT NULL,
      p_pidm       svbauth.svbauth_pidm%TYPE)
    RETURN course_auth_status_ref
  IS
  --
    lv_course_auth_status_c course_auth_status_ref;
  BEGIN
    -- return values displayed SVAAUTH from
    OPEN lv_course_auth_status_c FOR
        SELECT sftregs_term_code,
           sftregs_crn,
           sftregs_rsts_code,
           sftregs_rsts_date,
           svbauth_auth_code,
           nvl(sftregs_sect_crse_numb, ssbsect_crse_numb) sftregs_sect_crse_numb,
           nvl(sftregs_sect_subj_code, ssbsect_subj_code) sftregs_sect_subj_code,
           ssbsect_seq_numb,
           svbauth_assign_date,
           DECODE(sirasgn_term_code, NULL, '',
                      SUBSTR(LTRIM(RTRIM(spriden_last_name)) ||
                      ', '||LTRIM(RTRIM(spriden_first_name)) ||
                      ' ' ||LTRIM(RTRIM(spriden_mi)), 1, 45))   "r_instructor"
        FROM sftregs
             LEFT OUTER JOIN svbauth
                   ON   sftregs_term_code       = svbauth_term_code
                  AND   sftregs_crn             = svbauth_crn
                  AND   sftregs_pidm            = svbauth_pidm
                  AND   svbauth_active_ind      = 'Y'
             LEFT OUTER JOIN sirasgn
                   ON   sftregs_term_code = sirasgn_term_code
                  AND   sftregs_crn       = sirasgn_crn
                  AND SIRASGN_PRIMARY_IND = 'Y'
             LEFT OUTER JOIN spriden
                   ON   sirasgn_pidm = spriden_pidm
                  AND     spriden_change_ind IS NULL  ,
             ssbsect
     WHERE  sftregs_term_code = p_term_code
       AND  sftregs_pidm      = p_pidm
       AND  sftregs_term_code = ssbsect_term_code
       AND  sftregs_crn       = ssbsect_crn
       AND  sftregs_crn       LIKE NVL(p_crn, '%')
     ORDER
        BY  sftregs_add_date, sftregs_crn, sirasgn_primary_ind ASC;
--
    RETURN lv_course_auth_status_c;
  END f_get_course_auth_status;

/**------------------------------------------------------------------------------------**/

  FUNCTION f_is_valid_student(
      p_term_code   svbauth.svbauth_term_code%TYPE,
      p_pidm        svbauth.svbauth_pidm%TYPE)
  RETURN BOOLEAN IS
     CURSOR PTI_CURSOR IS
        SELECT ROWID
          FROM SGBSTDN
         WHERE SGBSTDN_TERM_CODE_EFF =  (
                 SELECT MAX(SGBSTDN_TERM_CODE_EFF)
                   FROM SGBSTDN
                  WHERE SGBSTDN_TERM_CODE_EFF <= p_term_code
                    AND SGBSTDN_PIDM = p_pidm )
           AND SGBSTDN_PIDM = p_pidm;
      lv_rowid  VARCHAR(18);
  BEGIN
     OPEN PTI_CURSOR ;
     FETCH PTI_CURSOR INTO lv_rowid;
     RETURN PTI_CURSOR%FOUND;
  END f_is_valid_student;

/**------------------------------------------------------------------------------------**/

/**  Criteria for valid authorization
  -- row found
  -- auth code is not null
  -- row is active
  -- if first_assigned date is not null, days reusasble is not exceeeded
**/
  FUNCTION f_is_authorized(
      p_term_code    svbauth.svbauth_term_code%TYPE,
      p_crn          svbauth.svbauth_crn%TYPE,
      p_pidm         spriden.spriden_pidm%TYPE,
      p_rsts_date    DATE,
      p_reason_out   OUT VARCHAR2)
  RETURN BOOLEAN IS
    lv_days_reuse            NUMBER   := DAYS_REUSE;
    lv_days_gone_past         NUMBER;
    lv_svbauth_rec           svbauth%ROWTYPE;

    -- C3SC Added [JR] - 08/09/2006
    CURSOR get_days_reusable_c ( term_in sobterm.sobterm_term_code%TYPE )
    IS
      SELECT sobterm_days_reusable
        FROM sobterm
       WHERE sobterm_term_code = term_in;

  BEGIN
    -- find days reusable days for given term
    /*
    FOR lv_gtvsdax_rec IN c_get_gtvsdax
                           (p_internal_code_group    => CALREG,
                            p_internal_code          => AUTHREUSE)

    LOOP
      IF lv_gtvsdax_rec.gtvsdax_external_code = p_term_code THEN
        lv_days_reuse  := TO_NUMBER(lv_gtvsdax_rec.gtvsdax_translation_code);
      END IF;
    END LOOP;
    */

    OPEN get_days_reusable_c(p_term_code);
    FETCH get_days_reusable_c
     INTO lv_days_reuse;
    CLOSE get_days_reusable_c;
    -- C3SC END

    -- find auth code row for given term, crn and pidm
    OPEN c_get_authorized
             (p_term_code  => p_term_code,
              p_crn        => p_crn,
              p_pidm       => p_pidm);
    FETCH c_get_authorized INTO lv_svbauth_rec;

    -- return false if now row found
    IF c_get_authorized%NOTFOUND THEN
      CLOSE c_get_authorized;
      p_reason_out := AUTHNOTFOUND;
      RETURN FALSE;
    END IF;
    CLOSE c_get_authorized;

    -- return false if reuse days exceed days allowed for reuse
    lv_days_gone_past  := TRUNC(NVL(p_rsts_date, sysdate)) - TRUNC(NVL(lv_svbauth_rec.svbauth_assign_date, sysdate));
    IF lv_days_gone_past >= lv_days_reuse THEN  --Internal Defect:1720 STU16 GRS 04/04/2006
      p_reason_out := AUTHEXPIRED;
      RETURN FALSE;
    END IF;

    -- all criteria have been met. auth code is valid for given pidm
    RETURN TRUE;
  END f_is_authorized;

/**------------------------------------------------------------------------------------**/

  FUNCTION f_is_auth_enabled(
      p_term_code  svbauth.svbauth_term_code%TYPE)
  RETURN BOOLEAN IS
--
    -- C3SC Added [JR] - 08/09/2006
    CURSOR get_sobterm_reg_auth_c ( term_in  sobterm.sobterm_term_code%TYPE )
    IS
      SELECT sobterm_reg_auth_active_ind
        FROM sobterm
       WHERE sobterm_term_code = term_in;

    lv_RegAuthActiveInd  sobterm.sobterm_reg_auth_active_ind%TYPE;
--
  BEGIN
    -- find auth enabled for term
    OPEN get_sobterm_reg_auth_c(p_term_code);
    FETCH get_sobterm_reg_auth_c
     INTO lv_RegAuthActiveInd;
    CLOSE get_sobterm_reg_auth_c;

    IF lv_RegAuthActiveInd = 'N' THEN
      RETURN FALSE;
    ELSE
      RETURN TRUE;
    END IF;
    -- C3SC END

  END f_is_auth_enabled;

/**------------------------------------------------------------------------------------**/

 FUNCTION f_is_auth_required(
      p_rmsg_cde  sftregs.sftregs_rmsg_cde%TYPE,
      p_orig_msg  SFTREGS.SFTREGS_MESSAGE%TYPE
      )
  RETURN BOOLEAN IS
  BEGIN
    IF (p_rmsg_cde = 'CLOS' OR
        p_rmsg_cde = 'RESC'                                -- Defect 1-E8O563 GRS 14/01/2011 ADDED
     OR INSTR(UPPER(p_orig_msg),G$_NLS.Get('SVKB_AUTH_ADDCODES_BP1-0000','SQL','CLOSED')) <> 0     -- Defect 1-KGBJGX  GRS 02/26/2013
       )
 THEN
    RETURN TRUE;
  ELSE
    RETURN FALSE;
 END IF;
  END f_is_auth_required;

/**------------------------------------------------------------------------------------**/

 FUNCTION f_check_closed_section(p_auth_over_closed VARCHAR2,
                                  p_rmsg_cde  sftregs.sftregs_rmsg_cde%TYPE,   --C3SC GRS 03/12/09 ADD
                                  p_orig_msg   SFTREGS.SFTREGS_MESSAGE%TYPE
                                 )
  RETURN BOOLEAN
  IS
  BEGIN
    IF NVL(p_auth_over_closed, 'N') <> 'Y'
             AND sv_auth_addcodes_bp.f_is_auth_required(p_rmsg_cde, --C3SC GRS 03/12/09 ADD
                                                        p_orig_msg)
    THEN
      RETURN TRUE;
    ELSE
      RETURN FALSE;
    END IF;
 END f_check_closed_section;

/**------------------------------------------------------------------------------------**/

-- will return first day a section meets
FUNCTION f_get_start_date(
      p_term_code  ssrmeet.ssrmeet_term_code%TYPE,
      p_crn        ssrmeet.ssrmeet_crn%TYPE)
   RETURN DATE IS

    CURSOR lv_ssrmeet_c IS
       SELECT *
         FROM ssrmeet
        WHERE ssrmeet_term_code = p_term_code
    AND ssrmeet_crn       = p_crn;
    lv_start_date  DATE;
    lv_meet_count  INTEGER := 0;
    lv_section_rec           sb_section.section_rec;
    lv_section_cursor        sb_section.section_ref;

    lv_ssrmeet_rowid         ROWID;
    lv_notfound_out          BOOLEAN := FALSE;


  BEGIN
  -- C3SC Defect: 1-2V556L Mod  [JR 01/09/2007]
    lv_section_cursor := sb_section.f_query_one ( p_term_code => p_term_code,  p_crn => p_crn);
    FETCH lv_section_cursor INTO lv_section_rec;

--
    lv_ssrmeet_rowid := sv_acad_calendar_bp.f_get_first_ssrmeetrow(
                          p_term_code     =>  p_term_code,
                          p_crn           =>  p_crn,
                          p_ptrm_code     =>  lv_section_rec.r_ptrm_code,
                          p_notfound_out  =>  lv_notfound_out
                        );

    IF NOT lv_notfound_out OR lv_notfound_out IS NULL THEN
      lv_start_date := sv_acad_calendar_bp.f_ssrmeetrow_startdate(
                         p_rowid      =>  lv_ssrmeet_rowid,
                         p_ptrm_code  =>  lv_section_rec.r_ptrm_code
                       );

      IF lv_start_date IS NULL   AND
         lv_section_cursor%FOUND AND
         lv_section_rec.r_ptrm_start_date IS NOT NULL THEN
        lv_start_date  := lv_section_rec.r_ptrm_start_date;
      END IF;

      -- C3SC SC_Defect: 1-3GBGMX ADDED  JRE 03/14/2008
      IF lv_section_rec.r_ptrm_start_date IS NULL THEN
        lv_start_date  := null;
      END IF;
      -- C3SC SC_Defect: 1-3GBGMX ENDS

    --look at ssbsect if no ssrmeet records found
    ELSE
    --IF lv_meet_count = 0 THEN
      -- ssbsect
    -- C3SC Defect: 1-2V556L Ends

      IF lv_section_cursor%FOUND THEN
        IF lv_section_rec.r_ptrm_start_date IS NOT NULL THEN
          lv_start_date  := lv_section_rec.r_ptrm_start_date;
        END IF;
      END IF;

    END IF; -- end start date

    CLOSE lv_section_cursor;
    RETURN lv_start_date;

  END f_get_start_date;

/**------------------------------------------------------------------------------------**/

 FUNCTION f_crn_started(
          p_term_code  svbauth.svbauth_term_code%TYPE,
          p_crn        svbauth.svbauth_crn%TYPE,
          --p_pidm       spriden.spriden_pidm%TYPE DEFAULT NULL,       C3SC GRS 03/12/09 REMOVE
          p_start_date sftregs.sftregs_start_date%TYPE DEFAULT NULL,
          p_reg_date   sftregs.sftregs_rsts_date%TYPE DEFAULT NULL)
  RETURN BOOLEAN
   IS
     lv_reg_inprogress_ref    sv_reg_inprogress.reg_inprogress_ref;
     lv_reg_inprogress_rec    sv_reg_inprogress.reg_inprogress_rec;
     lv_days_required         NUMBER  := 0;
     lv_start_date            DATE;
     lv_reg_date              DATE    := p_reg_date;

   -- C3SC Added [JR] - 08/09/2006
    CURSOR get_days_required_c ( term_in sobterm.sobterm_term_code%TYPE )
    IS
      SELECT sobterm_days_required
        FROM sobterm
       WHERE sobterm_term_code = term_in;

  BEGIN
     -- find days required
    OPEN get_days_required_c(p_term_code);
    FETCH get_days_required_c
     INTO lv_days_required;
    CLOSE get_days_required_c;
/*
-- CL 11/29/2006
-- SC_Defect 2497: commented out code to populate lv_start_date from
-- SFTREGS.SFTREGS_START_DATE because baseline does not populate the correct part of term date
-- call new function f_get_start_date to return start date
*/

   -- C3SC SC_Defect: 1-3GBGMX   GRS  03/14/2008   changed to be consistent with close checking only when open learning use the date entry by the user
     IF sfkolrl.f_open_learning_course (p_term_code, p_crn)
     THEN
       lv_start_date := p_start_date;
     ELSE
       lv_start_date:= f_get_start_date( p_term_code=>p_term_code,
                                                 p_crn => p_crn
                                                 );
     END IF;
   RETURN TRUNC(lv_reg_date) >= (TRUNC (lv_start_date) + TRUNC(lv_days_required));
  END f_crn_started;

/**------------------------------------------------------------------------------------**/
FUNCTION f_check_crn_started(p_auth_over_started  VARCHAR2,
                             p_term_code  svbauth.svbauth_term_code%TYPE,
                             p_crn        svbauth.svbauth_crn%TYPE,
                             --p_pidm       spriden.spriden_pidm%TYPE DEFAULT NULL,  C3SC GRS 03/19/09 REMOVE
                             p_start_date sftregs.sftregs_start_date%TYPE DEFAULT NULL,
                             p_reg_date   sftregs.sftregs_rsts_date%TYPE DEFAULT NULL
                             )
RETURN BOOLEAN
IS
BEGIN

  IF (NVL(p_auth_over_started, 'N') <> 'Y')
           AND sv_auth_addcodes_bp.f_crn_started(
                 p_term_code  => p_term_code,
                 p_crn        => p_crn,
                 --p_pidm       => p_pidm,  C3SC GRS 03/19/09  REMOVE
                 p_start_date => p_start_date,
                 p_reg_date   => p_reg_date )
  THEN
    RETURN TRUE;
  ELSE
    RETURN FALSE;
  END IF;

END f_check_crn_started;

/**------------------------------------------------------------------------------------**/

  FUNCTION f_census_date_past(
      p_term_code  svbauth.svbauth_term_code%TYPE,
      p_crn        svbauth.svbauth_crn%TYPE,
      p_rsts_date  sftregs.sftregs_rsts_date%TYPE)
  RETURN BOOLEAN IS
     lv_sect_ref              sb_section.section_ref;
     lv_sect_rec              sb_section.section_rec;

  BEGIN

    IF NOT sfkolrl.f_open_learning_course (p_term_code, p_crn)  THEN

      lv_sect_ref := sb_section.f_query_one(p_term_code, p_crn);
--
      FETCH lv_sect_ref INTO lv_sect_rec;
      IF lv_sect_ref%NOTFOUND THEN
          CLOSE lv_sect_ref;
          RETURN FALSE;
      END IF;
      CLOSE lv_sect_ref;
--
        RETURN TRUNC(lv_sect_rec.r_census_enrl_date) <= TRUNC(NVL(p_rsts_date,SYSDATE));
    END IF;
  RETURN FALSE;
  END f_census_date_past;


/**------------------------------------------------------------------------------------**/
  FUNCTION f_check_census_date(p_auth_over_census VARCHAR2,
                               p_term_code  svbauth.svbauth_term_code%TYPE,
                               p_crn        svbauth.svbauth_crn%TYPE,
                               p_rsts_date  sftregs.sftregs_rsts_date%TYPE
                               )
  RETURN BOOLEAN
  IS
  BEGIN
    IF (NVL(p_auth_over_census, 'N') <> 'Y')
         AND sv_auth_addcodes_bp.f_census_date_past(p_term_code => p_term_code,
                                                    p_crn       => p_crn,
                                                    p_rsts_date => p_rsts_date
                                                    )
    THEN
      RETURN TRUE;
    ELSE
      RETURN FALSE;
     END IF;
  END f_check_census_date;

/**------------------------------------------------------------------------------------**/

  FUNCTION f_exists_active (
    p_term_code    svbauth.svbauth_term_code%TYPE,
    p_crn          svbauth.svbauth_crn%TYPE,
    p_auth_code    svbauth.svbauth_auth_code%TYPE,
    p_rowid        gb_common.internal_record_id_type DEFAULT NULL)
  RETURN VARCHAR2 IS
    TYPE queryone_ref IS REF CURSOR;
    lv_cursor queryone_ref;
    lv_tempout VARCHAR2(1) := 'N';
  BEGIN
-- Same as f_exsits but with the added active_ind condition
    IF p_rowid IS NOT NULL THEN
      OPEN lv_cursor for
      SELECT 'Y'
        FROM svbauth
       WHERE ROWID = p_rowid
         AND svbauth_active_ind = 'Y';
    ELSE
      OPEN lv_cursor for
      SELECT 'Y'
        FROM svbauth
       WHERE svbauth_term_code = p_term_code
         AND svbauth_crn = p_crn
         AND svbauth_auth_code = p_auth_code
         AND svbauth_active_ind = 'Y';
    END IF;
    FETCH lv_cursor INTO lv_tempout;
    CLOSE lv_cursor;
    RETURN lv_tempout;
  END f_exists_active;

/**------------------------------------------------------------------------------------**/

  FUNCTION f_auth_req_msg(
      p_rmsg_cde          IN OUT      sftregs.sftregs_rmsg_cde%TYPE,           --C3SC GRS 13/01/2011 MOD Defect::1-E8O563
      p_orig_msg                      sftregs.sftregs_message%TYPE,
      xlst_group_in                   VARCHAR2	                               --C3SC GRS 03/12/09  ADD
       )
  RETURN sftregs.sftregs_message%TYPE
  IS
     lv_auth_msg  sftregs.sftregs_message%TYPE;
      xlst_gp  VARCHAR(2):=NULL;

  BEGIN
    IF xlst_group_in IS NOT NULL
    THEN
      xlst_gp := '-X';
    END IF;

     -- limited to 25 characters so abbreviate Wait List phrase
     -- remove error messages if authorizied
    IF(  p_rmsg_cde = 'CLOS' OR
         p_rmsg_cde='WAIT' OR
         p_rmsg_cde='WAIF'
         )
       AND   INSTR(UPPER(p_orig_msg),G$_NLS.Get('SVKB_AUTH_ADDCODES_BP1-0001','SQL','CLOSED')) <> 0   -- Defect 1-KGBJGX 02/26/2013 GRS Changed
    THEN
    	p_rmsg_cde:='CLOS';    --C3SC GRS 13/01/2010 ADD Defect:1-E8O563
      lv_auth_msg:= sb_registration_msg.f_get_message(
                            p_cde => 'CLOS',
                            p_seqno => 51,
                            p_placeholder_1 => xlst_gp
                            );

    ELSIF ( p_rmsg_cde = 'RESC'  OR
            ( p_rmsg_cde = 'RESF' AND
              INSTR(UPPER(p_orig_msg),G$_NLS.Get('SVKB_AUTH_ADDCODES_BP1-0002','SQL','CLOSED')) <> 0          -- Defect 1-KGBJGX 02/26/2013 GRS Changed
            )                    OR
          ( p_rmsg_cde = 'RESV' AND
           INSTR(UPPER(p_orig_msg),G$_NLS.Get('SVKB_AUTH_ADDCODES_BP1-0003','SQL','CLOSED')) <> 0            -- Defect 1-KGBJGX 02/26/2013 GRS Changed
            )
         )
    THEN
    	p_rmsg_cde:='RESC';  --C3SC GRS 13/01/2010 ADD Defect:1-E8O563
       lv_auth_msg:= sb_registration_msg.f_get_message(
                            p_cde => 'RESC',
                            p_seqno => 51,
                            p_placeholder_1 => xlst_gp
                            );
     ELSE

       lv_auth_msg:=p_orig_msg;
     END IF;
    RETURN lv_auth_msg;
  END f_auth_req_msg;



/**------------------------------------------------------------------------------------**/
  FUNCTION F_call_auth_page(p_term IN STVTERM.STVTERM_CODE%TYPE,
                            p_pidm IN SPRIDEN.SPRIDEN_PIDM%TYPE
                           ) RETURN BOOLEAN
  IS
    CURSOR Get_CrnAuthReq IS
      SELECT COUNT(SFTREGS_CRN)
      FROM SFTREGS,
           SVTAUTM
      WHERE SFTREGS_PIDM=p_pidm
        AND SFTREGS_TERM_CODE = p_term
        AND SVTAUTM_PIDM = SFTREGS_PIDM
        AND SVTAUTM_TERM_CODE = SFTREGS_TERM_CODE
        AND (   SVTAUTM_MSG_CODE = 'AUTH3'
             OR SVTAUTM_MSG_CODE = 'AUTH4'
             OR SVTAUTM_MSG_CODE = 'AUTH7'
             OR SVTAUTM_MSG_CODE = 'AUTH8'
            );
   ln_CrnAuthReq_Count NUMBER:=0;
  BEGIN
   OPEN Get_CrnAuthReq  ;
   FETCH Get_CrnAuthReq INTO ln_CrnAuthReq_Count;
   CLOSE Get_CrnAuthReq;

   IF ln_CrnAuthReq_Count = 0 THEN
     RETURN FALSE;
   ELSE
     RETURN TRUE;
   END IF;
  END F_call_auth_page;


/**------------------------------------------------------------------------------------**/
 FUNCTION F_set_capacity_value(p_term_code   STVTERM.STVTERM_CODE%TYPE,
                                p_pidm       SPRIDEN.SPRIDEN_PIDM%TYPE,
                                p_rsts_code  SFRSTCR.SFRSTCR_RSTS_CODE%TYPE,
                                p_crn        svbauth.svbauth_crn%TYPE,
                                p_rsts_date  sftregs.sftregs_rsts_date%TYPE,
                                p_capacity_in  VARCHAR2
                                )
   RETURN VARCHAR2
  IS
    lv_auth_reason  VARCHAR2(30);
  BEGIN
    IF sv_auth_addcodes_bp.F_process_with_auth(p_term_code  => p_term_code,
                                               p_rsts_code  => p_rsts_code)
    THEN
      IF sv_auth_addcodes_bp.f_is_authorized(p_term_code  => p_term_code,
                                                   p_crn        => p_crn,
                                                   p_pidm       => p_pidm,
                                                   p_rsts_date  => SYSDATE,  --SC_defect:2338 Changed GRS 10/26/2006
                                 p_reason_out => lv_auth_reason)
      THEN
        RETURN 'N';   /* no capacity check on update */
       ELSE
        RETURN p_capacity_in;
       END IF;
     ELSE
      RETURN p_capacity_in;
     END IF;
  END F_set_capacity_value;


--SC_Defect: 2668 BEGINS
FUNCTION F_RegWaitList(p_start_date_in IN  sftregs.sftregs_start_date%TYPE DEFAULT NULL,
                       p_rmsg_cde IN  sftregs.sftregs_rmsg_cde%TYPE,    --C3SC GRS 03/11/09 CHANGED
                       p_rsts_date_in IN sftregs.sftregs_rsts_date%TYPE         --Defect:1-3JAR07
                       )
RETURN VARCHAR2
IS
BEGIN
  IF  p_rsts_date_in <  p_start_date_in
  AND p_rmsg_cde = 'WAIT'
  --AND INSTR(p_message_in,G$_NLS.Get('SVKB_AUTH_ADDCODES_BP1-0014','SQL','WAIT')) <> 0
  --AND INSTR(p_message_in,G$_NLS.Get('SVKB_AUTH_ADDCODES_BP1-0004','SQL','FULL')) = 0    -- GR 01/23/2007
  THEN

   RETURN 'Y';
  ELSE
   RETURN 'N';
  END IF;

END F_RegWaitList;
--SC_Defect: 2668  END


--C3SC ADDED GRS 03/19/2009
FUNCTION f_reg_auth_active (p_term_code      stvterm.stvterm_code%TYPE,
                            p_crn            ssbsect.ssbsect_crn%TYPE,
                            p_start_date     sftregs.sftregs_start_date%TYPE DEFAULT NULL,
                            p_reg_date       sftregs.sftregs_rsts_date%TYPE DEFAULT NULL,
                            p_check_value    VARCHAR2 DEFAULT NULL
                            )
RETURN BOOLEAN IS


lv_section_ref              sv_section.section_ref;
lv_section_rec              sv_section.section_rec;
lv_term_ref                 sv_term.term_ref;
lv_term_rec                 sv_term.term_rec;
lv_SOBTERM_REG_AUTH_CLOSED_IND    SOBTERM.SOBTERM_REG_AUTH_CLOSED_IND%TYPE;
lv_no_notice_date       DATE;
lv_start_date               sftregs.sftregs_start_date%TYPE;
BEGIN
   --Get section information
  lv_section_ref:= sv_section.f_query_one(p_term_code=> p_term_code,
                                          p_crn=> p_crn
                                          );
  FETCH lv_section_ref INTO lv_section_rec;
  CLOSE lv_section_ref;

 --Get Term Information
  lv_term_ref:= sv_term.f_query_one(p_term_code=>p_term_code);
  FETCH lv_term_ref INTO lv_term_rec;
  CLOSE lv_term_ref;

  IF lv_section_rec.r_WAIT_CAPACITY > 0 THEN
   lv_SOBTERM_REG_AUTH_CLOSED_IND := 'Y';
  ELSE
    lv_SOBTERM_REG_AUTH_CLOSED_IND:= lv_term_rec.r_reg_auth_closed_ind;
  END IF;

  IF NVL(lv_sobterm_reg_auth_closed_ind,'N') = 'N' THEN
   IF NVL(lv_section_rec.r_reg_auth_active_ind,'N') = 'N' THEN
      RETURN FALSE;
   ELSE
     RETURN TRUE;
   END IF;
  ELSE
    IF NVL(lv_section_rec.r_reg_auth_active_ind,'N') = 'N' THEN
      RETURN FALSE;
    ELSE
      IF p_check_value = 'CENSUS' OR
         p_check_value = 'START'
      THEN
        RETURN TRUE;
      ELSE
        -- IF waitlist automation is not active
        IF sfkwlat.f_wl_automation_active( p_term => p_term_code,
                                  p_crn =>p_crn
                                 ) = 'N'
        THEN
          -- p_check_value is close
          IF f_crn_started(p_term_code  => p_term_code,
                           p_crn        => p_crn,
                           p_start_date => p_start_date,
                           p_reg_date   => p_reg_date
                           )
          THEN
             RETURN TRUE;
          ELSE
             RETURN FALSE;
          END IF;
        ELSE
		  -- C3SC:8.7 GRS 03/21/2013 ADDED
          --  When Open Learning use the entered date plus days required defined in Sobterm
		  IF lv_section_rec.r_ptrm_code IS NULL THEN
    		    lv_start_date :=  p_start_date + lv_term_rec.r_days_required;
		  ELSE
  		        lv_start_date:= f_get_auth_start_date(p_term_code=>p_term_code,
                                                      p_crn =>p_crn);
          END IF;
     	  -- C3SC:8.7 GRS 03/21/2013 END
           -- waitlist automation is active
          lv_no_notice_date := f_get_no_notice_date(p_term_code=>p_term_code,
                                                    p_crn =>p_crn
                                                   );
          IF ( (p_reg_date < lv_no_notice_date)
              OR  (p_reg_date < lv_start_date )
              )
          THEN
            RETURN FALSE;
          ELSE
           RETURN TRUE;
          END IF;
        END IF;
      END IF;
    END IF;
  END IF;

END f_reg_auth_active;


FUNCTION F_wl_notify(p_term_code    stvterm.stvterm_code%type,
                     p_crn          ssbsect.ssbsect_crn%type
                     )

RETURN BOOLEAN  IS

lv_section_ref              sv_section.section_ref;
lv_section_rec              sv_section.section_rec;
lv_term_ref                 sv_term.term_ref;
lv_term_rec                 sv_term.term_rec;
lv_wl_section_ctrl_ref      sb_wl_section_ctrl.wl_section_ctrl_ref;
lv_wl_section_ctrl_rec      sb_wl_section_ctrl.wl_section_ctrl_rec;
lv_number_hours             NUMBER;
lv_wl_notification_rec      sb_wl_notification.wl_notification_rec;
lv_wl_notification_ref      sb_wl_notification.wl_notification_ref;
lv_start_date               sftregs.sftregs_start_date%TYPE;
lv_wl_term_control_ref  sb_wl_term_control.wl_term_control_ref;
lv_wl_term_control_rec  sb_wl_term_control.wl_term_control_rec;
lv_no_notice_date       DATE;


BEGIN
-- This function is similiar to f_reg_auth_active but is used by the waitlist 8X code to
-- check if students needs to be notified or not depending on the no-notice-period.


  IF NOT f_is_auth_enabled(p_term_code => p_term_code) THEN
    RETURN TRUE; -- Authorization code is not active for the term, continue processing
  ELSE
      --Get section information
    lv_section_ref:= sv_section.f_query_one(p_term_code=> p_term_code,
                                            p_crn=> p_crn
                                            );
    FETCH lv_section_ref INTO lv_section_rec;
    CLOSE lv_section_ref;
    IF NVL(lv_section_rec.r_reg_auth_active_ind,'N') = 'N' THEN
      RETURN TRUE;   --section auth registration is not active continue processing
    ELSE
--
     lv_no_notice_date := f_get_no_notice_date(p_term_code=>p_term_code,
                                                p_crn =>p_crn
                                                );

      IF SYSDATE < lv_no_notice_date THEN
       RETURN TRUE;
      ELSIF SYSDATE >= lv_no_notice_date THEN
       RETURN FALSE;
      END IF;

     END IF;
  END IF;
 END F_wl_notify;

  FUNCTION f_get_no_notice_date (p_term_code stvterm.stvterm_code%type,
                                 p_crn  ssbsect.ssbsect_crn%type
                                 )
  RETURN DATE
  IS
    lv_wl_section_ctrl_ref      sb_wl_section_ctrl.wl_section_ctrl_ref;
    lv_wl_section_ctrl_rec      sb_wl_section_ctrl.wl_section_ctrl_rec;
    lv_number_hours             NUMBER;
    lv_wl_notification_rec      sb_wl_notification.wl_notification_rec;
    lv_wl_notification_ref      sb_wl_notification.wl_notification_ref;
    lv_start_date               sftregs.sftregs_start_date%TYPE;
    lv_wl_term_control_ref  sb_wl_term_control.wl_term_control_ref;
    lv_wl_term_control_rec  sb_wl_term_control.wl_term_control_rec;
    lv_no_notice_date       DATE;

  BEGIN
     lv_start_date:= f_get_auth_start_date(p_term_code=>p_term_code,
                                           p_crn =>p_crn);

      -- First look in the section control table --sfrwlnt
      lv_wl_section_ctrl_ref:= sb_wl_section_ctrl.f_query_one(p_term_code => p_term_code,
                                                              p_crn       => p_crn);
      FETCH lv_wl_section_ctrl_ref INTO lv_wl_section_ctrl_rec;
      IF lv_wl_section_ctrl_ref%NOTFOUND THEN
        lv_wl_term_control_ref:= sb_wl_term_control.f_query_one(p_term_code => p_term_code);
        FETCH lv_wl_term_control_ref INTO lv_wl_term_control_rec;
        IF lv_wl_term_control_ref%NOTFOUND THEN
          lv_wl_term_control_rec.r_deadline_notify := 0;
        END IF;
        CLOSE lv_wl_term_control_ref;
        lv_number_hours:= lv_wl_term_control_rec.r_deadline_notify;
      ELSE
        lv_number_hours:= lv_wl_section_ctrl_rec.r_deadline_notify;
      END IF;
      CLOSE lv_wl_section_ctrl_ref;
      lv_no_notice_date := lv_start_date - (lv_number_hours/24);
     RETURN lv_no_notice_date;
  END f_get_no_notice_date;

  FUNCTION f_get_auth_start_date(p_term_code stvterm.stvterm_code%type,
                                 p_crn  ssbsect.ssbsect_crn%type
                                 )
  RETURN DATE
  IS
    lv_start_date               sftregs.sftregs_start_date%TYPE;
    lv_term_ref                 sv_term.term_ref;
    lv_term_rec                 sv_term.term_rec;

  BEGIN
    --Get Term Information
      lv_term_ref:= sv_term.f_query_one(p_term_code=>p_term_code);
      FETCH lv_term_ref INTO lv_term_rec;
      CLOSE lv_term_ref;

      --Get the start date
      lv_start_date:= f_get_start_date( p_term_code=>p_term_code,
                                        p_crn => p_crn
                                       );
      lv_start_date:= lv_start_date + lv_term_rec.r_days_required;

      RETURN lv_start_date;
 END f_get_auth_start_date;


/**------------------------------------------------------------------------------------**/
-- Procedures
--
  PROCEDURE p_course_auth_status(
      p_term_code                      svbauth.svbauth_term_code%TYPE,
      p_crn                            svbauth.svbauth_crn%TYPE DEFAULT NULL,
      p_pidm                           svbauth.svbauth_pidm%TYPE,
      p_course_auth_status_data IN OUT course_auth_status_ref
                   )
  IS
    lv_course_auth_status_data    course_auth_status_ref;
  BEGIN
    lv_course_auth_status_data
       := f_get_course_auth_status(
        p_term_code  => p_term_code,
        p_crn        => p_crn,
        p_pidm       => p_pidm);
    p_course_auth_status_data :=  lv_course_auth_status_data;
  END p_course_auth_status;
--
/**------------------------------------------------------------------------------------**/
  PROCEDURE p_msg_flag_values(p_term_code_in           IN  svbauth.svbauth_term_code%TYPE,
                              p_crn_in                 IN  svbauth.svbauth_crn%TYPE ,
                              p_pidm_in                IN  svbauth.svbauth_pidm%TYPE,
                              p_rsts_date_in           IN  sftregs.sftregs_rsts_date%TYPE,
                              p_verify_ind             IN  VARCHAR2                      ,
                              p_rmsg_cde_in_out        IN OUT sftregs.sftregs_rmsg_cde%TYPE, --C3SC GRS 03/11/09 ADD
                              p_err_message_in_out     IN OUT sftregs.sftregs_message%TYPE,
                              p_err_flag_out           OUT  VARCHAR2                      ,
                              p_xlst_group_in          IN  ssrxlst.ssrxlst_xlst_group%TYPE DEFAULT NULL
                             )
  IS
    lv_auth_reason            VARCHAR2(30);
  BEGIN
    /*-- p_verify_ind has limited purpose. C=Closed section. S=Started section. So if we are checking for
      -- started section then we need to change the message to include a -X if it is a crosslist. */
    IF p_verify_ind = 'S' THEN
      p_rmsg_cde_in_out:='VADT';
      p_err_message_in_out :=sb_registration_msg.f_get_message(
                            p_cde => 'VADT',
                            p_seqno => 2
                            );

    END IF;
    p_err_message_in_out := sv_auth_addcodes_bp.f_auth_req_msg(p_rmsg_cde=>p_rmsg_cde_in_out,
                                                               p_orig_msg=> p_err_message_in_out,
                                                               xlst_group_in => p_xlst_group_in
                                                              );
    IF sv_auth_addcodes_bp.f_is_authorized(p_term_code  => p_term_code_in,
                                           p_crn        => p_crn_in,
                                           p_pidm       => p_pidm_in,
                                           p_rsts_date  => SYSDATE,  --SC_defect:2338 Changed p_rsts_date_in GRS 10/26/2006
                                           p_reason_out => lv_auth_reason)
    THEN
      -- O=override, F=Fatal
      p_err_flag_out := 'O';
    ELSE
      p_err_flag_out := 'F';
    END IF;
  END p_msg_flag_values ;

/**------------------------------------------------------------------------------------**/
  PROCEDURE p_auth_msg_flag_values(p_term_code_in           IN  svbauth.svbauth_term_code%TYPE,
                                   p_rsts_code_in           IN  SFRSTCR.SFRSTCR_RSTS_CODE%TYPE,
                                   p_rsts_date_in           IN  sftregs.sftregs_rsts_date%TYPE,
                                   p_crn_in                 IN  svbauth.svbauth_crn%TYPE ,
                                   p_pidm_in                IN  svbauth.svbauth_pidm%TYPE,
                                   p_start_date_in          IN  sftregs.sftregs_start_date%TYPE DEFAULT NULL,
                                   p_xlst_group_in          IN  ssrxlst.ssrxlst_xlst_group%TYPE DEFAULT NULL,
                                   p_auth_over_census_in    IN VARCHAR2,
                                   p_auth_over_closed_in    IN VARCHAR2,
                                   p_auth_over_started_in   IN VARCHAR2,
                                   p_for_upd_ind            IN VARCHAR2,
                                   p_rmsg_cde_in_out        IN OUT sftregs.sftregs_rmsg_cde%TYPE, --C3SC GRS 03/11/09 ADD
                                   p_err_message_in_out     IN OUT sftregs.sftregs_message%TYPE,
                                   p_err_flag_out           IN OUT  VARCHAR2,
                                   p_check_value            IN VARCHAR2 DEFAULT NULL
                                 )
  IS
  lv_auth_reason  VARCHAR2(50);
  lv_sftregs_start_date   sftregs.sftregs_start_date%type;  --defect 1-3JAR07

  BEGIN
     --C3SC GRS  ADDED defect 1-3JAR07
     IF sfkolrl.f_open_learning_course (p_term_code_in, p_crn_in)
     THEN
       lv_sftregs_start_date := p_start_date_in;
     ELSE
       lv_sftregs_start_Date:= f_get_start_date( p_term_code=>p_term_code_in,
                                                 p_crn => p_crn_in
                                                 );
     END IF;
     -- defect :1-3JAR07


       IF f_reg_auth_active (p_term_code   =>p_term_code_in,
                               p_crn        =>p_crn_in,
                               p_start_date =>p_start_date_in,
                               p_reg_date   =>p_rsts_date_in,
                               p_check_value=>p_check_value
                            )
       -- Defect 1-KGBJGX GRS 02/26/2013
	   AND  INSTR(UPPER(p_err_message_in_out),G$_NLS.Get('SVKB_AUTH_ADDCODES_BP1-0005','SQL','OPEN')) <> 0
       AND ( p_check_value ='CLOSE'  OR
             p_for_upd_ind= 'Y'
             )
      THEN
        p_rmsg_cde_in_out      :=NULL;   --C3SC GRS 03/11/2009
        p_err_message_in_out := NULL;
        p_err_flag_out := 'O';
        RETURN;
       END IF;





     -- add authorization is enabled for term and rsts_code
    IF sv_auth_addcodes_bp.F_process_with_auth(p_term_code  => p_term_code_in,
                                               p_rsts_code  => p_rsts_code_in)

    THEN
     /*** This logic is used on the sfkedit.p_pre_edit to change the baseline error message to Auth error message */
      IF p_for_upd_ind = 'N' THEN
        -- census date past - reg prohibited

        IF sv_auth_addcodes_bp.f_check_census_date(p_auth_over_census =>p_auth_over_census_in,
                                                   p_term_code        =>p_term_code_in,
                                                   p_crn              =>p_crn_in,
                                                   p_rsts_date        =>p_rsts_date_in
                                                  )
        AND p_check_value='CENSUS'
        AND f_reg_auth_active (p_term_code   =>p_term_code_in,
                               p_crn        =>p_crn_in,
                               p_start_date =>p_start_date_in,
                               p_reg_date   =>p_rsts_date_in,
                               p_check_value=>p_check_value
                            )
        THEN
          p_rmsg_cde_in_out      :='VADT';   --C3SC GRS 03/11/2009
          --C3SC GRS 03/11/2009 ADD
          p_err_message_in_out := sb_registration_msg.f_get_message(
                            p_cde => 'VADT',
                            p_seqno => 1
                            );

          --p_err_message_in_out := SUBSTR(G$_NLS.Get('SVKB_AUTH_ADDCODES_BP1-0018','SQL','CENSUS DATE PROHIBITS REG'),1,25); C3SC GRS 03/11/09 REMOVED
          p_err_flag_out := 'F';

        -- closed sections or full waitlist
        ELSIF p_check_value='CLOSE'
         AND sv_auth_addcodes_bp.f_check_closed_section(p_auth_over_closed=>p_auth_over_closed_in,
                                                        p_rmsg_cde=>p_rmsg_cde_in_out,       --C3SC GRS 03/12/09 ADDED
                                                         p_orig_msg => p_err_message_in_out
                                                         )
         AND f_reg_auth_active (p_term_code   =>p_term_code_in,
                               p_crn        =>p_crn_in,
                               p_start_date =>p_start_date_in,
                               p_reg_date   =>p_rsts_date_in,
                               p_check_value=>p_check_value
                            )
        THEN
          sv_auth_addcodes_bp.p_msg_flag_values(p_term_code_in => p_term_code_in,
                                                p_crn_in => p_crn_in,
                                                p_pidm_in => p_pidm_in,
                                                p_rsts_date_in => p_rsts_date_in,
                                                -- denotes we are checking closed section
                                                p_verify_ind => 'C',
                                                p_rmsg_cde_in_out=>p_rmsg_cde_in_out,   --C3SC GRS 03/12/09 ADD
                                                p_err_message_in_out => p_err_message_in_out,
                                                p_err_flag_out => p_err_flag_out,
                                                p_xlst_group_in =>p_xlst_group_in   --C3SC GRS 03/12/09 CHANGED from NULL
                                               );

       -- crn started
      ELSIF p_check_value='START'
        AND NVL(p_err_flag_out, '*') <> 'F'
        AND f_reg_auth_active (p_term_code   =>p_term_code_in,
                               p_crn        =>p_crn_in,
                               p_start_date =>p_start_date_in,
                               p_reg_date   =>p_rsts_date_in,
                               p_check_value=>p_check_value
                             )

        and sv_auth_addcodes_bp.f_check_crn_started(p_auth_over_started  => p_auth_over_started_in,
                                                    p_term_code          => p_term_code_in,
                                                    p_crn                => p_crn_in,
                                                   -- p_pidm               => p_pidm_in,    --C3SC GRS 03/19/09 REMOVE
                                                    p_start_date         => p_start_date_in,
                                                    p_reg_date           => p_rsts_date_in
                                                    )

        THEN
          sv_auth_addcodes_bp.p_msg_flag_values(p_term_code_in => p_term_code_in,
                                                p_crn_in => p_crn_in,
                                                p_pidm_in => p_pidm_in,
                                                p_rsts_date_in => p_rsts_date_in,
                                                -- denotes we are checking stared sectio
                                                p_verify_ind => 'S',
                                                p_rmsg_cde_in_out=>p_rmsg_cde_in_out,   --C3SC GRS 03/12/09 ADD
                                                p_err_message_in_out => p_err_message_in_out,
                                                p_err_flag_out => p_err_flag_out,
                                                p_xlst_group_in => p_xlst_group_in
                                               );

        END IF; -- end-if checks for census, closed and crn started messages

      /* p_for_upd_ind= Y , This logic is only used on sfkedit.p_update_regs */
      ELSIF p_for_upd_ind= 'Y' THEN  -- changed to from ELSE to ELSIF, CL 11/27/2006
        IF sv_auth_addcodes_bp.f_check_closed_section(p_auth_over_closed=>p_auth_over_closed_in,
                                                      p_rmsg_cde =>p_rmsg_cde_in_out,                   --C3SC GRS 03/11/2009 ADD
                                                      p_orig_msg => p_err_message_in_out
                                                      )
         AND f_reg_auth_active (p_term_code   =>p_term_code_in,
                               p_crn        =>p_crn_in,
                               p_start_date =>p_start_date_in,
                               p_reg_date   =>p_rsts_date_in,
                               p_check_value=>'CLOSE'
                            )
        THEN
         /* since the capacity is verified again, we need to change the baseline error message to  the auth message
            and to verify the auth codes to see if the error flag will be O or F*/
          sv_auth_addcodes_bp.p_msg_flag_values(p_term_code_in => p_term_code_in,
                                                p_crn_in => p_crn_in,
                                                p_pidm_in => p_pidm_in,
                                                p_rsts_date_in => p_rsts_date_in,
                                                p_verify_ind => 'C',
                                                p_rmsg_cde_in_out=>p_rmsg_cde_in_out,   --C3SC GRS 03/12/09 ADD
                                                p_err_message_in_out => p_err_message_in_out,
                                                p_err_flag_out => p_err_flag_out,
                                                p_xlst_group_in => p_xlst_group_in  --C3SC GRS 03/12/09 CHANGED from NULL
                                               );

        END IF; --end if check for closed section
      END  IF;  --end if for update_regs or pre_update logic
    END IF;  -- end-if to find out if authorization code is turned on
  END p_auth_msg_flag_values;
/**------------------------------------------------------------------------------------**/
   PROCEDURE P_call_auth_page(   term_in                IN       OWA_UTIL.ident_arr,
                                err_term               IN       OWA_UTIL.ident_arr,
                                pidm                   IN       SPRIDEN.SPRIDEN_PIDM%TYPE,
                                err_crn                IN       OWA_UTIL.ident_arr,
                                err_subj               IN       OWA_UTIL.ident_arr,
                                err_crse               IN       OWA_UTIL.ident_arr,
                                err_sec                IN       OWA_UTIL.ident_arr,
                                err_code               IN       OWA_UTIL.ident_arr,
                                err_levl               IN       OWA_UTIL.ident_arr,
                                err_cred               IN       OWA_UTIL.ident_arr,
                                err_gmod               IN       OWA_UTIL.ident_arr,
                                drop_result_label_in   IN       twgrinfo.twgrinfo_label%TYPE,
                                drop_problems_in       IN       sfkcurs.drop_problems_rec_tabtype,
                                drop_failures_in       IN       sfkcurs.drop_problems_rec_tabtype,
                                p_called_page_out            OUT      VARCHAR2,
                                add_row                 NUMBER,
                                orig_crn           IN    OWA_UTIL.ident_arr,
                                orig_rsts          IN    OWA_UTIL.ident_arr
                                )
  IS

   lv_call_authPage   VARCHAR2(1):='N';
   auth_code         OWA_UTIL.ident_arr;
   auth_msg          OWA_UTIL.ident_arr;
   num_attempts_in   OWA_UTIL.ident_arr;
   capp_tech_error    VARCHAR2 (4)                    := NULL;
 drop_problems        sfkcurs.drop_problems_rec_tabtype := drop_problems_in;
   drop_failures      sfkcurs.drop_problems_rec_tabtype := drop_failures_in;

  BEGIN

   FOR i IN 1 .. term_in.COUNT
   LOOP
    lv_call_authPage:='N';

     IF sv_auth_addcodes_bp.f_is_auth_enabled(term_in(i))
     AND sv_auth_addcodes_bp.F_call_auth_page(term_in(i),pidm)
     THEN
       lv_call_authPage:='Y';
      END IF;
    END LOOP;
    p_called_page_out:=lv_call_authPage;
   IF  lv_call_authPage='Y' THEN
    auth_code(1):=null;
    auth_msg(1):= null;
    num_attempts_in(1):=null;

    bwvkauth.P_DispAutCode (term_in  ,
                            err_term ,
                            err_crn  ,
                            err_subj ,
                            err_crse ,
                            err_sec  ,
                            err_code ,
                            err_levl ,
                            err_cred ,
                            err_gmod ,
                            capp_tech_error,
                            drop_result_label_in  ,
                            drop_problems_in         ,
                            drop_failures_in         ,
                            auth_code             ,
                            auth_msg              ,
                            num_attempts_in     ,
                            add_row,
                            orig_crn ,
                            orig_rsts
                           );
      --RETURN;
   END IF;
END P_call_auth_page;
/**------------------------------------------------------------------------------------**/
  PROCEDURE P_rest_message(p_error_flag_in IN VARCHAR2,
                           p_rmsg_cde_in_out      IN OUT sftregs.sftregs_rmsg_cde%TYPE,       -- C3SC GRS 04/12/2009 ADD
                           p_error_mesg_in_out IN OUT sftregs.sftregs_message%TYPE
                          )
 IS
  BEGIN

     IF (p_error_flag_in = 'O')
         AND (    p_error_mesg_in_out = sb_registration_msg.f_get_message(p_cde => 'VADT',p_seqno =>2 )
              OR  (INSTR(p_error_mesg_in_out, 'Authorization required') <> 0)
              )
      THEN
         p_rmsg_cde_in_out:=NULL;                                                       --C3SC GRS 04/12/2009 ADD
         p_error_mesg_in_out := NULL;
     END IF;
  END P_rest_message;



PROCEDURE P_Cancel_Process(term_in              IN   OWA_UTIL.ident_arr,
                           p_pidm_in            IN SPRIDEN.SPRIDEN_PIDM%TYPE,
                           crn_in               IN   OWA_UTIL.ident_arr,
                           regs_row             IN    NUMBER,
                           assoc_term_in        IN   OWA_UTIL.ident_arr
                           )
IS
   i                   INTEGER;
   j                   INTEGER:=1;
   term                stvterm.stvterm_code%TYPE := NULL;
   sgbstdn_rec         sgbstdn%ROWTYPE;
   err_term              OWA_UTIL.ident_arr;
   err_crn               OWA_UTIL.ident_arr;
   err_subj              OWA_UTIL.ident_arr;
   err_crse              OWA_UTIL.ident_arr;
   err_sec               OWA_UTIL.ident_arr;
   err_code              OWA_UTIL.ident_arr;
   err_levl              OWA_UTIL.ident_arr;
   err_cred              OWA_UTIL.ident_arr;
   err_gmod              OWA_UTIL.ident_arr;
   capp_tech_error       VARCHAR2 (4);
   drop_result_label_in  twgrinfo.twgrinfo_label%TYPE DEFAULT NULL;
   drop_problems_in      sfkcurs.drop_problems_rec_tabtype;
   drop_failures_in      sfkcurs.drop_problems_rec_tabtype;
   auth_term_active      VARCHAR2(1);


BEGIN
  term := term_in (term_in.COUNT);
  FOR sgbstdn IN sgklibs.sgbstdnc (p_pidm_in, term)
  LOOP
     sgbstdn_rec := sgbstdn;
  END LOOP;
     --
     -- Initialize arrays.
     -- ===================================================
      err_term (1) := 'dummy';
      err_crn (1) := 'dummy';
      err_code (1) := 'dummy';
      err_subj (1) := 'dummy';
      err_crse (1) := 'dummy';
      err_sec (1) := 'dummy';
      err_levl (1) := 'dummy';
      err_cred (1) := 'dummy';
      err_gmod (1) := 'dummy';

     FOR i IN 2 .. crn_in.COUNT
     LOOP
       EXIT WHEN crn_in (i) IS NULL;

      IF i < regs_row + 2
      THEN
         NULL;
      ELSE
        DECLARE
            my_subj   SSBSECT.SSBSECT_SUBJ_CODE%TYPE := NULL;
            my_crse   SSBSECT.SSBSECT_CRSE_NUMB%TYPE := NULL;
            my_seq    SSBSECT.SSBSECT_SEQ_NUMB%TYPE  := NULL;
        BEGIN

       bwckregs.p_getsection (
                    assoc_term_in(i),
                     crn_in(i),
                     my_subj,
                     my_crse,
                     my_seq
                  );

          j:=j+1;

        err_term(j):= assoc_term_in(i);
        err_crn (j) := crn_in(i);
        err_code (j) := 'AUTHC';
        err_subj (j) :=my_subj;
        err_crse (j) := my_crse;
        err_sec (j) :=  my_seq;
        err_levl (j) := sgbstdn_rec.sgbstdn_levl_code;
        err_cred (j) := null;
        err_gmod (j) := null;
       END;
        END IF;
     END LOOP;


 -- Redisplay the add/drop page.
     -- =======================================================================
   bwckcoms.p_adddrop2 (
      term_in,
      err_term,
      err_crn,
      err_subj,
      err_crse,
      err_sec,
      err_code,
      err_levl,
      err_cred,
      err_gmod,
      capp_tech_error,
      drop_result_label_in,
      drop_problems_in,
      drop_failures_in
   );

END P_Cancel_Process;

PROCEDURE P_local_reset_sftregs_fields( pidm_in IN sfrstcr.sfrstcr_pidm%TYPE,
                             drop_failures_in IN sfkcurs.drop_problems_rec_tabtype)

IS
   i   NUMBER;
BEGIN
   FOR i IN 1 .. drop_failures_in.COUNT
   LOOP
        IF (INSTR(drop_failures_in(i).message, 'Authorization required') <> 0) THEN
        NULL;
     ELSE

        IF drop_failures_in (i).connected_crns IS NULL
        THEN
          /* single class error, only reset this CRN */
          sfkmods.p_reset_sftregs_fields ( pidm_in,
                                           drop_failures_in (i).term_code,
                                           drop_failures_in (i).crn,
                                           NULL);
        ELSE
              sfkmods.p_reset_sftregs_fields ( pidm_in,
                                             drop_failures_in (i).term_code,
                                             drop_failures_in (i).crn,
                                            'AUTODROP');
        END IF;
      END IF;
   END LOOP;
END P_local_reset_sftregs_fields;

PROCEDURE p_local_final_updates(
   term_in                IN       OWA_UTIL.ident_arr,
   err_term               IN       OWA_UTIL.ident_arr,
   err_crn                IN       OWA_UTIL.ident_arr,
   err_subj               IN       OWA_UTIL.ident_arr,
   err_crse               IN       OWA_UTIL.ident_arr,
   err_sec                IN       OWA_UTIL.ident_arr,
   err_code               IN       OWA_UTIL.ident_arr,
   err_levl               IN       OWA_UTIL.ident_arr,
   err_cred               IN       OWA_UTIL.ident_arr,
   err_gmod               IN       OWA_UTIL.ident_arr,
   drop_result_label_in   IN       twgrinfo.twgrinfo_label%TYPE,
   drop_problems_in       IN       sfkcurs.drop_problems_rec_tabtype,
   drop_failures_in       IN       sfkcurs.drop_problems_rec_tabtype,
   add_row                         NUMBER,
   orig_crn           IN    OWA_UTIL.ident_arr,
   orig_rsts          IN    OWA_UTIL.ident_arr
   )
IS
   CURSOR sftregs_errors_c (
      pidm_in   sftregs.sftregs_pidm%TYPE,
      term_in   sftregs.sftregs_term_code%TYPE
   )
   IS
      SELECT *
        FROM sftregs
       WHERE sftregs_pidm = pidm_in
         AND sftregs_term_code = term_in
         AND sftregs_error_flag = 'F';

   CURSOR sftregs_test_c (
      pidm_in   sftregs.sftregs_pidm%TYPE,
      term_in   sftregs.sftregs_term_code%TYPE
   )
   IS
      SELECT *
        FROM sftregs
       WHERE sftregs_pidm = pidm_in
         AND sftregs_term_code = term_in;

   tmst_flag          sftregs.sftregs_error_flag%TYPE;
   group_error_flag   VARCHAR2 (1)                    := 'N';
   capp_tech_error    VARCHAR2 (4)                    := NULL;
   genpidm            spriden.spriden_pidm%TYPE;
   error_flag         sftregs.sftregs_error_flag%TYPE := 'N';
   error_rec          sftregs%ROWTYPE;
   source_system      VARCHAR2 (2);
   clas_code          sgrclsr.sgrclsr_clas_code%TYPE;
   may_drop_last      BOOLEAN                         := TRUE;
   drop_problems      sfkcurs.drop_problems_rec_tabtype := drop_problems_in;
   drop_failures      sfkcurs.drop_problems_rec_tabtype := drop_failures_in;
   lv_call_authPage   VARCHAR2(1):='N';
   minh_error         VARCHAR2 (4)                    := 'N';

BEGIN

   IF NOT twbkwbis.f_validuser (global_pidm)
   THEN
      RETURN;
   END IF;

   IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
   THEN
      genpidm :=
          TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
   ELSE
      genpidm := global_pidm;
   END IF;

   may_drop_last := sfkdrop.f_drop_last ('W');

 --C3SC CJL 24/02/2006 Added
 -- moved this code above sfkedit.p_update, so that we only perform final updates
 -- if no auth error are found
        P_call_auth_page(term_in    => term_in,
                                        err_term   =>err_term,
                                        pidm       =>genpidm,
                                        err_crn    =>err_crn,
                                        err_subj   =>err_subj,
                                        err_crse   =>err_crse,
                                        err_sec    =>err_sec,
                                        err_code   =>err_code,
                                        err_levl   =>err_levl,
                                        err_cred   =>err_cred,
                                        err_gmod   =>err_gmod,
                                        drop_result_label_in   =>drop_result_label_in,
                                        drop_problems_in       =>drop_problems_in,
                                        drop_failures_in       =>drop_failures_in,
                                        p_called_page_out=> lv_call_authPage,
                                        add_row=>add_row,
                                        orig_crn=>orig_crn,
                                        orig_rsts=>orig_rsts
                                          );

   IF  lv_call_authPage = 'Y' THEN
     RETURN;
   END IF;


--END C3SC

  <<term_loop>>
   FOR i IN 1 .. term_in.COUNT
   LOOP
    /* does term exist in sfbetrm? */
      IF sb_enrollment.f_exists( p_pidm => genpidm,
                                 p_term_code => term_in (i)) = 'Y'
      THEN
         p_local_init_final_update_vars (genpidm, term_in (i));

         IF drop_problems IS NOT NULL OR
             drop_failures IS NOT NULL
         THEN


             /* re-run group edits to find any conflicts with drops */
             /* that have been reset */
             group_error_flag := NULL;
             sfkedit.p_web_group_edits (
                genpidm,
                term_in (i),
                'S',
                NVL (sobterm_row.sobterm_dupl_severity, 'N'),
                NVL (sobterm_row.sobterm_time_severity, 'N'),
                NVL (sobterm_row.sobterm_corq_severity, 'N'),
                NVL (sobterm_row.sobterm_link_severity, 'N'),
                NVL (sobterm_row.sobterm_preq_severity, 'N'),
                NVL (sobterm_row.sobterm_maxh_severity, 'N'),
                NVL (sobterm_row.sobterm_minh_severity, 'N'),
                NVL (sobterm_row.sobterm_mexc_severity, 'N'),     -- C3SC VK 09/14/2009 ADD
                sfbetrm_row.sfbetrm_mhrs_over,
                sfbetrm_row.sfbetrm_min_hrs,
                SYS_CONTEXT ('USERENV', 'SESSIONID'),
                'WA',
                group_error_flag,
                capp_tech_error,
                minh_error,
                term_in
             );

           IF capp_tech_error IS NOT NULL
             THEN
                ROLLBACK;
                EXIT term_loop;
             END IF;

             -- Store the fact that a minimum hours error occurred, and quit all processing.
             IF minh_error = 'Y'
             THEN
                twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'M');
                sfkmods.p_delete_sftregs_by_pidm_term(genpidm, term_in (i));
                sfkmods.p_insert_sftregs_from_stcr(genpidm, term_in (i),SYSDATE);
             END IF;

             IF group_error_flag = 'Y'
             THEN
                /* update new courses to indicate they are in error */
                /* don't alter the rec_stat from 'N' */

                UPDATE sftregs
                   SET sftregs_rsts_code = SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2),
                       sftregs_remove_ind = 'Y'
                 WHERE sftregs_term_code = term_in (i)
                   AND sftregs_pidm = genpidm
                   AND sftregs_error_flag = 'F'
                   AND sftregs_rec_stat = 'N';
             END IF;



             bwcklibs.p_build_drop_problems_list ( genpidm,
                                                   term_in (i),
                                                   drop_problems,
                                                   drop_failures
                                                   );

          END IF;

         error_flag := 'Y';

         <<update_loop>>
         WHILE nvl(error_flag,'N') <> 'N'
         LOOP
   --
   -- Transfers sftregs changes into sfrstcr.
   -- =======================================================================

            sfkedit.p_update_regs (
               genpidm,
               term_in (i),
               SYSDATE,
               sgrclsr_clas_code,
               sgbstdn_row.sgbstdn_styp_code,
               NVL (sobterm_row.sobterm_capc_severity, 'N'),
               sobterm_row.sobterm_tmst_calc_ind,
               sfbetrm_row.sfbetrm_tmst_maint_ind,
               sfbetrm_row.sfbetrm_tmst_code,
               may_drop_last,
               'WA',
               error_rec,
               error_flag,
               tmst_flag
            );

            IF error_flag = 'D'
            THEN
               /* sfrstcr row missing - system problem */
               twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'D');
               ROLLBACK;
               EXIT term_loop;
            ELSIF error_flag = 'L'
            THEN
               /* last class drop attempted */
               twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'L');
               ROLLBACK;
               EXIT term_loop;
            ELSIF error_flag = 'Y'
            THEN
             /* capacity error */
               /* update new courses to indicate they are in error */
               UPDATE sftregs
                  SET sftregs_rsts_code = SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2),
                      sftregs_remove_ind = 'Y'
                WHERE sftregs_term_code = term_in (i)
                  AND sftregs_pidm = genpidm
                  AND sftregs_error_flag = 'F'
                  AND sftregs_rec_stat = 'N';

               group_error_flag := NULL;
               /* re-run group edits on remaining classes */
            sfkedit.p_web_group_edits (
                genpidm,
                term_in (i),
                'S',
                NVL (sobterm_row.sobterm_dupl_severity, 'N'),
                NVL (sobterm_row.sobterm_time_severity, 'N'),
                NVL (sobterm_row.sobterm_corq_severity, 'N'),
                NVL (sobterm_row.sobterm_link_severity, 'N'),
                NVL (sobterm_row.sobterm_preq_severity, 'N'),
                NVL (sobterm_row.sobterm_maxh_severity, 'N'),
                NVL (sobterm_row.sobterm_minh_severity, 'N'),
                NVL (sobterm_row.sobterm_mexc_severity, 'N'),  -- C3SC VK 09/14/2009 ADD
                sfbetrm_row.sfbetrm_mhrs_over,
                sfbetrm_row.sfbetrm_min_hrs,
                SYS_CONTEXT ('USERENV', 'SESSIONID'),
                'WA',
                group_error_flag,
                capp_tech_error,
                minh_error,
                term_in
             );


               IF capp_tech_error IS NOT NULL
               THEN
                  ROLLBACK;
                  EXIT term_loop;
               END IF;

               IF minh_error = 'Y'
               THEN
                  twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'M');
                  sfkmods.p_delete_sftregs_by_pidm_term(genpidm, term_in (i));
                  sfkmods.p_insert_sftregs_from_stcr(genpidm, term_in (i),SYSDATE);
               END IF;

               /* group edits found an error, reset all changes  */
               /* to classes associated with those now in error */
               IF group_error_flag = 'Y'
               THEN
               FOR error_rec IN sftregs_errors_c (genpidm, term_in (i))
                  LOOP
                     bwcklibs.p_handle_sftregs_error (error_rec);
                  END LOOP;
               END IF;
            END IF;
         END LOOP;
      END IF; /* term exists */
   END LOOP term_loop;

-- Redisplay the add/drop page.
-- =======================================================================
   bwckcoms.p_adddrop2 (
      term_in,
      err_term,
      err_crn,
      err_subj,
      err_crse,
      err_sec,
      err_code,
      err_levl,
      err_cred,
      err_gmod,
      capp_tech_error,
      drop_result_label_in,
      drop_problems,
      drop_failures
   );

   RETURN;

END p_local_final_updates;

PROCEDURE p_local_init_final_update_vars (pidm_in IN sfrracl.sfrracl_pidm%TYPE,
                                    term_in IN sfrracl.sfrracl_term_code%TYPE)
IS
   lv_levl_code         stvlevl.stvlevl_code%TYPE;

BEGIN
--
-- student variables.
-- =======================================================================
   FOR sgbstdn IN sgklibs.sgbstdnc (pidm_in, term_in)
   LOOP
      sgbstdn_row := sgbstdn;
   END LOOP;

--
-- term variables.
-- =======================================================================
   OPEN soklibs.sobterm_webc (term_in);
   FETCH soklibs.sobterm_webc INTO sobterm_row;

--
-- student class.
-- =======================================================================
   IF soklibs.sobterm_webc%NOTFOUND
   THEN
      atmp_hrs := NULL;
   ELSE
      IF sobterm_row.sobterm_incl_attmpt_hrs_ind = 'Y'
      THEN
         atmp_hrs := 'A';
      ELSE
         atmp_hrs := NULL;
      END IF;
   END IF;

   CLOSE soklibs.sobterm_webc;
   sgrclsr_clas_code := NULL;
   clas_desc := NULL;

   lv_levl_code := sokccur.f_curriculum_value(
                                     p_pidm=>pidm_in,
                                     p_lmod_code => sb_curriculum_str.f_learner,
                                     p_term_code => term_in,
                                     p_key_seqno => 99,
                                     p_eff_term => term_in,
                                     p_order => 1,
                                     p_field => 'LEVL');


   soklibs.p_class_calc (
      pidm_in,
      lv_levl_code,
      term_in,
      atmp_hrs,
      sgrclsr_clas_code,
      clas_desc
   );

--
-- registration term variables.
-- =======================================================================
   FOR sfbetrm IN sfkcurs.sfbetrmc (pidm_in, term_in)
   LOOP
      sfbetrm_row := sfbetrm;
   END LOOP;
END p_local_init_final_update_vars;



PROCEDURE p_local_problems (
   term_in            IN   OWA_UTIL.ident_arr,
   err_term           IN   OWA_UTIL.ident_arr,
   err_crn            IN   OWA_UTIL.ident_arr,
   err_subj           IN   OWA_UTIL.ident_arr,
   err_crse           IN   OWA_UTIL.ident_arr,
   err_sec            IN   OWA_UTIL.ident_arr,
   err_code           IN   OWA_UTIL.ident_arr,
   err_levl           IN   OWA_UTIL.ident_arr,
   err_cred           IN   OWA_UTIL.ident_arr,
   err_gmod           IN   OWA_UTIL.ident_arr,
   drop_problems_in   IN   sfkcurs.drop_problems_rec_tabtype,
   drop_failures_in   IN   sfkcurs.drop_problems_rec_tabtype,
   add_row                 NUMBER,
   call_procedure     IN   VARCHAR2,
   orig_crn           IN    OWA_UTIL.ident_arr,
   orig_rsts          IN    OWA_UTIL.ident_arr
)
IS
   unique_drop        BOOLEAN      := FALSE;
   autodrop_setting   VARCHAR2 (1);
   drop_msg           VARCHAR2 (15) := NULL;
BEGIN

--
-- Initialize genpidm based on faculty/student.
-- =================================================

   IF NOT twbkwbis.f_validuser (global_pidm)
   THEN
      RETURN;
   END IF;

   IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
   THEN
      genpidm :=
          TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
   ELSE
      genpidm := global_pidm;
   END IF;

   IF drop_problems_in.EXISTS (1)
   THEN
      autodrop_setting := sfkfunc.f_autodrop_setting;

--
-- Autodrop rule set to 'Y' - problems list related drops are
-- automatically dropped.
-- ======================================================================
      IF autodrop_setting = 'Y'
      THEN
         bwcklibs.p_confirm_drops (genpidm, term_in, drop_problems_in);
         drop_msg := 'DROPCONFIRMED';
--
-- Autodrop rule set to 'C' - The user can confirm/reject problems list
-- related drops.
-- ======================================================================
      ELSIF autodrop_setting = 'C'
      THEN
         bwckcoms.p_disp_confirm_drops (
            term_in,
            err_term,
            err_crn,
            err_subj,
            err_crse,
            err_sec,
            err_code,
            err_levl,
            err_cred,
            err_gmod,
            drop_problems_in,
            drop_failures_in
         );
         RETURN;
--
-- Autodrop rule set to 'N' - problems list related drops cannot be
-- dropped.
-- ======================================================================
      ELSE
         bwcklibs.p_reset_drops (genpidm, drop_problems_in);
         drop_msg := 'DROPPROHIBITED';
      END IF;
   END IF;


--
-- Failures list (problems that can't be given a drop code) courses
-- changes are rejected.
-- ======================================================================
   --C3SC GR 06/21/2006  ADDED
   IF sv_auth_addcodes_bp.f_is_auth_enabled(term_in(1))
    AND call_procedure <> 'bwvkauth.P_ProcAuthCode'
   THEN
     sv_auth_addcodes_bp.P_local_reset_sftregs_fields(genpidm, drop_failures_in);
   ELSE
     bwcklibs.p_reset_failures (genpidm, drop_failures_in);

   END IF;
  --END C3SC


--
-- Transfer all changes in SFTREGS to SFRSTCR.
-- =====================================================================
   --C3SC GR 06/21/2006 ADDED
  IF sv_auth_addcodes_bp.f_is_auth_enabled(term_in(1))
     AND call_procedure <> 'bwvkauth.P_ProcAuthCode'
   THEN
    sv_auth_addcodes_bp.p_local_final_updates(
      term_in,
      err_term,
      err_crn,
      err_subj,
      err_crse,
      err_sec,
      err_code,
      err_levl,
      err_cred,
      err_gmod,
      drop_msg,
      drop_problems_in,
      drop_failures_in,
      add_row,
      orig_crn   ,
      orig_rsts
      );

   ELSIF sv_auth_addcodes_bp.f_is_auth_enabled(term_in(1))
     AND call_procedure = 'bwvkauth.P_ProcAuthCode' THEN


      bwvkauth.p_final_upadates2(
      term_in,
      err_term,
      err_crn,
      err_subj,
      err_crse,
      err_sec,
      err_code,
      err_levl,
      err_cred,
      err_gmod,
      drop_msg,
      drop_problems_in,
      drop_failures_in,
      add_row
      );


   ELSE


      bwckregs.p_final_updates (
        term_in,
        err_term,
        err_crn,
        err_subj,
        err_crse,
        err_sec,
        err_code,
        err_levl,
        err_cred,
        err_gmod,
        drop_msg,
        drop_problems_in,
        drop_failures_in
     );


   END IF;
   RETURN;
END p_local_problems;

END SV_AUTH_ADDCODES_BP;
