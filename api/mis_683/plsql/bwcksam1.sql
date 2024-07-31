create or replace PACKAGE BODY bwcksams
AS
--AUDIT_TRAIL_MSGKEY_UPDATE
-- PROJECT : MSGKEY
-- MODULE  : BWCKSAM1
-- SOURCE  : enUS
-- TARGET  : I18N
-- DATE    : Wed Dec 02 12:19:54 2015
-- MSGSIGN : #d81d31b7a7fa65ae
--TMI18N.ETR DO NOT CHANGE--
--
-- FILE NAME..: bwcksam1.sql
-- RELEASE....: 8.7.1.1[C3SC 8.11.0.1]
-- OBJECT NAME: BWCKSAMS
-- PRODUCT....: CALB
-- USAGE......:
-- COPYRIGHT..: Copyright 2015 Ellucian Company L.P. and its affiliates.
--
   /* variables visible only in this package or
      any items which must be maintined throughout
      a session or across transactions */
--   curr_release   CONSTANT VARCHAR2 (30)    := '8.7.1.1[C3SC 8.11.0.1]';
   waitlist_call Varchar2(12);
   waitlist_position Varchar2(12);
   --end local09
   --local07 
   crn sftregs.sftregs_crn%Type;
   --end local07
-- local01 change release.
   curr_release   CONSTANT VARCHAR2 (30)    := '8.7.1.1[C3SC 8.11.0.1A]';
   row_count               NUMBER;
   global_pidm             spriden.spriden_pidm%TYPE;
   term                    stvterm.stvterm_code%TYPE;
   stvterm_rec             stvterm%ROWTYPE;
   sorrtrm_rec             sorrtrm%ROWTYPE;
   sfbetrm_rec             sfbetrm%ROWTYPE;
   sgbstdn_rec             sgbstdn%ROWTYPE;
   foo_rec                 sfbetrm%ROWTYPE;
   scbcrse_row             scbcrse%ROWTYPE;
   term_desc               stvterm%ROWTYPE;
   sobterm_row             sobterm%ROWTYPE;
   use_man_control         VARCHAR2 (1);
   internal_code           gtvsdax.gtvsdax_internal_code%TYPE;
   gtvsdax_group           gtvsdax.gtvsdax_internal_code_group%TYPE;

   CURSOR chk_sfrbtch (
      term      stvterm.stvterm_code%TYPE,
      pidm_in   spriden.spriden_pidm%TYPE
   )
   IS
      SELECT 'X'
        FROM sfrbtch
       WHERE sfrbtch_term_code = term
         AND sfrbtch_pidm = pidm_in;

--------------------------------------------------------------------------
   FUNCTION F_RegsStu (
      pidm_in     IN   spriden.spriden_pidm%TYPE,
      term        IN   stvterm.stvterm_code%TYPE DEFAULT NULL,
      proc_name   IN   VARCHAR2 DEFAULT NULL
   )
      RETURN BOOLEAN
   IS
      sql_err                     NUMBER;
      gen_proc_name               VARCHAR2 (80);
      genpidm                     spriden.spriden_pidm%TYPE;
      restrict_time_ticket        VARCHAR2 (1);
      restrict_time_ticket_code   gtvsdax.gtvsdax_internal_code%TYPE
                                                                := 'WEBRESTTKT';
      web_vr_ind                  VARCHAR2 (1)                       := 'W';
   BEGIN
--
-- Establish the student pidm.
-- ==================================================
      IF NVL (twbkwbis.f_getparam (pidm_in, 'STUFAC_IND'), 'STU') = 'FAC'
      THEN
         genpidm :=
              TO_NUMBER (twbkwbis.f_getparam (pidm_in, 'STUPIDM'), '999999999');
      ELSE
         genpidm := pidm_in;
      END IF;

--
--
-- ==================================================
      bwcklibs.p_initvalue (pidm_in, term, NULL, SYSDATE, NULL, NULL);

--
-- Check if person is deceased.
-- ==================================================
      IF bwckregs.f_deceasedperscheck (sql_err, genpidm)
      THEN
         RAISE bwcklibs.deceased_person_err;
      END IF;

--
-- Do either registration management control checking
-- or time ticket checking, based on a gtvsdax flag.
-- ==================================================
      internal_code := 'WEBMANCONT';
      gtvsdax_group := 'WEBREG';
      OPEN sfkcurs.get_gtvsdaxc (internal_code, gtvsdax_group);
      FETCH sfkcurs.get_gtvsdaxc INTO use_man_control;
      CLOSE sfkcurs.get_gtvsdaxc;
      OPEN sfkcurs.get_gtvsdaxc (restrict_time_ticket_code, gtvsdax_group);
      FETCH sfkcurs.get_gtvsdaxc INTO restrict_time_ticket;
      CLOSE sfkcurs.get_gtvsdaxc;

      IF NOT sfkrctl.f_check_reg_appointment (
            genpidm,
            term,
            NVL (use_man_control, 'N'),
            NVL (restrict_time_ticket, 'N'),
            web_vr_ind
         )
      THEN
         IF NVL (use_man_control, 'N') <> 'Y'
         THEN
            RAISE bwcklibs.time_ticket_err;
         ELSE
            RAISE bwcklibs.mgmt_control_err;
         END IF;
      END IF;

--
-- Get the student information record.
-- ==================================================
      FOR sgbstdn IN sgklibs.sgbstdnc (genpidm, term)
      LOOP
         sgbstdn_rec := sgbstdn;
      END LOOP;

--
-- Check the student record.
-- ==================================================
      bwckregs.p_regschk (sgbstdn_rec);
      row_count := 0;

--
-- Get the registration header record.
-- ==================================================
      FOR sfbetrm IN sfkcurs.sfbetrmc (genpidm, term)
      LOOP
         sfbetrm_rec := sfbetrm;
         row_count := sfkcurs.sfbetrmc%rowcount;
      END LOOP;

--
-- Check the registration header record if it exists.
-- If not it will be created elsewhere when required.
-- ==================================================
      IF row_count <> 0
      THEN
         bwckregs.p_regschk (sfbetrm_rec);
      END IF;

--
--
-- ==================================================
      sfkvars.regs_allowed := f_validregdate (term);
      RETURN TRUE;
   EXCEPTION
      WHEN bwcklibs.time_ticket_err
      THEN
         IF NVL (twbkwbis.f_getparam (pidm_in, 'STUFAC_IND'), 'STU') = 'FAC'
         THEN
/*
 * FACWEB-specific code
 */
            IF NOT bwlkilib.f_add_drp (term)
            THEN
               NULL;
            END IF;
         ELSE
/*
 * STUWEB-specific code
 */
            IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
            THEN
               NULL;
            END IF;
         END IF;                      /* END stu/fac specific code sections */

         bwckfrmt.p_open_doc (proc_name, term);
         --
         row_count := 0;

         FOR time_rec IN sfkcurs.timetksc (genpidm, term)
         LOOP
            row_count := sfkcurs.timetksc%rowcount;

            IF sfkcurs.timetksc%rowcount > 0
            THEN
               IF sfkcurs.timetksc%rowcount = 1
               THEN
                  twbkwbis.p_dispinfo (proc_name, 'TTTIMES');
                  twbkfrmt.p_tableopen (
                     'DATADISPLAY',
                     cattributes   => 'SUMMARY="' ||
                                         g$_nls.get ('BWCKSAM1-0000',
                                            'SQL',
                                            'This layout table is used to present time ticketing error related information') ||
                                         '."'
                  );
                  twbkfrmt.p_tablerowopen;
                  twbkfrmt.p_tabledataheader (
                     g$_nls.get ('BWCKSAM1-0001', 'SQL', 'From')
                  );
                  twbkfrmt.p_tabledataheader (
                     g$_nls.get ('BWCKSAM1-0002', 'SQL', 'Begin Time')
                  );
                  twbkfrmt.p_tabledataheader (
                     g$_nls.get ('BWCKSAM1-0003', 'SQL', 'To')
                  );
                  twbkfrmt.p_tabledataheader (
                     g$_nls.get ('BWCKSAM1-0004', 'SQL', 'End Time')
                  );
                  twbkfrmt.p_tablerowclose;
               END IF;

               twbkfrmt.p_tablerowopen;
               twbkfrmt.p_tabledata (
                  TO_CHAR (
                     time_rec.rec_sfrwctl_begin_date,
                     twbklibs.date_display_fmt
                  )
               );
               twbkfrmt.p_tabledata (
                  TO_CHAR (
                     TO_DATE (time_rec.rec_sfrwctl_hour_begin, 'HH24MI'),
                     twbklibs.twgbwrul_rec.twgbwrul_time_fmt
                  )
               );
               twbkfrmt.p_tabledata (
                  TO_CHAR (
                     time_rec.rec_sfrwctl_end_date,
                     twbklibs.date_display_fmt
                  )
               );
               twbkfrmt.p_tabledata (
                  TO_CHAR (
                     TO_DATE (time_rec.rec_sfrwctl_hour_end, 'HH24MI'),
                     twbklibs.twgbwrul_rec.twgbwrul_time_fmt
                  )
               );
               twbkfrmt.p_tablerowclose;
            END IF;
         END LOOP;

         IF row_count > 0
         THEN
            twbkfrmt.p_tableclose;
         ELSE
            twbkwbis.p_dispinfo (proc_name, 'TTERR');
         END IF;

         twbkwbis.p_closedoc (curr_release);
         RETURN FALSE;
      WHEN bwcklibs.mgmt_control_err
      THEN
         IF NVL (twbkwbis.f_getparam (pidm_in, 'STUFAC_IND'), 'STU') = 'FAC'
         THEN
            /* FACWEB-specific code */
            IF NOT bwlkilib.f_add_drp (term)
            THEN
               NULL;
            END IF;
         ELSE
            /* STUWEB-specific code */
            IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
            THEN
               NULL;
            END IF;
         END IF;                      /* END stu/fac specific code sections */

         bwckfrmt.p_open_doc (proc_name, term);
         twbkwbis.p_dispinfo (proc_name, 'MCERR');
         twbkwbis.p_closedoc (curr_release);
         RETURN FALSE;
      WHEN bwcklibs.deceased_person_err
      THEN
         IF NVL (twbkwbis.f_getparam (pidm_in, 'STUFAC_IND'), 'STU') = 'FAC'
         THEN
/*
 * FACWEB-specific code
 */
            IF NOT bwlkilib.f_add_drp (term)
            THEN
               NULL;
            END IF;
         ELSE
/*
 * STUWEB-specific code
 */
            IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
            THEN
               NULL;
            END IF;
         END IF;                      /* END stu/fac specific code sections */

         bwckfrmt.p_open_doc (proc_name, term);
         twbkfrmt.p_printmessage (
            g$_nls.get ('BWCKSAM1-0005',
               'SQL',
               'Please Contact the Registration Administrator; You May Not Register'),
            'ERROR'
         );
         twbkwbis.p_closedoc (curr_release);
         RETURN FALSE;
      WHEN OTHERS
      THEN
         IF NVL (twbkwbis.f_getparam (pidm_in, 'STUFAC_IND'), 'STU') = 'FAC'
         THEN
/*
 * FACWEB-specific code
 */
            IF NOT bwlkilib.f_add_drp (term)
            THEN
               NULL;
            END IF;
         ELSE
/*
 * STUWEB-specific code
 */
            IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
            THEN
               NULL;
            END IF;
         END IF;                      /* END stu/fac specific code sections */

         bwckfrmt.p_open_doc (proc_name, term);
         IF SQLCODE  <= -20000 AND SQLCODE >= -20999 THEN
             twbkfrmt.p_printmessage (
            SUBSTR (SQLERRM, INSTR (SQLERRM, ':') + 1),
            'ERROR'
         );
         ELSE
             twbkfrmt.p_printmessage (
                G$_NLS.Get ('BWCKSAM1-0006','SQL','Error occurred while checking if the current student is eligible to register for the current term.'),
                'ERROR'
             );
         END IF;
                  twbkwbis.p_closedoc (curr_release);
         RETURN FALSE;
   END F_RegsStu;

