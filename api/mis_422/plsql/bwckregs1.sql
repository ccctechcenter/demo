create or replace PACKAGE BODY bwckregs
AS
--AUDIT_TRAIL_MSGKEY_UPDATE
-- PROJECT : MSGKEY
-- MODULE  : BWCKREG1
-- SOURCE  : enUS
-- TARGET  : I18N
-- DATE    : Tue Mar 12 12:23:15 2013
-- MSGSIGN : #abfd03cc839f5f86
--TMI18N.ETR DO NOT CHANGE--
--
-- FILE NAME..: bwckreg1.sql
-- RELEASE....: 8.5.3 - 8.7.1.1 C3SC
-- OBJECT NAME: BWCKREGS
-- PRODUCT....: SCOMWEB
-- COPYRIGHT..: Copyright 1996 - 2014 Ellucian Company L.P. and its affiliates.
--
   /* variables visible only in this package or
      any items which must be maintined throughout
      a session or across transactions */
   global_pidm             spriden.spriden_pidm%TYPE;
   term                    stvterm.stvterm_code%TYPE;
   id1                     spriden.spriden_id%TYPE;
   regs_date               DATE                                 := SYSDATE;
   hold_passwd             VARCHAR2 (3);
   samsys                  VARCHAR2 (1);
   row_count               NUMBER                               := 0;
   ssbsect_count           NUMBER                               := 0;
   scrcrse_count           NUMBER                               := 0;
   sql_error               NUMBER                               := 0;
   sobterm_row             sobterm%ROWTYPE;
   sfbetrm_row             sfbetrm%ROWTYPE;
   sgbstdn_row             sgbstdn%ROWTYPE;
   sftregs_row             sftregs%ROWTYPE;
   ssbsect_row             ssbsect%ROWTYPE;
   scbcrse_row             scbcrse%ROWTYPE;
   stvrsts_row             stvrsts%ROWTYPE;
   old_sftregs_row         sftregs%ROWTYPE;
   old_stvrsts_row         stvrsts%ROWTYPE;
   sftregs_wait_over       VARCHAR2 (1);
   tot_cred_hrs            SFTREGS.SFTREGS_CREDIT_HR%TYPE;
   tot_bill_hrs            SFTREGS.SFTREGS_BILL_HR%TYPE;
   tot_ceu_hrs             SFTREGS.SFTREGS_CREDIT_HR%TYPE;
   astd_prevent_reg_ind    stvastd.stvastd_prevent_reg_ind%TYPE;
   cast_prevent_reg_ind    stvcast.stvcast_prevent_reg_ind%TYPE;
   ests_prev_reg           stvests.stvests_prev_reg%TYPE;
   stst_reg_ind            stvstst.stvstst_reg_ind%TYPE;
   ests_eff_crse_stat      stvests.stvests_eff_crse_stat%TYPE;
   sgrclsr_clas_code       stvclas.stvclas_code%TYPE;
   clas_desc               stvclas.stvclas_desc%TYPE;
   mhrs_min_hrs            sfrmhrs.sfrmhrs_min_hrs%TYPE;
   mhrs_max_hrs            sfrmhrs.sfrmhrs_max_hrs%TYPE;
   minh_srce               sfbetrm.sfbetrm_minh_srce_cde%TYPE;
   maxh_srce               sfbetrm.sfbetrm_maxh_srce_cde%TYPE;
   appr_error              VARCHAR2 (1);
   message_text            VARCHAR2 (60);
   xlst_group              ssrxlst.ssrxlst_xlst_group%TYPE;
   xlst_seats_avail        ssbxlst.ssbxlst_seats_avail%TYPE;
   ssrresv_seats_avail     ssrresv.ssrresv_seats_avail%TYPE;
   ssrresv_rowid           ROWID;
   ssrresv_wait_avail      ssrresv.ssrresv_wait_avail%TYPE;
   ssrresv_wait_count      ssrresv.ssrresv_wait_count%TYPE;
   ssrresv_wait_capacity   ssrresv.ssrresv_wait_capacity%TYPE;
   ssbsect_rowid           ROWID;
   ssbxlst_rowid           ROWID;
   override                VARCHAR2 (1);
   print_bill              VARCHAR2 (1);
   advr_row                soklibs.advr_rec;
-- The following variable have been added for Web Student Registration
-- to use existing VR Registration Management Controls

   reg_allowed             VARCHAR2 (1);
   contin_check            VARCHAR2 (1);
   next_cont_rec           VARCHAR2 (1);
   apin                    sprapin.sprapin_pin%TYPE;
   pin                     gobtpac.gobtpac_pin%TYPE;
   sfrctrl_rec             sfrctrl%ROWTYPE;
   stvterm_rec             stvterm%ROWTYPE;
   sorrtrm_rec             sorrtrm%ROWTYPE;
   gobtpac_rec             gobtpac%ROWTYPE;
   sprapin_rec             sprapin%ROWTYPE;
   curr_time               VARCHAR2 (4);
   curr_date               DATE;
   student_name            VARCHAR2 (185);
   stud_type               sgbstdn.sgbstdn_styp_code%TYPE;
   stud_level              sorlcur.sorlcur_levl_code%TYPE;
   stud_class              sgrclsr.sgrclsr_clas_code%TYPE;
-- Begin Defect #38686
   stud_earned_hrs_class   sgrclsr.sgrclsr_clas_code%TYPE;
   sobterm_rec             sobterm%ROWTYPE;
   atmp_hrs                VARCHAR2 (1);
-- End Defect #38686
   stud_major              sorlfos.sorlfos_majr_code%TYPE;
   stud_college            sorlcur.sorlcur_coll_code%TYPE;
   stud_dept               sorlfos.sorlfos_dept_code%TYPE;
   stud_degr               sorlcur.sorlcur_degc_code%TYPE;
   stud_camp               sorlcur.sorlcur_camp_code%TYPE;
   stud_hrs                shrlgpa.shrlgpa_hours_earned%TYPE;
   lcur_tab                sokccur.curriculum_savedtab;

-- The following cursors have been added for Web Student Registration
-- to use existing VR Registration Management Controls

   CURSOR sprapinc (pidm_in IN VARCHAR2, term IN stvterm.stvterm_code%TYPE) RETURN sprapin%ROWTYPE
   IS
      SELECT *
        FROM sprapin
       WHERE sprapin_pidm = pidm_in
         AND sprapin_term_code = term
         AND sprapin_process_name = 'TREG';

   CURSOR getsfrctrlc (term IN stvterm.stvterm_code%TYPE)
   IS
      SELECT *
        FROM sfrctrl
       WHERE sfrctrl_term_code_host = term
       ORDER BY sfrctrl_seq_no;

   CURSOR getearnedhrsc (pidm_in NUMBER,
                         stud_level sorlcur.sorlcur_levl_code%TYPE)
   IS
      SELECT NVL (shrlgpa_hours_earned, 0)
        FROM shrlgpa
       WHERE shrlgpa_pidm (+) = pidm_in
         AND shrlgpa_levl_code (+) = stud_level
         AND shrlgpa_gpa_type_ind (+) = 'O';

   CURSOR getstudentclasc (stud_hrs NUMBER,
                           stud_level sorlcur.sorlcur_levl_code%TYPE)
   IS
      SELECT *
        FROM sgrclsr
       WHERE sgrclsr_levl_code = stud_level
         AND (
                    (sgrclsr_from_hours <= stud_hrs)
                AND (sgrclsr_to_hours >= stud_hrs)
             );

   CURSOR getstudentnamec (pidm_in NUMBER)
   IS
      SELECT spriden_last_name || ', ' || spriden_first_name || ' ' || spriden_mi
        FROM spriden
       WHERE spriden_pidm = pidm_in
         AND spriden_change_ind IS NULL;

   CURSOR get_gtvsdax                                         -- defect #59523
   IS
      SELECT gtvsdax_external_code
        FROM gtvsdax
       WHERE gtvsdax_internal_code = 'FABASEMOD'
         AND gtvsdax_internal_code_group = 'FEE ASSESSMENT';

   --

   /* Fully define cursor specified in package if any */

   CURSOR overridec (
      pidm_in   NUMBER,
      term      VARCHAR2,
      crn       VARCHAR2,
      subj      VARCHAR2,
      crse      VARCHAR2,
      seq       VARCHAR2
   )
   IS
      SELECT *
        FROM sfrrovr, sfrsrpo
       WHERE sfrsrpo_term_code = term
         AND sfrsrpo_pidm = pidm_in
         AND sfrsrpo_subj_code = subj
         AND sfrsrpo_crse_numb = crse
         AND NVL (sfrsrpo_crn, crn) = crn
         AND sfrrovr_term_code = sfrsrpo_term_code
         AND sfrrovr_rovr_code = sfrsrpo_rovr_code;

   --

   /* Any forward declarations needed for the subprograms */

   /* Fully define subprograms specified in package */

   FUNCTION f_getstuclas
      RETURN stvclas.stvclas_code%TYPE
   IS
   BEGIN
      RETURN sgrclsr_clas_code;
   END f_getstuclas;

   FUNCTION f_stuhld (
      sql_err        OUT   NUMBER,
      pidm_in              NUMBER DEFAULT NULL,
      regs_date_in         DATE DEFAULT NULL,
      hold_serv            CHAR DEFAULT NULL,
      hold_pass            CHAR DEFAULT NULL
   )
      RETURN BOOLEAN
   IS
      hold_ind   VARCHAR2 (1);
      genpidm    spriden.spriden_pidm%TYPE;

      CURSOR hold_ind_c (pidm_in      spriden.spriden_pidm%TYPE,
                         regs_date_in DATE)
      IS
      SELECT DISTINCT 'Y'
        FROM stvhldd
       WHERE stvhldd_reg_hold_ind = 'Y'
         AND stvhldd_code IN (SELECT sprhold_hldd_code
                                FROM sprhold
                               WHERE (
                                            TRUNC (regs_date_in) >=
                                                     TRUNC (sprhold_from_date)
                                        AND TRUNC (regs_date_in) <
                                                       TRUNC (sprhold_to_date)
                                     )
                                 AND sprhold_pidm = pidm_in);

   BEGIN
      IF NVL (twbkwbis.f_getparam (pidm_in, 'STUFAC_IND'), 'STU') = 'FAC'
      THEN
         genpidm :=
              TO_NUMBER (twbkwbis.f_getparam (pidm_in, 'STUPIDM'), '999999999');
      ELSE
         genpidm := pidm_in;
      END IF;

      -- IF PIDM_IN IS NOT NULL THEN
      --    GENPIDM := PIDM_IN;
      -- END IF;
      IF regs_date_in IS NOT NULL
      THEN
         regs_date := regs_date_in;
      END IF;

      IF hold_serv IS NOT NULL
      THEN
         sobterm_row.sobterm_hold_severity := hold_serv;
      END IF;

      IF hold_pass IS NOT NULL
      THEN
         sobterm_row.sobterm_hold := hold_pass;
      END IF;

      OPEN hold_ind_c (genpidm, regs_date);
      FETCH hold_ind_c INTO hold_ind;
      IF hold_ind_c%notfound
      THEN
          CLOSE hold_ind_c;
          RETURN FALSE;
      END IF;
      CLOSE hold_ind_c;

      IF sobterm_row.sobterm_hold_severity = 'F'
      THEN
         IF    (hold_passwd IS NULL)
            OR (hold_passwd <> NVL (sobterm_row.sobterm_hold, 'OVR'))
         THEN
            sql_err := -20504;
            RETURN TRUE;
         END IF;
      END IF;

      RETURN FALSE;
   END f_stuhld;

   FUNCTION f_validacpt (sql_err OUT NUMBER, ar CHAR DEFAULT NULL)
      RETURN BOOLEAN
   IS
   BEGIN
      IF ar IS NOT NULL
      THEN
         sfbetrm_row.sfbetrm_ar_ind := ar;
      END IF;

      IF sfbetrm_row.sfbetrm_ar_ind NOT IN ('C', 'N')
      THEN
         sql_err := -20530;
         RETURN FALSE;
      END IF;

      RETURN TRUE;
   END f_validacpt;

   FUNCTION f_validappr (sql_err OUT NUMBER, appr_ind CHAR DEFAULT NULL)
      RETURN BOOLEAN
   IS
   BEGIN
      IF appr_ind IS NOT NULL
      THEN
         sftregs_row.sftregs_appr_received_ind := appr_ind;
      END IF;

      IF NVL (sftregs_row.sftregs_appr_received_ind, 'N') NOT IN ('Y', 'N')
      THEN
         sql_err := -20001;
         RETURN FALSE;
      END IF;

      IF sftregs_row.sftregs_appr_received_ind = 'Y'
      THEN
         IF appr_error = 'Y'
         THEN
            appr_error := '';
            sftregs_row.sftregs_error_flag := '';
         END IF;
      END IF;

      RETURN TRUE;
   END f_validappr;

   FUNCTION f_validbillhr (
      sql_err   OUT   NUMBER,
      bill_hr         SFTREGS.SFTREGS_BILL_HR%TYPE DEFAULT NULL
   )
      RETURN BOOLEAN
   IS
   BEGIN
      IF bill_hr IS NOT NULL
      THEN
         sftregs_row.sftregs_bill_hr := bill_hr;
      END IF;
      IF    (
                   sftregs_row.sftregs_bill_hr = 00.00
               AND stvrsts_row.stvrsts_incl_sect_enrl = 'N'
            )
         OR (stvrsts_row.stvrsts_incl_assess = 'N')
      THEN
         RETURN TRUE;
      END IF;

      IF scbcrse_row.scbcrse_bill_hr_ind = 'TO'
      THEN
         IF (
                  (
                         sftregs_row.sftregs_bill_hr BETWEEN scbcrse_row.scbcrse_bill_hr_low
                             AND scbcrse_row.scbcrse_bill_hr_high
                     AND ssbsect_row.ssbsect_bill_hrs IS NULL
                  )
               OR ssbsect_row.ssbsect_bill_hrs = sftregs_row.sftregs_bill_hr
            )
         THEN
            RETURN TRUE;
         END IF;
      ELSE
         IF (
                  (
                         sftregs_row.sftregs_bill_hr IS NOT NULL
                     AND (
                               sftregs_row.sftregs_bill_hr =
                                               scbcrse_row.scbcrse_bill_hr_low
                            OR sftregs_row.sftregs_bill_hr =
                                              scbcrse_row.scbcrse_bill_hr_high
                         )
                     AND ssbsect_row.ssbsect_bill_hrs IS NULL
                  )
               OR ssbsect_row.ssbsect_bill_hrs = sftregs_row.sftregs_bill_hr
            )
         THEN
            RETURN TRUE;
         END IF;
      END IF;

      sql_err := -20538;
      RETURN FALSE;
   END f_validbillhr;

   FUNCTION f_validcredhr (
      sql_err   OUT   NUMBER,
      cred_hr         SFTREGS.SFTREGS_CREDIT_HR%TYPE DEFAULT NULL
   )
      RETURN BOOLEAN
   IS
   BEGIN
      IF cred_hr IS NOT NULL
      THEN
         sftregs_row.sftregs_credit_hr := cred_hr;
      END IF;

      IF     sftregs_row.sftregs_credit_hr = 00.00
         AND stvrsts_row.stvrsts_incl_sect_enrl = 'N'
      THEN
         RETURN TRUE;
      END IF;

      IF scbcrse_row.scbcrse_credit_hr_ind = 'TO'
      THEN
         IF (
                  (
                         sftregs_row.sftregs_credit_hr BETWEEN scbcrse_row.scbcrse_credit_hr_low
                             AND scbcrse_row.scbcrse_credit_hr_high
                     AND ssbsect_row.ssbsect_credit_hrs IS NULL
                  )
               OR ssbsect_row.ssbsect_credit_hrs =
                                                 sftregs_row.sftregs_credit_hr
            )
         THEN
            RETURN TRUE;
         END IF;
      ELSE
         IF (
                  (
                         sftregs_row.sftregs_credit_hr IS NOT NULL
                     AND (
                               sftregs_row.sftregs_credit_hr =
                                             scbcrse_row.scbcrse_credit_hr_low
                            OR sftregs_row.sftregs_credit_hr =
                                            scbcrse_row.scbcrse_credit_hr_high
                         )
                     AND ssbsect_row.ssbsect_credit_hrs IS NULL
                  )
               OR ssbsect_row.ssbsect_credit_hrs =
                                                 sftregs_row.sftregs_credit_hr
            )
         THEN
            RETURN TRUE;
         END IF;
      END IF;

      sql_err := -20537;
      RETURN FALSE;
   END f_validcredhr;