--------------------------------------------------------------------------
   FUNCTION F_ValidRegDate (
      term        IN   stvterm.stvterm_code%TYPE,
      rsts        IN   stvrsts.stvrsts_code%TYPE DEFAULT NULL,
      ptrm        IN   VARCHAR2 DEFAULT NULL,
      regs_date   IN   DATE DEFAULT SYSDATE,
      err_msg     IN   VARCHAR2 DEFAULT 'N'
   )
      RETURN BOOLEAN
   IS
      valid_date   BOOLEAN;

      CURSOR ssrrstsc (rsts VARCHAR2, term VARCHAR2)
      IS
         SELECT DISTINCT ssrrsts_rsts_code
           FROM ssrrsts
          WHERE ssrrsts_rsts_code = rsts
            AND ssrrsts_term_code = term;
   BEGIN
      valid_date := FALSE;
      sfkvars.add_allowed := FALSE;

      FOR stvrsts IN sfkcurs.stvrstsc (rsts)
      LOOP
         FOR sfrrsts IN sfkcurs.sfrrstsc (stvrsts.stvrsts_code, term, ptrm)
         LOOP
            IF TRUNC (SYSDATE) BETWEEN sfrrsts.sfrrsts_start_date
                   AND sfrrsts.sfrrsts_end_date
            THEN
               valid_date := TRUE;

               IF stvrsts.stvrsts_code =
                                      SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2)
               THEN
                  sfkvars.add_allowed := TRUE;
               END IF;
            END IF;
         END LOOP;

         IF sfkvars.add_allowed
         THEN
            EXIT;
         END IF;
      END LOOP;

      IF ptrm IS NULL
      AND ( NOT sfkvars.add_allowed
            OR NOT valid_date )
      THEN
         FOR stvrsts IN sfkcurs.stvrstsc (rsts)
         LOOP
            FOR ssrrsts IN ssrrstsc (stvrsts.stvrsts_code, term)
            LOOP
               valid_date := TRUE;

               IF stvrsts.stvrsts_code =
                                      SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2)
               THEN
                  sfkvars.add_allowed := TRUE;
               END IF;
            END LOOP;

            IF sfkvars.add_allowed
            THEN
               EXIT;
            END IF;
         END LOOP;
      END IF;

      IF    valid_date
         OR err_msg = 'N'
      THEN
         RETURN valid_date;
      END IF;

      twbkfrmt.p_printmessage (
         g$_nls.get ('BWCKSAM1-0007',
            'SQL',
            'Registration is not allowed at this time'),
         'ERROR'
      );
      RETURN valid_date;
   END F_ValidRegDate;

   FUNCTION F_CheckCAPPCrn (
      pidm_in   IN   spriden.spriden_pidm%TYPE,
      term      IN   stvterm.stvterm_code%TYPE,
      crn       IN   ssbsect.ssbsect_crn%TYPE
   )
      RETURN VARCHAR2
   IS
      area_fnd   VARCHAR2 (1);

      CURSOR chk_sftarea( pidm_in spriden.spriden_pidm%TYPE,
                          term_in stvterm.stvterm_code%TYPE,
                          crn_in ssbsect.ssbsect_crn%TYPE )
      IS
         SELECT DISTINCT 'Y'
           FROM sftarea
          WHERE sftarea_pidm = pidm_in
            AND sftarea_term_code = term_in
            AND sftarea_crn = crn_in;
   BEGIN
      OPEN chk_sftarea(pidm_in, term, crn);
      FETCH chk_sftarea INTO area_fnd;

      IF chk_sftarea%NOTFOUND
      THEN
         area_fnd := 'N';
      END IF;

      CLOSE chk_sftarea;
      RETURN area_fnd;
   END F_CheckCAPPCrn;

--------------------------------------------------------------------------
    --local09 waitlist check to see if user has an email address.
    --if so then show options to waitlist else pop up error screen.
    Function f_got_email(pidm_in In Number) Return Boolean Is
        Result   Boolean;
        holdthis Varchar2(1) := '';
        Cursor got_email Is
            Select 'X'
            From   goremal a
            Where  a.goremal_pidm = pidm_in
            And    a.goremal_status_ind = 'A';
    Begin
        Open got_email;
        Fetch got_email
            Into holdthis;
        Close got_email;
        If nvl(holdthis,
               'Z') = 'X'
        Then
            Result := True;
        Else
            Result := False;
        End If;
        Return(Result);
    End f_got_email;

    --end local09
--------------------------------------------------------------------------
   PROCEDURE p_immediate_fee_assess (
      term      IN   stvterm.stvterm_code%TYPE,
      pidm_in   IN   spriden.spriden_pidm%TYPE
   )
   IS
      chk_sfrbtch_rec   VARCHAR2 (1);
      sfrbtch_row       sfrbtch%ROWTYPE;
   BEGIN
      bwcklibs.p_getsobterm (term, sobterm_row);

      -- is online fee assessment enabled?
      IF sobterm_row.sobterm_fee_assess_vr = 'Y'
      AND sobterm_row.sobterm_fee_assessment = 'Y'
      THEN
         -- have any assessable actions taken place
         -- since last assessment?
         OPEN chk_sfrbtch (term, pidm_in);
         FETCH chk_sfrbtch INTO chk_sfrbtch_rec;

         IF chk_sfrbtch%FOUND
         THEN
            bwckregs.p_regsfees;
            --BOGW C3SC ADDED GRS 12/09/2008
            IF sobterm_row.SOBTERM_EXEMP_ONLINE_IND = 'Y'
             AND sobterm_row.SOBTERM_EXEMPT_VR_IND = 'Y' 
            THEN 
              sv_state_aid_appl_bp.p_applyexemptions(term_in=>term,   
                                                    pidm_in=>pidm_in 
                                                    );
            END IF;                                         
           -- C3SC BOGW END   
            sfrbtch_row.sfrbtch_pidm := pidm_in;
            sfrbtch_row.sfrbtch_term_code := term;
            bwcklibs.p_del_sfrbtch (sfrbtch_row);
         END IF;

         CLOSE chk_sfrbtch;
      END IF;
--
   END p_immediate_fee_assess;

--------------------------------------------------------------------------
   PROCEDURE P_DispChgCrseOpt (
      term              IN   stvterm.stvterm_code%TYPE DEFAULT NULL,
      crn_in            IN   OWA_UTIL.ident_arr,
      cred_in           IN   OWA_UTIL.ident_arr,
      gmod_in           IN   OWA_UTIL.ident_arr,
      levl_in           IN   OWA_UTIL.ident_arr,
      err_code          IN   OWA_UTIL.ident_arr,
      err_msg           IN   twbklibs.error_msg_tabtype,
      first_pass        IN   BOOLEAN DEFAULT FALSE,
      capp_tech_error   IN   VARCHAR2 DEFAULT NULL,
      spath_in          IN   OWA_UTIL.ident_arr
   )
   IS
      cred_ctr                     NUMBER                    := 0;
      gmod_ctr                     NUMBER                    := 0;
      levl_ctr                     NUMBER                    := 0;
      regs_ctr                     NUMBER;
      i                            INTEGER                   := 2;
      error_exist                  BOOLEAN;
      gmod_code                    stvgmod.stvgmod_code%TYPE;
      levl_code                    stvlevl.stvlevl_code%TYPE;
      spath_seqno                  sftregs.sftregs_stsp_key_sequence%TYPE;
      inv_crn                      BOOLEAN;
      no_header                    BOOLEAN;
      genpidm                      spriden.spriden_pidm%TYPE;
      cred_hour_changes_allowed    BOOLEAN                   := FALSE;
      grade_mode_changes_allowed   BOOLEAN                   := FALSE;
      level_changes_allowed        BOOLEAN                   := FALSE;
      spath_changes_allowed        BOOLEAN                   := FALSE;
      course_changes_allowed       BOOLEAN                   := FALSE;
      course_is_graded             BOOLEAN                   := FALSE;
      minh_error                   BOOLEAN                   := FALSE;
      section_id                   INTEGER                   := 0;
      call_path                    VARCHAR2 (1);
      proc_called_by               VARCHAR2 (50);
      msg                          VARCHAR2 (200);
      wmnu_rec                     twgbwmnu%ROWTYPE;
      crndirect                    VARCHAR2(1) :=
                                     NVL(SUBSTR(bwcklibs.f_getgtvsdaxrule('CRNDIRECT','WEBREG'),1,1),'N');
      -- SP Changes
      
      
      CURSOR sgvstsp_all_c (pidm SPRIDEN.SPRIDEN_PIDM%TYPE,
                    term_code STVTERM.STVTERM_CODE%TYPE)
       IS
       SELECT sgvstsp_name,
              sgvstsp_key_seqno,
              sgvstsp_term_code_eff
         FROM sovlcur, sgvstsp
        WHERE sgvstsp_pidm = pidm        
          AND ( sgvstsp_astd_code IS NULL
                OR
                EXISTS                  
                (SELECT 'X' 
                   FROM stvastd 
                  WHERE stvastd_code = sgvstsp_astd_code
                    AND NVL(stvastd_prevent_reg_ind,'N') ='N' )
                )
          AND ( sgvstsp_cast_code IS NULL
                OR
                EXISTS                  
                (SELECT 'X'
                   FROM stvcast 
                  WHERE stvcast_code = sgvstsp_cast_code
                    AND stvcast_prevent_reg_ind ='N' )
                )
          AND sgvstsp_term_code_eff =
              (SELECT MAX(m.sgrstsp_term_code_eff)
                 FROM sgrstsp m
                WHERE m.sgrstsp_pidm = sgvstsp_pidm
                  AND m.sgrstsp_key_seqno = sgvstsp_key_seqno
                  AND m.sgrstsp_term_code_eff <= term_code)
          AND sgvstsp_enroll_ind  = 'Y'      
          AND sovlcur_current_ind = 'Y'
          AND sovlcur_active_ind = 'Y'
          AND sovlcur_pidm = sgvstsp_pidm
          AND sovlcur_key_seqno = sgvstsp_key_seqno
          AND sovlcur_lmod_code = sb_curriculum_str.f_learner
          AND NOT EXISTS
             (SELECT 'X' 
                FROM stvests, sfrensp
               WHERE sfrensp_pidm = sgvstsp_pidm
                 AND sfrensp_term_code = term_code
                 AND sfrensp_key_seqno = sgvstsp_key_seqno
                 AND sfrensp_ests_code = stvests_code
                 AND stvests_prev_reg = 'Y')
        order by sgvstsp_term_code_eff, sgvstsp_key_seqno;
       lv_valid_sp_count         NUMBER;
       lv_sp_name                VARCHAR2(4000);
       lv_sp_key_seq_no          SGRSTSP.SGRSTSP_KEY_SEQNO%TYPE;
       row_count                 NUMBER:=0;
       lv_sgvstsp_term           STVTERM.STVTERM_CODE%TYPE;
       lv_matched                BOOLEAN ;
       lv_sgrstsp_eff_term       STVTERM.STVTERM_CODE%TYPE;
       lv_sp_req                 VARCHAR2(1);  -- store it in a variable and use it inside the loop
       lv_sp_enabled             VARCHAR2(1);  -- for better performance
       lv_first_spath            SGRSTSP.SGRSTSP_KEY_SEQNO%TYPE;
          CURSOR get_sgrstsp_eff_term_c (p_pidm SPRIDEN.SPRIDEN_PIDM%TYPE,
                              p_term STVTERM.STVTERM_CODE%TYPE,
                              p_sp_seqno SGRSTSP.SGRSTSP_KEY_SEQNO%TYPE)  
             IS
         SELECT SGRSTSP_TERM_CODE_EFF 
           FROM SGRSTSP
          WHERE SGRSTSP_PIDM = p_pidm
            AND SGRSTSP_KEY_SEQNO = p_sp_seqno
            AND SGRSTSP_TERM_CODE_EFF  = (SELECT MAX(SGRSTSP_TERM_CODE_EFF )
                                            FROM SGRSTSP
                                           WHERE SGRSTSP_PIDM = p_pidm
                                             AND SGRSTSP_KEY_SEQNO = p_sp_seqno
                                             AND SGRSTSP_TERM_CODE_EFF <= p_term);



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
         call_path := 'F';
         proc_called_by := 'bwlkfrad.P_FacChangeCrseOpt';
         OPEN twbklibs.getmenuc (proc_called_by);
      ELSE
         genpidm := global_pidm;
         call_path := 'S';
         proc_called_by := 'bwskfreg.P_ChangeCrseOpt';
         OPEN twbklibs.getmenuc (proc_called_by);
      END IF;

      FETCH twbklibs.getmenuc INTO wmnu_rec;
      CLOSE twbklibs.getmenuc;

      IF capp_tech_error IS NOT NULL
      THEN
         IF capp_tech_error = 'PIPE'
         THEN
            msg := 'PIPE_ERROR';
         ELSE
            msg := 'CAPP_ERROR';
         END IF;

--
-- Return to the menu.
-- =================================================
         twbkwbis.p_genmenu (
            SUBSTR (
               wmnu_rec.twgbwmnu_back_url,
               INSTR (wmnu_rec.twgbwmnu_back_url, '/', -1) + 1
            ),
            msg,
            message_type   => 'ERROR'
         );
         RETURN;
      END IF;

--
-- Check term and start the web page for faculty.
-- =================================================
      IF call_path = 'F'
      THEN
         IF NOT bwlkilib.f_add_drp (term)
         THEN
            NULL;
         END IF;

         bwckfrmt.p_open_doc (proc_called_by, term);
         HTP.formopen (
            twbkwbis.f_cgibin || 'bwckgens.P_RegsUpd',
            cattributes   => 'onSubmit="return checkSubmit()"'
         );
--
-- Check term and start the web page for student.
-- =================================================
      ELSE
         IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
         THEN
            NULL;
         END IF;
         
         
           bwckfrmt.p_open_doc (proc_called_by, term);
           
         HTP.formopen (
            twbkwbis.f_cgibin || 'bwckgens.P_RegsUpd',
            cattributes   => 'onSubmit="return checkSubmit()"'
         );
      END IF;

--
-- Initialize variables.
-- =================================================
      twbkfrmt.P_FormHidden ('TERM', term);
      bwcklibs.p_initvalue (global_pidm, term, NULL, SYSDATE, NULL, NULL);
      lv_sp_req  := bwckcoms.F_StudyPathReq (term);
      lv_sp_enabled := bwckcoms.F_StudyPathEnabled;
--
-- Check to see if user tried to reduce registered
-- hours below a defined minimum.
-- ==================================================
      IF NVL (twbkwbis.f_getparam (genpidm, 'ERROR_FLAG'), 'N') = 'M'
      THEN
          minh_error := TRUE;
          DELETE
            FROM twgrwprm
           WHERE twgrwprm_pidm = genpidm
             AND twgrwprm_param_name = 'ERROR_FLAG';
          COMMIT;
      END IF;

--
-- If this procedure was called from the menu, ie. no work
-- yet done to sftregs, remove any remnant sftregs records,
-- and copy all sfrstcr rows into sftregs.
-- Also,
-- This stores the access_id we create in SFRRACL.
-- This will let other sessions that are already in a
-- registration page know that we have taken 'possession'
-- of sftregs, and if they try to update, they will be
-- expelled from their registration editing session.
-- ==================================================
      IF first_pass OR minh_error
      THEN
         IF NOT sfkfunc.f_registration_access (
               genpidm,
               term,
               'PROXY',
               call_path || global_pidm,
	           bypass_admin_in =>'Y'
            )
         THEN
            twbkwbis.p_dispinfo (proc_called_by, 'SESSIONBLOCKED');
            HTP.formclose;
            twbkwbis.p_closedoc (curr_release);
            RETURN;
         END IF;
      END IF;

      IF call_path = 'F'
      THEN
         bwcklibs.p_confidstudinfo (genpidm, term);
      END IF;

      IF lv_sp_enabled = 'Y' THEN     
        twbkwbis.p_dispinfo (proc_called_by, 'DEFAULT_SP');
      ELSE
        twbkwbis.p_dispinfo (proc_called_by, 'DEFAULT');
      END IF;
--
-- Get basic term info.
-- =================================================
      bwcklibs.p_getsobterm (term, sobterm_row);
--
-- Do minimum hours rules apply?
-- ==================================================
      IF sobterm_row.sobterm_minh_severity = 'F'
      THEN
         FOR sfbetrm IN sfkcurs.sfbetrmc (genpidm, term)
         LOOP
            IF sfbetrm.sfbetrm_min_hrs > 0
            THEN
               twbkwbis.p_dispinfo (proc_called_by, 'MINHRS_NOTE');
               EXIT;
            END IF;
         END LOOP;
      END IF;

--
-- If user tried to drop below min hours, issue messag
--
-- ==================================================
      IF minh_error
      THEN
         twbkwbis.p_dispinfo (proc_called_by, 'MINHRS_ERR');
      END IF;

--
-- Check which options can be changed on the web.
-- =================================================
      IF NVL (sobterm_row.sobterm_cred_web_upd_ind, 'N') = 'Y'
      THEN
         cred_hour_changes_allowed := TRUE;
      END IF;

      IF NVL (sobterm_row.sobterm_gmod_web_upd_ind, 'N') = 'Y'
      THEN
         grade_mode_changes_allowed := TRUE;
      END IF;

      IF NVL (sobterm_row.sobterm_levl_web_upd_ind, 'N') = 'Y'
      THEN
         level_changes_allowed := TRUE;
      END IF;

      IF NVL (sobterm_row.sobterm_sp_web_upd_ind, 'N') = 'Y'
      THEN
         spath_changes_allowed := TRUE;
      END IF;

--
-- If no options can be changed, then display a message
-- and go to the end of the procedure.
-- =================================================
      IF     NOT cred_hour_changes_allowed
         AND NOT grade_mode_changes_allowed
         AND NOT level_changes_allowed
         AND NOT spath_changes_allowed
      THEN
         twbkfrmt.p_printmessage (
            g$_nls.get ('BWCKSAM1-0008', 'SQL', 'No class change available'),
            'ERROR'
         );
         GOTO no_change;
      END IF;

--
--
-- If no active Study Paths are available, then display a message
-- and go to the end of the procedure
     IF lv_sp_enabled = 'Y' THEN     
       --IF NOT multi_term THEN
       
       soklcur.p_create_sotvcur(p_pidm    => global_pidm,
	                       p_term_code => term,
                         p_lmod_code => sb_curriculum_str.f_learner); 
                         
                         
         FOR i in sgvstsp_all_c(genpidm,term) LOOP
          lv_first_spath := i.sgvstsp_key_seqno;
          lv_valid_sp_count := sgvstsp_all_c%ROWCOUNT;
         END  LOOP;
         IF lv_valid_sp_count=0 THEN
           twbkwbis.p_dispinfo ('bwckcoms.P_SelStudyPath', 'NO_SPATH');
           GOTO no_change;
         END IF;
         row_count:=0;
       END IF;
     --END IF;
-- Loop through the student's registration records.
-- =================================================
      no_header := TRUE;
      regs_ctr := 0;
      error_exist := FALSE;


      BEGIN
         FOR i IN 2 .. crn_in.COUNT
         LOOP
            IF err_code.EXISTS (i)
            THEN
               IF err_code (i) IS NOT NULL
               THEN
                  error_exist := TRUE;
               END IF;
            END IF;

            IF err_msg.EXISTS (i)
            THEN
               IF err_msg (i) IS NOT NULL
               THEN
                  error_exist := TRUE;
               END IF;
            END IF;
         END LOOP;
      EXCEPTION
         WHEN OTHERS
         THEN
            i := 2;
      END;

      IF error_exist
      THEN
         twbkfrmt.p_printmessage (
            g$_nls.get ('BWCKSAM1-0009',
               'SQL',
               'Errors have occurred.'),
            'ERROR'
         );
         HTP.br;
      END IF;

      FOR sftregs_rec IN sfkcurs.sftregsc (genpidm, term)
      LOOP
         BEGIN
-- Check if student is counted in enrollment, based on
-- course status. If not, then skip this registration
-- record.
-- =================================================

            inv_crn := TRUE;

            FOR stvrsts IN stkrsts.stvrstsc (sftregs_rec.sftregs_rsts_code)
            LOOP
               IF stvrsts.stvrsts_incl_sect_enrl = 'Y'
               THEN
                  inv_crn := FALSE;
               END IF;
            END LOOP;

            IF inv_crn
            THEN
               GOTO next_row;
            END IF;

--
-- Check if course has been graded and rolled to history.
-- =================================================
            course_is_graded := FALSE;

            IF sftregs_rec.sftregs_grde_date IS NOT NULL
            THEN
               course_is_graded := TRUE;
            END IF;

--
--
-- =================================================
            WHILE crn_in (i) IS NOT NULL
            LOOP
               IF crn_in (i) = sftregs_rec.sftregs_crn
               THEN
                  EXIT;
               END IF;

               i := i + 1;
            END LOOP;
         EXCEPTION
            WHEN OTHERS
            THEN
               i := 2;
         END;

--
-- Count the registration records.
-- =================================================
         regs_ctr := sfkcurs.sftregsc%rowcount;

--
-- If it's the first registration record, then initialize
-- some variables.
-- =================================================
         IF no_header
         THEN
            twbkfrmt.P_FormHidden ('CRN', 'dummy');
            twbkfrmt.P_FormHidden ('CRED_OLD', 'dummy');
            twbkfrmt.P_FormHidden ('GMOD_OLD', 'dummy');
            twbkfrmt.P_FormHidden ('LEVL_OLD', 'dummy');
            twbkfrmt.P_FormHidden ('SPATH_OLD', 'dummy');  --SP changes
            twbkfrmt.P_FormHidden ('CRED', 'dummy');
            twbkfrmt.P_FormHidden ('GMOD', 'dummy');
            twbkfrmt.P_FormHidden ('LEVL', 'dummy');
            twbkfrmt.P_FormHidden ('SPATH', 'dummy');      --SP changes 
            no_header := FALSE;
         END IF;

         twbkfrmt.P_FormHidden ('CRN', sftregs_rec.sftregs_crn);

--
-- Loop through the section table. Get all sections
-- for the registered crn/term.
-- =================================================
         FOR ssbsect IN ssklibs.ssbsect3c (sftregs_rec.sftregs_crn, term)
         LOOP
            section_id := section_id + 1;
--
-- Get basic course information for the section.
-- =================================================
            OPEN scklibs.scbcrsec (
               ssbsect.ssbsect_subj_code,
               ssbsect.ssbsect_crse_numb,
               term
            );
            FETCH scklibs.scbcrsec INTO scbcrse_row;
            CLOSE scklibs.scbcrsec;
--
-- Display course information.
-- =================================================
            twbkfrmt.p_tableopen (
               'DATAENTRY',
               cattributes   => 'WIDTH="35%" SUMMARY="' ||
                                   g$_nls.get ('BWCKSAM1-0010',
                                      'SQL',
                                      'This table allows the changing of credits, level and grade mode for registered courses.') ||
                                   '."',
               ccaption      => bwcklibs.f_course_title (
                                   term,
                                   ssbsect.ssbsect_crn
                                )
            );

--
-- Display any error messages.
-- =================================================

            IF error_exist
            THEN
               twbkfrmt.p_tablerowopen;

               IF err_code.EXISTS (i)
               THEN
                  IF err_code (i) IS NOT NULL
                  THEN
                     twbkfrmt.p_tabledataopen (ccolspan => 2);

                     IF bwcklibs.error_msg_table.EXISTS (err_code (i))
                     THEN
                        twbkfrmt.p_printmessage (
                           bwcklibs.error_msg_table (err_code (i)),
                           'ERROR'
                        );
                     ELSIF err_msg.EXISTS (i)
                     THEN
                        twbkfrmt.p_printmessage (
                           err_code (i) || ': ' || err_msg (i),
                           'ERROR'
                        );
                     ELSE
                        twbkfrmt.p_printmessage (err_code (i), 'ERROR');
                     END IF;

                     twbkfrmt.p_tabledataclose;
                  ELSE
                     IF err_msg.EXISTS (i)
                     THEN
                        twbkfrmt.p_tabledataopen (ccolspan => 2);
                        twbkfrmt.p_printmessage (err_msg (i), 'ERROR');
                        twbkfrmt.p_tabledataclose;
                     END IF;
                  END IF;
               ELSIF err_msg.EXISTS (i)
               THEN
                  twbkfrmt.p_tabledataopen (ccolspan => 2);
                  twbkfrmt.p_printmessage (err_msg (i), 'ERROR');
                  twbkfrmt.p_tabledataclose;
               END IF;

               twbkfrmt.p_tablerowclose;
            END IF;

--
-- If course has been graded, display that information.
-- ====================================================

            IF course_is_graded
            THEN
               twbkfrmt.p_tablerowopen;
               twbkfrmt.p_tabledataopen (ccolspan => 2);
               twbkfrmt.p_printmessage (
                  g$_nls.get ('BWCKSAM1-0011',
                     'SQL',
                     'Course has been graded. No changes allowed.'),
                  'WARNING'
               );
               twbkfrmt.p_tabledataclose;
               twbkfrmt.p_tablerowclose;
            END IF;

--
-- Store current credit hours, grade mode, and level.
-- =================================================
            twbkfrmt.P_FormHidden ('CRED_OLD', sftregs_rec.sftregs_credit_hr);
            twbkfrmt.P_FormHidden ('GMOD_OLD', sftregs_rec.sftregs_gmod_code);
            twbkfrmt.P_FormHidden ('LEVL_OLD', sftregs_rec.sftregs_levl_code);
            twbkfrmt.P_FormHidden ('SPATH_OLD', sftregs_rec.sftregs_stsp_key_sequence);  --SP changes
--
-- Open a table to display options in table format.
-- =================================================
--
-- Display current credit hours.
-- =================================================
            twbkfrmt.p_tablerowopen;
            twbkfrmt.p_tabledatalabel (
               g$_nls.get ('BWCKSAM1-0012', 'SQL', 'Course') || ':'
            );
            twbkfrmt.p_tabledata (
               ssbsect.ssbsect_crn || '  ' || ssbsect.ssbsect_subj_code || ' ' ||
                  ssbsect.ssbsect_crse_numb ||
                  ' ' ||
                  ssbsect.ssbsect_seq_numb ||
                  '  '
            );
            twbkfrmt.p_tablerowclose;
            twbkfrmt.p_tablerowopen;

--
-- If credit hour changes are available, draw an arrow
-- to the change field, create the change field, populate
-- the change field with the current hours, and show
-- the valid range of hours.
-- =================================================
            IF    NOT cred_hour_changes_allowed
               OR scbcrse_row.scbcrse_credit_hr_ind IS NULL
               OR ssbsect.ssbsect_credit_hrs IS NOT NULL
               OR course_is_graded
               OR ( ssbsect.ssbsect_voice_avail = 'N'
                    AND crndirect = 'N')
            THEN
               twbkfrmt.p_tabledatalabel (
                  g$_nls.get ('BWCKSAM1-0013', 'SQL', 'Credit Hours') || ':'
               );
               twbkfrmt.p_tabledata (
                  LTRIM (
                     TO_CHAR (
                        NVL (sftregs_rec.sftregs_credit_hr, 0),
                        '9990D990'
                     )
                  )
               );
               twbkfrmt.P_FormHidden ('CRED', sftregs_rec.sftregs_credit_hr);
            ELSE
               twbkfrmt.p_tabledatalabel (
                  twbkfrmt.f_formlabel (
                     g$_nls.get ('BWCKSAM1-0014', 'SQL', 'Credit Hours') || ' (' ||
                        LTRIM (
                           TO_CHAR (
                              NVL (scbcrse_row.scbcrse_credit_hr_low, 0),
                              '9990D990'
                           )
                        ) ||
                        ' ' ||
                        LOWER (scbcrse_row.scbcrse_credit_hr_ind) ||
                        ' ' ||
                        LTRIM (
                           TO_CHAR (
                              scbcrse_row.scbcrse_credit_hr_high,
                              '9990D990'
                           )
                        ) ||
                        '):',
                     idname   => 'crhr' || TO_CHAR (section_id) || '_id'
                  )
               );

               IF error_exist
               THEN
                  twbkfrmt.p_tabledata (
                     twbkfrmt.f_formtext (
                        'CRED',
                        '8',
                        '8',
                        cvalue        => RTRIM (cred_in (i)),
                        cattributes   => 'ID="crhr' || TO_CHAR (section_id) ||
                                            '_id"'
                     ),
                     ccolspan   => 2
                  );
               ELSE
                  twbkfrmt.p_tabledata (
                     twbkfrmt.f_formtext (
                        'CRED',
                        '8',
                        '8',
                        cvalue        => LTRIM (
                                            TO_CHAR (
                                               NVL (
                                                  sftregs_rec.sftregs_credit_hr,
                                                  0
                                               ),
                                               '9990D990'
                                            )
                                         ),
                        cattributes   => 'ID="crhr' || TO_CHAR (section_id) ||
                                            '_id"'
                     ),
                     ccolspan   => 2
                  );
               END IF;
            END IF;

            twbkfrmt.p_tablerowclose;