-- ================================================================
-- This procedure gets section information for a given crn.
-- ================================================================
   PROCEDURE p_getsection (
      term_in         sftregs.sftregs_term_code%TYPE DEFAULT NULL,
      crn             sftregs.sftregs_crn%TYPE DEFAULT NULL,
      subj      OUT   ssbsect.ssbsect_subj_code%TYPE,
      crse      OUT   ssbsect.ssbsect_crse_numb%TYPE,
      seq       OUT   ssbsect.ssbsect_seq_numb%TYPE
   )
   IS
   BEGIN
--
-- Open the section cursor; get the section row;
-- set the output parameters.
-- ======================================================
      OPEN ssklibs.ssbsectc (crn, term_in);
      FETCH ssklibs.ssbsectc INTO ssbsect_row;

      IF ssklibs.ssbsectc%FOUND
      THEN
         subj := ssbsect_row.ssbsect_subj_code;
         crse := ssbsect_row.ssbsect_crse_numb;
         seq := ssbsect_row.ssbsect_seq_numb;
      ELSE
         subj := NULL;
         crse := NULL;
         seq := NULL;
      END IF;

      CLOSE ssklibs.ssbsectc;
   END p_getsection;

   PROCEDURE p_capcchk
   IS
      temp   NUMBER;
   BEGIN
      temp := 1;
   END;

   FUNCTION f_validcrn (
      sql_err   OUT      NUMBER,
      term_in   IN       sftregs.sftregs_term_code%TYPE,
      crn                sftregs.sftregs_crn%TYPE DEFAULT NULL,
      subj               ssbsect.ssbsect_subj_code%TYPE DEFAULT NULL,
      crse               ssbsect.ssbsect_crse_numb%TYPE DEFAULT NULL,
      seq                ssbsect.ssbsect_seq_numb%TYPE DEFAULT NULL
   )
      RETURN BOOLEAN
   IS
      stcr_crn   sftregs.sftregs_crn%TYPE;

      CURSOR stcr_crn_c(term_in sftregs.sftregs_term_code%TYPE,
                        crn_in  sftregs.sftregs_crn%TYPE)
      IS
         SELECT MIN (ssbsect_crn)
           FROM ssbsect
          WHERE UPPER (ssbsect_subj_code) =
                                         UPPER (ssbsect_row.ssbsect_subj_code)
            AND ssbsect_crse_numb = ssbsect_row.ssbsect_crse_numb
            AND ssbsect_seq_numb = ssbsect_row.ssbsect_seq_numb
            AND ssbsect_crn LIKE NVL (crn_in, '%')
            AND ssbsect_term_code = term_in;

   BEGIN
--
-- Voodoo
-- ===========================================================
      IF crn IS NOT NULL
      THEN
         sftregs_row.sftregs_crn := crn;
      END IF;

      IF subj IS NOT NULL
      THEN
         ssbsect_row.ssbsect_subj_code := subj;
      END IF;

      IF crse IS NOT NULL
      THEN
         ssbsect_row.ssbsect_crse_numb := crse;
      END IF;

      IF seq IS NOT NULL
      THEN
         ssbsect_row.ssbsect_seq_numb := seq;
      END IF;

--
--
-- ===========================================================
      IF     ssbsect_row.ssbsect_subj_code IS NOT NULL
         AND ssbsect_row.ssbsect_crse_numb IS NOT NULL
         AND ssbsect_row.ssbsect_seq_numb IS NOT NULL
      THEN
--
-- Check section records for subject,course,section,crn,term.
-- ===========================================================
         OPEN stcr_crn_c(term_in, sftregs_row.sftregs_crn);
         FETCH stcr_crn_c INTO stcr_crn;
         CLOSE stcr_crn_c;

--
-- If section not found, decide which condition was the cause:
-- 1. Invalid subject, course, section
-- 2. Invalid subject, course, section, crn
-- ...then set sql_err and return false.
-- ===========================================================
         IF stcr_crn IS NULL
         THEN
            IF sftregs_row.sftregs_crn IS NULL
            THEN
               sql_err := -20514;
               RETURN FALSE;
            ELSE
               sql_err := -20515;
               RETURN FALSE;
            END IF;
         ELSE
            IF stcr_crn <> NVL (sftregs_row.sftregs_crn, stcr_crn)
            THEN
               sql_err := -20515;
               RETURN FALSE;
            ELSE
               sftregs_row.sftregs_crn := stcr_crn;
            END IF;
         END IF;
      END IF;

--
-- Loop through section records.
-- ===========================================================
      OPEN ssklibs.ssbsectc (sftregs_row.sftregs_crn, term_in);
      ssbsect_count := 0;

      LOOP
         FETCH ssklibs.ssbsectc INTO ssbsect_row;
         EXIT WHEN ssklibs.ssbsectc%NOTFOUND;
         ssbsect_count := ssbsect_count + 1;
--
-- Loop through course records.
-- ===========================================================
         scrcrse_count := 0;
         OPEN scklibs.scbcrsec (
            ssbsect_row.ssbsect_subj_code,
            ssbsect_row.ssbsect_crse_numb,
            term_in
         );

         LOOP
            FETCH scklibs.scbcrsec INTO scbcrse_row;
            EXIT WHEN scklibs.scbcrsec%NOTFOUND;
            scrcrse_count := scrcrse_count + 1;

            IF scbcrse_row.scbcrse_repeat_limit IS NULL
            THEN
               scbcrse_row.scbcrse_repeat_limit := 99;
            END IF;

            IF scbcrse_row.scbcrse_max_rpt_units IS NULL
            THEN
               scbcrse_row.scbcrse_max_rpt_units := 9999.999;
            END IF;
         END LOOP;

         row_count := scklibs.scbcrsec%rowcount;
         CLOSE scklibs.scbcrsec;

--
-- If one, and only one, course record was not found, set
-- sql_err and return false.
-- ===========================================================
         IF scrcrse_count <> 1
         THEN
            CLOSE ssklibs.ssbsectc;
            sql_err := -20508;
            RETURN FALSE;
         END IF;

         IF ssbsect_row.ssbsect_wait_capacity IS NULL
         THEN
            ssbsect_row.ssbsect_wait_capacity := 0;
         END IF;
      END LOOP;

      row_count := ssklibs.ssbsectc%rowcount;
      CLOSE ssklibs.ssbsectc;

--
-- If one, and only one, section record was not found, set
-- sql_err and return false.
-- ===========================================================
      IF ssbsect_count <> 1
      THEN
         sql_err := -20508;
         RETURN FALSE;
      END IF;

      row_count := 0;

--
-- Check if the section allows web/vr registration.
-- ===========================================================
      IF ssbsect_row.ssbsect_voice_avail = 'Y' OR
         NVL(SUBSTR(bwcklibs.f_getgtvsdaxrule('CRNDIRECT','WEBREG'),1,1),'N') = 'Y'
      THEN
         NULL;
      ELSE
         sql_err := -20544;
         RETURN FALSE;
      END IF;

      RETURN TRUE;
   END f_validcrn;

   FUNCTION f_validenrl (
      sql_err   OUT      NUMBER,
      term_in   IN       sftregs.sftregs_term_code%TYPE,
      ests               sfbetrm.sfbetrm_ests_code%TYPE DEFAULT NULL
   )
      RETURN BOOLEAN
   IS
      ests_flag   VARCHAR2 (1);

      CURSOR ests_flag_c (term_in   sftregs.sftregs_term_code%TYPE,
                          regs_date_in DATE)
      IS
      SELECT 'X'
        FROM sfbests
       WHERE TRUNC (regs_date_in) BETWEEN TRUNC (sfbests_start_date)
                 AND TRUNC (sfbests_end_date)
         AND sfbests_ests_code = sfbetrm_row.sfbetrm_ests_code
         AND sfbests_term_code = term_in;

   BEGIN
      IF ests IS NOT NULL
      THEN
         sfbetrm_row.sfbetrm_ests_code := ests;
      END IF;

      row_count := 0;

      FOR stvests IN stkests.stvestsc (sfbetrm_row.sfbetrm_ests_code)
      LOOP
         ests_eff_crse_stat := stvests.stvests_eff_crse_stat;
         ests_prev_reg := stvests.stvests_prev_reg;
         row_count := stkests.stvestsc%rowcount;
      END LOOP;

      IF row_count <> 1
      THEN
         sql_err := -20503;
         RETURN FALSE;
      END IF;

      regs_date := bwcklibs.f_getregdate;

      OPEN ests_flag_c(term_in, regs_date);
      FETCH ests_flag_c INTO ests_flag;
      IF ests_flag_c%notfound
      THEN
          sql_err := -20503;
          CLOSE ests_flag_c;
          RETURN FALSE;
      END IF;
      CLOSE ests_flag_c;

      RETURN TRUE;
   EXCEPTION
      WHEN OTHERS
      THEN
         sql_err := -20503;
         RETURN FALSE;
   END f_validenrl;

   FUNCTION f_validgmod (
      sql_err     OUT   NUMBER,
      term_in           sftregs.sftregs_term_code%TYPE,
      gmod              sftregs.sftregs_gmod_code%TYPE DEFAULT NULL,
      subj              ssbsect.ssbsect_subj_code%TYPE DEFAULT NULL,
      crse              ssbsect.ssbsect_crse_numb%TYPE DEFAULT NULL,
      sect_gmod         ssbsect.ssbsect_gmod_code%TYPE DEFAULT NULL
   )
      RETURN BOOLEAN
   IS
   BEGIN
      IF gmod IS NOT NULL
      THEN
         sftregs_row.sftregs_gmod_code := gmod;
      END IF;

      IF subj IS NOT NULL
      THEN
         ssbsect_row.ssbsect_subj_code := subj;
      END IF;

      IF crse IS NOT NULL
      THEN
         ssbsect_row.ssbsect_crse_numb := crse;
      END IF;

      IF sect_gmod IS NOT NULL
      THEN
         ssbsect_row.ssbsect_gmod_code := sect_gmod;
      END IF;

      row_count := 0;

      FOR scrgmod IN scklibs.scrgmodc (
                        ssbsect_row.ssbsect_subj_code,
                        ssbsect_row.ssbsect_crse_numb,
                        term_in
                     )
      LOOP
         IF sftregs_row.sftregs_gmod_code IS NULL
         THEN
            IF scrgmod.scrgmod_default_ind = 'D'
            THEN
               sftregs_row.sftregs_gmod_code := scrgmod.scrgmod_gmod_code;
               row_count := scklibs.scrgmodc%rowcount;
               EXIT;
            END IF;
         ELSE
            IF     sftregs_row.sftregs_gmod_code = scrgmod.scrgmod_gmod_code
               AND NVL (
                      ssbsect_row.ssbsect_gmod_code,
                      sftregs_row.sftregs_gmod_code
                   ) = sftregs_row.sftregs_gmod_code
            THEN
               row_count := scklibs.scrgmodc%rowcount;
               EXIT;
            END IF;
         END IF;
      END LOOP;

      IF row_count = 0
      THEN
         sql_err := -20536;
         RETURN FALSE;
      END IF;

      RETURN TRUE;
   EXCEPTION
      WHEN OTHERS
      THEN
         sql_err := -20536;
         RETURN FALSE;
   END f_validgmod;

   FUNCTION f_validlevl (
      sql_err   OUT   NUMBER,
      term_in         sftregs.sftregs_term_code%TYPE,
      levl            sftregs.sftregs_levl_code%TYPE DEFAULT NULL
   )
      RETURN BOOLEAN
   IS
      genpidm   spriden.spriden_pidm%TYPE;
      CURSOR scrlevl_c(term_in   sftregs.sftregs_term_code%TYPE,
                        subj_code_in ssbsect.ssbsect_subj_code%TYPE,
                        crse_numb_in ssbsect.ssbsect_crse_numb%TYPE)
      IS
            SELECT MIN (scrlevl_levl_code)
              FROM scrlevl
             WHERE scrlevl_subj_code = subj_code_in
               AND scrlevl_crse_numb = crse_numb_in
               AND scrlevl_eff_term =
                    (SELECT MAX (scrlevl_eff_term)
                       FROM scrlevl
                      WHERE scrlevl_subj_code = subj_code_in
                        AND scrlevl_crse_numb = crse_numb_in
                        AND scrlevl_eff_term <= term_in)
            HAVING COUNT (*) = 1;

      CURSOR sovccur_levl_c(p_pidm  sorlcur.sorlcur_pidm%TYPE,
                            p_term  sorlcur.sorlcur_term_code%TYPE)
      IS
        SELECT sovccur_levl_code
          FROM sovccur
         WHERE sovccur_pidm = p_pidm
           AND sovccur_term_code <= p_term
           AND sovccur_order > 0
           AND sovccur_lmod_code = sb_curriculum_str.f_learner
      ORDER BY sovccur_priority_no;

   BEGIN
      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN FALSE;
      END IF;

      IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
      THEN
         genpidm :=
           TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
      ELSE
         genpidm := global_pidm;
      END IF;
      IF levl IS NOT NULL
      THEN
         sftregs_row.sftregs_levl_code := levl;
      END IF;

      IF sftregs_row.sftregs_levl_code IS NULL
      THEN
         row_count := 0;
         OPEN scrlevl_c(term_in,
                        ssbsect_row.ssbsect_subj_code,
                        ssbsect_row.ssbsect_crse_numb);
         FETCH scrlevl_c INTO sftregs_row.sftregs_levl_code;
         IF scrlevl_c%notfound
         THEN
--
-- Loop through student's current/active curriculum in priority order.
-- The first curriculum level that matches one of the valid course
-- levels is the one that will be used for the registration.
--
            soklcur.p_create_sotvcur(p_pidm      => genpidm,
                                     p_term_code => term_in,
                                     p_lmod_code => sb_curriculum_str.f_learner);
            OPEN sovccur_levl_c(genpidm,
                                term_in);
            LOOP
              FETCH sovccur_levl_c INTO stud_level;
              EXIT WHEN (sovccur_levl_c%NOTFOUND) OR (row_count > 0);
              FOR scrlevl IN scklibs.scrlevlc (
                                               ssbsect_row.ssbsect_subj_code,
                                               ssbsect_row.ssbsect_crse_numb,
                                               term_in,
                                               stud_level
                                              )
                LOOP
                  row_count := scklibs.scrlevlc%rowcount;
                END LOOP;
                sftregs_row.sftregs_levl_code := stud_level;
            END LOOP;
            CLOSE sovccur_levl_c;

            IF row_count = 0
            THEN
              sql_err := -20532;
              RETURN FALSE;
            END IF;
         END IF;
         CLOSE scrlevl_c;
      END IF;

      RETURN TRUE;
   END f_validlevl;

   FUNCTION f_validrgre (
      sql_err   OUT   NUMBER,
      rgre            stvrgre.stvrgre_code%TYPE DEFAULT NULL
   )
      RETURN BOOLEAN
   IS
   BEGIN
      IF rgre IS NOT NULL
      THEN
         sfbetrm_row.sfbetrm_rgre_code := rgre;
      END IF;

      row_count := 0;

      FOR stvrgre IN stkrgre.stvrgrec (sfbetrm_row.sfbetrm_rgre_code)
      LOOP
         row_count := stkrgre.stvrgrec%rowcount;
      END LOOP;

      IF row_count <> 1
      THEN
         sql_err := -20531;
         RETURN FALSE;
      END IF;

      RETURN TRUE;
   END f_validrgre;

   FUNCTION f_validrsts (
      sql_err   OUT   NUMBER,
      rsts            stvrsts.stvrsts_code%TYPE DEFAULT NULL
   )
      RETURN BOOLEAN
   IS
   BEGIN
      IF rsts IS NOT NULL
      THEN
         sftregs_row.sftregs_rsts_code := rsts;
      END IF;

      row_count := 0;
      regs_date := bwcklibs.f_getregdate;

-- Defect 10737
-- Implicit cursor for STVRSTS
-- ===============================================================
      FOR stvrsts IN stkrsts.stvrstsc (sftregs_row.sftregs_rsts_code)
      LOOP
         IF    stvrsts.stvrsts_enterable_ind = 'Y'
            OR (
                      stvrsts.stvrsts_enterable_ind = 'N'
                  AND stvrsts.stvrsts_code = sfbetrm_row.sfbetrm_ests_code
               )
         THEN
            stvrsts_row := stvrsts;
            row_count := stkrsts.stvrstsc%rowcount;
         END IF;
      END LOOP;

--

      /* local declaration of stvrstsc */