--
-- Display current grade mode.
-- =================================================
            twbkfrmt.p_tablerowopen;
            IF error_exist
            THEN
               /* 080700-1 start */
               IF err_code.EXISTS (i) THEN
                   gmod_code := sftregs_rec.sftregs_gmod_code;
                   levl_code := sftregs_rec.sftregs_levl_code;
                   IF bwckcoms.F_StudyPathEnabled = 'Y' THEN
                     spath_seqno :=sftregs_rec.sftregs_stsp_key_sequence;
                   END IF;
               ELSE
                    gmod_code := gmod_in (i);
                    levl_code := levl_in (i);
                    IF bwckcoms.F_StudyPathEnabled = 'Y' THEN
                       spath_seqno := spath_in (i);
                    END IF;                    
               END IF;
	       /* 080700-1 end */
            ELSE
               gmod_code := sftregs_rec.sftregs_gmod_code;
               levl_code := sftregs_rec.sftregs_levl_code;
               IF bwckcoms.F_StudyPathEnabled = 'Y' THEN
                 spath_seqno :=sftregs_rec.sftregs_stsp_key_sequence;
               END IF;
            END IF;
--
-- If grade mode changes are allowed...
-- =================================================
            IF     grade_mode_changes_allowed
               AND NOT course_is_graded
               AND ( ssbsect.ssbsect_voice_avail = 'Y'
                     OR crndirect = 'Y')
            THEN
--
-- Count possible grade modes.
-- =================================================
               row_count := 0;

               FOR scrgmod IN scklibs.scrgmodc (
                                 ssbsect.ssbsect_subj_code,
                                 ssbsect.ssbsect_crse_numb,
                                 term
                              )
               LOOP
                  row_count := scklibs.scrgmodc%rowcount;
               END LOOP;

--
-- If there are multiple grade modes to choose from,
-- and there is no default grade mode on the ssbsect
-- record...
-- =================================================
               IF     row_count > 1
                  AND ssbsect.ssbsect_gmod_code IS NULL
               THEN
                  twbkfrmt.p_tabledatalabel (
                     twbkfrmt.f_formlabel (
                        g$_nls.get ('BWCKSAM1-0015', 'SQL', 'Grade Mode') ||
                        ':',
                        idname   => 'gmod' || TO_CHAR (section_id) || '_id'
                     )
                  );

--
-- Get the grade mode description for the current
-- grade mode.
-- =================================================
                  FOR stvgmod IN stkgmod.stvgmodc (gmod_code)
                  LOOP
--
-- Draw an arrow to the pulldown, create the pulldown,
-- and populate the pulldown with the current grade mode.
-- =================================================
                     twbkfrmt.p_tabledataopen (ccolspan => 2);
                     HTP.formselectopen (
                        'GMOD',
                        nsize         => 1,
                        cattributes   => 'ID="gmod' || TO_CHAR (section_id) ||
                                            '_id"'
                     );
                     twbkwbis.p_formselectoption (
                        stvgmod.stvgmod_desc,
                        gmod_code,
                        'SELECTED'
                     );
                  END LOOP;

--
-- Use the grade mode records from scrgmod to populate
-- the pulldown.
-- =================================================
                  FOR scrgmod IN sfkcurs.scrgmodc (
                                    ssbsect.ssbsect_subj_code,
                                    ssbsect.ssbsect_crse_numb,
                                    gmod_code,
                                    term
                                 )
                  LOOP
                     twbkwbis.p_formselectoption (
                        scrgmod.stvgmod_desc,
                        scrgmod.scrgmod_gmod_code
                     );
                  END LOOP;

                  HTP.formselectclose;
                  twbkfrmt.p_tabledataclose;
               ELSE
                  twbkfrmt.p_tabledatalabel (
                     g$_nls.get ('BWCKSAM1-0016', 'SQL', 'Grade Mode') || ':'
                  );

                  FOR stvgmod IN
                      stkgmod.stvgmodc (sftregs_rec.sftregs_gmod_code)
                  LOOP
                     twbkfrmt.p_tabledata (stvgmod.stvgmod_desc);
                  END LOOP;

                  twbkfrmt.P_FormHidden ('GMOD', sftregs_rec.sftregs_gmod_code);
                  twbkfrmt.p_tabledata;                    -- (ccolspan => 3);
               END IF;
            ELSE
               twbkfrmt.p_tabledatalabel (
                  g$_nls.get ('BWCKSAM1-0017', 'SQL', 'Grade Mode') || ':'
               );

               FOR stvgmod IN stkgmod.stvgmodc (sftregs_rec.sftregs_gmod_code)
               LOOP
                  twbkfrmt.p_tabledata (stvgmod.stvgmod_desc);
               END LOOP;

               twbkfrmt.P_FormHidden ('GMOD', sftregs_rec.sftregs_gmod_code);
               twbkfrmt.p_tabledata;                       -- (ccolspan => 3);
            END IF;

            twbkfrmt.p_tablerowclose;
--
-- Display the current level. Indent to width of 20.
-- =================================================
            twbkfrmt.p_tablerowopen;

--
-- If level changes are allowed...
-- =================================================
            IF     level_changes_allowed
               AND NOT course_is_graded
               AND ( ssbsect.ssbsect_voice_avail = 'Y'
                     OR crndirect = 'Y')
            THEN
--
-- Count the possible levels.
-- =================================================
               row_count := 0;

               FOR scrlevl IN scklibs.scrlevlc (
                                 ssbsect.ssbsect_subj_code,
                                 ssbsect.ssbsect_crse_numb,
                                 term
                              )
               LOOP
                  row_count := scklibs.scrlevlc%rowcount;
               END LOOP;

--
-- If there are multiple levels to choose from...
-- =================================================
               IF row_count > 1
               THEN
                  twbkfrmt.p_tabledatalabel (
                     twbkfrmt.f_formlabel (
                        g$_nls.get ('BWCKSAM1-0018', 'SQL', 'Course Level') || ':',
                        idname   => 'level' || TO_CHAR (section_id) || '_id'
                     )
                  );

--
-- Get the level description.
-- =================================================
                  FOR stvlevl IN stklevl.stvlevlc (levl_code)
                  LOOP
--
-- Draw an arrow to the pulldown, create the pulldown,
-- and populate the pulldown with the current level.
-- =================================================
                     twbkfrmt.p_tabledataopen (ccolspan => 2);
                     HTP.formselectopen (
                        'LEVL',
                        nsize         => 1,
                        cattributes   => 'ID="level' || TO_CHAR (section_id) ||
                                            '_id"'
                     );
                     twbkwbis.p_formselectoption (
                        stvlevl.stvlevl_desc,
                        levl_code,
                        'SELECTED'
                     );
                  END LOOP;

--
-- If there are multiple level records in scrlevl,
-- then use them to populate the pulldown.
-- =================================================
                  FOR scrlevl IN sfkcurs.scrlevlc (
                                    ssbsect.ssbsect_subj_code,
                                    ssbsect.ssbsect_crse_numb,
                                    levl_code,
                                    term
                                 )
                  LOOP
                     twbkwbis.p_formselectoption (
                        scrlevl.stvlevl_desc,
                        scrlevl.scrlevl_levl_code
                     );
                  END LOOP;

                  HTP.formselectclose;
                  twbkfrmt.p_tabledataclose;
               ELSE
                  twbkfrmt.p_tabledatalabel (
                     g$_nls.get ('BWCKSAM1-0019', 'SQL', 'Course Level') || ':'
                  );

                  FOR stvlevl IN
                      stklevl.stvlevlc (sftregs_rec.sftregs_levl_code)
                  LOOP
                     twbkfrmt.p_tabledata (stvlevl.stvlevl_desc);
                  END LOOP;

                  twbkfrmt.P_FormHidden ('LEVL', sftregs_rec.sftregs_levl_code);
                  twbkfrmt.p_tabledata;                    -- (ccolspan => 3);
               END IF;
            ELSE
               twbkfrmt.p_tabledatalabel (
                  g$_nls.get ('BWCKSAM1-0020', 'SQL', 'Course Level') || ':'
               );

               FOR stvlevl IN stklevl.stvlevlc (sftregs_rec.sftregs_levl_code)
               LOOP
                  twbkfrmt.p_tabledata (stvlevl.stvlevl_desc);
               END LOOP;

               twbkfrmt.P_FormHidden ('LEVL', sftregs_rec.sftregs_levl_code);
               twbkfrmt.p_tabledata;                       -- (ccolspan => 3);
            END IF;
            twbkfrmt.p_tablerowclose;

--  Populate the Study Path Drop down Information
--  =============================================

            IF lv_sp_enabled = 'Y' THEN     
              twbkfrmt.p_tablerowopen;
      
              twbkfrmt.p_tabledatalabel (
                     twbkfrmt.f_formlabel (
                        g$_nls.get ('BWCKSAM1-0021', 'SQL', 'Study Path') || ':',
                        idname   => 'spath' || TO_CHAR (section_id) || '_id'
                     )
                  );

                IF course_is_graded  OR NOT spath_changes_allowed OR (lv_sp_req = 'Y' and lv_valid_sp_count = 1 and lv_first_spath = spath_seqno) THEN   
                           OPEN get_sgrstsp_eff_term_c(genpidm,term, sftregs_rec.sftregs_stsp_key_sequence);
                           FETCH get_sgrstsp_eff_term_c INTO lv_sgrstsp_eff_term;
                           CLOSE get_sgrstsp_eff_term_c;
                    
                  twbkfrmt.p_tabledata (NVL(sb_studypath_name.f_learner_study_path_name(genpidm,lv_sgrstsp_eff_term, sftregs_rec.sftregs_stsp_key_sequence),
                                         g$_nls.get ('BWCKSAM1-0022','SQL',  'None') )
                                         );
                  twbkfrmt.P_FormHidden ('SPATH', sftregs_rec.sftregs_stsp_key_sequence); 
                ELSE   
                              
                
                  row_count := 0;
                  lv_matched := FALSE;
          	       OPEN sgvstsp_all_c(genpidm,term);
                   LOOP
                     FETCH sgvstsp_all_c
                     INTO lv_sp_name,lv_sp_key_seq_no,lv_sgvstsp_term;
                     EXIT WHEN sgvstsp_all_c%NOTFOUND;
                        
                     row_count := row_count + 1;               
                     
                     IF row_count = 1 THEN
                       twbkfrmt.p_tabledataopen;
                       htp.formSelectOpen('SPATH',
                                   nsize         => 1,
                                   cattributes   => 'ID="spath'||TO_CHAR (section_id)||'_id"'); 
                     END IF;              

                     --  Populate the list with  Study Paths which are 
                     --  a. Not having any SFRENSP records
                     --  b. Have SFRENSP records with a  status which permits registration.
                     
                     IF lv_sp_key_seq_no = spath_seqno  AND spath_seqno IS NOT NULL THEN
                       twbkwbis.P_FormSelectOption(G$_NLS.FormatMsg('BWGKCCR1-0044','SQL',lv_sp_name),
                                                                           lv_sp_key_seq_no,        -- Not appending the term as it should be for a term
                                                                           'SELECTED');                             
                       lv_matched := TRUE;                                                                 
                     ELSE
                       twbkwbis.P_FormSelectOption(G$_NLS.FormatMsg('BWGKCCR1-0044','SQL',lv_sp_name),
                                                                           lv_sp_key_seq_no);             
                     END IF;                                                                 
                                                                           
                   END LOOP;
                     
                   CLOSE sgvstsp_all_c;
                   
                   --  If the spath of the course is not found in the cursor 
                   -- (the registration status of the spath does not allow registration)

                   IF NOT lv_matched THEN
                     row_count := row_count + 1;               
                     IF row_count = 1 THEN
                       twbkfrmt.p_tabledataopen;
                       htp.formSelectOpen('SPATH',
                                   nsize         => 1,
                                   cattributes   => 'ID="spath'||TO_CHAR (section_id)||'_id"'); 
                     END IF;              
                     IF spath_seqno IS NOT NULL THEN
                       OPEN get_sgrstsp_eff_term_c(genpidm,term, spath_seqno);
                       FETCH get_sgrstsp_eff_term_c INTO lv_sgrstsp_eff_term;
                       CLOSE get_sgrstsp_eff_term_c;
                       twbkwbis.P_FormSelectOption(G$_NLS.FormatMsg('BWGKCCR1-0044','SQL',NVL(sb_studypath_name.f_learner_study_path_name(genpidm,lv_sgrstsp_eff_term, spath_seqno),'None')),
                                                                             spath_seqno,
                                                                             'SELECTED');                             
                     ELSE
                       twbkwbis.P_FormSelectOption(G$_NLS.Get('BWCKSAM1-0023','SQL','None'),
                                                                               NULL,
                                                                              'SELECTED');             
                     
                     END IF;
                     lv_matched := TRUE;                                                      
                   END IF;
                   
                   
                   
                   --  Populate the list with "None" Study Path            
                  IF lv_sp_req <> 'Y' and spath_seqno IS NOT NULL THEN
                   row_count := row_count + 1;               
                   IF row_count = 1 THEN
                     twbkfrmt.p_tabledataopen;
                     htp.formSelectOpen('SPATH',
                                 nsize         => 1,
                                 cattributes   => 'ID="spath'||TO_CHAR (section_id)||'_id"'); 
                   END IF;              
                   
                   twbkwbis.P_FormSelectOption(G$_NLS.Get('BWCKSAM1-0024','SQL','None'),
                                                                         NULL);             
                 END IF;
             
                 IF row_count > 0 THEN
                   htp.formselectclose; 
                   twbkfrmt.p_tabledataclose; 
                 END IF;
                   
                                                    
                 
                 twbkfrmt.p_tablerowclose;
                END IF;
            END IF;         
                        
            twbkfrmt.p_tableclose;
            HTP.br;
         END LOOP;

         <<next_row>>
         NULL;
      END LOOP;