/*
   DECLARE cursor stvrstsc is
     select *
     from stvrsts
     where stvrsts_code = sfrstcr_row.sfrstcr_rsts_code
     and stvrsts_enterable_ind = 'Y'
     or  (stvrsts_enterable_ind = 'N'
       and stvrsts_code = sfbetrm_row.sfbetrm_ests_code);

   begin
       open stvrstsc;
       fetch stvrstsc into stvrsts_row;
       if stvrstsc%notfound then
         row_count := 0;
       else
         row_count := row_count + 1 ;
       end if ;
    end ;
*/
      IF row_count = 0
      THEN
         sql_err := -20516;
         RETURN FALSE;
      END IF;

      IF     stvrsts_row.stvrsts_wait_ind = 'Y'
         AND ssbsect_row.ssbsect_wait_capacity = '0'
      THEN
         sql_err := -20517;
         RETURN FALSE;
      END IF;

      sftregs_row.sftregs_rsts_date := regs_date;

      IF NVL (ssbsect_row.ssbsect_gradable_ind, 'N') <> 'N'
      THEN
         sftregs_row.sftregs_grde_code := stvrsts_row.stvrsts_auto_grade;
      END IF;

      row_count := 0;

      IF sftregs_row.sftregs_ptrm_code IS NOT NULL
      THEN
         FOR sfrrsts IN sfkcurs.sfrrstsc (
                           sftregs_row.sftregs_rsts_code,
                           sftregs_row.sftregs_term_code,
                           sftregs_row.sftregs_ptrm_code
                        )
         LOOP
            IF     TRUNC (sftregs_row.sftregs_rsts_date) >=
                                                    sfrrsts.sfrrsts_start_date
               AND TRUNC (sftregs_row.sftregs_rsts_date) <=
                                                      sfrrsts.sfrrsts_end_date
            THEN
               row_count := sfkcurs.sfrrstsc%rowcount;
            END IF;
         END LOOP;

         IF row_count = 0
         THEN
            IF sftregs_row.sftregs_rsts_code =
                                      SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2)
            THEN
               sql_err := -20518;
            ELSE
               sql_err := -20519;
            END IF;

            RETURN FALSE;
         END IF;
      ELSE
         FOR ssrrsts_rec IN sfkcurs.ssrrstsc (
                               sftregs_row.sftregs_rsts_code,
                               sftregs_row.sftregs_term_code,
                               sftregs_row.sftregs_crn
                            )
         LOOP
            IF ( TRUNC (sftregs_row.sftregs_rsts_date) BETWEEN
               ssrrsts_rec.ssbsect_reg_from_date AND ssrrsts_rec.ssbsect_reg_to_date
			   )
           --c3sc GRS 26/02/2006 Added(OL override)
            OR ( sv_auth_addcodes_bp.F_process_with_auth(p_term_code=>sftregs_row.sftregs_term_code,
                                                         p_rsts_code =>sftregs_row.sftregs_rsts_code)
                )
           --END c3sc
            THEN
            /* store row_count only if either start_tdate is current or course isnt being added */
              IF TRUNC (sftregs_row.sftregs_rsts_date) <=
                    ssrrsts_rec.ssbsect_learner_regstart_tdate
                 OR sftregs_row.sftregs_rsts_code <>
                        SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2)
             --c3sc GRS 26/02/2006 Added(OL override)
                OR ( sv_auth_addcodes_bp.F_process_with_auth(p_term_code=>sftregs_row.sftregs_term_code,
                                                              p_rsts_code =>sftregs_row.sftregs_rsts_code)
                    )

             --END c3sc
              THEN
                 row_count := sfkcurs.ssrrstsc%rowcount;
              END IF;
            END IF;
         END LOOP;

         IF row_count = 0
         THEN
            IF sftregs_row.sftregs_rsts_code =
                                      SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2)
            THEN
               sql_err := -20606;
            ELSE
               sql_err := -20607;
            END IF;

            RETURN FALSE;
         END IF;
      END IF;

      IF stvrsts_row.stvrsts_incl_sect_enrl = 'N'
      THEN
         IF NVL (sftregs_row.sftregs_credit_hr_hold, 0) = 0
         THEN
            sftregs_row.sftregs_credit_hr_hold :=
                                                sftregs_row.sftregs_credit_hr;
         END IF;

         sftregs_row.sftregs_credit_hr := 00.00;
         sftregs_row.sftregs_error_flag := 'D';
         sftregs_row.sftregs_dupl_over := 'N';
         sftregs_row.sftregs_link_over := 'N';
         sftregs_row.sftregs_corq_over := 'N';
         sftregs_row.sftregs_preq_over := 'N';
         sftregs_row.sftregs_time_over := 'N';
         sftregs_row.sftregs_capc_over := 'N';
         sftregs_row.sftregs_levl_over := 'N';
         sftregs_row.sftregs_coll_over := 'N';
         sftregs_row.sftregs_degc_over := 'N';
         sftregs_row.sftregs_prog_over := 'N';
         sftregs_row.sftregs_majr_over := 'N';
         sftregs_row.sftregs_clas_over := 'N';
         sftregs_row.sftregs_appr_over := 'N';
         sftregs_wait_over := 'N';
         sftregs_row.sftregs_rept_over := 'N';
         sftregs_row.sftregs_rpth_over := 'N';
         sftregs_row.sftregs_camp_over := 'N';
         sftregs_row.sftregs_dept_over := 'N';
         sftregs_row.sftregs_chrt_over := 'N';
         sftregs_row.sftregs_mexc_over := 'N';
         sftregs_row.sftregs_atts_over := 'N';
      END IF;

      IF     stvrsts_row.stvrsts_wait_ind = 'Y'
         AND stvrsts_row.stvrsts_incl_sect_enrl = 'N'
      THEN
         sftregs_row.sftregs_error_flag := 'L';
      END IF;

      IF stvrsts_row.stvrsts_incl_assess = 'N'
      THEN
         IF NVL (sftregs_row.sftregs_bill_hr_hold, 0) = 0
         THEN
            sftregs_row.sftregs_bill_hr_hold := sftregs_row.sftregs_bill_hr;
         END IF;

         sftregs_row.sftregs_bill_hr := 00.00;
      ELSIF sftregs_row.sftregs_bill_hr IS NULL
      OR (old_stvrsts_row.stvrsts_incl_assess = 'N'
          AND sftregs_row.sftregs_bill_hr = 0)
      THEN
         sftregs_row.sftregs_bill_hr := sftregs_row.sftregs_bill_hr_hold;

         IF NVL (sftregs_row.sftregs_bill_hr, 00.00) = 00.00
         THEN
            sftregs_row.sftregs_bill_hr :=
              NVL (
                 ssbsect_row.ssbsect_bill_hrs,
                 scbcrse_row.scbcrse_bill_hr_low
              );
         END IF;
      END IF;

      IF stvrsts_row.stvrsts_incl_sect_enrl = 'Y'
      THEN
         IF    NVL (sftregs_row.sftregs_error_flag, '#') = 'D'
            OR NVL (sftregs_row.sftregs_error_flag, '#') = 'L'
         THEN
            sftregs_row.sftregs_credit_hr := ssbsect_row.ssbsect_credit_hrs;
            sftregs_row.sftregs_error_flag := '';
         END IF;

         IF    (
                      NVL (sftregs_row.sftregs_error_flag, '#') = 'F'
                  AND old_stvrsts_row.stvrsts_wait_ind = 'Y'
               )
            OR (
                      old_stvrsts_row.stvrsts_incl_sect_enrl = 'N'
                  AND sftregs_row.sftregs_credit_hr = 0
               )
            OR sftregs_row.sftregs_credit_hr is NULL
         THEN
            sftregs_row.sftregs_credit_hr :=
                                           sftregs_row.sftregs_credit_hr_hold;

            IF NVL (sftregs_row.sftregs_credit_hr, 00.00) = 00.00
            THEN
               sftregs_row.sftregs_credit_hr :=
                                            scbcrse_row.scbcrse_credit_hr_low;
            END IF;
         END IF;
      END IF;

      sftregs_row.sftregs_crse_title :=
              NVL (ssbsect_row.ssbsect_crse_title, scbcrse_row.scbcrse_title);
      RETURN TRUE;
   END f_validrsts;

   FUNCTION f_deceasedperscheck (sql_err OUT NUMBER, pidm_in NUMBER)
      RETURN BOOLEAN
   IS
      dead_ind   VARCHAR2 (1);

      CURSOR chk_deceased_ind (pidm_in NUMBER)
      IS
         SELECT NVL (spbpers_dead_ind, 'N')
           FROM spbpers
          WHERE spbpers_pidm = pidm_in;
   BEGIN
      OPEN chk_deceased_ind(pidm_in);
      FETCH chk_deceased_ind INTO dead_ind;

      IF chk_deceased_ind%NOTFOUND
      THEN
         CLOSE chk_deceased_ind;
         RETURN FALSE;
      ELSE
         CLOSE chk_deceased_ind;

         IF dead_ind = 'Y'
         THEN
            RETURN TRUE;
         ELSE
            RETURN FALSE;
         END IF;
      END IF;
   END f_deceasedperscheck;

----------------------------------------------------------------------------------------------------

   FUNCTION f_readmit_required (
                     term_in IN stvterm.stvterm_code%TYPE,
                     pidm_in IN spriden.spriden_pidm%TYPE,
                     term_code_admit_in IN sorlcur.sorlcur_term_code_admit%TYPE,
                     readm_req_in IN sobterm.sobterm_readm_req%TYPE,
                     multi_term_in IN BOOLEAN DEFAULT FALSE)
      RETURN BOOLEAN
   IS
      last_term_attended   stvterm.stvterm_code%TYPE;

      CURSOR last_term_attended_c (p_term stvterm.stvterm_code%TYPE,
                         			  p_pidm spriden.spriden_pidm%TYPE,
                                   p_last_term_attended stvterm.stvterm_code%TYPE)
      IS
      SELECT GREATEST (
                NVL (MAX (sfbetrm_term_code), '000000'),
                NVL (p_last_term_attended, '000000')
             )
        FROM sfbetrm, stvests
       WHERE sfbetrm_term_code < p_term
         AND sfbetrm_pidm = p_pidm
         AND sfbetrm_ests_code = stvests_code
         AND stvests_eff_headcount = 'Y';

   BEGIN
      IF     (   term_code_admit_in <> term_in
              OR term_code_admit_in IS NULL )
         AND readm_req_in IS NOT NULL
      THEN

         FOR shrttrm IN shklibs.shrttrmc (pidm_in, term_in)
         LOOP
            last_term_attended := shrttrm.shrttrm_term_code;
         END LOOP;

         OPEN last_term_attended_c(term_in, pidm_in, last_term_attended);
         FETCH last_term_attended_c INTO last_term_attended;
         CLOSE last_term_attended_c;

         IF     NVL (last_term_attended, '0') < readm_req_in
            AND NOT multi_term_in
         THEN
            RETURN TRUE;
         END IF;
      END IF;

      RETURN FALSE;

   END f_readmit_required;

----------------------------------------------------------------------------------------------------

   PROCEDURE p_addcrse (
      stcr_row        sftregs%ROWTYPE,
      subj            ssbsect.ssbsect_subj_code%TYPE DEFAULT NULL,
      crse            ssbsect.ssbsect_crse_numb%TYPE DEFAULT NULL,
      seq             ssbsect.ssbsect_seq_numb%TYPE DEFAULT NULL,
      start_date_in   VARCHAR2 DEFAULT NULL,
      end_date_in     VARCHAR2 DEFAULT NULL
   )
   IS
      sgbstdn_rec         sgbstdn%ROWTYPE;
      stcr_err_ind        sftregs.sftregs_error_flag%TYPE;
      appr_err            sftregs.sftregs_error_flag%TYPE;
      local_ssbsect_row   ssbsect%ROWTYPE;

      CURSOR sobptrmc (term VARCHAR2, ptrm VARCHAR2)
      IS
         SELECT *
           FROM sobptrm
          WHERE sobptrm_term_code = term
            AND sobptrm_ptrm_code = ptrm;

      CURSOR stvssts_c(ssts_code_in ssbsect.ssbsect_ssts_code%type)
      IS
         SELECT 'STAT',
                sb_registration_msg.f_get_message('STAT', 1),
                'F'
           FROM stvssts
          WHERE stvssts_code = ssts_code_in
            AND stvssts_reg_ind = 'N';

      local_sobptrm_row   sobptrmc%ROWTYPE;
   BEGIN
--      ssbsect_row.ssbsect_subj_code := subj;
--      ssbsect_row.ssbsect_crse_numb := crse;
--      ssbsect_row.ssbsect_seq_numb := seq;
      row_count := 0;
      sgbstdn_rec := bwckcoms.sgbstdn_rec;

      bwckregs.p_init_final_update_vars(stcr_row.sftregs_pidm,
                                        stcr_row.sftregs_term_code);

      IF stcr_row.sftregs_crn IS NOT NULL
      THEN
         FOR stcr_rec IN sfkcurs.sftregsc (
                            stcr_row.sftregs_pidm,
                            stcr_row.sftregs_term_code
                         )
         LOOP
            IF stcr_rec.sftregs_crn = stcr_row.sftregs_crn
            THEN
               row_count := sfkcurs.sftregsc%rowcount;
               EXIT;
            END IF;
         END LOOP;

         IF row_count <> 0
         THEN
            raise_application_error (-20525, bwcklibs.error_msg_table (-20525));
         END IF;
      END IF;

      old_stvrsts_row.stvrsts_incl_sect_enrl := 'N';
      old_stvrsts_row.stvrsts_wait_ind := 'N';
      old_sftregs_row.sftregs_credit_hr := 00.00;
      bwckregs.p_regschk (stcr_row, subj, crse, seq, NULL, 'Y');

      sftregs_row.sftregs_vr_status_type :=
          sfkfunc.f_get_rsts_type(sftregs_row.sftregs_rsts_code);

--
-- determine the sftregs start and end dates,
-- if OLR, they will have been entered by the user and passed in,
-- if trad course, they will be found in ssbsect, or sobptrm.
-----------------------------------------------------------------
      OPEN ssklibs.ssbsectc (stcr_row.sftregs_crn, stcr_row.sftregs_term_code);
      FETCH ssklibs.ssbsectc INTO local_ssbsect_row;
      CLOSE ssklibs.ssbsectc;
      sftregs_row.sftregs_start_date :=
        NVL (
           TO_DATE (start_date_in, twbklibs.date_input_fmt),
           local_ssbsect_row.ssbsect_ptrm_start_date
        );
      sftregs_row.sftregs_completion_date :=
        NVL (
           TO_DATE (end_date_in, twbklibs.date_input_fmt),
           local_ssbsect_row.ssbsect_ptrm_end_date
        );

      IF sftregs_row.sftregs_start_date IS NULL
      THEN
         OPEN sobptrmc (
            stcr_row.sftregs_term_code,
            stcr_row.sftregs_ptrm_code
         );
         FETCH sobptrmc INTO local_sobptrm_row;
         CLOSE sobptrmc;
         sftregs_row.sftregs_start_date :=
                                         local_sobptrm_row.sobptrm_start_date;
         sftregs_row.sftregs_completion_date :=
                                           local_sobptrm_row.sobptrm_end_date;
      END IF;

-----------------------------------------------------------------

      sftregs_row.sftregs_number_of_units :=
                                    local_ssbsect_row.ssbsect_number_of_units;
      sftregs_row.sftregs_dunt_code := local_ssbsect_row.ssbsect_dunt_code;

      OPEN stvssts_c(ssbsect_row.ssbsect_ssts_code);
      FETCH stvssts_c INTO
          sftregs_row.sftregs_rmsg_cde,
          sftregs_row.sftregs_message,
          sftregs_row.sftregs_error_flag;
      CLOSE stvssts_c;

      IF (
                sftregs_row.sftregs_error_flag = 'F'
            AND sftregs_row.sftregs_rmsg_cde = 'STAT'
         )
      THEN
         stcr_err_ind := 'Y';
      ELSE