--
-- If we went through the loop and did not display
-- anything, then display a message, else close the
-- display table.
-- =================================================
      IF regs_ctr = 0
      THEN
         twbkfrmt.p_printmessage (
            g$_nls.get ('BWCKSAM1-0025', 'SQL', 'No class change available'),
            'ERROR'
         );
         GOTO no_change;
      END IF;


--
-- perform fee assessment, if applicable
-- =================================================
      p_immediate_fee_assess (term, genpidm);
--
-- Buttons.
-- =================================================
      HTP.formsubmit (
         NULL,
         g$_nls.get ('BWCKSAM1-0026', 'SQL', 'Submit Changes')
      );
      HTP.formreset (g$_nls.get ('BWCKSAM1-0027', 'SQL', 'Reset'));

      <<no_change>>
      HTP.formclose;
      HTP.br;
      bwckfrmt.p_disp_back_anchor;
      twbkwbis.p_closedoc (curr_release);
   END P_DispChgCrseOpt;

--------------------------------------------------------------------------
   PROCEDURE P_DispChgCrseOpt2 (
      term IN stvterm.stvterm_code%TYPE DEFAULT NULL
   )
   IS
      crn        OWA_UTIL.ident_arr;
      cred       OWA_UTIL.ident_arr;
      gmod       OWA_UTIL.ident_arr;
      levl       OWA_UTIL.ident_arr;
      err_code   OWA_UTIL.ident_arr;
      err_msg    twbklibs.error_msg_tabtype;
      spath      OWA_UTIL.ident_arr;
   BEGIN
      crn (1) := 'dummy';
      cred (1) := 'dummy';
      gmod (1) := 'dummy';
      levl (1) := 'dummy';
      err_code (1) := 'dummy';
      err_msg (1) := 'dummy';
      spath (1) := 'dummy';
       
      P_DispChgCrseOpt (
         term,
         crn,
         cred,
         gmod,
         levl,
         err_code,
         err_msg,
         TRUE,
         NULL,
         spath
      );
   END P_DispChgCrseOpt2;

--------------------------------------------------------------------------
PROCEDURE p_regsresult (
   term_in                  IN       OWA_UTIL.ident_arr,
   errs_count               IN OUT   NUMBER,
   regs_count               IN OUT   NUMBER,
   wait_count               IN OUT   NUMBER,
   first_pass               IN       BOOLEAN DEFAULT FALSE,
   reg_access_allowed_out   OUT      BOOLEAN,
   capp_tech_error_in_out   IN OUT   VARCHAR2,
   drop_result_label_in     IN       twgrinfo.twgrinfo_label%TYPE DEFAULT NULL,
   drop_problems_in         IN       sfkcurs.drop_problems_rec_tabtype,
   drop_failures_in         IN       sfkcurs.drop_problems_rec_tabtype
)
IS
   tot_credit_hr               sftregs.sftregs_credit_hr%TYPE         := 0;
   tot_bill_hr                 sftregs.sftregs_bill_hr%TYPE           := 0;
   tot_ceu                     sftregs.sftregs_credit_hr%TYPE         := 0;
   ssbxlst_count               NUMBER                                 := 0;
   gmod_code                   stvgmod.stvgmod_code%TYPE;
   gmod_desc                   stvgmod.stvgmod_desc%TYPE;
   ssbxlst_row                 ssbxlst%ROWTYPE;
   wait_crn                    OWA_UTIL.ident_arr;
   sdax_rsts_code              VARCHAR2 (2);
   i                           INTEGER;
   j                           INTEGER;
   k                           INTEGER;
   next_rec                    BOOLEAN;
   sftregs_rec                 sftregs%ROWTYPE;
   sgbstdn_rec                 sgbstdn%ROWTYPE;
   sfbetrm_rec                 sfbetrm%ROWTYPE;
   genpidm                     spriden.spriden_pidm%TYPE;
   call_path                   VARCHAR2 (1);
   term                        stvterm.stvterm_code%TYPE              := NULL;
   multi_term                  BOOLEAN                                := TRUE;
   crse_title                  ssrsyln.ssrsyln_long_course_title%TYPE;
   change_class_options_proc   VARCHAR2 (100);
   msg                         VARCHAR2 (200);
   wmnu_rec                    twgbwmnu%ROWTYPE;
   heading_displayed           BOOLEAN                                := FALSE;
   row_count_out               NUMBER                                 := 0;
   drops_table_open            BOOLEAN                                := FALSE;
   last_class_error            BOOLEAN := FALSE;
   update_error                BOOLEAN := FALSE;
   crndirect                   VARCHAR2(1) :=
                                  NVL(SUBSTR(bwcklibs.f_getgtvsdaxrule('CRNDIRECT','WEBREG'),1,1),'N');
   update_wait_count           NUMBER  := 0;
   waitlist_option             BOOLEAN := FALSE;
   minh_error                  BOOLEAN := FALSE;
   minh_restriction            BOOLEAN := FALSE;

-- DegreeWorks: Variables for retrieving PreReq_Chk_Method 
   sect_cv                     SYS_REFCURSOR;
   sect_rec                    sb_section.section_rec;
   error_msg                   SFTDWER.SFTDWER_ERROR_MESSAGE%TYPE;
-- Cursor to get data from DegreeWorks Temporary Registration Error Message Table
   CURSOR csr_sftdwer(c_pidm IN SFTDWER.SFTDWER_PIDM%TYPE,
                      c_term IN SFTDWER.SFTDWER_TERM_CODE%TYPE,
                      c_crn  IN SFTDWER.SFTDWER_CRN%TYPE)
   IS
   SELECT sftdwer_error_message
   FROM   sftdwer
   WHERE  sftdwer_pidm = c_pidm
   AND    sftdwer_term_code = c_term
   AND    sftdwer_crn = c_crn
   ORDER BY sftdwer_seq_no;

-- SP changes   
   lv_sgrstsp_eff_term        stvterm.stvterm_code%TYPE              := NULL;
   CURSOR get_sgrstsp_eff_term_c (p_pidm SPRIDEN.SPRIDEN_PIDM%TYPE,
                              p_term STVTERM.STVTERM_CODE%TYPE,
                              p_sp_seqno SGRSTSP.SGRSTSP_KEY_SEQNO%TYPE)  
       IS
   SELECT SGRSTSP_TERM_CODE_EFF 
     FROM SGRSTSP
    WHERE SGRSTSP_PIDM = p_pidm
      AND SGRSTSP_KEY_SEQNO = p_sp_seqno
      AND SGRSTSP_TERM_CODE_EFF  = (SELECT MAX(SGRSTSP_TERM_CODE_EFF )
                                      FROM SGRSTSP
                                     WHERE SGRSTSP_PIDM = p_pidm
                                       AND SGRSTSP_KEY_SEQNO = p_sp_seqno
                                       AND SGRSTSP_TERM_CODE_EFF <= p_term);
BEGIN
--
-- Validate the user.
-- ==================================================
   IF NOT twbkwbis.f_validuser (global_pidm)
   THEN
      RETURN;
   END IF;

--
-- Check if procedure is being run from facweb or stuweb.
-- If stuweb, then use the pidm of the user. Else, use
-- the pidm that corresponds to the student that was
-- specified by the user.
-- ==================================================
   IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
   THEN
      genpidm :=
          TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
      call_path := 'F';
      change_class_options_proc := 'bwlkfrad.P_FacChangeCrseOpt';
      OPEN twbklibs.getmenuc ('bwlkfrad.P_FacAddDropCrse');
   ELSE
      call_path := 'S';
      genpidm := global_pidm;
      change_class_options_proc := 'bwskfreg.P_ChangeCrseOpt';
      OPEN twbklibs.getmenuc ('bwskfreg.P_AddDropCrse');
   END IF;

   FETCH twbklibs.getmenuc INTO wmnu_rec;
   CLOSE twbklibs.getmenuc;
   term := term_in (term_in.COUNT);

   IF term_in.COUNT = 1
   THEN
      multi_term := FALSE;
   END IF;

--
-- Check to see if user tried to reduce registered
-- hours below a defined minimum.
-- ==================================================
   IF NVL (twbkwbis.f_getparam (genpidm, 'ERROR_FLAG'), 'N') = 'M'
   THEN
       minh_error := TRUE;
       DELETE
           FROM twgrwprm
           WHERE twgrwprm_pidm = genpidm
           AND twgrwprm_param_name = 'ERROR_FLAG';
       COMMIT;
   END IF;

--
-- Check to see if user tried to drop last class and
-- it is not allowed.
-- ==================================================
   IF NVL (twbkwbis.f_getparam (genpidm, 'ERROR_FLAG'), 'N') = 'L'
   THEN
       last_class_error := TRUE;
       DELETE
           FROM twgrwprm
           WHERE twgrwprm_pidm = genpidm
           AND twgrwprm_param_name = 'ERROR_FLAG';
       COMMIT;
   END IF;

--
-- Check to see if user tried to update registration
-- and sfrstcr rows were not found to match sftregs.
-- ==================================================
   IF NVL (twbkwbis.f_getparam (genpidm, 'ERROR_FLAG'), 'N') = 'D'
   THEN
       update_error := TRUE;
       DELETE
           FROM twgrwprm
           WHERE twgrwprm_pidm = genpidm
           AND twgrwprm_param_name = 'ERROR_FLAG';
       COMMIT;
   END IF;

--
-- If this procedure was called from the menu, ie. no work
-- yet done to sftregs, remove any remnant sftregs records,
-- and copy all sfrstcr rows into sftregs.
-- Also,
-- This stores the access_id we create in SFRRACL.
-- This will let other sessions that are already in a
-- registration page know that we have taken 'possession'
-- of sftregs, and if they try to update, they will be
-- expelled from their registration editing session.
-- ==================================================
   reg_access_allowed_out := TRUE;

   IF (first_pass OR last_class_error OR update_error or minh_error)
     AND capp_tech_error_in_out IS NULL
   THEN
      FOR i IN 1 .. term_in.COUNT
      LOOP
         IF NOT bwckregs.f_registration_access (
               genpidm,
               term_in (i),
               call_path || global_pidm
            )
         THEN
            capp_tech_error_in_out :=
                sfkfunc.f_get_capp_tech_error(genpidm, term_in(i));
            IF capp_tech_error_in_out IS NOT NULL
            THEN
               EXIT;
            END IF;
            reg_access_allowed_out := FALSE;
            RETURN;
         END IF;
      END LOOP;
   END IF;

   IF capp_tech_error_in_out IS NOT NULL
   THEN
      IF capp_tech_error_in_out = 'PIPE'
      THEN
         msg := 'PIPE_ERROR';
      ELSE
         msg := 'CAPP_ERROR';
      END IF;

--
-- Return to the menu.
-- =================================================
      twbkwbis.p_genmenu (
         SUBSTR (
            wmnu_rec.twgbwmnu_back_url,
            INSTR (wmnu_rec.twgbwmnu_back_url, '/', -1) + 1
         ),
         msg,
         message_type   => 'ERROR'
      );
      RETURN;
   END IF;


--
-- Do minimum hours rules apply?
-- ==================================================
   FOR i IN 1 .. term_in.COUNT
   LOOP
      bwcklibs.p_getsobterm (term_in(i), sobterm_row);
      IF sobterm_row.sobterm_minh_severity = 'F'
      THEN
         FOR sfbetrm IN sfkcurs.sfbetrmc (genpidm, term_in(i))
         LOOP
            IF sfbetrm.sfbetrm_min_hrs > 0
            THEN
               minh_restriction := TRUE;
               EXIT;
            END IF;
         END LOOP;
      END IF;
      IF minh_restriction
      THEN
         EXIT;
      END IF;
   END LOOP;

--
-- Initialize web page for facweb.
-- ==================================================
   IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
   THEN
--
--
-- ==================================================
      IF NOT bwlkilib.f_add_drp (term)
      THEN
         NULL;
      END IF;

--
-- Open doc and pass the term description to be printed
-- at top of page.
-- ==================================================
      bwckfrmt.p_open_doc (
         'bwlkfrad.P_FacAddDropCrse',
         term,
         NULL,
         multi_term,
         term_in (1)
      );
      HTP.br;
--
-- Start a form.
-- ==================================================
      HTP.formopen (
         twbkwbis.f_cgibin || 'bwckcoms.P_Regs',
         cattributes   => 'onSubmit="return checkSubmit()"'
      );
--
-- Print info text.
-- ==================================================
      IF multi_term THEN
        twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'DEFAULT_MT');
      ELSE
        twbkwbis.p_dispinfo ('bwlkfrad.P_FacAddDropCrse', 'DEFAULT');
      END IF;  
      IF minh_restriction
      THEN
         twbkwbis.p_dispinfo ('bwlkfrad.P_FacAddDropCrse', 'MINHRS_NOTE');
      END IF;
      IF NOT (last_class_error OR update_error OR minh_error)
      THEN
          IF drop_result_label_in = 'DROPCONFIRMED'
          THEN
              twbkwbis.p_dispinfo ('bwlkfrad.P_FacAddDropCrse', 'DROPCONFIRMED');
          ELSIF drop_result_label_in = 'DROPREJECTED'
          THEN
              twbkwbis.p_dispinfo ('bwlkfrad.P_FacAddDropCrse', 'DROPREJECTED');
          ELSIF drop_result_label_in = 'DROPPROHIBITED'
          THEN
              twbkwbis.p_dispinfo ('bwlkfrad.P_FacAddDropCrse', 'DROPPROHIBITED');
          END IF;
          FOR df IN 1..drop_failures_in.COUNT
          LOOP
             IF drop_failures_in(df).connected_crns IS NOT NULL
             THEN
                 twbkwbis.p_dispinfo ('bwlkfrad.P_FacAddDropCrse', 'DROPFAILED');
                 EXIT;
             END IF;
          END LOOP;
      END IF;