-- =======================================================
-- This procedure performs edits based on a single course
-- Checks approval code restrictions.
-- Checks level restrictions.
-- Checks college restrictions.
-- Checks degree restrictions.
-- Checks program restrictions.
-- Checks major restrictions.
-- Checks campus restrictions.
-- Checks class restrictions.
-- Checks repeat restrictions
-- Checks capacity.
-- =======================================================
         sfkedit.p_pre_edit (
            sftregs_row,
            stcr_err_ind,
            appr_err,
            old_stvrsts_row.stvrsts_incl_sect_enrl,
            old_stvrsts_row.stvrsts_wait_ind,
            stvrsts_row.stvrsts_incl_sect_enrl,
            stvrsts_row.stvrsts_wait_ind,
            sobterm_row.sobterm_appr_severity,
            sobterm_row.sobterm_levl_severity,
            sobterm_row.sobterm_coll_severity,
            sobterm_row.sobterm_degree_severity,
            sobterm_row.sobterm_program_severity,
            sobterm_row.sobterm_majr_severity,
            sobterm_row.sobterm_camp_severity,
            sobterm_row.sobterm_clas_severity,
            sobterm_row.sobterm_capc_severity,
            sobterm_row.sobterm_rept_severity,
            sobterm_row.sobterm_rpth_severity,
            sobterm_row.sobterm_dept_severity,
            sobterm_row.sobterm_atts_severity,
            sobterm_row.sobterm_chrt_severity,
            sgrclsr_clas_code,
            scbcrse_row.scbcrse_max_rpt_units,
            scbcrse_row.scbcrse_repeat_limit,
            ssbsect_row.ssbsect_sapr_code,
            ssbsect_row.ssbsect_reserved_ind,
            ssbsect_row.ssbsect_seats_avail,
            ssbsect_row.ssbsect_wait_count,
            ssbsect_row.ssbsect_wait_capacity,
            ssbsect_row.ssbsect_wait_avail,
            'WA'
         );
      END IF;
-- C3SC, GR 19/02/06, Added
     -- bypass AUTH REQD errors if override flag is O

   	sv_auth_addcodes_bp.P_rest_message(p_error_flag_in=>sftregs_row.sftregs_error_flag,
   	                                   p_rmsg_cde_in_out=>sftregs_row.sftregs_rmsg_cde,  --C3SC GRS 04/12/2009 ADD
   	                                   p_error_mesg_in_out=>sftregs_row.sftregs_message
   	                                   );
    -- End C3SC

	IF ssbsect_row.ssbsect_tuiw_ind = 'Y'
      THEN
         sftregs_row.sftregs_waiv_hr := '0';
      ELSE
         sftregs_row.sftregs_waiv_hr := sftregs_row.sftregs_bill_hr;
      END IF;

      sftregs_row.sftregs_activity_date := SYSDATE;

      IF stcr_err_ind = 'Y'
      THEN
         sftregs_row.sftregs_rsts_code :=
                                     SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2);
         sftregs_row.sftregs_vr_status_type :=
             sfkfunc.f_get_rsts_type(sftregs_row.sftregs_rsts_code);
         sftregs_row.sftregs_remove_ind := 'Y';
         sftregs_row.sftregs_rec_stat := 'N';
      END IF;

      bwcklibs.p_add_sftregs (sftregs_row);
   END p_addcrse;

--
-- p_reset_dupl_waitlist_regs
-- This procedure resets any changes to waitlisted
-- courses the student is attempting to reinstate
-- that would cause a duplicate course error.
-- ==================================================
   PROCEDURE p_reset_dupl_waitlist_regs(
                p_pidm IN sftregs.sftregs_pidm%TYPE,
                p_term IN sftregs.sftregs_term_code%TYPE)
   IS

      CURSOR dupl_err_crn_c (pidm sftregs.sftregs_pidm%TYPE,
                             term sftregs.sftregs_term_code%TYPE)
      IS
      SELECT sftregs_crn crn, sftregs_sect_subj_code subj,
			    sftregs_sect_crse_numb crse,sftregs_sect_schd_code schd
        FROM sftregs
       WHERE sftregs_term_code = term
         AND sftregs_pidm = pidm
         AND sftregs_rec_stat = 'Q'
         AND sftregs_error_flag = 'F'
         AND sftregs_rmsg_cde = 'DUPL';

      CURSOR dupl_crn_c (pidm sftregs.sftregs_pidm%TYPE,
                         term sftregs.sftregs_term_code%TYPE,
                         crn  sftregs.sftregs_crn%TYPE,
                         subj sftregs.sftregs_sect_subj_code%TYPE,
			                crse sftregs.sftregs_sect_crse_numb%TYPE,
                         schd sftregs.sftregs_sect_schd_code%TYPE)
      IS
      SELECT sftregs_crn
        FROM sftregs
       WHERE sftregs_term_code = term
         AND sftregs_pidm = pidm
         AND sftregs_sect_subj_code = subj
         AND sftregs_sect_crse_numb = crse
         AND sftregs_sect_schd_code = schd
         AND sftregs_crn <> crn
         AND NVL(sftregs_rec_stat,'X') <> 'Q'
         AND NVL(sftregs_error_flag,'N') NOT IN ('F', 'D', 'L')
         AND NVL(sftregs_vr_status_type,'N') NOT IN ('D', 'W');

   BEGIN

   FOR dupl_err_crn_rec IN dupl_err_crn_c(p_pidm,p_term)
   LOOP
      /* duplicate courses */
      FOR dupl_crn_rec IN dupl_crn_c(p_pidm, p_term,
                                     dupl_err_crn_rec.crn,
                                     dupl_err_crn_rec.subj,
                                     dupl_err_crn_rec.crse,
                                     dupl_err_crn_rec.schd)
      LOOP
         sfkmods.p_reset_sftregs_fields(pidm_in=>p_pidm,
                                        term_in=>p_term,
                                        crn_in=>dupl_crn_rec.sftregs_crn,
                                        type_in=>NULL);
      END LOOP;
      /* equivalent courses */
      FOR dupl_equiv_crn IN sfkfunc.screqiv1_c(p_pidm, p_term,
                                               dupl_err_crn_rec.crn,
                                               dupl_err_crn_rec.subj,
                                               dupl_err_crn_rec.crse,
                                               dupl_err_crn_rec.schd)
      LOOP
         sfkmods.p_reset_sftregs_fields(pidm_in=>p_pidm,
                                        term_in=>p_term,
                                        crn_in=>dupl_equiv_crn.sftregs_crn,
                                        type_in=>NULL);
      END LOOP;
      FOR dupl_equiv_crn IN sfkfunc.screqiv2_c(p_pidm, p_term,
                                               dupl_err_crn_rec.crn,
                                               dupl_err_crn_rec.subj,
                                               dupl_err_crn_rec.crse,
                                               dupl_err_crn_rec.schd)
      LOOP
         sfkmods.p_reset_sftregs_fields(pidm_in=>p_pidm,
                                        term_in=>p_term,
                                        crn_in=>dupl_equiv_crn.sftregs_crn,
                                        type_in=>NULL);
      END LOOP;
   END LOOP;

   END p_reset_dupl_waitlist_regs;


--
-- P_ALLCRSECHK
-- This procedure validates relationships between all
-- courses that the student is registering for.
-- ==================================================
PROCEDURE p_allcrsechk (
   term_in                IN       sftregs.sftregs_term_code%TYPE,
   called_by_in           IN       VARCHAR2 DEFAULT 'ADD_DROP',
   capp_tech_error_out    OUT      VARCHAR2,
   drop_problems_in_out   IN OUT   sfkcurs.drop_problems_rec_tabtype,
   drop_failures_in_out   IN OUT   sfkcurs.drop_problems_rec_tabtype,
   multi_term_list_in     IN       OWA_UTIL.ident_arr
)
IS
   genpidm           spriden.spriden_pidm%TYPE;
   error_rec         sftregs%ROWTYPE;
   error_flag        sftregs.sftregs_error_flag%TYPE;
   tmst_flag         sftregs.sftregs_error_flag%TYPE;
   capp_tech_error   VARCHAR2 (4);
   minh_error        VARCHAR2 (1) := 'N';
   source_system     VARCHAR2 (2);
   may_drop_last     BOOLEAN;
   sftregs_row       sfkcurs.sftregsc%ROWTYPE;
   call_type         VARCHAR2(1);

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

   IF called_by_in = 'ADD_DROP'
   THEN
      source_system := 'WA';
      call_type := 'F';
   ELSE
      source_system := 'WC';
      call_type := 'S';
   END IF;

   bwckregs.p_init_final_update_vars(genpidm, term_in);

   DELETE
     FROM twgrwprm
    WHERE twgrwprm_pidm = genpidm
      AND twgrwprm_param_name = 'ERROR_FLAG'
      AND twgrwprm_param_value <> 'M';

-- =======================================================
-- This procedure Checks all courses in sftregs for the
-- current pidm/term:
-- Checks for duplicate courses.
-- Checks for time conflicts.
-- Checks for pre-requisites.
-- Checks for co-requisites.
-- Checks for links.
-- Checks for max hours.
-- Checks for mutual exsclusion.
-- =======================================================

   sfkedit.p_web_group_edits (
      genpidm,
      term_in,
      call_type,
      NVL (sobterm_row.sobterm_dupl_severity, 'N'),
      NVL (sobterm_row.sobterm_time_severity, 'N'),
      NVL (sobterm_row.sobterm_corq_severity, 'N'),
      NVL (sobterm_row.sobterm_link_severity, 'N'),
      NVL (sobterm_row.sobterm_preq_severity, 'N'),
      NVL (sobterm_row.sobterm_maxh_severity, 'N'),
      NVL (sobterm_row.sobterm_minh_severity, 'N'),
      NVL (sobterm_row.sobterm_mexc_severity, 'N'),
      sfbetrm_row.sfbetrm_mhrs_over,
      sfbetrm_row.sfbetrm_min_hrs,
      SYS_CONTEXT ('USERENV', 'SESSIONID'),
      source_system,
      error_flag,
      capp_tech_error,
      minh_error,
      multi_term_list_in
   );

   IF capp_tech_error IS NOT NULL
   THEN
      capp_tech_error_out := capp_tech_error;
      RETURN;
   END IF;

   -- Store the fact that a minimum hours error occurred, and quit all processing.
   IF minh_error = 'Y'
   THEN
      twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'M');
      RETURN;
   END IF;

   IF called_by_in = 'ADD_DROP'
   THEN
      IF error_flag = 'Y'
      THEN
         /* update new courses to indicate they are in error */
         /* don't alter the rec_stat from 'N' */
         UPDATE sftregs
            SET sftregs_rsts_code = SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2),
                sftregs_remove_ind = 'Y',
                sftregs_user = goksels.f_get_ssb_id_context,
                sftregs_activity_date = SYSDATE
          WHERE sftregs_term_code = term_in
            AND sftregs_pidm = genpidm
            AND sftregs_error_flag = 'F'
            AND sftregs_rec_stat = 'N';

         p_reset_dupl_waitlist_regs(genpidm, term_in);
      END IF;

      bwcklibs.p_build_drop_problems_list ( genpidm,
                                            term_in,
                                            drop_problems_in_out,
                                            drop_failures_in_out
                                            );

   ELSIF error_flag <> 'Y'          /* change class options - and no errors */
   THEN
      may_drop_last := sfkdrop.f_drop_last ('W');
      sfkedit.p_update_regs (
         genpidm,
         term_in,
         SYSDATE,
         sgrclsr_clas_code,
         sgbstdn_row.sgbstdn_styp_code,
         NVL (sobterm_row.sobterm_capc_severity, 'N'),
         sobterm_row.sobterm_tmst_calc_ind,
         sfbetrm_row.sfbetrm_tmst_maint_ind,
         sfbetrm_row.sfbetrm_tmst_code,
         may_drop_last,
         source_system,
         error_rec,
         error_flag,
         tmst_flag
      );
   END IF;
END p_allcrsechk;

   PROCEDURE p_calchrs (
      tot_cred            OUT      SFTREGS.SFTREGS_CREDIT_HR%TYPE,
      tot_bill            OUT      SFTREGS.SFTREGS_BILL_HR%TYPE,
      tot_ceu             OUT      SFTREGS.SFTREGS_CREDIT_HR%TYPE,
      min_hour_override   IN       CHAR DEFAULT NULL,
      max_hour_override   IN       CHAR DEFAULT NULL
   )
   IS
      genpidm   spriden.spriden_pidm%TYPE;

      CURSOR hours_c(pidm_in spriden.spriden_pidm%TYPE,
                     term_in sfrstcr.sfrstcr_term_code%TYPE)
      IS
         SELECT SUM (DECODE (
                        stvlevl_ceu_ind,
                        'Y', 0,
                        NVL (sfrstcr_credit_hr, 0)
                     )),
                SUM (NVL (sfrstcr_bill_hr, 0)),
                SUM (DECODE (
                        stvlevl_ceu_ind,
                        'Y', NVL (sfrstcr_credit_hr, 0),
                        0
                     ))
           FROM stvlevl, sfrstcr, stvrsts
          WHERE stvlevl_code = sfrstcr_levl_code
            AND sfrstcr_pidm = pidm_in
            AND sfrstcr_term_code = term_in
            AND sfrstcr_rsts_code = stvrsts_code
            AND (   sfrstcr_error_flag <> 'F'
                 OR sfrstcr_error_flag IS NULL);

      CURSOR hours_override_c(pidm_in spriden.spriden_pidm%TYPE,
                              term_in sfrstcr.sfrstcr_term_code%TYPE)
      IS
         SELECT SUM (DECODE (
                        stvlevl_ceu_ind,
                        'Y', 0,
                        NVL (sfrstcr_credit_hr, 0)
                     )),
                SUM (NVL (sfrstcr_bill_hr, 0)),
                SUM (DECODE (
                        stvlevl_ceu_ind,
                        'Y', NVL (sfrstcr_credit_hr, 0),
                        0
                     ))
           FROM stvlevl, sfrstcr, stvrsts
          WHERE stvlevl_code = sfrstcr_levl_code
            AND sfrstcr_pidm = pidm_in
            AND sfrstcr_term_code = term_in
            AND sfrstcr_rsts_code = stvrsts_code
            AND (
                      (   sfrstcr_error_flag <> 'F'
                       OR sfrstcr_error_flag IS NULL)
                   OR (
                             sfrstcr_error_flag = 'F'
                         AND sfrstcr_rmsg_cde = 'MAXI'
                      )
                );

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

      term := bwcklibs.f_getterm;

      IF max_hour_override IS NULL
      THEN
         OPEN hours_c(genpidm, term);
         FETCH hours_c INTO tot_cred, tot_bill, tot_ceu;
         CLOSE hours_c;

      ELSE
         OPEN hours_override_c(genpidm, term);
         FETCH hours_override_c INTO tot_cred, tot_bill, tot_ceu;
         CLOSE hours_override_c;

      END IF;
   END p_calchrs;


PROCEDURE p_defstcr (add_ind CHAR DEFAULT NULL)
IS
   ssts_code   stvssts.stvssts_code%TYPE;
   gmod_code   stvgmod.stvgmod_code%TYPE;
   l_found     BOOLEAN;

   CURSOR ssts_code_c (ssts_code_in ssbsect.ssbsect_ssts_code%TYPE)
   IS
      SELECT MIN (stvssts_code)
        FROM stvssts
       WHERE (    ssts_code_in IS NOT NULL
              AND stvssts_code = ssts_code_in)
         AND stvssts_reg_ind = 'N';
BEGIN
   OPEN ssts_code_c (ssbsect_row.ssbsect_ssts_code);
   FETCH ssts_code_c INTO ssts_code;
   CLOSE ssts_code_c;

   IF ssts_code IS NOT NULL
   THEN
      raise_application_error (-20511, bwcklibs.error_msg_table (-20511));
   END IF;

   row_count := 0;
   regs_date := bwcklibs.f_getregdate;
   l_found := FALSE;

   IF ssbsect_row.ssbsect_ptrm_code IS NOT NULL
   THEN
      FOR sfrrsts IN sfkcurs.sfrrstsc (
                        sftregs_row.sftregs_rsts_code,
                        sftregs_row.sftregs_term_code,
                        ssbsect_row.ssbsect_ptrm_code
                     )
      LOOP
         IF     TRUNC (regs_date) >= TRUNC (sfrrsts.sfrrsts_start_date)
            AND TRUNC (regs_date) <= TRUNC (sfrrsts.sfrrsts_end_date)
         THEN
             IF add_ind = 'Y' THEN
                sftregs_row.sftregs_ptrm_code :=
                    ssbsect_row.ssbsect_ptrm_code;
                sftregs_row.sftregs_gmod_code :=
                    ssbsect_row.ssbsect_gmod_code;
                sftregs_row.sftregs_credit_hr :=
                    ssbsect_row.ssbsect_credit_hrs;
                sftregs_row.sftregs_bill_hr :=
                    ssbsect_row.ssbsect_bill_hrs;
                sftregs_row.sftregs_camp_code :=
                    ssbsect_row.ssbsect_camp_code;
             END IF;
            l_found := TRUE;
         END IF;

         row_count := sfkcurs.sfrrstsc%rowcount;
      END LOOP;

      IF row_count = 0
      THEN
         raise_application_error (-20512, bwcklibs.error_msg_table (-20512));
      END IF;

      IF NOT l_found
      THEN
         raise_application_error (-20513, bwcklibs.error_msg_table (-20513));
      END IF;
   ELSE
      FOR ssrrsts_rec IN sfkcurs.ssrrstsc (
                            sftregs_row.sftregs_rsts_code,
                            sftregs_row.sftregs_term_code,
                            sftregs_row.sftregs_crn
                         )
      LOOP
         IF (TRUNC (regs_date) BETWEEN ssrrsts_rec.ssbsect_reg_from_date
                AND ssrrsts_rec.ssbsect_reg_to_date
			)
       --c3sc GR 26/02/2006  Added
         OR ( sv_auth_addcodes_bp.F_process_with_auth(p_term_code=>sftregs_row.sftregs_term_code,
                                                         p_rsts_code =>sftregs_row.sftregs_rsts_code)
                )
      --END c3sc
         THEN
             IF add_ind = 'Y' THEN
                sftregs_row.sftregs_ptrm_code :=
                    ssbsect_row.ssbsect_ptrm_code;
                sftregs_row.sftregs_gmod_code :=
                    ssbsect_row.ssbsect_gmod_code;
                sftregs_row.sftregs_credit_hr :=
                    ssbsect_row.ssbsect_credit_hrs;
                sftregs_row.sftregs_bill_hr :=
                    ssbsect_row.ssbsect_bill_hrs;
                sftregs_row.sftregs_camp_code :=
                    ssbsect_row.ssbsect_camp_code;
             END IF;
            l_found := TRUE;
         END IF;

         row_count := sfkcurs.ssrrstsc%rowcount;
      END LOOP;

      IF row_count = 0
      THEN
         raise_application_error (-20608, bwcklibs.error_msg_table (-20608));
      END IF;

      IF NOT l_found
      THEN
         raise_application_error (-20609, bwcklibs.error_msg_table (-20609));
      END IF;
   END IF;

   IF     add_ind IS NOT NULL
      AND add_ind = 'Y'
   THEN
      IF NVL (sftregs_row.sftregs_credit_hr, 00.00) = 00.00
      THEN
         sftregs_row.sftregs_credit_hr := scbcrse_row.scbcrse_credit_hr_low;
      END IF;

      IF NVL (sftregs_row.sftregs_bill_hr, 00.00) = 00.00
      THEN
         sftregs_row.sftregs_bill_hr := scbcrse_row.scbcrse_bill_hr_low;
      END IF;

      IF ssbsect_row.ssbsect_gmod_code IS NOT NULL
      THEN
         sftregs_row.sftregs_gmod_code := ssbsect_row.ssbsect_gmod_code;
      ELSE
         FOR scrgmod IN scklibs.scrgmodc (
                           ssbsect_row.ssbsect_subj_code,
                           ssbsect_row.ssbsect_crse_numb,
                           sftregs_row.sftregs_term_code
                        )
         LOOP
            gmod_code := scrgmod.scrgmod_gmod_code;

            IF scrgmod.scrgmod_default_ind = 'D'
            THEN
               sftregs_row.sftregs_gmod_code := scrgmod.scrgmod_gmod_code;
               EXIT;
            END IF;
         END LOOP;

         IF sftregs_row.sftregs_gmod_code IS NULL
         THEN
            sftregs_row.sftregs_gmod_code := gmod_code;
         END IF;
      END IF;
   END IF;
EXCEPTION
   WHEN bwcklibs.sect_prev_regs
   THEN
      raise_application_error (-20511, bwcklibs.error_msg_table (-20511));
   WHEN bwcklibs.ptrm_stat_undefined
   THEN
      raise_application_error (-20512, bwcklibs.error_msg_table (-20512));
   WHEN bwcklibs.crse_date_err
   THEN
      raise_application_error (-20513, bwcklibs.error_msg_table (-20513));
   WHEN bwcklibs.olr_stat_undefined
   THEN
      raise_application_error (-20608, bwcklibs.error_msg_table (-20608));
   WHEN bwcklibs.olr_crse_date_err
   THEN
      raise_application_error (-20609, bwcklibs.error_msg_table (-20609));
   WHEN OTHERS
   THEN
      raise_application_error (-20533, bwcklibs.error_msg_table (-20533));
END p_defstcr;

------------------------------------------------------------------------



   PROCEDURE p_dropcrse (
      term           stvterm.stvterm_code%TYPE,
      crn            sftregs.sftregs_crn%TYPE,
      rsts           sftregs.sftregs_rsts_code%TYPE,
      reserved_key   sftregs.sftregs_reserved_key%TYPE,
      rec_stat       sftregs.sftregs_rec_stat%TYPE,
      subj           ssbsect.ssbsect_subj_code%TYPE DEFAULT NULL,
      crse           ssbsect.ssbsect_crse_numb%TYPE DEFAULT NULL,
      seq            ssbsect.ssbsect_seq_numb%TYPE DEFAULT NULL,
      del_ind        VARCHAR2 DEFAULT NULL
   )
   IS
      genpidm            spriden.spriden_pidm%TYPE;
      loop_sftregs_row   sftregs%ROWTYPE;
      discount_exist           varchar2(200);  -- FR 8.4.0.2
      lv_function varchar2(300);               -- FR 8.4.0.2
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
--
--  FR 8.4.0.2
--
    IF genutil.product_installed('SF') = 'Y' THEN
      lv_function := 'begin :1 := sffkutil.f_check_discount(:2,:3,:4); end;';
      EXECUTE IMMEDIATE lv_function
      USING   OUT discount_exist,
              IN  genpidm,crn,term;
      IF discount_exist = 'Y' THEN
          raise_application_error(-20610, bwcklibs.error_msg_table(-20610));
      END IF;
    END IF;
--
--
      ssbsect_row.ssbsect_subj_code := subj;
      ssbsect_row.ssbsect_crse_numb := crse;
      ssbsect_row.ssbsect_seq_numb := seq;
      row_count := 0;
      sql_error := 0;
      regs_date := bwcklibs.f_getregdate;

      IF NOT bwckregs.f_validcrn (sql_error, term, crn => crn)
      THEN
         raise_application_error (
            sql_error,
            bwcklibs.error_msg_table (sql_error)
         );
      END IF;

      loop_sftregs_row := NULL;
      OPEN sfkcurs.sftregsc (genpidm, term);

      LOOP
         FETCH sfkcurs.sftregsc INTO loop_sftregs_row;
         EXIT WHEN sfkcurs.sftregsc%NOTFOUND;
         IF sftregs_row.sftregs_crn = loop_sftregs_row.sftregs_crn
         THEN
            row_count := row_count + 1;
            old_sftregs_row := loop_sftregs_row;
         END IF;
      END LOOP;

      CLOSE sfkcurs.sftregsc;

      -- IF all conditions true, display can't drop last class message
         IF row_count = 0
         THEN
            raise_application_error (-20539, bwcklibs.error_msg_table (-20539));
         END IF;

         OPEN stkrsts.stvrstsc (old_sftregs_row.sftregs_rsts_code);
         FETCH stkrsts.stvrstsc INTO stvrsts_row;

         IF stkrsts.stvrstsc%NOTFOUND
         THEN
            NULL;
         ELSE
            old_stvrsts_row.stvrsts_wait_ind := stvrsts_row.stvrsts_wait_ind;
            old_stvrsts_row.stvrsts_incl_sect_enrl :=
                                           stvrsts_row.stvrsts_incl_sect_enrl;
         END IF;

         CLOSE stkrsts.stvrstsc;
         sftregs_row.sftregs_rsts_code := rsts;

         FOR stvrsts IN stkrsts.stvrstsc (rsts)
         LOOP
            stvrsts_row := stvrsts;
         END LOOP;

         IF del_ind IS NULL
         THEN
            IF NOT bwckregs.f_validrsts (sql_error)
            THEN
               raise_application_error (
                  sql_error,
                  bwcklibs.error_msg_table (sql_error)
               );
            END IF;
         END IF;

         IF    stvrsts_row.stvrsts_incl_sect_enrl = 'Y'
            OR stvrsts_row.stvrsts_incl_assess = 'Y'
         THEN
            raise_application_error (-20540, bwcklibs.error_msg_table (-20540));
         END IF;

         IF sftregs_row.sftregs_grde_date IS NOT NULL
         THEN
            raise_application_error (-20541, bwcklibs.error_msg_table (-20541));
         END IF;

         bwcklibs.p_del_sftregs (genpidm, term, crn, rsts, rec_stat);
   END p_dropcrse;

------------------------------------------------------------------------

   PROCEDURE p_getoverride
   IS
      genpidm   spriden.spriden_pidm%TYPE;
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

      FOR ssbsect IN ssklibs.ssbsectc (
                        sftregs_row.sftregs_crn,
                        sftregs_row.sftregs_term_code
                     )
      LOOP
         FOR over_rec IN overridec (
                            genpidm,
                            sftregs_row.sftregs_term_code,
                            sftregs_row.sftregs_crn,
                            ssbsect.ssbsect_subj_code,
                            ssbsect.ssbsect_crse_numb,
                            ssbsect.ssbsect_seq_numb
                         )
         LOOP
            IF over_rec.sfrrovr_dupl_over = 'Y'
            THEN
               sftregs_row.sftregs_dupl_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_link_over = 'Y'
            THEN
               sftregs_row.sftregs_link_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_corq_over = 'Y'
            THEN
               sftregs_row.sftregs_corq_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_preq_over = 'Y'
            THEN
               sftregs_row.sftregs_preq_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_time_over = 'Y'
            THEN
               sftregs_row.sftregs_time_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_levl_over = 'Y'
            THEN
               sftregs_row.sftregs_levl_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_coll_over = 'Y'
            THEN
               sftregs_row.sftregs_coll_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_degc_over = 'Y'
            THEN
               sftregs_row.sftregs_degc_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_prog_over = 'Y'
            THEN
               sftregs_row.sftregs_prog_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_majr_over = 'Y'
            THEN
               sftregs_row.sftregs_majr_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_clas_over = 'Y'
            THEN
               sftregs_row.sftregs_clas_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_appr_over = 'Y'
            THEN
               sftregs_row.sftregs_appr_over := 'Y';
               appr_error := '';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_rept_over = 'Y'
            THEN
               sftregs_row.sftregs_rept_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_rpth_over = 'Y'
            THEN
               sftregs_row.sftregs_rpth_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_camp_over = 'Y'
            THEN
               sftregs_row.sftregs_camp_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_dept_over = 'Y'
            THEN
               sftregs_row.sftregs_dept_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_chrt_over = 'Y'
            THEN
               sftregs_row.sftregs_chrt_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;
            IF over_rec.sfrrovr_mexc_over = 'Y'
            THEN
               sftregs_row.sftregs_mexc_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;
            IF over_rec.sfrrovr_atts_over = 'Y'
            THEN
               sftregs_row.sftregs_atts_over := 'Y';
               sftregs_row.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_capc_over = 'Y'
            THEN
               IF stvrsts_row.stvrsts_incl_sect_enrl = 'Y'
               THEN
                  sftregs_row.sftregs_capc_over := 'Y';
               END IF;

               IF stvrsts_row.stvrsts_wait_ind = 'Y'
               THEN
                  sftregs_wait_over := 'Y';
               END IF;
            END IF;
         END LOOP;
      END LOOP;
   END p_getoverride;

------------------------------------------------------------------------

   PROCEDURE p_regschk (
      stdn_row        sgbstdn%ROWTYPE,
      multi_term_in   BOOLEAN DEFAULT FALSE
   )
   IS
      astd_code            stvastd.stvastd_code%TYPE;
      cast_code            stvcast.stvcast_code%TYPE;
      sobterm_rec          sobterm%ROWTYPE;
      atmp_hrs             VARCHAR2 (1);
      genpidm              spriden.spriden_pidm%TYPE;
      lv_levl_code         sorlcur.sorlcur_levl_code%TYPE;
      lv_admit_term        sorlcur.sorlcur_term_code_admit%TYPE;

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

      sgbstdn_row := stdn_row;
      sql_error := 0;
      regs_date := bwcklibs.f_getregdate;
      term := bwcklibs.f_getterm;

/*************************************************************/
/* Determine the Class of the Student - for Fees calculation */
/*************************************************************/

      OPEN soklibs.sobterm_webc (term);
      FETCH soklibs.sobterm_webc INTO sobterm_rec;

      IF soklibs.sobterm_webc%NOTFOUND
      THEN
         atmp_hrs := NULL;
      ELSE
         IF sobterm_rec.sobterm_incl_attmpt_hrs_ind = 'Y'
         THEN
            atmp_hrs := 'A';
         ELSE
            atmp_hrs := NULL;
         END IF;
      END IF;

      CLOSE soklibs.sobterm_webc;
      sgrclsr_clas_code := NULL;
      clas_desc := NULL;
--
      lv_levl_code := sokccur.f_curriculum_value(
                                p_pidm=> genpidm,
                                p_lmod_code => sb_curriculum_str.f_learner,
                                p_term_code => term,
                                p_key_seqno => 99,
                                p_eff_term => term,
                                p_order => 1,
                                p_field => 'LEVL');
      soklibs.p_class_calc (
         genpidm,
         lv_levl_code,
         term,
         atmp_hrs,
         sgrclsr_clas_code,
         clas_desc
      );

/*************************************************************/
/* End of Class Determination                                */
/*************************************************************/

      IF     bwckregs.f_stuhld (sql_error, global_pidm)
         AND NOT multi_term_in
      THEN
         raise_application_error (
            sql_error,
            bwcklibs.error_msg_table (sql_error)
         );
      END IF;

      row_count := 0;

      FOR stvstst IN stkstst.stvststc (sgbstdn_row.sgbstdn_stst_code)
      LOOP
         stst_reg_ind := stvstst.stvstst_reg_ind;
         row_count := stkstst.stvststc%rowcount;
      END LOOP;

      IF     row_count <> 1
         AND NOT multi_term_in
      THEN
         raise_application_error (-20526, bwcklibs.error_msg_table (-20526));
      END IF;

      FOR shrttrm IN shklibs.shrttrmc (genpidm, term)
      LOOP
         astd_code := shrttrm.shrttrm_astd_code_end_of_term;
         cast_code := shrttrm.shrttrm_cast_code;
      END LOOP;

      IF sgbstdn_row.sgbstdn_term_code_astd = term
      THEN
         astd_code := sgbstdn_row.sgbstdn_astd_code;
      END IF;

      IF sgbstdn_row.sgbstdn_term_code_cast = term
      THEN
         cast_code := sgbstdn_row.sgbstdn_cast_code;
      END IF;

      IF cast_code IS NOT NULL
      THEN
         row_count := 0;

         FOR stvcast IN stkcast.stvcastc (cast_code)
         LOOP
            cast_prevent_reg_ind := stvcast.stvcast_prevent_reg_ind;
            row_count := stkcast.stvcastc%rowcount;
         END LOOP;

         IF     row_count <> 1
            AND NOT multi_term_in
         THEN
            raise_application_error (-20603, bwcklibs.error_msg_table (-20603));
         END IF;
      ELSIF astd_code IS NOT NULL
      THEN
         row_count := 0;

         FOR stvastd IN stkastd.stvastdc (astd_code)
         LOOP
            astd_prevent_reg_ind := stvastd.stvastd_prevent_reg_ind;
            row_count := stkastd.stvastdc%rowcount;
         END LOOP;

         IF NOT multi_term_in
         THEN
            IF row_count <> 1
            THEN
               raise_application_error (-20527, bwcklibs.error_msg_table (-20527));
            ELSIF astd_prevent_reg_ind = 'Y'
            THEN
               raise_application_error (-20529, bwcklibs.error_msg_table (-20529));
            END IF;
         END IF;
      END IF;

      sfksels.p_get_min_max_by_curric_stand (
             p_term        => term,
             p_pidm        => genpidm,
             p_seq_no      => NULL,
             p_astd_code   => astd_code,
             p_cast_code   => cast_code,
             p_min_hrs_out => mhrs_min_hrs,
             p_max_hrs_out => mhrs_max_hrs,
             p_min_srce_out => minh_srce,
             p_max_srce_out => maxh_srce );

      lv_admit_term := sokccur.f_curriculum_value(
                                p_pidm=>genpidm,
                                p_lmod_code => sb_curriculum_str.f_learner,
                                p_term_code => term,
                                p_key_seqno => 99,
                                p_eff_term => term,
                                p_order => 1,
                                p_field => 'TADMIT');

      IF f_readmit_required(term, genpidm,
                            lv_admit_term,
                            sobterm_row.sobterm_readm_req,
                            multi_term_in)
      THEN
         raise_application_error (-20505, bwcklibs.error_msg_table (-20505));
      END IF;

      sobterm_row.sobterm_fee_assessmnt_eff_date :=
        GREATEST (
           TRUNC (regs_date),
           NVL (
              TRUNC (sobterm_row.sobterm_fee_assessmnt_eff_date),
              TRUNC (regs_date)
           )
        );

      FOR advr IN soklibs.advisorc (genpidm, term)
      LOOP
         advr_row := advr;
      END LOOP;
   END p_regschk;