--
-- If user tried to drop last class and its not allowed,
-- display error message.
-- ==================================================
      IF last_class_error
      THEN
          twbkwbis.p_dispinfo ('bwlkfrad.P_FacAddDropCrse', 'DROPLAST');
      END IF;

--
-- Initialize web page for stuweb.
-- ==================================================
   ELSE
--
--
-- ==================================================
      IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
      THEN
         NULL;
      END IF;

-- Open doc and pass the term description to be printed
-- at top of page.
-- ==================================================
      bwckfrmt.p_open_doc (
         'bwskfreg.P_AddDropCrse',
         term,
         NULL,
         multi_term,
         term_in (1)
      );
--Local06 drop notification, checks for any drops on file......
      pw_register.p_szrptcn_Inform(term_in(1),  genpidm);
--end local06
---------------------------------------------------------
      HTP.br;
--
-- Start a form.
-- ==================================================
      HTP.formopen (
         twbkwbis.f_cgibin || 'bwckcoms.P_Regs',
         cattributes   => 'onSubmit="return checkSubmit()"'
      );
--
-- Print info text.
-- ==================================================
      IF multi_term THEN
        twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'DEFAULT_MT');
      ELSE
        twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'DEFAULT');
      END IF;    
      IF minh_restriction
      THEN
         twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'MINHRS_NOTE');
      END IF;
      IF NOT (last_class_error OR update_error OR minh_error)
      THEN
          IF drop_result_label_in = 'DROPCONFIRMED'
          THEN
              twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'DROPCONFIRMED');
          ELSIF drop_result_label_in = 'DROPREJECTED'
          THEN
              twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'DROPREJECTED');
          ELSIF drop_result_label_in = 'DROPPROHIBITED'
          THEN
              twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'DROPPROHIBITED');
          END IF;
          FOR df IN 1..drop_failures_in.COUNT
          LOOP
             IF drop_failures_in(df).connected_crns IS NOT NULL
             THEN
                 twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'DROPFAILED');
                 EXIT;
             END IF;
          END LOOP;
      END IF;
--
-- If user tried to drop last class and its not allowed,
-- display error message.
-- ==================================================
      IF last_class_error
      THEN
          twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'DROPLAST');
      END IF;

   END IF;

--
-- Initialize hidden variables.
-- ==================================================
   FOR i IN 1 .. term_in.COUNT
   LOOP
      twbkfrmt.P_FormHidden ('term_in', term_in (i));
   END LOOP;

   twbkfrmt.P_FormHidden ('RSTS_IN', 'DUMMY');
   twbkfrmt.P_FormHidden ('assoc_term_in', 'DUMMY');
   twbkfrmt.P_FormHidden ('CRN_IN', 'DUMMY');
   twbkfrmt.P_FormHidden ('start_date_in', 'DUMMY');
   twbkfrmt.P_FormHidden ('end_date_in', 'DUMMY');
   twbkfrmt.P_FormHidden ('SUBJ', 'DUMMY');
   twbkfrmt.P_FormHidden ('CRSE', 'DUMMY');
   twbkfrmt.P_FormHidden ('SEC', 'DUMMY');
   twbkfrmt.P_FormHidden ('LEVL', 'DUMMY');
   twbkfrmt.P_FormHidden ('CRED', 'DUMMY');
   twbkfrmt.P_FormHidden ('GMOD', 'DUMMY');
   twbkfrmt.P_FormHidden ('TITLE', 'DUMMY');
   twbkfrmt.P_FormHidden ('MESG', 'DUMMY');
   twbkfrmt.P_FormHidden ('REG_BTN', 'DUMMY');
--
--
-- ==================================================
   bwcklibs.p_initvalue (global_pidm, term, NULL, NULL, NULL, NULL);

--
-- If user tried to drop below min hours, issue message
--
-- ==================================================
   IF minh_error
   THEN
      IF multi_term
      THEN
         twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'MINHRS_OLR');
      ELSE
         twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'MINHRS_ERR');
      END IF;
   END IF;

--
-- If user tried to update registration and sfrstcr
-- rows were not found to match sftregs, a system
-- problem has occured, display error message.
-- ==================================================
   IF update_error
   THEN
      twbkfrmt.p_printmessage (
         g$_nls.get ('BWCKSAM1-0028', 'SQL',
                     'System Error - Changes could not be saved'),
         'ERROR'
      );
   END IF;

--
-- Calculate credit hours, billing hours, and earned credits.
-- ==================================================

   bwckregs.p_calchrs (tot_credit_hr, tot_bill_hr, tot_ceu);
   regs_count := 0;
   update_wait_count := 0;

--
-- Print current schedule. Loop through registration records.
-- ==========================================================
   FOR i IN 1 .. term_in.COUNT
   LOOP
      FOR sftregs_row IN sfkcurs.sftregsrowc (genpidm, term_in (i))
      LOOP
         regs_count := regs_count + 1;
--
-- Print the headers.
-- ==================================================
         bwckcoms.p_curr_sched_heading (heading_displayed, term, multi_term);
         twbkfrmt.p_tablerowopen;

--
-- Display the data cell that shows the status description
-- and date.
-- ========================================================
 --local09
        --take into consideration the waitlisted bits...
        If sftregs_row.rec_sftregs_rsts_code = 'WL'
        Then
            waitlist_call     := fw_waitlist_position(sftregs_row.rec_sftregs_crn,
                                                      genpidm,
                                                      term_in(i));
            waitlist_position := (f_split_fields(waitlist_call,
                                                 1) || ' of ' ||
                                 f_split_fields(waitlist_call,
                                                 2));
            For stvrsts In stkrsts.stvrstsc(sftregs_row.rec_sftregs_rsts_code)
            Loop
                twbkfrmt.p_tabledata(htf.formhidden('MESG',
                                                    'DUMMY') ||
                                     g$_nls.get('BWCKSAM1-0216',
                                                'SQL',
                                                '%01% on %02%',
                                                stvrsts.stvrsts_desc,
                                                to_char(sftregs_row.rec_sftregs_rsts_date,
                                                        twbklibs.date_display_fmt) || '<br>You are ' ||
                                                waitlist_position || ' waitlisted'));
            End Loop;
         ELSE         
            --end local09
         FOR stvrsts IN stkrsts.stvrstsc (sftregs_row.rec_sftregs_rsts_code)
         LOOP
            twbkfrmt.p_tabledata (
              twbkfrmt.F_FormHidden ('MESG', 'DUMMY') ||
               g$_nls.get ('BWCKSAM1-0029',
                  'SQL',
                  '%01% on %02%',
                  stvrsts.stvrsts_desc,
                  TO_CHAR (
                     sftregs_row.rec_sftregs_rsts_date,
                     twbklibs.date_display_fmt
                  )
               )
            );
         END LOOP;

         End If;  --local09
--
-- Get the reg status code that is defined as the register code.
-- ==================================================
         sdax_rsts_code := SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2);

--
-- Build the data cell that contains the Action pulldown.
-- Only build the action pulldown if the class does not
-- have fatal errors.
-- ==================================================

         IF NVL (sftregs_row.rec_sftregs_error_flag, '#') <> 'F'
         THEN
            row_count := 0;
            waitlist_option := FALSE;

--
-- If course has been graded...
-- ==================================================
            IF sftregs_row.rec_sftregs_grde_date IS NOT NULL
            OR ( sftregs_row.rec_ssbsect_voice_avail = 'N'
                 AND crndirect = 'N')
            THEN
               twbkfrmt.p_tabledata (twbkfrmt.F_FormHidden ('RSTS_IN', ''));
--
-- If attempt has been made to reinstate course, but must be waitlisted
-- =====================================================================
            ELSIF sftregs_row.rec_sftregs_error_flag = 'D' AND
                  drop_failures_in.EXISTS(1)
            THEN
               FOR drop_failure_index IN drop_failures_in.FIRST .. drop_failures_in.LAST
               LOOP
                  IF drop_failures_in(drop_failure_index).crn = sftregs_row.rec_sftregs_crn
                  THEN
-- 8.5.1.1
--                     IF drop_failures_in(drop_failure_index).message LIKE '%WAITLISTED%' OR
--                        drop_failures_in(drop_failure_index).message LIKE '%ON WL%'
                  IF ((drop_failures_in(drop_failure_index).rmsg_cde = 'WAIT'  
                  OR drop_failures_in(drop_failure_index).rmsg_cde = 'WLRS'  
                  OR drop_failures_in(drop_failure_index).rmsg_cde = 'RESV') 
                       AND sfksels.f_check_refund_percent(sftregs_row.rec_sftregs_term_code,
                                                          sftregs_row.rec_sftregs_rsts_code,
                                                          sftregs_row.rec_sftregs_rsts_date,
                                                          sftregs_row.rec_sftregs_ptrm_code) = 'Y')                  
                     THEN
                        waitlist_option := TRUE;
                        update_wait_count := update_wait_count + 1;
                        bwckcoms.p_build_wait_action_pulldown (
                           term_in (i),
                           genpidm,
                           sftregs_row.rec_sftregs_crn,
                           update_wait_count,
                           sftregs_row.rec_ssbsect_ptrm_code,
                           NULL,
                          sftregs_row.rec_sftregs_start_date,    --C3SC GRS 03/16/2009   ADD
                          sftregs_row.rec_sftregs_rsts_date,     --C3SC GRS 03/16/2009   ADD  
                           row_count_out
                        );

                        EXIT;
                     END IF;
                     EXIT;
                  END IF;
               END LOOP;
               IF NOT waitlist_option
               THEN
                  bwckcoms.p_build_action_pulldown (
                     term_in (i),
                     genpidm,
                     sftregs_row.rec_sftregs_crn,
                     sftregs_row.rec_sftregs_start_date,
                     sftregs_row.rec_sftregs_completion_date,
                     trunc(SYSDATE),
                     sftregs_row.rec_sftregs_dunt_code,
                     regs_count,
                     sftregs_row.rec_sftregs_rsts_code,
                     sftregs_row.rec_ssbsect_ptrm_code,
                     sdax_rsts_code
                  );
               END IF;
--
-- If course has not been graded...
-- ==================================================
            ELSE
--
-- Build the Action pulldown. Loop through the
-- NOT waitlisted reg status records.
-- ==========================================
               bwckcoms.p_build_action_pulldown (
                  term_in (i),
                  genpidm,
                  sftregs_row.rec_sftregs_crn,
                  sftregs_row.rec_sftregs_start_date,
                  sftregs_row.rec_sftregs_completion_date,
                  trunc(SYSDATE),
                  sftregs_row.rec_sftregs_dunt_code,
                  regs_count,
                  sftregs_row.rec_sftregs_rsts_code,
                  sftregs_row.rec_ssbsect_ptrm_code,
                  sdax_rsts_code
               );

            END IF;
--
-- If class has fatal error, then display a blank cell.
-- ==================================================
         ELSE
            twbkfrmt.p_tabledata (twbkfrmt.F_FormHidden ('RSTS_IN', NULL));
         END IF;

--
-- Display data cells that show crn, subject, course,
-- and section.
-- ==================================================
         IF multi_term
         THEN
            twbkfrmt.p_tabledata (sftregs_row.rec_stvterm_desc);
         END IF;

         twbkfrmt.p_tabledata (
            twbkfrmt.F_FormHidden ('assoc_term_in', sftregs_row.rec_sftregs_term_code) ||
            twbkfrmt.F_FormHidden ('CRN_IN', sftregs_row.rec_sftregs_crn) ||
            sftregs_row.rec_sftregs_crn ||
            twbkfrmt.F_FormHidden ('start_date_in',
               TO_CHAR (sftregs_row.rec_sftregs_start_date,twbklibs.date_input_fmt)) ||
            twbkfrmt.F_FormHidden ('end_date_in',
               TO_CHAR (sftregs_row.rec_sftregs_completion_date,twbklibs.date_input_fmt))
         );
         twbkfrmt.p_tabledata (
            twbkfrmt.F_FormHidden ('SUBJ', sftregs_row.rec_ssbsect_subj_code) ||
               sftregs_row.rec_ssbsect_subj_code
         );
         twbkfrmt.p_tabledata (
            twbkfrmt.F_FormHidden ('CRSE', sftregs_row.rec_ssbsect_crse_numb) ||
               sftregs_row.rec_ssbsect_crse_numb
         );
         twbkfrmt.p_tabledata (
            twbkfrmt.F_FormHidden ('SEC', sftregs_row.rec_ssbsect_seq_numb) ||
               sftregs_row.rec_ssbsect_seq_numb
         );