--------------------------------------------------------------------------

   PROCEDURE p_regschk (
      etrm_row        sfbetrm%ROWTYPE,
      add_ind         VARCHAR2 DEFAULT NULL,
      multi_term_in   BOOLEAN DEFAULT FALSE
   )
   IS
      genpidm   spriden.spriden_pidm%TYPE;
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

      sfbetrm_row := etrm_row;
      regs_date := bwcklibs.f_getregdate;

      IF sfbetrm_row.sfbetrm_term_code IS NULL
      THEN
         sfbetrm_row.sfbetrm_term_code := term;
      END IF;

      IF sfbetrm_row.sfbetrm_pidm IS NULL
      THEN
         sfbetrm_row.sfbetrm_pidm := genpidm;
      END IF;

      IF sfbetrm_row.sfbetrm_ests_code IS NULL
      THEN
         sfbetrm_row.sfbetrm_ests_code := 'EL';
         sfbetrm_row.sfbetrm_ests_date := TRUNC (regs_date);
      END IF;

      IF sfbetrm_row.sfbetrm_mhrs_over IS NULL
      THEN
         sfbetrm_row.sfbetrm_mhrs_over := mhrs_max_hrs;
         sfbetrm_row.sfbetrm_maxh_srce_cde := maxh_srce;
      END IF;

      IF sfbetrm_row.sfbetrm_min_hrs IS NULL
      THEN
         sfbetrm_row.sfbetrm_min_hrs := mhrs_min_hrs;
         sfbetrm_row.sfbetrm_minh_srce_cde := minh_srce;
      END IF;

      IF sfbetrm_row.sfbetrm_ar_ind IS NULL
      THEN
         sfbetrm_row.sfbetrm_ar_ind := 'N';
      END IF;

      IF sfbetrm_row.sfbetrm_add_date IS NULL
      THEN
         sfbetrm_row.sfbetrm_add_date := SYSDATE;
      END IF;

      sfbetrm_row.sfbetrm_user := USER;
      sfbetrm_row.sfbetrm_activity_date := SYSDATE;

      IF     stst_reg_ind = 'N'
         AND NOT multi_term_in
      THEN
         raise_application_error (-20507, bwcklibs.error_msg_table (-20507));
      END IF;

      sql_error := 0;

      IF     NOT bwckregs.f_validenrl (
                sql_error,
                sfbetrm_row.sfbetrm_term_code
             )
         AND NOT multi_term_in
      THEN
         raise_application_error (
            sql_error,
            bwcklibs.error_msg_table (sql_error)
         );
      END IF;
--      sfbetrm_row.sfbetrm_term_code||'.'||
--      sfbetrm_row.sfbetrm_ests_code||'.'||
--      regs_date||'.'

      IF sfbetrm_row.sfbetrm_ar_ind <> 'Y'
      THEN
         IF     NOT bwckregs.f_validacpt (sql_error)
            AND NOT multi_term_in
         THEN
            raise_application_error (
               sql_error,
               bwcklibs.error_msg_table (sql_error)
            );
         END IF;
      END IF;

      IF sfbetrm_row.sfbetrm_rgre_code IS NOT NULL
      THEN
         IF     NOT bwckregs.f_validrgre (sql_error)
            AND NOT multi_term_in
         THEN
            raise_application_error (
               sql_error,
               bwcklibs.error_msg_table (sql_error)
            );
         END IF;
      END IF;

      IF     ests_prev_reg = 'Y'
         AND NOT multi_term_in
      THEN
         raise_application_error (-20506, bwcklibs.error_msg_table (-20506));
      END IF;

      IF     astd_prevent_reg_ind = 'Y'
         AND NOT multi_term_in
      THEN
         raise_application_error (-20529, bwcklibs.error_msg_table (-20529));
      END IF;

      IF     cast_prevent_reg_ind = 'Y'
         AND NOT multi_term_in
      THEN
         raise_application_error (-20604, bwcklibs.error_msg_table (-20604));
      END IF;

      IF NVL (add_ind, 'N') = 'Y'
      THEN
         bwcklibs.p_add_sfbetrm (sfbetrm_row);
      END IF;
   END p_regschk;

--------------------------------------------------------------------------

   PROCEDURE p_regschk (
      stcr_row   sftregs%ROWTYPE,
      subj       ssbsect.ssbsect_subj_code%TYPE DEFAULT NULL,
      crse       ssbsect.ssbsect_crse_numb%TYPE DEFAULT NULL,
      seq        ssbsect.ssbsect_seq_numb%TYPE DEFAULT NULL,
      over       CHAR DEFAULT NULL,
      add_ind    CHAR DEFAULT NULL
   )
   IS
      stvrsts_row   stkrsts.stvrstsc%ROWTYPE;
   BEGIN
      sftregs_row := stcr_row;
      regs_date := bwcklibs.f_getregdate;

      ssbsect_row.ssbsect_subj_code := subj;
      ssbsect_row.ssbsect_crse_numb := crse;
      ssbsect_row.ssbsect_seq_numb := seq;

      sftregs_row.sftregs_sect_subj_code := ssbsect_row.ssbsect_subj_code;
      sftregs_row.sftregs_sect_crse_numb := ssbsect_row.ssbsect_crse_numb;
      sftregs_row.sftregs_sect_seq_numb := ssbsect_row.ssbsect_seq_numb;
      sftregs_row.sftregs_sect_schd_code := ssbsect_row.ssbsect_schd_code;

      IF over IS NOT NULL
      THEN
         override := over;
      END IF;

      IF sftregs_row.sftregs_preq_over IS NULL
      THEN
         sftregs_row.sftregs_preq_over := 'N';
      END IF;

      IF sftregs_row.sftregs_rept_over IS NULL
      THEN
         sftregs_row.sftregs_rept_over := 'N';
      END IF;

      IF sftregs_row.sftregs_coll_over IS NULL
      THEN
         sftregs_row.sftregs_coll_over := 'N';
      END IF;

      IF sftregs_row.sftregs_degc_over IS NULL
      THEN
         sftregs_row.sftregs_degc_over := 'N';
      END IF;

      IF sftregs_row.sftregs_prog_over IS NULL
      THEN
         sftregs_row.sftregs_prog_over := 'N';
      END IF;

      IF sftregs_row.sftregs_clas_over IS NULL
      THEN
         sftregs_row.sftregs_clas_over := 'N';
      END IF;

      IF sftregs_row.sftregs_camp_over IS NULL
      THEN
         sftregs_row.sftregs_camp_over := 'N';
      END IF;

      IF sftregs_row.sftregs_dept_over IS NULL
      THEN
         sftregs_row.sftregs_dept_over := 'N';
      END IF;

      IF sftregs_row.sftregs_chrt_over IS NULL
      THEN
         sftregs_row.sftregs_chrt_over := 'N';
      END IF;
      IF sftregs_row.sftregs_mexc_over IS NULL
      THEN
         sftregs_row.sftregs_mexc_over := 'N';
      END IF;
      IF sftregs_row.sftregs_atts_over IS NULL
      THEN
         sftregs_row.sftregs_atts_over := 'N';
      END IF;

      IF sftregs_row.sftregs_link_over IS NULL
      THEN
         sftregs_row.sftregs_link_over := 'N';
      END IF;

      IF sftregs_row.sftregs_corq_over IS NULL
      THEN
         sftregs_row.sftregs_corq_over := 'N';
      END IF;

      IF sftregs_row.sftregs_appr_over IS NULL
      THEN
         sftregs_row.sftregs_appr_over := 'N';
      END IF;

      IF sftregs_row.sftregs_majr_over IS NULL
      THEN
         sftregs_row.sftregs_majr_over := 'N';
      END IF;

      IF sftregs_row.sftregs_dupl_over IS NULL
      THEN
         sftregs_row.sftregs_dupl_over := 'N';
      END IF;

      IF sftregs_row.sftregs_levl_over IS NULL
      THEN
         sftregs_row.sftregs_levl_over := 'N';
      END IF;

      IF sftregs_row.sftregs_capc_over IS NULL
      THEN
         sftregs_row.sftregs_capc_over := 'N';
      END IF;

      IF sftregs_row.sftregs_time_over IS NULL
      THEN
         sftregs_row.sftregs_time_over := 'N';
      END IF;

      IF sftregs_row.sftregs_rpth_over IS NULL
      THEN
         sftregs_row.sftregs_rpth_over := 'N';
      END IF;

      IF sftregs_row.sftregs_add_date IS NULL
      THEN
         sftregs_row.sftregs_add_date := SYSDATE;
      END IF;

      sftregs_row.sftregs_user := USER;
      sftregs_row.sftregs_activity_date := SYSDATE;
      sql_error := 0;

      IF NOT bwckregs.f_validcrn (sql_error, sftregs_row.sftregs_term_code)
      THEN
         raise_application_error (
            sql_error,
            bwcklibs.error_msg_table (sql_error)
         );
      END IF;

      bwckregs.p_defstcr (add_ind);

      IF NOT bwckregs.f_validlevl (sql_error, sftregs_row.sftregs_term_code)
      THEN
         raise_application_error (
            sql_error,
            bwcklibs.error_msg_table (sql_error)
         );
      END IF;

      IF NOT bwckregs.f_validrsts (sql_error)
      THEN
         raise_application_error (
            sql_error,
            bwcklibs.error_msg_table (sql_error)
         );
      END IF;

      IF NOT bwckregs.f_validgmod (sql_error, sftregs_row.sftregs_term_code)
      THEN
         raise_application_error (
            sql_error,
            bwcklibs.error_msg_table (sql_error)
         );
      END IF;

      IF NOT bwckregs.f_validcredhr (sql_error)
      THEN
         raise_application_error (
            sql_error,
            bwcklibs.error_msg_table (sql_error)
         );
      END IF;

      IF NOT bwckregs.f_validbillhr (sql_error)
      THEN
         raise_application_error (
            sql_error,
            bwcklibs.error_msg_table (sql_error)
         );
      END IF;

      IF NOT bwckregs.f_validappr (sql_error)
      THEN
         raise_application_error (
            sql_error,
            bwcklibs.error_msg_table (sql_error)
         );
      END IF;

      FOR stvrsts IN stkrsts.stvrstsc (sftregs_row.sftregs_rsts_code)
      LOOP
         stvrsts_row := stvrsts;
      END LOOP;

      IF stvrsts_row.stvrsts_incl_sect_enrl = 'Y' OR
          (stvrsts_row.stvrsts_wait_ind = 'Y' AND NVL(add_ind,'N') = 'Y')
      THEN
         bwckregs.p_getoverride;
      END IF;
   END p_regschk;

--------------------------------------------------------------------------

   PROCEDURE p_regsfees
   IS
      fee_assess_eff_date   DATE;
      fee_assess_date       VARCHAR2 (50);
      return_status         NUMBER                             := 0;
      genpidm               spriden.spriden_pidm%TYPE;
      sessionid             twgrwprm.twgrwprm_param_value%TYPE;
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

      sessionid := SYS_CONTEXT ('USERENV', 'SESSIONID');
      regs_date := bwcklibs.f_getregdate;
      term := bwcklibs.f_getterm;

      bwcklibs.p_getsobterm(term,sobterm_row);

      fee_assess_eff_date :=
        NVL (
           GREATEST (
              TRUNC (sobterm_row.sobterm_fee_assessmnt_eff_date),
              TRUNC (regs_date)
           ),
           TRUNC (regs_date)
        );

      SFKFEES.p_processfeeassessment (
         term,
         genpidm,
         fee_assess_eff_date,
         TRUNC (SYSDATE),
         'R',                -- rule entry type - 'R'egular Registration Rules
         'Y',                              -- create TBRACCD records indicator
         'BWCKREGS',                 -- source program invoking fee assessment
         'Y',                                              -- commit indicator
         fee_assess_date,
         'N',
         return_status
      );

   END p_regsfees;

--------------------------------------------------------------
   PROCEDURE p_statuschg
   IS
   BEGIN
      row_count := 0;
      regs_date := bwcklibs.f_getregdate;

      IF sftregs_row.sftregs_ptrm_code IS NOT NULL
      THEN
         FOR sfrrsts IN sfkcurs.sfrrstsc (
                           sftregs_row.sftregs_rsts_code,
                           sftregs_row.sftregs_term_code,
                           sftregs_row.sftregs_ptrm_code
                        )
         LOOP
            IF sfrrsts.sfrrsts_eff_by_enrl_stat = 'Y'
            THEN
               row_count := sfkcurs.sfrrstsc%rowcount;
            END IF;
         END LOOP;
      ELSE
         FOR ssrrsts_rec IN sfkcurs.ssrrstsc (
                               sftregs_row.sftregs_rsts_code,
                               sftregs_row.sftregs_term_code,
                               sftregs_row.sftregs_crn
                            )
         LOOP
            IF ssrrsts_rec.ssrrsts_eff_by_stu_stat_ind = 'Y'
            THEN
               row_count := sfkcurs.ssrrstsc%rowcount;
            END IF;
         END LOOP;
      END IF;

      IF row_count > 0
      THEN
         IF sftregs_row.sftregs_grde_date IS NOT NULL
         THEN
            message_text :=
              g$_nls.get ('BWCKREG1-0000',
                 'SQL',
                 '*WARNING* Status change does not effect course. Course has been graded.'
              );
         ELSE
            row_count := 0;

            IF sftregs_row.sftregs_ptrm_code IS NOT NULL
            THEN
               FOR sfrrsts IN
                   sfkcurs.sfrrstsc (
                      sfbetrm_row.sfbetrm_ests_code,
                      sftregs_row.sftregs_term_code,
                      sftregs_row.sftregs_ptrm_code
                   )
               LOOP
                  IF     sfbetrm_row.sfbetrm_ests_date >=
                                                    sfrrsts.sfrrsts_start_date
                     AND sfbetrm_row.sfbetrm_ests_date <=
                                                      sfrrsts.sfrrsts_end_date
                  THEN
                     row_count := sfkcurs.sfrrstsc%rowcount;
                  END IF;
               END LOOP;
            ELSE
               FOR ssrrsts_rec IN
                   sfkcurs.ssrrstsc (
                      sfbetrm_row.sfbetrm_ests_code,
                      sftregs_row.sftregs_term_code,
                      sftregs_row.sftregs_crn
                   )
               LOOP
                  IF sfbetrm_row.sfbetrm_ests_date BETWEEN ssrrsts_rec.ssbsect_reg_from_date
                         AND ssrrsts_rec.ssbsect_reg_to_date
                  THEN
                     row_count := sfkcurs.ssrrstsc%rowcount;
                  END IF;
               END LOOP;
            END IF;

            IF row_count = 0
            THEN
               message_text :=
                 g$_nls.get ('BWCKREG1-0001',
                    'SQL',
                    '*WARNING* Status change does not effect course. See Course Status Table.'
                 );
            ELSE
               sftregs_row.sftregs_rsts_code := sfbetrm_row.sfbetrm_ests_code;
               sftregs_row.sftregs_rsts_date := sfbetrm_row.sfbetrm_ests_date;

               IF NOT bwckregs.f_validrsts (sql_error)
               THEN
                  raise_application_error (
                     sql_error,
                     bwcklibs.error_msg_table (sql_error)
                  );
               END IF;

               sftregs_row.sftregs_activity_date := SYSDATE;
            END IF;
         END IF;
      END IF;
   END p_statuschg;


PROCEDURE p_updcrse (
   stcr_row   sftregs%ROWTYPE,
   crn        sftregs.sftregs_crn%TYPE DEFAULT NULL,
   subj       ssbsect.ssbsect_subj_code%TYPE DEFAULT NULL,
   crse       ssbsect.ssbsect_crse_numb%TYPE DEFAULT NULL,
   seq        ssbsect.ssbsect_seq_numb%TYPE DEFAULT NULL
)
IS
   genpidm            spriden.spriden_pidm%TYPE;
   loop_sftregs_row   sftregs%ROWTYPE;
   stcr_err_ind       sftregs.sftregs_error_flag%TYPE;
   appr_err           sftregs.sftregs_error_flag%TYPE;
   capc_severity      sobterm.sobterm_capc_severity%TYPE;
   source_system     VARCHAR2 (2);
   /*Defect:1-3UQPUS C3SC ADDED GRS  12/03/2008*/
   lv_auth_course_reg_rec      sv_auth_course_reg.auth_course_reg_rec;
   lv_auth_course_reg_ref      sv_auth_course_reg.auth_course_reg_ref;
   lv_auth_course_reg_found    BOOLEAN := FALSE;
   lv_rowid_out                gb_common.internal_record_id_type;
   /*Defect:1-3UQPUS C3SC END GRS  12/03/2008*/
   vr_rsts_Code       VARCHAR2(255);  -- FR 8.4.0.2
   discount_exist     VARCHAR2(200);  -- FR 8.4.0.2
   lv_function        VARCHAR2(300);  -- FR 8.4.0.2

   appr_severity         sobterm.sobterm_appr_severity%TYPE;
   levl_severity         sobterm.sobterm_levl_severity%TYPE;
   coll_severity         sobterm.sobterm_coll_severity%TYPE;
   degree_severity       sobterm.sobterm_degree_severity%TYPE;
   program_severity      sobterm.sobterm_program_severity%TYPE;
   majr_severity         sobterm.sobterm_majr_severity%TYPE;
   camp_severity         sobterm.sobterm_camp_severity%TYPE;
   clas_severity         sobterm.sobterm_clas_severity%TYPE;
   rept_severity         sobterm.sobterm_rept_severity%TYPE;
   rpth_severity         sobterm.sobterm_rpth_severity%TYPE;
   dept_severity         sobterm.sobterm_dept_severity%TYPE;
   atts_severity         sobterm.sobterm_atts_severity%TYPE;
   chrt_severity         sobterm.sobterm_chrt_severity%TYPE;



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

   sftregs_row := stcr_row;


   IF crn is null
   THEN
      /* adding a class */
      source_system := 'WA';
   ELSE
      /* updating an existing class */
      source_system := 'WC';
   END IF;

   bwckregs.p_init_final_update_vars(genpidm, stcr_row.sftregs_term_code);

   ssbsect_row.ssbsect_subj_code := subj;
   ssbsect_row.ssbsect_crse_numb := crse;
   ssbsect_row.ssbsect_seq_numb := seq;
   row_count := 0;
   sql_error := 0;


--
-- BEGIN FR 8.4.0.2
--
   IF (stcr_row.sftregs_rsts_code IS NOT NULL)
   THEN
   vr_rsts_code := sfkfunc.f_get_rsts_type(stcr_row.sftregs_rsts_code);

     IF vr_rsts_code = 'D'
     OR vr_rsts_code = 'W'
     THEN
       IF genutil.product_installed('SF') = 'Y'
       THEN
      lv_function := 'begin :1 := sffkutil.f_check_discount(:2,:3,:4); end;';
      EXECUTE IMMEDIATE lv_function
         USING OUT discount_exist,
               IN  genpidm, stcr_row.sftregs_crn,stcr_row.sftregs_term_code;
      IF discount_exist = 'Y' THEN
          raise_application_error(-20610, bwcklibs.error_msg_table(-20610));
      END IF;
    END IF;
   END IF;

   END IF;

--
-- end FR 8.4.0.2
--

   IF NOT bwckregs.f_validcrn (sql_error, sftregs_row.sftregs_term_code)
   THEN
      raise_application_error (
         sql_error,
         bwcklibs.error_msg_table (sql_error)
      );
   END IF;

--
-- Get the old sftregs record from the database.
-- ====================================================================
   loop_sftregs_row := NULL;
   OPEN sfkcurs.sftregsc (genpidm, sftregs_row.sftregs_term_code);

   LOOP
      FETCH sfkcurs.sftregsc INTO loop_sftregs_row;
      EXIT WHEN sfkcurs.sftregsc%NOTFOUND;
      IF sftregs_row.sftregs_crn = loop_sftregs_row.sftregs_crn
      THEN
         row_count := row_count + 1;
         old_sftregs_row := loop_sftregs_row;
      END IF;
   END LOOP;

   CLOSE sfkcurs.sftregsc;

   -- If the course is found, it's the only row, drop last class
   -- is set to N to not allow the last class to be dropped and
   -- the rsts flag was not found and there isn't a fatal error
   -- display the drop last class error
      IF row_count = 0
      THEN
         raise_application_error (-20539, bwcklibs.error_msg_table (-20539));
      END IF;

      FOR stvrsts IN stkrsts.stvrstsc (old_sftregs_row.sftregs_rsts_code)
      LOOP
         old_stvrsts_row := stvrsts;
      END LOOP;

      sftregs_row.sftregs_ptrm_code :=
          NVL (stcr_row.sftregs_ptrm_code, old_sftregs_row.sftregs_ptrm_code);
      sftregs_row.sftregs_rsts_code :=
          NVL (stcr_row.sftregs_rsts_code, old_sftregs_row.sftregs_rsts_code);
      sftregs_row.sftregs_vr_status_type :=
        sfkfunc.f_get_rsts_type (sftregs_row.sftregs_rsts_code);
      sftregs_row.sftregs_rsts_date :=
          NVL (stcr_row.sftregs_rsts_date, old_sftregs_row.sftregs_rsts_date);
      sftregs_row.sftregs_error_flag :=
        NVL (stcr_row.sftregs_error_flag, old_sftregs_row.sftregs_error_flag);
      sftregs_row.sftregs_rmsg_cde :=
              NVL (stcr_row.sftregs_rmsg_cde, old_sftregs_row.sftregs_rmsg_cde);
      sftregs_row.sftregs_message :=
              NVL (stcr_row.sftregs_message, old_sftregs_row.sftregs_message);
      sftregs_row.sftregs_bill_hr :=
              NVL (stcr_row.sftregs_bill_hr, old_sftregs_row.sftregs_bill_hr);
      sftregs_row.sftregs_waiv_hr :=
              NVL (stcr_row.sftregs_waiv_hr, old_sftregs_row.sftregs_waiv_hr);
      sftregs_row.sftregs_credit_hr :=
          NVL (stcr_row.sftregs_credit_hr, old_sftregs_row.sftregs_credit_hr);
      sftregs_row.sftregs_bill_hr_hold :=
        NVL (
           stcr_row.sftregs_bill_hr_hold,
           old_sftregs_row.sftregs_bill_hr_hold
        );
      sftregs_row.sftregs_credit_hr_hold :=
        NVL (
           stcr_row.sftregs_credit_hr_hold,
           old_sftregs_row.sftregs_credit_hr_hold
        );
      sftregs_row.sftregs_gmod_code :=
          NVL (stcr_row.sftregs_gmod_code, old_sftregs_row.sftregs_gmod_code);
      sftregs_row.sftregs_grde_code :=
          NVL (stcr_row.sftregs_grde_code, old_sftregs_row.sftregs_grde_code);
      sftregs_row.sftregs_grde_code_mid :=
        NVL (
           stcr_row.sftregs_grde_code_mid,
           old_sftregs_row.sftregs_grde_code_mid
        );
      sftregs_row.sftregs_grde_date :=
          NVL (stcr_row.sftregs_grde_date, old_sftregs_row.sftregs_grde_date);
      sftregs_row.sftregs_dupl_over :=
          NVL (stcr_row.sftregs_dupl_over, old_sftregs_row.sftregs_dupl_over);
      sftregs_row.sftregs_link_over :=
          NVL (stcr_row.sftregs_link_over, old_sftregs_row.sftregs_link_over);
      sftregs_row.sftregs_corq_over :=
          NVL (stcr_row.sftregs_corq_over, old_sftregs_row.sftregs_corq_over);
      sftregs_row.sftregs_preq_over :=
          NVL (stcr_row.sftregs_preq_over, old_sftregs_row.sftregs_preq_over);
      sftregs_row.sftregs_time_over :=
          NVL (stcr_row.sftregs_time_over, old_sftregs_row.sftregs_time_over);
      sftregs_row.sftregs_capc_over :=
          NVL (stcr_row.sftregs_capc_over, old_sftregs_row.sftregs_capc_over);
      sftregs_row.sftregs_levl_over :=
          NVL (stcr_row.sftregs_levl_over, old_sftregs_row.sftregs_levl_over);
      sftregs_row.sftregs_coll_over :=
          NVL (stcr_row.sftregs_coll_over, old_sftregs_row.sftregs_coll_over);
      sftregs_row.sftregs_degc_over :=
          NVL (stcr_row.sftregs_degc_over, old_sftregs_row.sftregs_degc_over);
      sftregs_row.sftregs_prog_over :=
          NVL (stcr_row.sftregs_prog_over, old_sftregs_row.sftregs_prog_over);
      sftregs_row.sftregs_majr_over :=
          NVL (stcr_row.sftregs_majr_over, old_sftregs_row.sftregs_majr_over);
      sftregs_row.sftregs_clas_over :=
          NVL (stcr_row.sftregs_clas_over, old_sftregs_row.sftregs_clas_over);
      sftregs_row.sftregs_appr_over :=
          NVL (stcr_row.sftregs_appr_over, old_sftregs_row.sftregs_appr_over);
      sftregs_row.sftregs_appr_received_ind :=
        NVL (
           stcr_row.sftregs_appr_received_ind,
           old_sftregs_row.sftregs_appr_received_ind
        );
      sftregs_row.sftregs_add_date :=
            NVL (stcr_row.sftregs_add_date, old_sftregs_row.sftregs_add_date);
      sftregs_row.sftregs_activity_date := SYSDATE;
      sftregs_row.sftregs_levl_code :=
          NVL (stcr_row.sftregs_levl_code, old_sftregs_row.sftregs_levl_code);
      sftregs_row.sftregs_camp_code :=
          NVL (stcr_row.sftregs_camp_code, old_sftregs_row.sftregs_camp_code);
      sftregs_row.sftregs_reserved_key :=
        NVL (
           stcr_row.sftregs_reserved_key,
           old_sftregs_row.sftregs_reserved_key
        );
      sftregs_row.sftregs_rept_over :=
          NVL (stcr_row.sftregs_rept_over, old_sftregs_row.sftregs_rept_over);
      sftregs_row.sftregs_rpth_over :=
          NVL (stcr_row.sftregs_rpth_over, old_sftregs_row.sftregs_rpth_over);
      sftregs_row.sftregs_test_over :=
          NVL (stcr_row.sftregs_test_over, old_sftregs_row.sftregs_test_over);
      sftregs_row.sftregs_camp_over :=
          NVL (stcr_row.sftregs_camp_over, old_sftregs_row.sftregs_camp_over);
      sftregs_row.sftregs_dept_over :=
          NVL (stcr_row.sftregs_dept_over, old_sftregs_row.sftregs_dept_over);
      sftregs_row.sftregs_chrt_over :=
          NVL (stcr_row.sftregs_chrt_over, old_sftregs_row.sftregs_chrt_over);
      sftregs_row.sftregs_mexc_over :=
          NVL (stcr_row.sftregs_mexc_over, old_sftregs_row.sftregs_mexc_over);
      sftregs_row.sftregs_atts_over :=
          NVL (stcr_row.sftregs_atts_over, old_sftregs_row.sftregs_atts_over);
      sftregs_row.sftregs_user := USER;
     /*Defect:1-3UQPUS C3SC ADDED GRS  12/03/2008*/
       lv_auth_course_reg_ref := sv_auth_course_reg.f_query_one( sftregs_row.sftregs_term_code,genpidm,crn);
       FETCH lv_auth_course_reg_ref INTO lv_auth_course_reg_rec;
       CLOSE lv_auth_course_reg_ref;
     /*C3SC END*/



      bwckregs.p_regschk (sftregs_row,
                          sftregs_row.sftregs_sect_subj_code,
                          sftregs_row.sftregs_sect_crse_numb,
                          sftregs_row.sftregs_sect_seq_numb);

     IF bwckcoms.F_StudyPathEnabled = 'Y' THEN
        sftregs_row.sftregs_stsp_key_sequence := NVL(stcr_row.sftregs_stsp_key_sequence, old_sftregs_row.sftregs_stsp_key_sequence);--Added nvl 8.5.0.3
        -- Create the sfrensp record if one doesnt exist
        IF sftregs_row.sftregs_stsp_key_sequence IS NOT NULL THEN--changed to sftregs_row reference 8.5.0.3
          bwckcoms.p_regs_ensp_chk (p_pidm              =>     stcr_row.sftregs_pidm,
                                    p_sp_seq_no         =>     sftregs_row.sftregs_stsp_key_sequence,--changed to sftregs_row reference 8.5.0.3
                                    p_term_code         =>     stcr_row.sftregs_term_code);
        END IF;

     END IF;



-- =======================================================
-- This procedure performs edits based on a single course,
-- if not withdrawing
-- =======================================================
-- 8.4.0.2
-- set severity flags to term settings when voice status not dropped or withdrawn
-- to process all potential registration errors.  If voice status is
-- dropped or withdrawn, set severity flags to N except for capacity.
-- This allows capacity errors to be generated in the pre edit and resolves
-- the loop that was occuring in p_final_update.

      IF     NVL (sftregs_row.sftregs_error_flag, 'X') <> 'D' THEN
         IF NVL (sftregs_row.sftregs_vr_status_type,'X') NOT IN ('D', 'W')
         THEN
            appr_severity     :=    sobterm_row.sobterm_appr_severity ;
             levl_severity    :=    sobterm_row.sobterm_levl_severity ;
            coll_severity     :=    sobterm_row.sobterm_coll_severity ;
            degree_severity   :=    sobterm_row.sobterm_degree_severity ;
             program_severity :=    sobterm_row.sobterm_program_severity ;
             majr_severity    :=    sobterm_row.sobterm_majr_severity ;
             camp_severity    :=    sobterm_row.sobterm_camp_severity ;
              clas_severity   :=    sobterm_row.sobterm_clas_severity ;
             rept_severity    :=    sobterm_row.sobterm_rept_severity ;
             rpth_severity    :=    sobterm_row.sobterm_rpth_severity ;
             dept_severity    :=    sobterm_row.sobterm_dept_severity ;
             atts_severity    :=    sobterm_row.sobterm_atts_severity ;
              chrt_severity   :=    sobterm_row.sobterm_chrt_severity ;
           -- C3SC 8.7.1.1 GRS 04/OCT/2013 Modified
             capc_severity    :=    sobterm_row.sobterm_capc_severity ;
          /* capc_severity:= sv_auth_addcodes_bp.F_set_capacity_value(p_term_code =>sftregs_row.sftregs_term_code,
                                                 p_pidm =>     sftregs_row.sftregs_pidm,
                                                 p_rsts_code =>sftregs_row.sftregs_rsts_code,
                                                 p_crn        =>sftregs_row.sftregs_crn,
                                                 p_rsts_date  =>sftregs_row.sftregs_rsts_date,
                                                 p_capacity_in =>sobterm_row.sobterm_capc_severity
                                                 );    */

          --END C3SC
        ELSE
             appr_severity     :=   'N' ;
             levl_severity    :=    'N' ;
            coll_severity     :=    'N' ;
            degree_severity   :=    'N' ;
             program_severity :=    'N' ;
             majr_severity    :=    'N' ;
             camp_severity    :=    'N' ;
              clas_severity   :=    'N' ;
             rept_severity    :=    'N' ;
             rpth_severity    :=    'N' ;
             dept_severity    :=    'N' ;
             atts_severity    :=    'N' ;
              chrt_severity   :=    'N' ;
             -- C3SC 8.7.1.1 GRS 04/OCT/2013 Modified
             capc_severity    :=    sobterm_row.sobterm_capc_severity;
            /* capc_severity:= sv_auth_addcodes_bp.F_set_capacity_value(p_term_code =>sftregs_row.sftregs_term_code,
                                                 p_pidm =>     sftregs_row.sftregs_pidm,
                                                 p_rsts_code =>sftregs_row.sftregs_rsts_code,
                                                 p_crn        =>sftregs_row.sftregs_crn,
                                                 p_rsts_date  =>sftregs_row.sftregs_rsts_date,
                                                 p_capacity_in =>sobterm_row.sobterm_capc_severity
                                                 );
           */
            --END C3SC
        END IF;

         sfkedit.p_pre_edit (
            sftregs_row,
            stcr_err_ind,
            appr_err,
            old_stvrsts_row.stvrsts_incl_sect_enrl,
            old_stvrsts_row.stvrsts_wait_ind,
            stvrsts_row.stvrsts_incl_sect_enrl,
            stvrsts_row.stvrsts_wait_ind,
            appr_severity,
            levl_severity,
            coll_severity,
            degree_severity,
            program_severity,
            majr_severity,
            camp_severity,
            clas_severity,
            capc_severity,
            rept_severity,
            rpth_severity,
            dept_severity,
            atts_severity,
            chrt_severity,
            sgrclsr_clas_code,
            scbcrse_row.scbcrse_max_rpt_units,
            scbcrse_row.scbcrse_repeat_limit,
            ssbsect_row.ssbsect_sapr_code,
            ssbsect_row.ssbsect_reserved_ind,
            ssbsect_row.ssbsect_seats_avail,
            ssbsect_row.ssbsect_wait_count,
            ssbsect_row.ssbsect_wait_capacity,
            ssbsect_row.ssbsect_wait_avail,
            source_system,
            auth_over_census => lv_auth_course_reg_rec.r_over_census,  --Defect:1-3UQPUS C3SC ADDED GRS  12/03/2008
            auth_over_closed =>NULL,                                   --Defect:1-3UQPUS C3SC ADDED GRS  12/03/2008
            auth_over_started =>lv_auth_course_reg_rec.r_over_started  --Defect:1-3UQPUS C3SC ADDED GRS  12/03/2008
         );
      END IF;

	  -- C3SC, GR 19/02/06, Added