--
-- Display the data cell that shows level.
-- Count the possible levels.
-- =================================================
         row_count := 0;

         FOR scrlevl IN scklibs.scrlevlc (
                           sftregs_row.rec_ssbsect_subj_code,
                           sftregs_row.rec_ssbsect_crse_numb,
                           sftregs_row.rec_sftregs_term_code
                        )
         LOOP
            row_count := scklibs.scrlevlc%rowcount;
         END LOOP;

         FOR stvlevl IN stklevl.stvlevlc (sftregs_row.rec_sftregs_levl_code)
         LOOP
            bwcklibs.p_getsobterm (
               sftregs_row.rec_sftregs_term_code,
               sobterm_row
            );

            IF     NVL (sobterm_row.sobterm_levl_web_upd_ind, 'N') = 'Y'
               AND sftregs_row.rec_sftregs_grde_date IS NULL
               AND row_count > 1
               AND ( sftregs_row.rec_ssbsect_voice_avail = 'Y' OR
                     crndirect = 'Y')
            THEN
               twbkfrmt.p_tabledata (
                  twbkfrmt.F_FormHidden (cname=>'LEVL',
                                         cvalue=>stvlevl.stvlevl_desc,
                                         bypass_esc=>'Y') ||
                     twbkfrmt.f_printanchor (
                        twbkfrmt.f_encodeurl (
                           change_class_options_proc || '?term_in=' ||
                              sftregs_row.rec_sftregs_term_code
                        ),
                        stvlevl.stvlevl_desc,
                        cattributes   => bwckfrmt.f_anchor_focus (
                                            g$_nls.get ('BWCKSAM1-0030',
                                               'SQL',
                                               'Change')
                                         )
                     ), cattributes=>'BYPASS_ESC=Y'
               );
            ELSE
               twbkfrmt.p_tabledata (
                  twbkfrmt.F_FormHidden ('LEVL', stvlevl.stvlevl_desc) ||
                     stvlevl.stvlevl_desc
               );
            END IF;
         END LOOP;

         IF scklibs.scbcrsec%ISOPEN
         THEN
            CLOSE scklibs.scbcrsec;
         END IF;

--
-- Display the data cell that shows credit hours.
-- ==================================================

         IF     NVL (sobterm_row.sobterm_cred_web_upd_ind, 'N') = 'Y'
            AND sftregs_row.rec_sftregs_grde_date IS NULL
            AND sftregs_row.rec_scbcrse_credit_hr_ind IS NOT NULL
            AND sftregs_row.rec_ssbsect_credit_hrs IS NULL
            AND ( sftregs_row.rec_ssbsect_voice_avail = 'Y' OR
                  crndirect = 'Y')
         THEN
            twbkfrmt.p_tabledata (
               twbkfrmt.F_FormHidden (cname=>'CRED',
                                      cvalue=>TO_CHAR (sftregs_row.rec_sftregs_credit_hr, '9990D990'),
                                      bypass_esc=>'Y') ||
                  twbkfrmt.f_printanchor (
                     twbkfrmt.f_encodeurl (
                        change_class_options_proc || '?term_in=' ||
                           sftregs_row.rec_sftregs_term_code
                     ),
                     TO_CHAR (sftregs_row.rec_sftregs_credit_hr, '9990D990'),
                     cattributes   => bwckfrmt.f_anchor_focus (
                                         g$_nls.get ('BWCKSAM1-0031',
                                            'SQL',
                                            'Change')
                                      )
                  ), cattributes=>'BYPASS_ESC=Y'
            );
         ELSE
            twbkfrmt.p_tabledata (
               twbkfrmt.F_FormHidden (
                  'CRED',
                  TO_CHAR (sftregs_row.rec_sftregs_credit_hr, '9990D990')
               ) ||
                  TO_CHAR (sftregs_row.rec_sftregs_credit_hr, '9990D990')
            );
         END IF;

--
-- Display the data cell that shows grade mode.
-- ==================================================
         row_count := 0;

         FOR scrgmod IN scklibs.scrgmodc (
                           sftregs_row.rec_ssbsect_subj_code,
                           sftregs_row.rec_ssbsect_crse_numb,
                           sftregs_row.rec_sftregs_term_code
                        )
         LOOP
            row_count := scklibs.scrgmodc%rowcount;
         END LOOP;

         IF     NVL (sobterm_row.sobterm_gmod_web_upd_ind, 'N') = 'Y'
            AND sftregs_row.rec_sftregs_grde_date IS NULL
            AND row_count > 1
            AND sftregs_row.rec_ssbsect_gmod_code IS NULL
            AND ( sftregs_row.rec_ssbsect_voice_avail = 'Y' OR
                  crndirect = 'Y')
         THEN
            twbkfrmt.p_tabledata (
               twbkfrmt.F_FormHidden (cname=>'GMOD',
                                      cvalue=>sftregs_row.rec_stvgmod_desc,
                                      bypass_esc=>'Y') ||
                  twbkfrmt.f_printanchor (
                     twbkfrmt.f_encodeurl (
                        twbkwbis.f_cgibin || change_class_options_proc ||
                           '?term_in=' ||
                           sftregs_row.rec_sftregs_term_code
                     ),
                     sftregs_row.rec_stvgmod_desc,
                     cattributes   => bwckfrmt.f_anchor_focus (
                                         g$_nls.get ('BWCKSAM1-0032',
                                            'SQL',
                                            'Change')
                                      )
                  ), cattributes=>'BYPASS_ESC=Y'
            );
         ELSE
            twbkfrmt.p_tabledata (
               twbkfrmt.F_FormHidden ('GMOD', sftregs_row.rec_stvgmod_desc) ||
                  sftregs_row.rec_stvgmod_desc
            );
         END IF;

--
-- Display the data cell that shows course title.
-- ==================================================
         crse_title :=
           bwcklibs.f_course_title (term_in (i), sftregs_row.rec_sftregs_crn);
         twbkfrmt.p_tabledata (
            twbkfrmt.F_FormHidden ('TITLE', crse_title) || crse_title
         );
         
-- Display the studypath info
-- ======================================================
   
     IF bwckcoms.F_StudyPathEnabled  = 'Y' THEN  
         OPEN get_sgrstsp_eff_term_c(genpidm,term_in(i), sftregs_row.rec_sftregs_stsp_key_sequence);
         FETCH get_sgrstsp_eff_term_c INTO lv_sgrstsp_eff_term;
         CLOSE get_sgrstsp_eff_term_c;
         IF lv_sgrstsp_eff_term IS NOT NULL THEN
           twbkfrmt.p_tabledata (
                                NVL(sb_studypath_name.f_learner_study_path_name(genpidm,
                                                                                lv_sgrstsp_eff_term,
                                                                                sftregs_row.rec_sftregs_stsp_key_sequence),
                                     g$_nls.get ('BWCKSAM1-0033','SQL',  'None')
                              )
                                )
            ;             
         ELSE
           twbkfrmt.p_tabledata (g$_nls.get ('BWCKSAM1-0034','SQL',  'None'));
         END IF;          
     END IF;  

--
         twbkfrmt.p_tablerowclose;
      END LOOP;
   END LOOP;

   twbkfrmt.p_tableclose;

--
-- After displaying the current schedule, display a few
-- summary data lines for credit hours, billing hours, etc.
-- ==================================================
   IF NOT multi_term
   THEN
      bwckcoms.p_summary (
         term,
         tot_credit_hr,
         tot_bill_hr,
         tot_ceu,
         regs_count
      );
   END IF;

--
-- Display any indirect Drops that failed because a
-- single drop code cannot be determined.
-- ==================================================
   bwckcoms.p_drop_failures_list (
      drop_failures_in,
      multi_term,
      drops_table_open
   );

   IF drop_result_label_in
       NOT IN ('DROPCONFIRMED','DROPREJECTED')
   THEN
--
-- Display any indirect Drops that were performed.
-- ==================================================
      bwckcoms.p_drop_problems_list (
         drop_problems_in,
         multi_term,
         drops_table_open
      );
   END IF;

--
-- If any indirect Drops were displayed, close the
-- table that was opened for them.
-- ==================================================
   IF drops_table_open
   THEN
      twbkfrmt.p_tableclose;
   END IF;

-- Now display the waitlisted section.
-- ==================================================
   j := 0;
   wait_count := 0;

   FOR i IN 1 .. term_in.COUNT
   LOOP
      FOR sftregs_row IN sfkcurs.sftregswatrow1c (genpidm, term_in (i))
      LOOP
         wait_count := wait_count + 1;
         j := j + 1;
         wait_crn (j) := sftregs_row.rec_sftregs_crn;

         IF wait_count = 1
         THEN
            --local09
            If not f_got_email(genpidm)  --local09 email check
            Then
                  twbkwbis.p_dispinfo('bwckcoms.P_Regs',
                                     'NOEMAIL'); --local09 pop up info;
            Else
                  twbkwbis.p_dispinfo('bwckcoms.P_Regs',
                                    'WAITLIST'); --local09 pop up info
            End If;
            --END local09
            bwckcoms.p_reg_err_heading (1, multi_term);
         END IF;

         twbkfrmt.p_tablerowopen;
         twbkfrmt.p_tabledata (
            twbkfrmt.F_FormHidden ('MESG', sftregs_row.rec_sftregs_message) ||
               sftregs_row.rec_sftregs_message
         );
--
-- Build the Action pulldown.
-- ==================================================
         bwckcoms.p_build_wait_action_pulldown (
            term_in (i),
            genpidm,
            sftregs_row.rec_sftregs_crn,
            update_wait_count + wait_count,
            sftregs_row.rec_ssbsect_ptrm_code,
            NULL,
            sftregs_row.rec_sftregs_start_date,    --C3SC GRS 03/16/2009   ADD
            sftregs_row.rec_sftregs_rsts_date,     --C3SC GRS 03/16/2009   ADD  
            row_count_out
         );
         twbkfrmt.P_FormHidden ('assoc_term_in', sftregs_row.rec_sftregs_term_code);

         IF multi_term
         THEN
            twbkfrmt.p_tabledata (sftregs_row.rec_stvterm_desc);
         END IF;

--
-- CAPP.
-- ==================================================
         IF call_path = 'S'
         THEN
            IF SUBSTR (
                  f_checkcappcrn (
                     global_pidm,
                     term_in (i),
                     sftregs_row.rec_sftregs_crn
                  ),
                  1,
                  1
               ) = 'Y'
            THEN
               twbkfrmt.p_tabledata (
                  twbkfrmt.F_FormHidden (cname=>'CRN_IN',
                                         cvalue=>sftregs_row.rec_sftregs_crn,
                                         bypass_esc=>'Y') ||
                     twbkfrmt.f_printanchor (
                        twbkfrmt.f_encodeurl (
                           'bwskscrl.P_StuDisplayCrnAreas' || '?term_in=' ||
                              term_in (i) ||
                              '&crn_in=' ||
                              sftregs_row.rec_sftregs_crn
                        ),
                        sftregs_row.rec_sftregs_crn,
                        cattributes   => bwckfrmt.f_anchor_focus (
                                            g$_nls.get ('BWCKSAM1-0035',
                                               'SQL',
                                               'Display Areas')
                                         )
                     ), cattributes=>'BYPASS_ESC=Y'
               );
            ELSE
               twbkfrmt.p_tabledata (
                  twbkfrmt.F_FormHidden ('CRN_IN', sftregs_row.rec_sftregs_crn) ||
                     sftregs_row.rec_sftregs_crn
               );
            END IF;
         ELSIF call_path = 'F'
         THEN
            IF SUBSTR (
                  f_checkcappcrn (
                     genpidm,
                     term_in (i),
                     sftregs_row.rec_sftregs_crn
                  ),
                  1,
                  1
               ) = 'Y'
            THEN
               twbkfrmt.p_tabledata (
                  twbkfrmt.F_FormHidden (cname=>'CRN_IN',
                                         cvalue=>sftregs_row.rec_sftregs_crn,
                                         bypass_esc=>'Y') ||
                     twbkfrmt.f_printanchor (
                        twbkfrmt.f_encodeurl (
                           'bwckfcrl.P_FacDisplayCrnAreas' || '?genpidm=' ||
                              --genpidm || /* defect CR-000126703 fix */
                              '&term_in=' ||
                              term_in (i) ||
                              '&crn_in=' ||
                              sftregs_row.rec_sftregs_crn
                        ),
                        sftregs_row.rec_sftregs_crn,
                        cattributes   => bwckfrmt.f_anchor_focus (
                                            g$_nls.get ('BWCKSAM1-0036',
                                               'SQL',
                                               'Display Areas')
                                         )
                     ), cattributes=>'BYPASS_ESC=Y'
               );
            ELSE
               twbkfrmt.p_tabledata (
                  twbkfrmt.F_FormHidden ('CRN_IN', sftregs_row.rec_sftregs_crn) ||
                     sftregs_row.rec_sftregs_crn
               );
            END IF;
         END IF;

         twbkfrmt.P_FormHidden (
            'start_date_in',
            TO_CHAR (
               sftregs_row.rec_sftregs_start_date,
               twbklibs.date_input_fmt
            )
         );
         twbkfrmt.P_FormHidden (
            'end_date_in',
            TO_CHAR (
               sftregs_row.rec_sftregs_completion_date,
               twbklibs.date_input_fmt
            )
         );
--
-- Display the data cells that show subject, course,
-- section, level, credit hours, grade mode, title,
-- and error message.
-- ==================================================
         twbkfrmt.p_tabledata (
            twbkfrmt.F_FormHidden ('SUBJ', sftregs_row.rec_ssbsect_subj_code) ||
               sftregs_row.rec_ssbsect_subj_code
         );
         twbkfrmt.p_tabledata (
            twbkfrmt.F_FormHidden ('CRSE', sftregs_row.rec_ssbsect_crse_numb) ||
               sftregs_row.rec_ssbsect_crse_numb
         );
         twbkfrmt.p_tabledata (
            twbkfrmt.F_FormHidden ('SEC', sftregs_row.rec_ssbsect_seq_numb) ||
               sftregs_row.rec_ssbsect_seq_numb
         );

         FOR stvlevl IN stklevl.stvlevlc (sftregs_row.rec_sftregs_levl_code)
         LOOP
            twbkfrmt.p_tabledata (
               twbkfrmt.F_FormHidden ('LEVL', sftregs_row.rec_sftregs_levl_code) ||
                  stvlevl.stvlevl_desc
            );
         END LOOP;

         twbkfrmt.p_tabledata (
            twbkfrmt.F_FormHidden (
               'CRED',
               TO_CHAR (sftregs_row.rec_sftregs_credit_hr, '9990D990')
            ) ||
               TO_CHAR (sftregs_row.rec_sftregs_credit_hr, '9990D990')
         );
         twbkfrmt.p_tabledata (
            twbkfrmt.F_FormHidden ('GMOD', sftregs_row.rec_stvgmod_desc) ||
               sftregs_row.rec_stvgmod_desc
         );
         crse_title :=
           bwcklibs.f_course_title (term_in (i), sftregs_row.rec_sftregs_crn);
         twbkfrmt.p_tabledata (
            twbkfrmt.F_FormHidden ('TITLE', crse_title) || crse_title
         );
         IF bwckcoms.F_StudyPathEnabled  = 'Y' THEN
           twbkfrmt.p_tabledata (bwckcoms.F_GetStudyPathName(genpidm,
                                                             sftregs_row.rec_sftregs_term_code,
                                                             sftregs_row.rec_sftregs_stsp_key_sequence)
                                );
         END IF;  
         twbkfrmt.p_tablerowclose;