-- bypass AUTH REQD errors if override flag is O

	sv_auth_addcodes_bp.P_rest_message(p_error_flag_in=>sftregs_row.sftregs_error_flag,
	                                   p_rmsg_cde_in_out=>sftregs_row.sftregs_rmsg_cde,  --C3SC GRS 04/12/2009 ADD
   	                                 p_error_mesg_in_out=>sftregs_row.sftregs_message
   	                                   );
-- End C3SC

      IF stcr_err_ind = 'Y'
      THEN
         sftregs_row.sftregs_rsts_code :=
                                     SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2);
         sftregs_row.sftregs_vr_status_type :=
             sfkfunc.f_get_rsts_type(sftregs_row.sftregs_rsts_code);

         sftregs_row.sftregs_remove_ind := 'Y';
      END IF;

      IF ssbsect_row.ssbsect_tuiw_ind = 'Y'
      THEN
         sftregs_row.sftregs_waiv_hr := '0';
      ELSE
         sftregs_row.sftregs_waiv_hr := sftregs_row.sftregs_bill_hr;
      END IF;

      IF crn IS NULL
      THEN
         bwcklibs.p_upd_sftregs (sftregs_row);

      ELSE
         bwcklibs.p_updvardata_sftregs (sftregs_row);
      END IF;
END p_updcrse;

/*****************************************************************************/
/* VR Registration Management Controls - coding to allow use in Student to   */
/* process with Alternate PIN                                                */
/*****************************************************************************/

   FUNCTION f_getstudentname (pidm_in NUMBER)
      RETURN VARCHAR2
   IS
      stud_name   VARCHAR2 (185);
   BEGIN
      OPEN getstudentnamec (pidm_in);
      FETCH getstudentnamec INTO stud_name;
      CLOSE getstudentnamec;
      RETURN stud_name;
   END;

   PROCEDURE p_getstudentinfo (
      pidm_in   IN   spriden.spriden_pidm%TYPE,
      term_in   IN   stvterm.stvterm_code%TYPE
   )
   IS
      CURSOR studentinfo_c (pidm_in spriden.spriden_pidm%TYPE,
                            term_in stvterm.stvterm_code%TYPE)
      IS
      SELECT sgbstdn_styp_code
        FROM stvstst, sgbstdn
       WHERE sgbstdn_pidm = pidm_in
         AND sgbstdn_stst_code = stvstst_code
         AND stvstst_reg_ind = 'Y'
         AND sgbstdn_term_code_eff =
              (SELECT MAX (a.sgbstdn_term_code_eff)
                 FROM sgbstdn a
                WHERE a.sgbstdn_pidm = pidm_in
                  AND a.sgbstdn_term_code_eff <= term_in);

   BEGIN
      OPEN studentinfo_c(pidm_in, term_in);
      FETCH studentinfo_c INTO stud_type;
      IF studentinfo_c%notfound
      THEN
          contin_check := 'N';
      END IF;
      CLOSE studentinfo_c;

      lcur_tab := sokccur.f_current_active_curriculum(
                             p_pidm => pidm_in,
                             p_lmod_code => sb_curriculum_str.f_learner,
                             p_key_seqno => 99,
                             p_eff_term  => term_in);
      stud_level := lcur_tab(1).r_levl_code;
      stud_college := lcur_tab(1).r_coll_code;
      stud_degr := lcur_tab(1).r_degc_code;
      stud_camp := lcur_tab(1).r_camp_code;
      stud_major := sokccur.f_fieldofstudy_value(
                             p_pidm => pidm_in,
                             p_lmod_code => sb_curriculum_str.f_learner,
                             p_term_code => lcur_tab(1).r_term_code,
                             p_key_seqno => lcur_tab(1).r_key_seqno,
                             p_lcur_seqno => lcur_tab(1).r_seqno,
                             p_lfst_code => sb_fieldofstudy_str.f_major,
                             p_order => 1,
                             p_field => 'MAJR');
      stud_dept := sokccur.f_fieldofstudy_value(
                             p_pidm => pidm_in,
                             p_lmod_code => sb_curriculum_str.f_learner,
                             p_term_code => lcur_tab(1).r_term_code,
                             p_key_seqno => lcur_tab(1).r_key_seqno,
                             p_lcur_seqno => lcur_tab(1).r_seqno,
                             p_lfst_code => sb_fieldofstudy_str.f_major,
                             p_order => 1,
                             p_field => 'DEPT');


   END p_getstudentinfo;

   FUNCTION f_getearnedhrsclas (pidm_in NUMBER)
      RETURN VARCHAR2
   IS
      sgrclsr_rec   sgrclsr%ROWTYPE;
   BEGIN
      OPEN getearnedhrsc (pidm_in, stud_level);
      FETCH getearnedhrsc INTO stud_hrs;
      CLOSE getearnedhrsc;
      stud_hrs := NVL (stud_hrs, 0);
      OPEN getstudentclasc (stud_hrs, stud_level);
      FETCH getstudentclasc INTO sgrclsr_rec;
      CLOSE getstudentclasc;
      RETURN sgrclsr_rec.sgrclsr_clas_code;
   END;

--
-- This procedure initialises the variables required as parameters to
-- the group edits and final update procedures.
-- =======================================================================
PROCEDURE p_init_final_update_vars (pidm_in IN sfrracl.sfrracl_pidm%TYPE,
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
--
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
END p_init_final_update_vars;


--
-- This procedure calls sfkfunc.f_registration_access upon entry to
-- add/drop processing, to ensure the integrity of SFTREGS for the given
-- student/term.
-- If admindrops are being processed, this procedure then drops those
-- courses that have been found to be in error by the baseline function.
-- =======================================================================

FUNCTION f_registration_access (
   pidm_in        IN   sfrracl.sfrracl_pidm%TYPE,
   term_in        IN   sfrracl.sfrracl_term_code%TYPE,
   access_id_in   IN   sfrracl.sfrracl_reg_access_ID%TYPE
)
   RETURN BOOLEAN
IS
   local_capp_tech_error VARCHAR2 (30);
   minh_admin_error      VARCHAR2 (1);

BEGIN
   IF NOT sfkfunc.f_registration_access (
         pidm_in,
         term_in,
         'PROXY',
         access_id_in
      )
   THEN
      RETURN FALSE;
   END IF;

   minh_admin_error :=
       sfkfunc.f_get_minh_admin_error (pidm_in, term_in);
   IF minh_admin_error = 'Y'
   THEN
      RETURN FALSE;
   END IF;

   local_capp_tech_error :=
       sfkfunc.f_get_capp_tech_error (pidm_in, term_in);
   IF local_capp_tech_error IS NOT NULL
   THEN
      RETURN FALSE;
   END IF;

   IF NOT f_finalize_admindrops (pidm_in, term_in, access_id_in)
   THEN
       RETURN FALSE;
   END IF;

   RETURN TRUE;

END f_registration_access;

FUNCTION f_finalize_admindrops (
   pidm_in        IN   sfrracl.sfrracl_pidm%TYPE,
   term_in        IN   sfrracl.sfrracl_term_code%TYPE,
   access_id_in   IN   sfrracl.sfrracl_reg_access_ID%TYPE
)
   RETURN BOOLEAN
IS
    error_rec             sftregs%ROWTYPE;
    error_flag            sftregs.sftregs_error_flag%TYPE;
    tmst_flag             sftregs.sftregs_error_flag%TYPE;
    drop_code             stvrsts.stvrsts_code%TYPE;
    number_of_dropcodes   NUMBER;
    may_drop_last         BOOLEAN;
    drop_failed           BOOLEAN;
    drop_attempted        BOOLEAN;

-- begin FR 8.4.0.2
    discount_exist varchar2(200);
    lv_function varchar2(300);
    sf_installed_value varchar(100);
    drop_setup_tregs BOOLEAN;
-- end FR 8.4.0.2

    CURSOR fatal_errors_c (pidm_in sfrracl.sfrracl_pidm%TYPE,
                           term_in sfrracl.sfrracl_term_code%TYPE)
    IS
       SELECT *
         FROM sftregs
        WHERE sftregs_pidm = pidm_in
          AND sftregs_term_code = term_in
          AND sftregs_error_flag = 'F';

BEGIN

    drop_attempted := FALSE;
    drop_failed := FALSE;
    drop_setup_tregs := TRUE;  -- FR 8.4.0.2

 --
 -- Get value to see if Flexible Registration is installed
    sf_installed_value := genutil.product_installed('SF');
 -- =======================================================================
 --
 -- If admindrops are being processed.
 -- =======================================================================
    IF sfkfunc.f_do_admindrop
    THEN
 --
 -- For every course found to be in error.
 -- =======================================================================
       FOR sftregs_rec IN fatal_errors_c(pidm_in, term_in)
       LOOP
           drop_attempted := TRUE;

 --
 -- Find a dropcode.
 -- =======================================================================
          bwcklibs.p_get_dropcode (
             sftregs_rec.sftregs_term_code,
             sftregs_rec.sftregs_crn,
             sftregs_rec.sftregs_ptrm_code,
             sftregs_rec.sftregs_start_date,
             sftregs_rec.sftregs_completion_date,
             sftregs_rec.sftregs_rsts_date,
             sftregs_rec.sftregs_dunt_code,
             drop_code,
             number_of_dropcodes
          );

 --
 -- We found a dropcode. So drop, or drop/delete if approp.
 -- =======================================================================
          IF number_of_dropcodes = 1
          THEN
             drop_setup_tregs := TRUE;

---
-- Check if Flexible Registration is installed and if the CRN has a discount applied. If it does do not do a drop on it.
-- =======================================================================
              IF NVL (sf_installed_value, 'N')  = 'Y' THEN
                  lv_function := 'begin :1 := sffkutil.f_check_discount(:2,:3,:4); end;';
                  EXECUTE IMMEDIATE lv_function
                  USING OUT discount_exist,  IN pidm_in, sftregs_rec.sftregs_crn,sftregs_rec.sftregs_term_code;
                   IF (NVL (discount_exist, 'N') = 'Y') THEN

                    drop_setup_tregs := FALSE;
                  END IF;
              END IF;

              IF drop_setup_tregs
              THEN
              bwcklibs.p_update_to_drop ( pidm_in,
                                          sftregs_rec.sftregs_term_code,
                                          sftregs_rec.sftregs_crn,
                                          drop_code );

              ELSE -- FR 8.4.0.2
                 sfkmods.p_reset_sftregs_fields (pidm_in, sftregs_rec.sftregs_term_code, sftregs_rec.sftregs_crn, null);
              END IF;


 --
 -- We didn't find a dropcode - all drops for all courses will be ignored.
 -- =======================================================================
          ELSE
             drop_failed := TRUE;
             EXIT;
          END IF;

           discount_exist:= null;  -- FR 8.4.0.2
            lv_function :=null;    -- FR 8.4.0.2
       END LOOP;

       IF NOT drop_attempted THEN
           RETURN TRUE;
       END IF;

       IF drop_failed
       THEN
 --
 -- We didn't find a dropcode - all drops for all courses will be ignored.
 -- =======================================================================
          sfkmods.p_reset_sftregs_fields (pidm_in, term_in, NULL, 'ADMINDROP');

       ELSE

 --
 -- We found dropcodes for all courses, so transfer drops into sfrstcr.
 -- =======================================================================
          may_drop_last := sfkdrop.f_drop_last ('W');

          bwckregs.p_init_final_update_vars (pidm_in, term_in);

          sfkedit.p_update_regs (
             pidm_in,
             term_in,
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

 --
 -- Transfer drops into sfrstcr failed, so cancel all admindrop changes,
 -- refresh sftregs from sfrstcr.
 -- =======================================================================
          IF NVL (error_flag, 'N') <> 'N'
          THEN
              IF NOT sfkfunc.f_registration_access ( pidm_in,
                                                     term_in,
                                                     'PROXY',
                                                     access_id_in,
                                                     bypass_admin_in => 'Y')
              THEN
                 RETURN FALSE;
              END IF;
 --
 -- Transfer drops into sfrstcr went ok, so remove redundant temporary
 -- table rows.
 -- =======================================================================
          ELSE
             sfkmods.p_delete_sftregs_removed (pidm_in, term_in);
             sfkmods.p_delete_sftrgam (pidm_in, term_in);
          END IF;
       END IF;
    END IF;

    COMMIT;

    RETURN TRUE;
 END f_finalize_admindrops;


--
-- This procedure transfers sftregs changes into sfrstcr.
-- If capacity errors are found while doing this, the problem course is
-- droppped, and group edits are redone for the other courses. Any
-- problem courses found then are dropped, and the sfrstcr transfer is
-- re-tried, and so on until no new errors are found.
-- It then redisplays the add/drop page.
-- =======================================================================
PROCEDURE p_final_updates (
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
   drop_failures_in       IN       sfkcurs.drop_problems_rec_tabtype
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
   minh_error         VARCHAR2 (4)                    := 'N';
   genpidm            spriden.spriden_pidm%TYPE;
   error_flag         sftregs.sftregs_error_flag%TYPE := 'N';
   error_rec          sftregs%ROWTYPE;
   source_system      VARCHAR2 (2);
   clas_code          sgrclsr.sgrclsr_clas_code%TYPE;
   may_drop_last      BOOLEAN                         := TRUE;
   drop_problems      sfkcurs.drop_problems_rec_tabtype := drop_problems_in;
   drop_failures      sfkcurs.drop_problems_rec_tabtype := drop_failures_in;


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

   <<term_loop>>
   FOR i IN 1 .. term_in.COUNT
   LOOP

      /* does term exist in sfbetrm? */
      IF sb_enrollment.f_exists( p_pidm => genpidm,
                                 p_term_code => term_in (i)) = 'Y'
      THEN

         p_init_final_update_vars (genpidm, term_in (i));

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
                NVL (sobterm_row.sobterm_mexc_severity, 'N'),
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
                       sftregs_remove_ind = 'Y',
                       sftregs_user = goksels.f_get_ssb_id_context,
                       sftregs_activity_date = SYSDATE
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

   IF NVL (sftregs_row.sftregs_vr_status_type,'X') IN ('D', 'W') THEN
           sobterm_row.sobterm_capc_severity := 'N';
   END IF;

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
                      sftregs_remove_ind = 'Y',
                      sftregs_user = goksels.f_get_ssb_id_context,
                      sftregs_activity_date = SYSDATE
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
                  NVL (sobterm_row.sobterm_mexc_severity, 'N'),
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

END p_final_updates;

BEGIN
   /* initialization part start here */
   term := bwcklibs.f_getterm;
   id1 := bwcklibs.f_getid;
   regs_date := bwcklibs.f_getregdate;
   hold_passwd := bwcklibs.f_getholdpass;
   samsys := bwcklibs.f_getsamsys;
   sobterm_row := bwcklibs.f_getsobterm;
   ssbsect_row.ssbsect_subj_code := '';
   ssbsect_row.ssbsect_crse_numb := '';
   ssbsect_row.ssbsect_seq_numb := '';
   ssbsect_row.ssbsect_term_code := '';
   ssbsect_row.ssbsect_crn := '';
   tot_cred_hrs := '';
   tot_bill_hrs := '';
   tot_ceu_hrs := '';
   astd_prevent_reg_ind := '';
   cast_prevent_reg_ind := '';
   ests_prev_reg := '';
   stst_reg_ind := '';
   ests_eff_crse_stat := '';
   sgrclsr_clas_code := '';
   clas_desc := '';
   mhrs_min_hrs := NULL;
   mhrs_max_hrs := NULL;
   minh_srce := NULL;
   maxh_srce := NULL;
   appr_error := '';
   message_text := '';
   xlst_group := '';
   xlst_seats_avail := 0;
   ssrresv_seats_avail := 0;
   ssrresv_rowid := '';
   ssrresv_wait_avail := 0;
   ssrresv_wait_count := 0;
   ssrresv_wait_capacity := 0;
   ssbsect_rowid := '';
   ssbxlst_rowid := '';
   override := '';
   print_bill := '';
   sftregs_wait_over := '';
END bwckregs;