--
-- Set variables to be passed to the drop course procedure.
-- ==================================================
         sftregs_rec.sftregs_crn := sftregs_row.rec_sftregs_crn;
         sftregs_rec.sftregs_pidm := genpidm;
         sftregs_rec.sftregs_term_code := term_in (i);
         sftregs_rec.sftregs_rsts_code :=
                                     SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2);
--
-- Drop a course that is waitlisted?
-- ==================================================

--local09 waitlist delete of sftregs row. This is required before a 
--valid wl can be added.
-- =================================================================
        If instr(upper(sftregs_row.rec_sftregs_message),
                         'WAITLISTED',
                         1) > 0
            Then
                Begin
                        Delete From sftregs t
                        Where  t.sftregs_term_code = sftregs_row.rec_sftregs_term_code
                        And    t.sftregs_pidm = genpidm
                        And    t.sftregs_crn = sftregs_row.rec_sftregs_crn;
                        Commit;
                End;
            Else 
--END local09

         bwckregs.p_dropcrse (
            sftregs_rec.sftregs_term_code,
            sftregs_rec.sftregs_crn,
            sftregs_rec.sftregs_rsts_code,
            sftregs_row.rec_sftregs_reserved_key,
            rec_stat   => 'E',
            del_ind    => 'Y'
         );
         End If; -- local09         
      END LOOP;
   END LOOP;

--
-- Now display the error section.
-- ==================================================
   errs_count := 0;
   FOR i IN 1 .. term_in.COUNT
   LOOP

      FOR sftregs_row IN sfkcurs.sftregserrc (genpidm, term_in (i))
      LOOP
         next_rec := FALSE;

--
-- Loop through waitlisted pl/sql table to see if
-- the current class is a waitlisted class. If so,
-- do not display it in the errors section.
-- ==================================================
         FOR k IN 1 .. j
         LOOP
            IF wait_crn (k) = sftregs_row.rec_sftregs_crn
            THEN
               next_rec := TRUE;
               EXIT;
            END IF;
         END LOOP;

         IF next_rec
         THEN
            GOTO next1;
         END IF;

--
-- Print the title for the registration errors section.
-- ==================================================
         errs_count := errs_count + 1;

         IF     errs_count = 1
            AND wait_count = 0
         THEN
            bwckcoms.p_reg_err_heading (NULL, multi_term);
         END IF;

--
-- Open an html table for the errors section.
-- ==================================================
         twbkfrmt.p_tablerowopen;
         twbkfrmt.p_tabledata (sftregs_row.rec_sftregs_message);

         IF wait_count > 0
         THEN
            twbkfrmt.p_tabledata;
         END IF;

         IF multi_term
         THEN
            twbkfrmt.p_tabledata (sftregs_row.rec_stvterm_desc);
         END IF;

--
-- CAPP.
-- ==================================================
         IF call_path = 'S'
         THEN
            IF SUBSTR (
                  f_checkcappcrn (
                     global_pidm,
                     term_in (i),
                     sftregs_row.rec_sftregs_crn
                  ),
                  1,
                  1
               ) = 'Y'
            THEN
               twbkfrmt.p_tabledata (
                  twbkfrmt.F_FormHidden (cname=>'CRN_IN',
                                         cvalue=>sftregs_row.rec_sftregs_crn,
                                         bypass_esc=>'Y') ||
                     twbkfrmt.f_printanchor (
                        twbkfrmt.f_encodeurl (
                           'bwskscrl.P_StuDisplayCrnAreas' || '?term_in=' ||
                              term_in (i) ||
                              '&crn_in=' ||
                              sftregs_row.rec_sftregs_crn
                        ),
                        sftregs_row.rec_sftregs_crn,
                        cattributes   => bwckfrmt.f_anchor_focus (
                                            g$_nls.get ('BWCKSAM1-0037',
                                               'SQL',
                                               'Display Areas')
                                         )
                     ), cattributes=>'BYPASS_ESC=Y'
               );
            ELSE
--
-- Don't set up CRN error as input to next registration
-- attempt.
-- ==================================================
               twbkfrmt.p_tabledata (sftregs_row.rec_sftregs_crn);
            END IF;
         ELSIF call_path = 'F'
         THEN
            IF SUBSTR (
                  f_checkcappcrn (
                     genpidm,
                     term_in (i),
                     sftregs_row.rec_sftregs_crn
                  ),
                  1,
                  1
               ) = 'Y'
            THEN
               twbkfrmt.p_tabledata (
                  twbkfrmt.F_FormHidden (cname=>'CRN_IN',
                                         cvalue=>sftregs_row.rec_sftregs_crn,
                                         bypass_esc=>'Y') ||
                     twbkfrmt.f_printanchor (
                        twbkfrmt.f_encodeurl (
                           'bwckfcrl.P_FacDisplayCrnAreas' || '?genpidm=' ||
                              --genpidm || /* defect CR-000126703 fix */
                              '&term_in=' ||
                              term_in (i) ||
                              '&crn_in=' ||
                              sftregs_row.rec_sftregs_crn
                        ),
                        sftregs_row.rec_sftregs_crn,
                        cattributes   => bwckfrmt.f_anchor_focus (
                                            g$_nls.get ('BWCKSAM1-0038',
                                               'SQL',
                                               'Display Areas')
                                         )
                     ), cattributes=>'BYPASS_ESC=Y'
               );
            ELSE
--
-- Don't set up CRN error as input to next registration
-- attempt.
-- ==================================================
               twbkfrmt.p_tabledata (sftregs_row.rec_sftregs_crn);
            END IF;
         END IF;

--
-- Display the data cells that show subject, course,
-- section, level, credit hours, grade mode, title,
-- and error message.
-- ==================================================
         twbkfrmt.p_tabledata (sftregs_row.rec_ssbsect_subj_code);
         twbkfrmt.p_tabledata (sftregs_row.rec_ssbsect_crse_numb);
         twbkfrmt.p_tabledata (sftregs_row.rec_ssbsect_seq_numb);

         FOR stvlevl IN stklevl.stvlevlc (sftregs_row.rec_sftregs_levl_code)
         LOOP
            twbkfrmt.p_tabledata (stvlevl.stvlevl_desc);
         END LOOP;

         twbkfrmt.p_tabledata (
            TO_CHAR (sftregs_row.rec_sftregs_credit_hr, '9990D990')
         );
         twbkfrmt.p_tabledata (sftregs_row.rec_stvgmod_desc);
         crse_title :=
           bwcklibs.f_course_title (term_in (i), sftregs_row.rec_sftregs_crn);
         twbkfrmt.p_tabledata (crse_title);
         IF bwckcoms.F_StudyPathEnabled  = 'Y' THEN
           twbkfrmt.p_tabledata (bwckcoms.F_GetStudyPathName(genpidm,
                                                             sftregs_row.rec_sftregs_term_code,
                                                             sftregs_row.rec_sftregs_stsp_key_sequence)
                                );
         END IF;  
         twbkfrmt.p_tablerowclose;

         --
         --DegreeWorks: Determine if DegreeWorks PreReqChk Method is available
         --
         sect_cv := sb_section.f_query_one(p_term_code => sftregs_row.rec_sftregs_term_code,
                                           p_crn       => sftregs_row.rec_sftregs_crn );
         FETCH sect_cv 
         INTO  sect_rec;
         CLOSE sect_cv;

         --Check the PreReqChk Method to determine if messages produced by DW are to be displayed.
         IF sect_rec.r_prereq_chk_method_cde = 'D' 
         THEN
           --DegreeWorks PreReq checking in affect.	
           BEGIN
             --Display the DegreeWorks PreReq results           
             OPEN csr_sftdwer(c_pidm => genpidm,
                              c_term => sect_rec.r_term_code,
                              c_crn  => sect_rec.r_crn);
             LOOP
               FETCH csr_sftdwer
               INTO  error_msg;
               EXIT  WHEN csr_sftdwer%NOTFOUND;         
               twbkfrmt.p_tablerowopen;
               --Display the error message
               --(Specifically preceeded by 10 spaces and spanning 6 of the columns)
               twbkfrmt.p_tabledata(twbkfrmt.f_PrintText( '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' ||  
                                                          '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' || 
                                                          error_msg,
                                                          class_in=>'fieldsmalltext'),
                                                          ccolspan => 6);
               twbkfrmt.p_tablerowclose;
             END LOOP;
             --If any DegreeWorks messages, add a blank line
             IF csr_sftdwer%ROWCOUNT > 0 THEN
                twbkfrmt.p_tablerowopen;
                twbkfrmt.p_tabledata;
                twbkfrmt.p_tablerowclose;
             END IF;
             CLOSE csr_sftdwer;
             twbkfrmt.p_tablerowclose;
           EXCEPTION
             WHEN OTHERS
             THEN
                twbkfrmt.p_printmessage(G$_NLS.Get ('BWCKSAM1-0039', 'SQL',
                                                   'Error occurred while checking prerequisites.'),
                                       'ERROR');  	  
           END;

         ELSE
           --DegreeWorks PreReq checking not in affect.
           NULL;
         END IF;
         --
         --DegreeWorks: End
         --
  --SIG=============================================================
          -- LOCAL02 - SFRSTCR holds status of reg try now, so see if there
          --           is an indication that the MESA self-assessment test
          --           needs to be taken (add next line).
          --  converted to 6X.  Changed references from sfrstcr to sftregs.
          -------------------------------------------------------------------
          crn := sftregs_row.rec_sftregs_crn;
          p_checkmesatest(term,
                          genpidm,
                          crn,
                          'p_regsresult');
  --end local02

         <<next1>>
         NULL;
      END LOOP;
   END LOOP;

   FOR i IN 1 .. term_in.COUNT
   LOOP
--
-- delete rows that have been flagged as in error, or waitlist
-- eligible, or the user has requested to be dropped
-- ============================================================
      sfkmods.p_delete_sftregs_removed (genpidm, term_in (i));

--
-- perform fee assessment, if applicable
-- =================================================
      p_immediate_fee_assess (term_in (i), genpidm);

   END LOOP;

END p_regsresult;

--SIG=============================================================
--LOCAL02  added mesa test.  converted to run in 6.0
-----------------------------------------------------------------------------------  
Procedure p_checkmesatest
(
    term         In stvterm.stvterm_code%Type Default Null,
    pidm         In spriden.spriden_pidm%Type Default Null,
    crn          In sftregs.sftregs_crn%Type Default Null,
    calling_proc In Varchar2
) Is
    mesa_found Varchar2(1) := 'N';
    espt_url   Varchar2(128) := '';
    --local07
    Cursor checkmesac(term In stvterm.stvterm_code%Type, pidm In spriden.spriden_pidm%Type) Is
        Select 'Y'
        From   sftregs
        Where  upper(sftregs_message) Like upper('PREQ and TEST SCORE-ERROR%')
        And    sftregs_pidm = pidm
        And    sftregs_term_code = term
        And    sftregs_crn = crn
        And    Exists (Select 'X'
                From   ssrrtst r
                Where  r.ssrrtst_term_code = term
                And    r.ssrrtst_subj_code_preq = 'ENGL'
                And    r.ssrrtst_crse_numb_preq = 'M01A'
                And    r.ssrrtst_crn = crn);
    --end local07
Begin
    If Not twbkwbis.f_validuser(global_pidm)
    Then
        Return;
    End If;
    /*          htp.p(calling_proc||' has called P_CheckMesaTest.'); htp.br;
    htp.p('Pidm = '|| pidm ||'. Term = ' || term); htp.br;*/
    Open checkmesac(term,
                    pidm);
    Fetch checkmesac
        Into mesa_found;
    /*         htp.p('mesa_found = ' || mesa_found); htp.br;*/
    If checkmesac%Found
    Then
        If twbkwbis.f_validlink('pw_mesa.P_StuEngPlaIntro')
        Then
            twbkfrmt.p_printbold('The course you have chosen has a prerequisite requirement ' ||
                                 ' that you must complete before registering for this class.  If it is ENGL M01A ' ||
                                 ' at Moorpark College you may take the ' ||
                                 twbkfrmt.f_printanchor(twbklibs.twgbwrul_rec.twgbwrul_cgibin_dir ||
                                                        '/pw_mesa.P_StuEngPlaIntro',
                                                        ctext => 'English Self Placement Process') ||
                                 '.  Otherwise please refer to the course catalog for specific prerequisites' ||
                                 ' for this class.  If you have fulfilled the prerequisite at another college,' ||
                                 ' please contact the Counseling Dept. for approval.');
            htp.br;
        Else
            twbkfrmt.p_printbold('You need to contact the Matriculation Office to complete ' ||
                                 'the self placement process before enrolling in this class.');
            htp.br;
        End If;
    End If;
    Close checkmesac;
End p_checkmesatest;
-- End LOCAL02


BEGIN                                                     /* initialization */
   NULL;
END bwcksams;
