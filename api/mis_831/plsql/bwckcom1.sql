create or replace PACKAGE BODY bwckcoms
AS
--
-- ***** COAST MODIFICATION *****
--
--AUDIT_TRAIL_MSGKEY_UPDATE
-- PROJECT : MSGKEY
-- MODULE  : BWCKCOM1
-- SOURCE  : enUS
-- TARGET  : I18N
-- DATE    : Wed Dec 02 12:19:51 2015
-- MSGSIGN : #8a0d39cb95f7e521
--TMI18N.ETR DO NOT CHANGE--
--
-- FILE NAME..: bwckcom1.sql
-- RELEASE....: 8.7.1.1[C3SC 8.11.0.1]
-- OBJECT NAME: BWCKCOMS
-- PRODUCT....: CALB
-- COPYRIGHT..: Copyright 2015 Ellucian Company L.P. and its affiliates.
--

   curr_release            CONSTANT VARCHAR2 (30)             := '8.7.1.1[C3SC 8.11.0.1]';
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
   C_FONTSML     CONSTANT VARCHAR2(30) := '''fieldformattext''';
--------------------------------------------------------------------------

-- =====================================================================
-- This function calls the sfkfunc function of the same name.
-- This checks to see if the access_id we have created is the same as
-- the one stored in SFRRACL.
-- If they are different, we know that some other session has taken
-- 'possession' of sftregs, and we need to prevent the current session
-- from also trying to use it.
-- =====================================================================
   FUNCTION f_reg_access_still_good (
      pidm_in        IN   SPRIDEN.SPRIDEN_PIDM%TYPE,
      term_in        IN   STVTERM.STVTERM_CODE%TYPE,
      call_path_in   IN   VARCHAR2,
      proc_name_in   IN   VARCHAR2
   )
      RETURN BOOLEAN
   IS
      reg_sess_id    VARCHAR2 (7)    := '';
      cookie_value   VARCHAR2 (2000) := '';
      sessid         VARCHAR2 (7);
      gtvsdax_row    gtvsdax%ROWTYPE;
      reg_seqno      NUMBER;

      CURSOR seqno_c ( pidm_in SPRIDEN.SPRIDEN_PIDM%TYPE,
                       term_in STVTERM.STVTERM_CODE%TYPE)
      IS
         SELECT NVL (MAX (sfrstca_seq_number), 0)
           FROM sfrstca
          WHERE sfrstca_term_code = term_in
            AND sfrstca_pidm (+) = pidm_in;
   BEGIN
      IF NOT sfkfunc.f_reg_access_still_good (pidm_in, term_in, call_path_in)
      THEN
         bwckfrmt.p_open_doc (proc_name_in, term_in);

         twbkwbis.p_dispinfo (proc_name_in, 'SESSIONEXP');
         twbkwbis.p_closedoc (curr_release);
         RETURN FALSE;
      END IF;

      OPEN seqno_c(pidm_in, term_in);
      FETCH seqno_c INTO reg_seqno;
      CLOSE seqno_c;
      sfkcurs.p_get_gtvsdax ('MAXREGNO', 'WEBREG', gtvsdax_row);

      IF reg_seqno > NVL (gtvsdax_row.gtvsdax_external_code, 9999)
      THEN
         bwckfrmt.p_open_doc (proc_name_in, term_in);
         twbkwbis.p_dispinfo (proc_name_in, 'MAXREGNO');
         twbkwbis.p_closedoc (curr_release);
         RETURN FALSE;
      END IF;

      RETURN TRUE;
   END f_reg_access_still_good;

--------------------------------------------------------------------------
   FUNCTION f_find_associated_term (
      term_in   IN   OWA_UTIL.ident_arr,
      crn_in    IN   VARCHAR2
   )
      RETURN VARCHAR2
   IS
      CURSOR ssbsectc (term_in VARCHAR2, crn_in VARCHAR2)
      IS
         SELECT *
           FROM ssbsect
          WHERE ssbsect_term_code = term_in
            AND ssbsect_crn = crn_in;

      ssbsect_rec   ssbsect%ROWTYPE;
   BEGIN
      FOR i IN 1 .. term_in.COUNT
      LOOP
         OPEN ssbsectc (term_in (i), crn_in);
         FETCH ssbsectc INTO ssbsect_rec;
         CLOSE ssbsectc;
      END LOOP;

      RETURN ssbsect_rec.ssbsect_term_code;
   END f_find_associated_term;

--------------------------------------------------------------------------
   FUNCTION f_trim_sel_crn (sel_crn_in IN VARCHAR2)
      RETURN VARCHAR2
   IS
      space_position   NUMBER;
   BEGIN
      space_position := INSTR (sel_crn_in, ' ');

      IF space_position = 0
      THEN
         RETURN sel_crn_in;
      ELSE
         RETURN SUBSTR (sel_crn_in, 1, space_position - 1);
      END IF;
   END f_trim_sel_crn;

--------------------------------------------------------------------------
   FUNCTION f_trim_sel_crn_term (sel_crn_in IN VARCHAR2)
      RETURN VARCHAR2
   IS
      space_position   NUMBER;
   BEGIN
      space_position := INSTR (sel_crn_in, ' ');

      IF space_position = 0
      THEN
         RETURN sel_crn_in;
      ELSE
         RETURN SUBSTR (sel_crn_in, space_position + 1);
      END IF;
   END f_trim_sel_crn_term;

-- =====================================================================
-- This procedure displays the add drop page after a course has been
-- chosen from the lookup classes to add menu option (as opposed to the
-- add/drop classes menu option).
-- It is called from P_AddFromSearch.
-- =====================================================================
   PROCEDURE p_adddrop (
      term         IN   OWA_UTIL.ident_arr,
      assoc_term   IN   OWA_UTIL.ident_arr,
      sel_crn      IN   OWA_UTIL.ident_arr
   )
   IS
      j                    INTEGER := 0;
      errs_count           NUMBER;
      regs_count           NUMBER;
      wait_count           NUMBER;
      reg_access_allowed   BOOLEAN;
      multi_term           BOOLEAN := TRUE;
      drop_problems        sfkcurs.drop_problems_rec_tabtype;
      drop_failures        sfkcurs.drop_problems_rec_tabtype;
      capp_tech_error      VARCHAR2 (4) := NULL;

   BEGIN
       IF NOT twbkwbis.f_validuser (global_pidm)
       THEN
          RETURN;
       END IF;

       IF term.COUNT = 1
       THEN
          multi_term := FALSE;
       END IF;

--
-- Display the current registration information.
-- ======================================================
      bwcksams.p_regsresult (
         term,
         errs_count,
         regs_count,
         wait_count,
         NULL,
         reg_access_allowed,
         capp_tech_error,
         NULL,
         drop_problems,
         drop_failures
      );

      IF capp_tech_error IS NOT NULL
      THEN
         RETURN;
      END IF;

      IF    errs_count > 0
         OR wait_count > 0
      THEN
         twbkfrmt.p_tableclose;
         HTP.br;
      END IF;

      IF NOT multi_term
      THEN
--
-- Display the add table.
-- ======================================================
         p_add_drop_crn1 (1, sel_crn.COUNT);

         BEGIN
            j := 2;

            WHILE sel_crn (j) IS NOT NULL
            LOOP
               twbkfrmt.p_tabledataopen;
               twbkfrmt.P_FormHidden (
                  'rsts_in',
                  SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2)
               );
               twbkfrmt.p_formlabel (
                  g$_nls.get ('BWCKCOM1-0000', 'SQL', 'CRN'),
                  visible   => 'INVISIBLE',
                  idname    => 'crn_id' || TO_CHAR (j - 1)
               );
               twbkfrmt.p_formtext (
                  'crn_in',
                  '5',
                  '5',
                  cvalue        => f_trim_sel_crn (sel_crn (j)),
                  cattributes   => 'ID="crn_id' || TO_CHAR (j - 1) || '"'
               );
               twbkfrmt.P_FormHidden ('assoc_term_in', assoc_term (j));
               twbkfrmt.P_FormHidden ('start_date_in', NULL);
               twbkfrmt.P_FormHidden ('end_date_in', NULL);
               twbkfrmt.p_tabledataclose;
               j := j + 1;
            END LOOP;
         EXCEPTION
            WHEN NO_DATA_FOUND
            THEN
               FOR i IN j - 1 .. j + 8
               LOOP
                  twbkfrmt.p_tabledataopen;
                  twbkfrmt.P_FormHidden (
                     'rsts_in',
                     SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2)
                  );
                  twbkfrmt.p_formlabel (
                     g$_nls.get ('BWCKCOM1-0001', 'SQL', 'CRN'),
                     visible   => 'INVISIBLE',
                     idname    => 'crn_id' || TO_CHAR (i)
                  );
                  twbkfrmt.p_formtext (
                     'crn_in',
                     '5',
                     '5',
                     cattributes   => 'ID="crn_id' || TO_CHAR (i) || '"'
                  );
                  twbkfrmt.P_FormHidden ('assoc_term_in', NULL);
                  twbkfrmt.P_FormHidden ('start_date_in', NULL);
                  twbkfrmt.P_FormHidden ('end_date_in', NULL);
                  twbkfrmt.p_tabledataclose;
               END LOOP;
         END;

         twbkfrmt.p_tableclose;
      END IF;

      BEGIN
         IF sel_crn (2) IS NULL
         THEN
            j := 1;
         END IF;
      EXCEPTION
         WHEN OTHERS
         THEN
            j := 1;
      END;

      p_add_drop_crn2 (TO_NUMBER (10 + j), regs_count, 0);
      HTP.formclose;
      twbkwbis.p_closedoc (curr_release);
   END p_adddrop;

--------------------------------------------------------------------------


-- =====================================================================
-- This procedure displays the add drop page.
-- It gets called from P_AddFromSearch1.
-- =====================================================================
   PROCEDURE p_adddrop1 (
      term_in         IN   OWA_UTIL.ident_arr,
      sel_crn         IN   OWA_UTIL.ident_arr,
      assoc_term_in   IN   OWA_UTIL.ident_arr,
      crn_in          IN   OWA_UTIL.ident_arr,
      start_date_in   IN   OWA_UTIL.ident_arr,
      end_date_in     IN   OWA_UTIL.ident_arr,
      rsts_in         IN   OWA_UTIL.ident_arr,
      subj            IN   OWA_UTIL.ident_arr,
      crse            IN   OWA_UTIL.ident_arr,
      sec             IN   OWA_UTIL.ident_arr,
      levl            IN   OWA_UTIL.ident_arr,
      cred            IN   OWA_UTIL.ident_arr,
      gmod            IN   OWA_UTIL.ident_arr,
      title           IN   bwckcoms.varchar2_tabtype,
      mesg            IN   OWA_UTIL.ident_arr,
      regs_row             NUMBER,
      add_row              NUMBER,
      wait_row             NUMBER
   )
   IS
      j                           INTEGER                                := 2;
      k                           INTEGER                                := 2;
      l                           NUMBER                                 := 0;
      sdax_rsts_code              VARCHAR2 (2);
      tot_credit_hr               SFTREGS.SFTREGS_CREDIT_HR%TYPE         := 0;
      tot_bill_hr                 SFTREGS.SFTREGS_BILL_HR%TYPE           := 0;
      max_hr                      SFBETRM.SFBETRM_MHRS_OVER%TYPE         := 0;
      min_hr                      SFBETRM.SFBETRM_MIN_HRS%TYPE           := 0;
      tot_ceu                     SFTREGS.SFTREGS_CREDIT_HR%TYPE         := 0;
      regs_count                  NUMBER                                 := 0;
      errs_count                  NUMBER                                 := 0;
      wait_count                  NUMBER                                 := 0;
      ssbxlst_count               NUMBER                                 := 0;
      hold_rsts                   VARCHAR2 (4);
      gmod_code                   STVGMOD.STVGMOD_CODE%TYPE;
      gmod_desc                   STVGMOD.STVGMOD_DESC%TYPE;
      ssbxlst_row                 ssbxlst%ROWTYPE;
      term                        stvterm.stvterm_code%TYPE            := NULL;
      multi_term                  BOOLEAN                              := TRUE;
      crse_title                  SSRSYLN.SSRSYLN_LONG_COURSE_TITLE%TYPE;
      change_class_options_proc   VARCHAR2 (100);
      heading_displayed           BOOLEAN                             := FALSE;
      row_count                   NUMBER                              := 0;
      crndirect                   VARCHAR2(1) :=
                                  NVL(SUBSTR(bwcklibs.f_getgtvsdaxrule('CRNDIRECT','WEBREG'),1,1),'N');
                                  

-- SP changes
      lv_sp_name  VARCHAR2(4000);                                   
                                  
   BEGIN
      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN;
      END IF;

      IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
      THEN
         genpidm :=
           TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
         change_class_options_proc := 'bwlkfrad.P_FacChangeCrseOpt';
      ELSE
         genpidm := global_pidm;
         change_class_options_proc := 'bwskfreg.P_ChangeCrseOpt';
      END IF;

      p_add_drop_init (term_in);
      term := term_in (term_in.COUNT);

      IF term_in.COUNT = 1
      THEN
         multi_term := FALSE;
      END IF;
      
      IF twbkwbis.f_getparam (global_pidm, 'G_FROM_SP')  IS NOT NULL THEN    
        twbkmods.p_delete_twgrwprm_pidm_name (global_pidm, 'G_FROM_SP');    
      END IF;

--
-- Set the submit button to call p_regs.
-- ======================================================
      HTP.formopen (
         twbkwbis.f_cgibin || 'bwckcoms.P_Regs',
         cattributes   => 'onSubmit="return checkSubmit()"'
      );
--
-- Display instructions, pictures, etc.
-- ======================================================

      twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'DEFAULT');

--
-- Display the current registration information.
-- ======================================================
      FOR i IN 1 .. term_in.COUNT
      LOOP
         twbkfrmt.P_FormHidden ('term_in', term_in (i));
      END LOOP;

      twbkfrmt.P_FormHidden ('RSTS_IN', 'DUMMY');
      twbkfrmt.P_FormHidden ('assoc_term_in', 'DUMMY');
      twbkfrmt.P_FormHidden ('crn_in', 'DUMMY');
      twbkfrmt.P_FormHidden ('start_date_in', 'DUMMY');
      twbkfrmt.P_FormHidden ('end_date_in', 'DUMMY');
      twbkfrmt.P_FormHidden ('SUBJ', 'DUMMY');
      twbkfrmt.P_FormHidden ('CRSE', 'DUMMY');
      twbkfrmt.P_FormHidden ('SEC', 'DUMMY');
      twbkfrmt.P_FormHidden ('LEVL', 'DUMMY');
      twbkfrmt.P_FormHidden ('CRED', 'DUMMY');
      twbkfrmt.P_FormHidden ('GMOD', 'DUMMY');
      twbkfrmt.P_FormHidden ('MESG', 'DUMMY');
      twbkfrmt.P_FormHidden ('TITLE', 'DUMMY');
      twbkfrmt.P_FormHidden ('REG_BTN', 'DUMMY');
      bwcklibs.p_initvalue (global_pidm, term, '', '', '', '');
      bwckregs.p_calchrs (tot_credit_hr, tot_bill_hr, tot_ceu);
      regs_count := 0;

      FOR i IN 1 .. term_in.COUNT
      LOOP
         FOR sftregs_row IN sfkcurs.sftregsrowc (genpidm, term_in (i))
         LOOP
            regs_count := regs_count + 1;
            p_curr_sched_heading (heading_displayed, term, multi_term);
            hold_rsts := '';

            BEGIN
               WHILE crn_in (k) IS NOT NULL
               LOOP
                  IF     crn_in (k) = sftregs_row.rec_sftregs_crn
                     AND rsts_in (k) IS NOT NULL
                  THEN
                     hold_rsts := rsts_in (k);
                     EXIT;
                  END IF;

                  k := k + 1;
               END LOOP;
            EXCEPTION
               WHEN OTHERS
               THEN
                  k := 2;
            END;

            twbkfrmt.p_tablerowopen;

            FOR stvrsts IN
                stkrsts.stvrstsc (sftregs_row.rec_sftregs_rsts_code)
            LOOP
               twbkfrmt.p_tabledata (
                  g$_nls.get ('BWCKCOM1-0002',
                     'SQL',
                     '%01%%02% on %03%',
                     twbkfrmt.F_FormHidden ('MESG', 'DUMMY'),
                     stvrsts.stvrsts_desc,
                     TO_CHAR (
                        sftregs_row.rec_sftregs_rsts_date,
                        twbklibs.date_display_fmt
                     )
                  )
               );
            END LOOP;

            sdax_rsts_code := SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2);

            IF NVL (sftregs_row.rec_sftregs_error_flag, '#') <> 'F'
            THEN
               IF sftregs_row.rec_sftregs_grde_date IS NOT NULL
                  OR ( sftregs_row.rec_ssbsect_voice_avail = 'N'
                       AND crndirect = 'N')
               THEN
                  twbkfrmt.p_tabledata (twbkfrmt.F_FormHidden ('RSTS_IN', '') || '&nbsp');
               ELSE
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
                     sdax_rsts_code,
                     hold_rsts
                  );
               END IF;
            ELSE
               twbkfrmt.p_tabledata (twbkfrmt.F_FormHidden ('RSTS_IN', '') || '&nbsp');
            END IF;

            IF multi_term
            THEN
               twbkfrmt.p_tabledata (sftregs_row.rec_stvterm_desc);
            END IF;

            twbkfrmt.p_tabledata (
               twbkfrmt.F_FormHidden ('assoc_term_in', sftregs_row.rec_sftregs_term_code) ||
               twbkfrmt.F_FormHidden ('crn_in', sftregs_row.rec_sftregs_crn) ||
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

            FOR scrlevl IN scklibs.scrlevlc (
                              sftregs_row.rec_ssbsect_subj_code,
                              sftregs_row.rec_ssbsect_crse_numb,
                              sftregs_row.rec_sftregs_term_code
                           )
            LOOP
               row_count := scklibs.scrlevlc%rowcount;
            END LOOP;

            FOR stvlevl IN
                stklevl.stvlevlc (sftregs_row.rec_sftregs_levl_code)
            LOOP
               bwcklibs.p_getsobterm (
                  sftregs_row.rec_sftregs_term_code,
                  sobterm_row
               );

               IF     NVL (sobterm_row.sobterm_levl_web_upd_ind, 'N') = 'Y'
                  AND sftregs_row.rec_sftregs_grde_date IS NULL
                  AND row_count > 1
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
                                               g$_nls.get ('BWCKCOM1-0003',
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

            FOR scbcrse IN
                scklibs.scbcrsec (
                   sftregs_row.rec_ssbsect_subj_code,
                   sftregs_row.rec_ssbsect_crse_numb,
                   term_in (i)
                )
            LOOP
               scbcrse_row := scbcrse;
            END LOOP;

            IF     NVL (sobterm_row.sobterm_cred_web_upd_ind, 'N') = 'Y'
               AND sftregs_row.rec_sftregs_grde_date IS NULL
               AND sftregs_row.rec_scbcrse_credit_hr_ind IS NOT NULL
               AND sftregs_row.rec_ssbsect_credit_hrs IS NULL
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
                                            g$_nls.get ('BWCKCOM1-0004',
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
                                            g$_nls.get ('BWCKCOM1-0005',
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

            crse_title :=
              bwcklibs.f_course_title (
                 term_in (i),
                 sftregs_row.rec_sftregs_crn
              );
            twbkfrmt.p_tabledata (
               twbkfrmt.F_FormHidden ('TITLE', crse_title) || crse_title
            );


-- Display the studypath info
-- ======================================================
           IF bwckcoms.F_StudyPathEnabled  = 'Y' THEN
                     twbkfrmt.p_tabledata (bwckcoms.F_GetStudyPathName(genpidm,
                                                                       term_in(i),
                                                                       sftregs_row.rec_sftregs_stsp_key_sequence)
                                );
             
           END IF;
            
            twbkfrmt.p_tablerowclose;
         END LOOP;
      END LOOP;

      twbkfrmt.p_tableclose;

--
-- Display the hours information.
-- ===================================================
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
-- Display registration errors.
-- ===================================================
      IF wait_row > 0
      THEN
         FOR i IN regs_row + 2 .. regs_row + wait_row + 1
         LOOP
            IF i = regs_row + 2
            THEN
               p_reg_err_heading (1, multi_term);
            END IF;

            twbkfrmt.p_tablerowopen;
            twbkfrmt.p_tabledata (twbkfrmt.F_FormHidden ('MESG', mesg (i)) || mesg (
                                                                          i
                                                                       ));

            FOR ssbsect IN ssklibs.ssbsectc (crn_in (i), assoc_term_in (i))
            LOOP
--
-- Build the Action pulldown - if waitlistable.
-- ==================================================
               bwckcoms.p_build_wait_action_pulldown (
                  assoc_term_in (i),
                  genpidm,
                  crn_in (i),
                  i,
                  ssbsect.ssbsect_ptrm_code,
                  rsts_in (i),
                  start_date_in(i),    --C3SC GRS 03/16/2009   ADD
                  SYSDATE,     --C3SC GRS 03/16/2009   ADD   
                  row_count
               );

               IF row_count = 0
               THEN
                  twbkfrmt.P_FormHidden ('RSTS_IN', '');
               END IF;
            END LOOP;

            IF multi_term
            THEN
               twbkfrmt.p_tabledata (bwcklibs.f_term_desc (assoc_term_in (i)));
            END IF;

            twbkfrmt.p_tabledata (
               twbkfrmt.F_FormHidden ('assoc_term_in', assoc_term_in (i)) ||
               twbkfrmt.F_FormHidden ('crn_in', crn_in (i)) || crn_in (i) ||
               twbkfrmt.F_FormHidden ('start_date_in', start_date_in (i)) ||
               twbkfrmt.F_FormHidden ('end_date_in', end_date_in (i))
            );
            twbkfrmt.p_tabledata (twbkfrmt.F_FormHidden ('SUBJ', subj (i)) || subj (
                                                                          i
                                                                       ));
            twbkfrmt.p_tabledata (twbkfrmt.F_FormHidden ('CRSE', crse (i)) || crse (
                                                                          i
                                                                       ));
            twbkfrmt.p_tabledata (twbkfrmt.F_FormHidden ('SEC', sec (i)) || sec (i));
            twbkfrmt.p_tabledata (twbkfrmt.F_FormHidden ('LEVL', levl (i)) || levl (
                                                                          i
                                                                       ));
            twbkfrmt.p_tabledata (twbkfrmt.F_FormHidden ('CRED', cred (i)) || cred (
                                                                          i
                                                                       ));
            twbkfrmt.p_tabledata (twbkfrmt.F_FormHidden ('GMOD', gmod (i)) || gmod (
                                                                          i
                                                                       ));
            twbkfrmt.p_tabledata (
               twbkfrmt.F_FormHidden ('TITLE', title (i)) || title (i)
            );
            
            
-- Display the studypath info
-- ======================================================
           
           
           IF bwckcoms.F_StudyPathEnabled  = 'Y' THEN
             IF NOT multi_term THEN
             lv_sp_name :=                                                   sb_studypath_name.f_learner_study_path_name(genpidm,
                                                       SUBSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),INSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),'|')+1),
                                                       SUBSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),1, INSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),'|')-1)
                                                                                                   );             
             twbkfrmt.p_tabledata (NVL(lv_sp_name,g$_nls.get ('BWCKCOM1-0006', 'SQL', 'None')));                                                                                                   
             END IF;
           END IF;
           
         END LOOP;
      END IF;

      IF wait_row > 0
      THEN
         twbkfrmt.p_tableclose;
         HTP.br;
      END IF;

      IF NOT multi_term
      THEN
--
-- Display the add table.
-- ===================================================
         p_add_drop_crn1 (1, sel_crn.COUNT);

         FOR k IN regs_row + wait_row + 2 .. regs_row + wait_row + add_row +
                                                1
         LOOP
            BEGIN
               IF crn_in (k) IS NOT NULL
               THEN
                  l := l + 1;
                  twbkfrmt.p_tabledataopen;
                  twbkfrmt.P_FormHidden (
                     'RSTS_IN',
                     SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2)
                  );
                  twbkfrmt.p_formlabel (
                     g$_nls.get ('BWCKCOM1-0007', 'SQL', 'CRN'),
                     visible   => 'INVISIBLE',
                     idname    => 'crn_id' || TO_CHAR (l)
                  );
                  twbkfrmt.p_formtext (
                     'crn_in',
                     '5',
                     '5',
                     cvalue        => crn_in (k),
                     cattributes   => 'ID="crn_id' || TO_CHAR (l) || '"'
                  );
                  twbkfrmt.P_FormHidden ('assoc_term_in', assoc_term_in (k));
                  twbkfrmt.P_FormHidden ('start_date_in', NULL);
                  twbkfrmt.P_FormHidden ('end_date_in', NULL);
                  twbkfrmt.p_tabledataclose;
               END IF;
            EXCEPTION
               WHEN OTHERS
               THEN
                  NULL;
            END;
         END LOOP;

--
-- ?????
-- ===================================================
         BEGIN
            WHILE sel_crn (j) IS NOT NULL
            LOOP
               l := l + 1;
               twbkfrmt.p_tabledataopen;
               twbkfrmt.P_FormHidden (
                  'RSTS_IN',
                  SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2)
               );
               twbkfrmt.p_formlabel (
                  g$_nls.get ('BWCKCOM1-0008', 'SQL', 'CRN'),
                  visible   => 'INVISIBLE',
                  idname    => 'crn_id' || TO_CHAR (l)
               );
               twbkfrmt.p_formtext (
                  'crn_in',
                  '5',
                  '5',
                  cvalue        => f_trim_sel_crn (sel_crn (j)),
                  cattributes   => 'ID="crn_id' || TO_CHAR (l) || '"'
               );
               twbkfrmt.P_FormHidden ('assoc_term_in', assoc_term_in (j));
               twbkfrmt.P_FormHidden ('start_date_in', NULL);
               twbkfrmt.P_FormHidden ('end_date_in', NULL);
               twbkfrmt.p_tabledataclose;
               j := j + 1;
            END LOOP;
         EXCEPTION
            WHEN NO_DATA_FOUND
            THEN
               l := l + 1;

               FOR i IN l .. l + 9
               LOOP
                  twbkfrmt.p_tabledataopen;
                  twbkfrmt.P_FormHidden (
                     'RSTS_IN',
                     SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2)
                  );
                  twbkfrmt.p_formlabel (
                     g$_nls.get ('BWCKCOM1-0009', 'SQL', 'CRN'),
                     visible   => 'INVISIBLE',
                     idname    => 'crn_id' || TO_CHAR (i)
                  );
                  twbkfrmt.p_formtext (
                     'crn_in',
                     '5',
                     '5',
                     cattributes   => 'ID="crn_id' || TO_CHAR (i) || '"'
                  );
                  twbkfrmt.P_FormHidden ('assoc_term_in', assoc_term_in (l));
                  twbkfrmt.P_FormHidden ('start_date_in', NULL);
                  twbkfrmt.P_FormHidden ('end_date_in', NULL);
                  twbkfrmt.p_tabledataclose;
               END LOOP;
         END;

         twbkfrmt.p_tablerowclose;
         twbkfrmt.p_tableclose;
      ELSE
         l := l + 1;
      END IF;


      p_add_drop_crn2 (TO_NUMBER (9 + l), regs_count, wait_count);
      HTP.formclose;
      twbkwbis.p_closedoc (curr_release);
   END p_adddrop1;

--------------------------------------------------------------------------

--
-- =====================================================================
-- This procedure displays the add/drop page.
-- 1. The p_adddropcrse procedure executes when "Add/Drop Classes" is selected
--    from the registration menu. It displays the add/drop page.
-- 2. The P_Regs procedure executes when the submit button is pressed on the
--    add/drop page. It processes the registration changes.
-- 3. This procedure executes after registration changes are processed by
--    the P_Regs procedure. It redisplays the add/drop page.
-- =====================================================================
   PROCEDURE p_adddrop2 (
      term_in           IN   OWA_UTIL.ident_arr,
      err_term          IN   OWA_UTIL.ident_arr,
      err_crn           IN   OWA_UTIL.ident_arr,
      err_subj          IN   OWA_UTIL.ident_arr,
      err_crse          IN   OWA_UTIL.ident_arr,
      err_sec           IN   OWA_UTIL.ident_arr,
      err_code          IN   OWA_UTIL.ident_arr,
      err_levl          IN   OWA_UTIL.ident_arr,
      err_cred          IN   OWA_UTIL.ident_arr,
      err_gmod          IN   OWA_UTIL.ident_arr,
      capp_tech_error_in_out   IN OUT  VARCHAR2,
      drop_result_label_in IN twgrinfo.twgrinfo_label%TYPE DEFAULT NULL,
      drop_problems_in     IN sfkcurs.drop_problems_rec_tabtype,
      drop_failures_in     IN sfkcurs.drop_problems_rec_tabtype
      )
   IS
      i                    INTEGER                         := 2;
      errs_count           NUMBER;
      regs_count           NUMBER;
      wait_count           NUMBER;
      err_levl_desc        STVLEVL.STVLEVL_DESC%TYPE;
      err_crhrs            SSBSECT.SSBSECT_CREDIT_HRS%TYPE;
      err_gmod_desc        STVGMOD.STVGMOD_DESC%TYPE;
      reg_access_allowed   BOOLEAN;
      term                 stvterm.stvterm_code%TYPE       := NULL;
      multi_term           BOOLEAN                         := TRUE;
      lv_sp_name           VARCHAR2(4000);
      lv_sgrstsp_eff_term  stvterm.stvterm_code%TYPE       := NULL;
      
     
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

      term := term_in (term_in.COUNT);

      IF term_in.COUNT = 1
      THEN
         multi_term := FALSE;
      END IF;

--
-- Display the current registration information.
-- ===================================================
      bwcksams.p_regsresult (
         term_in,
         errs_count,
         regs_count,
         wait_count,
         NULL,
         reg_access_allowed,
         capp_tech_error_in_out,
         drop_result_label_in,
         drop_problems_in,
          drop_failures_in
      );

--
-- only redisplay add/drop page if no capp technical problems.
-- ===========================================================
      IF capp_tech_error_in_out IS NOT NULL
      THEN
         RETURN;
      END IF;

--
-- Display registration errors.
-- ===================================================
      DECLARE
--
-- Cursor to get level description.
-- =======================================================
         CURSOR level_desc_c (levl_in VARCHAR2)
         IS
            SELECT NVL (stvlevl_desc, ' ')
              FROM stvlevl
             WHERE stvlevl_code = levl_in;

--
-- Cursor to get grade mode description.
-- =======================================================
         CURSOR gmod_desc_c (gmod_in VARCHAR2)
         IS
            SELECT NVL (stvgmod_desc, ' ')
              FROM stvgmod
             WHERE stvgmod_code = gmod_in;
      BEGIN
         WHILE err_crn (i) IS NOT NULL
         LOOP
            IF     errs_count = 0
               AND i = 2
               AND wait_count = 0
            THEN
               p_reg_err_heading (NULL, multi_term);
            END IF;

--
-- Start a row of error data in the error display table.
-- ======================================================
            twbkfrmt.p_tablerowopen;

            BEGIN
               IF err_code (i) IS NOT NULL
               THEN
                 --C3SC GRS 24/02/2006 
                 IF err_code(i)='AUTHC' THEN
                  twbkfrmt.p_tabledata (
                    g$_nls.get ('BWCKCOM1-0010', 'SQL', 'Canceled Registration')
                    );  

                 --END C3SC
                 ELSE 

                  twbkfrmt.p_tabledata (
                     bwcklibs.error_msg_table (err_code (i))
                  );
                 END IF; --C3SC GRS 24/02/2006  
               ELSE
                  twbkfrmt.p_tabledata (
                     sb_registration_msg.f_get_message(p_cde => 'SYST', p_seqno => 1)
                  );
               END IF;
            EXCEPTION
               WHEN OTHERS
               THEN
                  twbkfrmt.p_tabledata (
                     sb_registration_msg.f_get_message(p_cde => 'SYST', p_seqno => 1)
                  );
            END;

            IF wait_count > 0
            THEN
               twbkfrmt.p_tabledata;
            END IF;

            IF multi_term
            THEN
               twbkfrmt.p_tabledata (bwcklibs.f_term_desc (err_term (i)));
            END IF;

            twbkfrmt.p_tabledata (err_crn (i));

            BEGIN
               IF err_subj (i) IS NOT NULL
               THEN
                  twbkfrmt.p_tabledata (err_subj (i));
                  twbkfrmt.p_tabledata (err_crse (i));
-- <<Coast>> -- start changes TRvvvv 9/30/2015
/*  Remove section and level data
                  twbkfrmt.p_tabledata (err_sec (i));
                  OPEN level_desc_c (err_levl (i));
                  FETCH level_desc_c INTO err_levl_desc;
                  twbkfrmt.p_tabledata (err_levl_desc);
                  CLOSE level_desc_c;
*/
                  twbkfrmt.p_tabledata (' ');  -- Skip over Part of Term column
                  twbkfrmt.p_tabledata (' ');  -- Skip over Dates
-- <<Coast>> -- start changes TR^^^^ 9/30/2015
                  twbkfrmt.p_tabledata (err_cred (i));
                  OPEN gmod_desc_c (err_gmod (i));
                  FETCH gmod_desc_c INTO err_gmod_desc;
                  twbkfrmt.p_tabledata (err_gmod_desc);
                  CLOSE gmod_desc_c;
                  twbkfrmt.p_tabledata (
                     bwcklibs.f_course_title (err_term (i), err_crn (i))
                  );
                  
                  -- Display the studypath info
-- ======================================================
                 
                 
                 IF bwckcoms.F_StudyPathEnabled  = 'Y' THEN  
                   IF NOT multi_term THEN
                   
                   lv_sp_name :=                                                   sb_studypath_name.f_learner_study_path_name(genpidm,
                                                             SUBSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),INSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),'|')+1),
                                                             SUBSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),1, INSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),'|')-1)
                                                                                                         );             
                   
                   ELSE
                   lv_sgrstsp_eff_term := null;
                                      
                    lv_sp_name :=   bwckcoms.F_GetStudyPathName(genpidm,
                                                             err_term(i),
                                                             twbkwbis.f_getparam (global_pidm, 'G_SP'||err_term (i))) ;
                   
               
                   
                   END IF;
                   twbkfrmt.p_tabledata (NVL(lv_sp_name,g$_nls.get ('BWCKCOM1-0011', 'SQL', 'None')));                                                                                                   
                 END IF;

               ELSE
                  twbkfrmt.p_tabledata;
                  twbkfrmt.p_tabledata;
                  twbkfrmt.p_tabledata;
                  twbkfrmt.p_tabledata;
                  twbkfrmt.p_tabledata;
                  twbkfrmt.p_tabledata;
                  twbkfrmt.p_tabledata;
                  IF bwckcoms.F_StudyPathEnabled  = 'Y' THEN
                    twbkfrmt.p_tabledata;
                  END IF;  
               END IF;
            EXCEPTION
               WHEN OTHERS
               THEN
                  twbkfrmt.p_tabledata;
                  twbkfrmt.p_tabledata;
                  twbkfrmt.p_tabledata;
                  twbkfrmt.p_tabledata;
                  twbkfrmt.p_tabledata;
                  twbkfrmt.p_tabledata;
                  twbkfrmt.p_tabledata;
                  IF bwckcoms.F_StudyPathEnabled  = 'Y' THEN
                    twbkfrmt.p_tabledata;
                  END IF;
            END;

            twbkfrmt.p_tablerowclose;
            i := i + 1;
         END LOOP;
      EXCEPTION
         WHEN OTHERS
         THEN
            IF    errs_count > 0
               OR wait_count > 0
               OR i > 2
            THEN
               twbkfrmt.p_tableclose;
               HTP.br;
            END IF;
      END;
      IF NOT multi_term
      THEN
         p_add_drop_crn1;
      END IF;

      IF NOT multi_term
      THEN
        p_add_drop_crn2 (10, regs_count, wait_count);
      ELSE
        p_add_drop_crn2 (10, regs_count, wait_count,'Y');
      END IF; 
      
      HTP.formclose;
      twbkwbis.p_closedoc (curr_release);
   END p_adddrop2;

--------------------------------------------------------------------
--
-- =====================================================================
-- This procedure is called from 'lookup classes to add' search results
-- =====================================================================
   PROCEDURE p_addfromsearch (
      term_in         IN   OWA_UTIL.ident_arr,
      assoc_term_in   IN   OWA_UTIL.ident_arr,
      sel_crn         IN   OWA_UTIL.ident_arr,
      add_btn         IN   OWA_UTIL.ident_arr
   )
   IS
      i                INTEGER                   := 2;
      j                INTEGER                   := 0;
      rsts             OWA_UTIL.ident_arr;
      subj             OWA_UTIL.ident_arr;
      crse             OWA_UTIL.ident_arr;
      sec              OWA_UTIL.ident_arr;
      levl             OWA_UTIL.ident_arr;
      cred             OWA_UTIL.ident_arr;
      gmod             OWA_UTIL.ident_arr;
      title            bwckcoms.varchar2_tabtype;
      mesg             OWA_UTIL.ident_arr;
      btn              OWA_UTIL.ident_arr;
      start_date_out   OWA_UTIL.ident_arr;
      end_date_out     OWA_UTIL.ident_arr;
      proc_called_by   VARCHAR2 (50);
      call_path        VARCHAR2 (1);
      term             stvterm.stvterm_code%TYPE := NULL;
      multi_term       BOOLEAN                   := TRUE;
      assoc_term_out   OWA_UTIL.ident_arr;
      crn_out          OWA_UTIL.ident_arr;
      msg              VARCHAR2 (200);
      wmnu_rec         twgbwmnu%ROWTYPE;
      capp_tech_error  VARCHAR2 (4);
      minh_admin_error VARCHAR2 (1) := 'N';
      lv_newsearch_btn VARCHAr2(30);       
      p_sp_dummy       OWA_UTIL.ident_arr;
      lv_literal       OWA_UTIL.ident_arr;
      
      reg_term         OWA_UTIL.ident_arr;
      
   BEGIN
      -- Validate PL/SQL TABLE post variables
      twbksecr.p_chk_parms_tab_05(term_in, assoc_term_in, sel_crn, add_btn);  /*080701-1*/
      
      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN;
      END IF;
      
-- <<Coast>> -- start changes - MV - 6/16/16      
     twbkwbis.P_SetParam(global_pidm, 'calledFrom', 'lookup_classes');
-- <<Coast>> -- end changes           
      
      lv_newsearch_btn := g$_nls.get ('BWCKCOM1-0012','SQL','New Search');
      p_sp_dummy(1):= 'dummy';
      title(1) := 'dummy';
      term := term_in (term_in.COUNT);

      IF term_in.COUNT = 1
      THEN
         multi_term := FALSE;
      END IF;
      
      --8.5.0.2
      reg_term (1) := twbkwbis.f_getparam (global_pidm, 'TERM');
      IF NOT bwcksams.F_RegsStu (
            global_pidm,
            reg_term(1),
            'bwskfreg.P_AddDropCrse'
         )
      THEN
         RETURN;
      END IF;

      IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
      THEN
         genpidm :=
           TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
         call_path := 'F';
         proc_called_by := 'bwlkfrad.P_FacAddDropCrse';
      ELSE
         genpidm := global_pidm;
         call_path := 'S';
         proc_called_by := 'bwskfreg.P_AddDropCrse';
      END IF;
      lv_literal(1):=  g$_nls.get ('BWCKCOM1-0013',
                               'SQL',
                               'Submit Changes');
                               
      lv_literal(2) := g$_nls.get ('BWCKCOM1-0014',
                                     'SQL',
                                     'Register');                               

      lv_literal(3) := g$_nls.get ('BWCKCOM1-0015', 'SQL', 'Add to WorkSheet');
      lv_literal(4) := g$_nls.get ('BWCKCOM1-0016', 'SQL', 'Back to WorkSheet');
      lv_literal(5) := g$_nls.get ('BWCKCOM1-0017',
                                 'SQL',
                                 'Class Search');
      IF    LTRIM (RTRIM (add_btn (2))) = lv_literal(1)
                            
         OR LTRIM (RTRIM (add_btn (2))) = lv_literal(2)
                                  
      THEN
-- Study Path Changes
     IF bwckcoms.F_StudyPathEnabled  = 'Y' THEN
       
       IF multi_term  THEN  
         IF (twbkwbis.f_getparam (global_pidm, 'G_FROM_MSP') IS NULL) THEN
           bwckcoms.P_DeleteMultiStudyPath(global_pidm);
           bwckcoms.P_SelMultiStudyPath (
                             term_in         =>term_in,
                                                   assoc_term_in   =>assoc_term_in,
                             sel_crn         =>sel_crn,
                             rsts            =>p_sp_dummy,
                             crn             =>p_sp_dummy,
                             start_date_in   =>p_sp_dummy,
                             end_date_in     =>p_sp_dummy,
                             subj            =>p_sp_dummy,
                             crse            =>p_sp_dummy,
                             sec             =>p_sp_dummy,
                             levl            =>p_sp_dummy,
                             cred            =>p_sp_dummy,
                             gmod            =>p_sp_dummy,
                             title           =>title,
                             mesg            =>p_sp_dummy,
                             regs_row        =>NULL,
                             add_row         =>NULL,
                             wait_row        =>NULL,
                             add_btn         =>add_btn,
                             calling_proc_name=>proc_called_by
                                                );
           
           RETURN;
         ELSE
           twbkmods.p_delete_twgrwprm_pidm_name (global_pidm, 'G_FROM_MSP');    
         END IF;
       END IF;        
     END IF; 

-- Study Path Changes



--
-- Check to see if anyone else 'owns' sftregs for this
-- student. If not, remove any remnant sftregs records,
-- and copy all sfrstcr rows into sftregs.
-- ==================================================

         FOR i IN 1 .. term_in.COUNT
         LOOP
            IF NOT bwckregs.f_registration_access (
                  genpidm,
                  term_in (i),
                  call_path || global_pidm
               )
            THEN
               minh_admin_error := sfkfunc.f_get_minh_admin_error(genpidm, term_in(i));
               IF minh_admin_error = 'Y' THEN
                  bwckfrmt.p_open_doc (proc_called_by, term_in (i));
                  twbkwbis.P_DispInfo (proc_called_by, 'SEE_ADMIN');
                  twbkwbis.P_CloseDoc (curr_release);
                  RETURN;
               ELSE      
                  capp_tech_error :=
                     sfkfunc.f_get_capp_tech_error(genpidm, term_in(i));
                  IF capp_tech_error IS NOT NULL
                  THEN
                     OPEN twbklibs.getmenuc (proc_called_by);
                     FETCH twbklibs.getmenuc INTO wmnu_rec;
                     CLOSE twbklibs.getmenuc;
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
               END IF;

               bwckfrmt.p_open_doc (proc_called_by, term_in (i));
               twbkwbis.P_DispInfo (proc_called_by, 'SESSIONBLOCKED');
               twbkwbis.P_CloseDoc (curr_release);
               RETURN;
            END IF;
         END LOOP;

         rsts (1) := 'dummy';
         subj (1) := 'dummy';
         crse (1) := 'dummy';
         sec (1) := 'dummy';
         levl (1) := 'dummy';
         cred (1) := 'dummy';
         gmod (1) := 'dummy';
         mesg (1) := 'dummy';
         title (1) := 'dummy';
         assoc_term_out (1) := 'dummy';
         crn_out (1) := 'dummy';
         start_date_out (1) := 'dummy';
         end_date_out (1) := 'dummy';
         btn (1) := 'dummy';
         btn (2) := g$_nls.get ('BWCKCOM1-0018', 'SQL', 'Submit Changes');

         FOR i IN 2 .. sel_crn.COUNT
         LOOP
            assoc_term_out (i) := f_trim_sel_crn_term (sel_crn (i));
            crn_out (i) := f_trim_sel_crn (sel_crn (i));
            rsts (i) := SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2);
            start_date_out (i) := NULL;
            end_date_out (i) := NULL;
            j := j + 1;
         END LOOP;

         bwckcoms.p_regs (
            term_in,
            rsts,
            assoc_term_out,
            crn_out,
            start_date_out,
            end_date_out,
            subj,
            crse,
            sec,
            levl,
            cred,
            gmod,
            title,
            mesg,
            btn,
            0,
            j,
            0
            );
      ELSIF LTRIM (RTRIM (add_btn (2))) IN
               (
                  lv_literal(3),
                  lv_literal(4)
               )
      THEN
--
-- Check to see if anyone else 'owns' sftregs for this
-- student. If not, remove any remnant sftregs records,
-- and copy all sfrstcr rows into sftregs.
-- ==================================================

         FOR i IN 1 .. term_in.COUNT
         LOOP
            IF NOT bwckregs.f_registration_access (
                  genpidm,
                  term_in (i),
                  call_path || global_pidm
               )
            THEN
               minh_admin_error := sfkfunc.f_get_minh_admin_error(genpidm, term_in(i));
               IF minh_admin_error = 'Y' THEN
                  bwckfrmt.p_open_doc (proc_called_by, term_in (i));
                  twbkwbis.P_DispInfo (proc_called_by, 'SEE_ADMIN');
                  twbkwbis.P_CloseDoc (curr_release);
                  RETURN;
               ELSE      
                  capp_tech_error :=
                     sfkfunc.f_get_capp_tech_error(genpidm, term_in(i));
                  IF capp_tech_error IS NOT NULL
                  THEN
                     OPEN twbklibs.getmenuc (proc_called_by);
                     FETCH twbklibs.getmenuc INTO wmnu_rec;
                     CLOSE twbklibs.getmenuc;
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
               END IF; 

               bwckfrmt.p_open_doc (proc_called_by, term_in (i));
               twbkwbis.P_DispInfo (proc_called_by, 'SESSIONBLOCKED');
               twbkwbis.P_CloseDoc (curr_release);
               RETURN;
            END IF;
         END LOOP;

         --
         bwckcoms.p_adddrop (term_in, assoc_term_in, sel_crn);
      --
-- 8.5.1 PL
     ELSIF LTRIM (RTRIM (add_btn (2))) = lv_literal(5)                             
     OR    LTRIM (RTRIM (add_btn (2))) = lv_newsearch_btn
                             
      THEN
         IF NOT twbkwbis.f_validuser (global_pidm)
         THEN
            RETURN;
         END IF;

         IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') =
                                                                         'FAC'
         THEN
            /* FACWEB */
            bwlkffcs.P_FacCrseSearch(term_in);
         ELSE
            /* STUWEB */
           bwskfcls.P_CrseSearch(term_in);
         END IF;
      END IF;
   /*080701-2 Start*/
     EXCEPTION
      WHEN OTHERS THEN
       IF SQLCODE = gb_event.APP_ERROR THEN
         twbkfrmt.p_storeapimessages(SQLERRM);
       ELSE
         IF SQLCODE  <= -20000 AND SQLCODE >= -20999 THEN
            twbkfrmt.p_printmessage ( SUBSTR (SQLERRM, INSTR (SQLERRM, ':') + 1),'ERROR');
         ELSE
           twbkfrmt.p_printmessage (G$_NLS.Get ('BWCKCOM1-0019','SQL','Error occurred while displaying page.ERROR: %01%',LTRIM(REPLACE( SUBSTR (SQLERRM, INSTR (SQLERRM, ':') + 1),'PL/SQL:'))), 'ERROR');
       END IF;
      END IF;
    /*080701-2 End*/
   END p_addfromsearch;

--------------------------------------------------------------------------

--
-- =====================================================================
-- This procedure is called upon submit from the class search results
-- page - which is called from add/drop.
-- =====================================================================
   PROCEDURE p_addfromsearch1 (
      term_in         IN   OWA_UTIL.ident_arr,
      assoc_term_in   IN   OWA_UTIL.ident_arr,
      sel_crn         IN   OWA_UTIL.ident_arr,
      rsts            IN   OWA_UTIL.ident_arr,
      crn             IN   OWA_UTIL.ident_arr,
      start_date_in   IN   OWA_UTIL.ident_arr,
      end_date_in     IN   OWA_UTIL.ident_arr,
      subj            IN   OWA_UTIL.ident_arr,
      crse            IN   OWA_UTIL.ident_arr,
      sec             IN   OWA_UTIL.ident_arr,
      levl            IN   OWA_UTIL.ident_arr,
      cred            IN   OWA_UTIL.ident_arr,
      gmod            IN   OWA_UTIL.ident_arr,
      title           IN   bwckcoms.varchar2_tabtype,
      mesg            IN   OWA_UTIL.ident_arr,
      regs_row             NUMBER,
      add_row              NUMBER,
      wait_row             NUMBER,
      add_btn         IN   OWA_UTIL.ident_arr
   )
   IS
      i                INTEGER                   := 2;
      j                INTEGER                   := 2;
      add_ctr          NUMBER;
      assoc_term_out   OWA_UTIL.ident_arr;
      crn_out          OWA_UTIL.ident_arr;
      rsts_out         OWA_UTIL.ident_arr;
      start_date_out   OWA_UTIL.ident_arr;
      end_date_out     OWA_UTIL.ident_arr;
      btn              OWA_UTIL.ident_arr;
      term             stvterm.stvterm_code%TYPE := NULL;
      multi_term       BOOLEAN                   := TRUE;
      lv_literal              OWA_UTIL.ident_arr;

   BEGIN
   
      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN;
      END IF;

      term := term_in (term_in.COUNT);

      IF term_in.COUNT = 1
      THEN
         multi_term := FALSE;
      END IF;

      IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
      THEN
         genpidm :=
           TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');

         FOR i IN 1 .. term_in.COUNT
         LOOP
            IF NOT bwckcoms.f_reg_access_still_good (
                  genpidm,
                  term_in (i),
                  'F' || global_pidm,
                  'bwlkfrad.P_FacAddDropCrse'
               )
            THEN
               RETURN;
            END IF;
         END LOOP;
      ELSE
         genpidm := global_pidm;

         FOR i IN 1 .. term_in.COUNT
         LOOP
            IF NOT bwckcoms.f_reg_access_still_good (
                  genpidm,
                  term_in (i),
                  'S' || global_pidm,
                  'bwskfreg.P_AddDropCrse'
               )
            THEN
               RETURN;
            END IF;
         END LOOP;
      END IF;

      lv_literal(1) :=         g$_nls.get ('BWCKCOM1-0020',
                                           'SQL',
                                           'Submit Changes');
      lv_literal(2) :=         g$_nls.get ('BWCKCOM1-0021',
                                           'SQL',
                                           'Register');
      lv_literal(3) :=         g$_nls.get ('BWCKCOM1-0022',
                                          'SQL',
 -- 8.5.1
 --                                         'Class Search');  
                                            'New Search');                                

      IF    LTRIM (RTRIM (add_btn (2))) = lv_literal(1) 
                  
         OR LTRIM (RTRIM (add_btn (2))) =lv_literal(2) 
                                  
      THEN
      
-- Study Path Changes
     IF bwckcoms.F_StudyPathEnabled  = 'Y' THEN
       
       IF multi_term  THEN  
         IF (twbkwbis.f_getparam (global_pidm, 'G_FROM_MSP') IS NULL) THEN
           bwckcoms.P_DeleteMultiStudyPath(global_pidm);
           bwckcoms.P_SelMultiStudyPath (
                             term_in         =>term_in,
                                                   assoc_term_in   =>assoc_term_in,
                             sel_crn         =>sel_crn,
                             rsts            =>rsts,
                             crn             =>crn,
                             start_date_in   =>start_date_in,
                             end_date_in     =>end_date_in ,
                             subj            =>subj,
                             crse            =>crse,
                             sec             =>sec,
                             levl            =>levl,
                             cred            =>cred,
                             gmod            =>gmod,
                             title           =>title,
                             mesg            =>mesg,
                             regs_row        =>regs_row,
                             add_row         =>add_row,
                             wait_row        =>wait_row,
                             add_btn         =>add_btn,
                             calling_proc_name=>'bwckcoms.p_addfromsearch1'
                                                );
           RETURN;
         ELSE
           twbkmods.p_delete_twgrwprm_pidm_name (global_pidm, 'G_FROM_MSP');    
         END IF;
       END IF;        
     END IF; 

-- Study Path Changes
      
         add_ctr := add_row;
         assoc_term_out (1) := 'dummy';
         crn_out (1) := 'dummy';
         rsts_out (1) := 'dummy';
         start_date_out (1) := 'dummy';
         end_date_out (1) := 'dummy';
         btn (1) := 'dummy';
         btn (2) := g$_nls.get ('BWCKCOM1-0023', 'SQL', 'Submit Changes');
         BEGIN
            WHILE crn (j) IS NOT NULL
            LOOP
               assoc_term_out (j) := assoc_term_in (j);
               crn_out (j) := crn (j);
               rsts_out (j) := rsts (j);
               start_date_out (j) := start_date_in (j);
               end_date_out (j) := end_date_in (j);
               j := j + 1;
            END LOOP;
         EXCEPTION
            WHEN OTHERS
            THEN
               NULL;
         END;

         BEGIN
            WHILE sel_crn (i) IS NOT NULL
            LOOP
               assoc_term_out (j) := f_trim_sel_crn_term (sel_crn (i));
               crn_out (j) := f_trim_sel_crn (sel_crn (i));
               start_date_out (j) := NULL;
               end_date_out (j) := NULL;
               rsts_out (j) := SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2);
               i := i + 1;
               j := j + 1;
               add_ctr := add_ctr + 1;
            END LOOP;
         EXCEPTION
            WHEN NO_DATA_FOUND
            THEN
               i := i - 1;
         END;

         bwckcoms.p_regs (
            term_in,
            rsts_out,
            assoc_term_out,
            crn_out,
            start_date_out,
            end_date_out,
            subj,
            crse,
            sec,
            levl,
            cred,
            gmod,
            title,
            mesg,
            btn,
            regs_row,
            add_ctr,
            wait_row
            );
      ELSIF LTRIM (RTRIM (add_btn (2))) IN
               (
                  g$_nls.get ('BWCKCOM1-0024', 'SQL', 'Add to WorkSheet'),
                  g$_nls.get ('BWCKCOM1-0025', 'SQL', 'Back to WorkSheet')
               )
      THEN
         bwckcoms.p_adddrop1 (
            term_in,
            sel_crn,
            assoc_term_in,
            crn,
            start_date_in,
            end_date_in,
            rsts,
            subj,
            crse,
            sec,
            levl,
            cred,
            gmod,
            title,
            mesg,
            regs_row,
            add_row,
            wait_row
         );
      ELSIF LTRIM (RTRIM (add_btn (2))) = lv_literal(3)                              
      THEN
         bwckgens.p_disp_term_date_advanced('P_CrseSearch');
/*
         bwckgens.P_RegsCrseSearch (
            term_in,
            rsts,
            assoc_term_in,
            crn,
            start_date_in,
            end_date_in,
            subj,
            crse,
            sec,
            levl,
            cred,
            gmod,
            title,
            mesg,
            regs_row,
            add_row,
            wait_row
         ); */
      END IF;
   END p_addfromsearch1;

--------------------------------------------------------------------------
--
-- Get the latest student and registration records.
-- =====================================================================
   PROCEDURE p_regs_etrm_chk (
      pidm_in                      NUMBER,
      term_in                      stvterm.stvterm_code%TYPE,
      clas_code           IN OUT   SGRCLSR.SGRCLSR_CLAS_CODE%TYPE,
      multi_term_in                BOOLEAN DEFAULT FALSE,
      create_sfbetrm_in            BOOLEAN DEFAULT TRUE
   )
   IS
   BEGIN
--
-- Get the latest student record. Do some validation and
-- retrieve more info based on the student record.
-- ===================================================
      FOR sgbstdn IN sgklibs.sgbstdnc (pidm_in, term_in)
      LOOP
         sgbstdn_rec := sgbstdn;
      END LOOP;
--
-- Get student's curriculum records
--
      lcur_tab := sokccur.f_current_active_curriculum(
                             p_pidm => pidm_in,
                             p_lmod_code => sb_curriculum_str.f_learner,
                             p_eff_term  => term_in);

      bwckregs.p_regschk (sgbstdn_rec, multi_term_in);
--
-- Get the class code from another package (bwckregs).
-- ===================================================
      clas_code := bwckregs.f_getstuclas;
--
-- Get the latest registration record. Do some validation
-- and retrieve more info based on the registration record.
-- ===================================================
      row_count := 0;

      FOR sfbetrm IN sfkcurs.sfbetrmc (pidm_in, term_in)
      LOOP
         sfbetrm_rec := sfbetrm;
         row_count := sfkcurs.sfbetrmc%rowcount;
      END LOOP;

      IF     row_count = 0
         AND create_sfbetrm_in
      THEN
         /* in a multi term scenario, when no sfbetrm_rec is found, */
         /* the record will be for a different term to the one we */
         /* expect, so reset it */
         sfbetrm_rec.sfbetrm_term_code := term_in;
         bwckregs.p_regschk (sfbetrm_rec, 'Y', multi_term_in);
      ELSE
         bwckregs.p_regschk (sfbetrm_rec, NULL, multi_term_in);
      END IF;
   END p_regs_etrm_chk;

--------------------------------------------------------------------------

--
-- P_REGS
-- This procedure processes registration changes (add/drop/change status).
-- =====================================================================
   PROCEDURE p_regs (
      term_in         IN   OWA_UTIL.ident_arr,
      rsts_in         IN   OWA_UTIL.ident_arr,
      assoc_term_in   IN   OWA_UTIL.ident_arr,
      crn_in          IN   OWA_UTIL.ident_arr,
      start_date_in   IN   OWA_UTIL.ident_arr,
      end_date_in     IN   OWA_UTIL.ident_arr,
      subj            IN   OWA_UTIL.ident_arr,
      crse            IN   OWA_UTIL.ident_arr,
      sec             IN   OWA_UTIL.ident_arr,
      levl            IN   OWA_UTIL.ident_arr,
      cred            IN   OWA_UTIL.ident_arr,
      gmod            IN   OWA_UTIL.ident_arr,
      title           IN   bwckcoms.varchar2_tabtype,
      mesg            IN   OWA_UTIL.ident_arr,
      reg_btn         IN   OWA_UTIL.ident_arr,
      regs_row             NUMBER,
      add_row              NUMBER,
      wait_row             NUMBER
   )
   IS
      i                     INTEGER                        := 2;
      k                     INTEGER                        := 1;
      err_term              OWA_UTIL.ident_arr;
      err_crn               OWA_UTIL.ident_arr;
      err_subj              OWA_UTIL.ident_arr;
      err_crse              OWA_UTIL.ident_arr;
      err_sec               OWA_UTIL.ident_arr;
      err_code              OWA_UTIL.ident_arr;
      err_levl              OWA_UTIL.ident_arr;
      err_cred              OWA_UTIL.ident_arr;
      err_gmod              OWA_UTIL.ident_arr;
      clas_code             SGRCLSR.SGRCLSR_CLAS_CODE%TYPE;
      stvterm_rec           stvterm%ROWTYPE;
      sorrtrm_rec           sorrtrm%ROWTYPE;
      stufac_ind            VARCHAR2 (1);
      term                  stvterm.stvterm_code%TYPE      := NULL;
      multi_term            BOOLEAN                        := TRUE;
      olr_course_selected   BOOLEAN                        := FALSE;
      capp_tech_error       VARCHAR2 (4);
      minh_admin_error      VARCHAR2 (1); 
      assoc_term            OWA_UTIL.ident_arr             := assoc_term_in;
      crn                   OWA_UTIL.ident_arr;
      crn_index             NUMBER;
      crn_in_index          NUMBER;
      start_date            OWA_UTIL.ident_arr             := start_date_in;
      end_date              OWA_UTIL.ident_arr             := end_date_in;
      etrm_done             BOOLEAN                        := FALSE;
      drop_problems         sfkcurs.drop_problems_rec_tabtype;
      drop_failures         sfkcurs.drop_problems_rec_tabtype;
      local_capp_tech_error VARCHAR2 (30);
      called_by_proc_name   VARCHAR2 (100);
      web_rsts_checkc_flag  VARCHAR2(1);
      lv_literal            VARCHAR2(30);
      sfrrscm_rec           sfrrscm%rowtype;
      prev_rsts_code       sftregs.sftregs_rsts_code%TYPE;  -- C3SC 8.10 GRS ADDED  CR-000119477  
      
      cursor rscm_c (p_pidm sfrrscm.sfrrscm_pidm%type,
                     p_term sfrrscm.sfrrscm_term_code%type)
      is
      select * from sfrrscm
      where sfrrscm_pidm = p_pidm
      and sfrrscm_term_code = p_term;
      
      /*
      CURSOR web_rsts_checkc(rsts_in VARCHAR2)
         IS
         SELECT 'Y'
           FROM stvrsts
          WHERE stvrsts_code = rsts_in
            AND stvrsts_web_ind = 'Y';
      */ -- Moved the cursor in bwcklibs.is_crs_reg_status_valid().

   BEGIN

      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN;
      END IF;
      
      term := term_in (term_in.COUNT);

      IF term_in.COUNT = 1
      THEN
         multi_term := FALSE;
      END IF;

      --
      -- Remove any null leading crns from the to-add list.
      -- ==================================================
      crn_index := 1;

      FOR crn_in_index IN 1..crn_in.COUNT
      LOOP
         IF crn_in(crn_in_index) IS NOT NULL
         THEN
            crn(crn_index) := crn_in(crn_in_index);
            crn_index := crn_index + 1;
         END IF;
       END LOOP;
      --

      IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
      THEN
         genpidm :=
           TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
         stufac_ind := 'F';
         called_by_proc_name := 'bwlkfrad.P_FacAddDropCrse';

         FOR i IN 1 .. term_in.COUNT
         LOOP
            IF NOT bwckcoms.f_reg_access_still_good (
                  genpidm,
                  term_in (i),
                  stufac_ind || global_pidm,
                  called_by_proc_name
               )
            THEN
               RETURN;
            END IF;
         END LOOP;
      ELSE
         genpidm := global_pidm;
         stufac_ind := 'S';
         called_by_proc_name := 'bwskfreg.P_AddDropCrse';

         FOR i IN 1 .. term_in.COUNT
         LOOP
            IF NOT bwckcoms.f_reg_access_still_good (
                  genpidm,
                  term_in (i),
                  stufac_ind || global_pidm,
                  called_by_proc_name
               )
            THEN
               RETURN;
            END IF;
         END LOOP;
      END IF;

      /*
      FOR i IN 2 .. rsts_in.COUNT
         LOOP
            IF rsts_in(i) IS NOT NULL
            THEN
               OPEN web_rsts_checkc(rsts_in(i));
               FETCH web_rsts_checkc INTO web_rsts_checkc_flag;

               IF web_rsts_checkc%NOTFOUND
               THEN
                  CLOSE web_rsts_checkc;
                  twbkwbis.p_dispinfo (called_by_proc_name, 'BADRSTS');
                  RETURN;
               END IF;

               CLOSE web_rsts_checkc;
            END IF;
         END LOOP;
      */ -- Moved the logic in function bwcklibs.is_crs_reg_status_valid().

      IF NOT multi_term
      THEN
         -- This added for security reasons, in order to prevent students
         -- saving the add/droppage while registration is open, and
         -- re-using the saved page after it has closed
         IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
         THEN
            twbkwbis.p_dispinfo ('bwskflib.P_SelDefTerm', 'BADTERM');
            RETURN;
         END IF;
      END IF;

--
-- If the Select Study Path button was pressed...
-- ===================================================
      lv_literal := g$_nls.get ('BWCKCOM1-0026',
                       'SQL',
                       'Select Study Path');
      IF LTRIM (RTRIM (reg_btn (2))) = lv_literal
      THEN
        bwckcoms.P_DeleteStudyPath ;
        IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC' THEN     
            bwckcoms.P_SelStudyPath(NULL,
                             'bwlkfrad.P_FacAddDropCrseCRN',
                            NULL,
                            NULL,
                            term_in,
                            assoc_term_in,
                            crn,
                            rsts_in,
                            crn_in,
                            start_date_in,
                            end_date_in,
                            subj,
                            crse,
                            sec,
                            levl,
                            cred,
                            gmod,
                            title,
                            mesg,
                            regs_row,
                            add_row,
                            wait_row);

        ELSE
           bwckcoms.P_SelStudyPath(NULL,
                            'bwskfreg.p_adddropcrseCRN',
                            NULL,
                            NULL,
                            term_in,
                            assoc_term_in,
                            crn,
                            rsts_in,
                            crn_in,
                            start_date_in,
                            end_date_in,
                            subj,
                            crse,
                            sec,
                            levl,
                            cred,
                            gmod,
                            title,
                            mesg,
                            regs_row,
                            add_row,
                            wait_row);

           
        END IF;
        RETURN;        
      END IF;

-- If the class search button was pressed...
-- ===================================================
      lv_literal := g$_nls.get ('BWCKCOM1-0027',
                       'SQL',
                       'Class Search');
      IF LTRIM (RTRIM (reg_btn (2))) = lv_literal
      THEN

         bwckgens.P_RegsCrseSearch (
            term_in,
            rsts_in,
            assoc_term_in,
            crn,
            start_date_in,
            end_date_in,
            subj,
            crse,
            sec,
            levl,
            cred,
            gmod,
            title,
            mesg,
            regs_row,
            add_row,
            wait_row
         );
         RETURN;
      END IF;

--
-- If the Register, or Submit button was pressed.
-- ===================================================
--
-- Setup globals.
-- ===================================================
      bwcklibs.p_initvalue (global_pidm, term, '', SYSDATE, '', '');

--
-- Check if student is allowed to register - single term
-- only, as multi term is handled in the search results
-- page.
-- ===================================================
      IF NOT multi_term
      THEN
         IF NOT bwcksams.f_regsstu (
                  genpidm,
                  term,
                  called_by_proc_name
               )
         THEN
            RETURN;
         END IF;
      END IF;

--
-- Check whether OLR courses have been chosen, and if so,
-- confirm their start/end dates
-- ======================================================

      FOR i IN 2 .. crn.COUNT
      LOOP
         IF crn (i) IS NULL
         THEN
            EXIT;
         END IF;

         IF     NOT multi_term
            AND (   assoc_term (i) IS NULL
                 OR assoc_term (i) = 'dummy')
         THEN
            assoc_term (i) := term;
         END IF;

         IF NOT start_date.EXISTS (i)
         THEN
            start_date (i) := NULL;
            end_date (i) := NULL;
         END IF;

         IF NOT olr_course_selected
         THEN
            IF sfkolrl.f_open_learning_course (assoc_term (i), crn (i))
            THEN
               IF start_date (i) IS NULL
               THEN
                  olr_course_selected := TRUE;
               END IF;
            END IF;
         END IF;
      END LOOP;

      IF olr_course_selected
      THEN
         bwckcoms.p_disp_start_date_confirm (
            term_in,
            rsts_in,
            assoc_term,
            crn,
            start_date,
            end_date,
            subj,
            crse,
            sec,
            levl,
            cred,
            gmod,
            title,
            mesg,
            reg_btn,
            regs_row,
            add_row,
            wait_row,
            NULL,
            NULL
         );
         RETURN;
      END IF;

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
--
-- Check for admin errors that may have been introduced
-- during this add/drop session.
-- ====================================================
      FOR i IN 1 .. term_in.COUNT
      LOOP

        /* Reset any linked courses that should be dropped. */
        /* This is so that they will be re-flagged as requiring drop confirmation. */
        /* This handles the scenario where a user presses the back button from */
        /* the drop confirmation page. */
         UPDATE sftregs a
         SET a.sftregs_error_flag = a.sftregs_hold_error_flag,
             a.sftregs_rmsg_cde = a.sftregs_hold_rmsg_cde,
             a.sftregs_message = a.sftregs_hold_message,
             a.sftregs_rsts_code = a.sftregs_hold_rsts_code,
             a.sftregs_rsts_date = a.sftregs_hold_rsts_date,
             a.sftregs_vr_status_type = a.sftregs_hold_rsts_type,
             a.sftregs_grde_code = a.sftregs_hold_grde_code,
             a.sftregs_user = goksels.f_get_ssb_id_context,
             a.sftregs_activity_date = SYSDATE
         WHERE a.sftregs_term_code = term_in (i)
         AND a.sftregs_pidm = genpidm
         AND a.sftregs_error_link IS NOT NULL
         AND a.sftregs_error_link = ( SELECT b.sftregs_error_link
                                    FROM sftregs b
                                    WHERE b.sftregs_term_code = a.sftregs_term_code
                                    AND b.sftregs_pidm = a.sftregs_pidm
                                    AND b.sftregs_crn <> a.sftregs_crn
                                    AND b.sftregs_error_flag = 'F'
                                    AND ROWNUM = 1);

         UPDATE sftregs
         SET sftregs_error_flag = sftregs_hold_error_flag,
             sftregs_rmsg_cde = sftregs_hold_rmsg_cde,
             sftregs_message = sftregs_hold_message,
             sftregs_rsts_code = sftregs_hold_rsts_code,
             sftregs_rsts_date = sftregs_hold_rsts_date,
             sftregs_vr_status_type = sftregs_hold_rsts_type,
             sftregs_grde_code = sftregs_hold_grde_code,
             sftregs_user = goksels.f_get_ssb_id_context,
             sftregs_activity_date = SYSDATE
         WHERE sftregs_term_code = term_in (i)
         AND sftregs_pidm = genpidm
         AND sftregs_error_flag = 'F';

-- C3SC GRS 04/09/2008 Modify
      -- Defect 1-3JBTM3  Move delete from sftregs to fix back button issue
      -- because it was causing dissapearing issues when a course was already registered.
        delete from sftregs 
        where sftregs_pidm = genpidm
       and sftregs_term_code = term_in (i)
       and sftregs_error_flag = 'F'
       and sftregs_message like G$_NLS.FormatMsg ('x','SQL','%AUTH REQD%');
     -- C3SC END

         sfkmods.p_admin_msgs (
               genpidm,
               term_in (i),
               stufac_ind || global_pidm
            );

         minh_admin_error := sfkfunc.f_get_minh_admin_error(genpidm, term_in(i));  
         IF minh_admin_error = 'Y' THEN
            bwckfrmt.p_open_doc (called_by_proc_name, term, NULL,
                                 multi_term, term_in(1));
            twbkwbis.p_dispinfo (called_by_proc_name, 'SEE_ADMIN');
            twbkwbis.p_closedoc (curr_release);
            RETURN;
         END IF;

         local_capp_tech_error :=
           sfkfunc.f_get_capp_tech_error (genpidm, term_in(i));
         IF local_capp_tech_error IS NOT NULL
         THEN
            bwckcoms.p_adddrop2 ( term_in,
                                  err_term,
                                  err_crn,
                                  err_subj,
                                  err_crse,
                                  err_sec,
                                  err_code,
                                  err_levl,
                                  err_cred,
                                  err_gmod,
                                  local_capp_tech_error,
                                  NULL,
                                  drop_problems,
                                  drop_failures );
            RETURN;
         END IF;

         IF NOT bwckregs.f_finalize_admindrops (genpidm, term_in(i),
                                                stufac_ind || global_pidm)
         THEN
             bwckfrmt.p_open_doc (called_by_proc_name, term, NULL,
                                  multi_term, term_in(1));
             twbkwbis.p_dispinfo (called_by_proc_name, 'SESSIONBLOCKED');
             twbkwbis.p_closedoc (curr_release);
             RETURN;
         END IF;

      END LOOP;
      
      -- Start of Perform some validations
      called_by_proc_name := 'bwckcoms.P_Regs';
      i := 2;
      WHILE i <= regs_row + 1
      LOOP
            IF LTRIM (RTRIM (rsts_in (i))) IS NOT NULL
            THEN
                IF NOT bwcklibs.f_is_crs_reg_status_valid( rsts_in (i), assoc_term (i), crn (i) ) THEN
                  twbkwbis.p_dispinfo (called_by_proc_name, 'CRSREGSTATUSNOTVALID');
                  RETURN;                
                -- CALBSTU 8.9.1.1 GRS 06/18/2014 BEGIN
                ELSE
                    -- This make sure the rsts for drops is the calculated drop code 
                    IF rsts_in (i) = SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2)  THEN -- C3SC 8.10 ADDED GRS 02/16/2015
                    -- Get previous rsts code , if waitlisted then it does not need to check for calculated drops
                      -- CALBSTU 8.10  GRS CR-000119477  BEGIN
                      SELECT SFTREGS_RSTS_CODE INTO prev_rsts_code
                        FROM SFTREGS
                       WHERE SFTREGS_PIDM = genpidm
                         AND SFTREGS_TERM_CODE = assoc_term (i)
                         AND SFTREGS_CRN = crn (i);

                      IF SV_ACAD_CALENDAR_BP.f_is_waitlisted(p_rsts=>prev_rsts_code) = 'Y' 
                      THEN
                        NULL;
                      ELSE
                        -- CALBSTU 8.10  GRS CR-000119477  ENDS                      
                        IF sv_acad_calendar_bp.F_Calc_Drop_is_Active(p_term_code=>term) = 'Y' THEN
                          IF NOT sv_acad_calendar_bp.f_is_crs_calc_drop_valid(p_pidm_in=>genpidm,
                                                                              p_rsts_code_in=>rsts_in(i),
                                                                              p_term_code_in=>assoc_term (i),
                                                                              p_crn_code_in=>crn (i) ) THEN
                              -- Create new error message for calbstu
                            twbkwbis.p_dispinfo (called_by_proc_name, 'CRSREGSTATUSNOTVALID');
                            RETURN;
                END IF; 
                        END IF;
                      END IF;
                    END IF;
                 END IF;                    
                -- CALBSTU 8.9.1.1 GRS 06/18/2014 END
            END IF;  
            i := i + 1;    
      END LOOP;
--    -- End of Perform some validations
-- Loop through the registration records on the page.
-- ===================================================
      i := 2;
      WHILE i <= regs_row + 1
      LOOP
         BEGIN
            IF LTRIM (RTRIM (rsts_in (i))) IS NOT NULL
            THEN
               sftregs_rec.sftregs_crn := crn (i);
               sftregs_rec.sftregs_pidm := genpidm;
               sftregs_rec.sftregs_term_code := assoc_term (i);
               sftregs_rec.sftregs_rsts_code := rsts_in (i);
               BEGIN
                  IF multi_term
                  THEN
--
-- Get the latest student and registration records.
-- ===================================================
                     bwckcoms.p_regs_etrm_chk (
                        genpidm,
                        assoc_term (i),
                        clas_code,
                        multi_term
                     );
                  ELSIF NOT etrm_done
                  THEN
                     bwckcoms.p_regs_etrm_chk (genpidm, term, clas_code);
                     etrm_done := TRUE;
                  END IF;

--
-- Get the section information.
-- ===================================================
                  bwckregs.p_getsection (
                     sftregs_rec.sftregs_term_code,
                     sftregs_rec.sftregs_crn,
                     sftregs_rec.sftregs_sect_subj_code,
                     sftregs_rec.sftregs_sect_crse_numb,
                     sftregs_rec.sftregs_sect_seq_numb
                  );

--
-- If the action corresponds to gtvsdax web drop code,
-- then call the procedure to drop a course.
-- ===================================================
                  IF rsts_in (i) = SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2)
                  THEN
                     bwckregs.p_dropcrse (
                        sftregs_rec.sftregs_term_code,
                        sftregs_rec.sftregs_crn,
                        sftregs_rec.sftregs_rsts_code,
                        sftregs_rec.sftregs_reserved_key,
                        rec_stat   => 'D',
                        del_ind    => 'Y'
                     );
--
-- If the second character of the action is not D, then
-- call the procedure to update a course.
-- ===================================================
                  ELSE
                     bwckregs.p_updcrse (sftregs_rec);
                  END IF;

--
-- Create a batch fee assessment record.
-- ===================================================
                  sfrbtch_row.sfrbtch_term_code := assoc_term (i);
                  sfrbtch_row.sfrbtch_pidm := genpidm;
                  sfrbtch_row.sfrbtch_clas_code := clas_code;
                  sfrbtch_row.sfrbtch_activity_date := SYSDATE;
                  bwcklibs.p_add_sfrbtch (sfrbtch_row);
                  tbrcbrq_row.tbrcbrq_term_code := assoc_term (i);
                  tbrcbrq_row.tbrcbrq_pidm := genpidm;
                  tbrcbrq_row.tbrcbrq_activity_date := SYSDATE;
                  bwcklibs.p_add_tbrcbrq (tbrcbrq_row);
               EXCEPTION
                  WHEN OTHERS
                  THEN
                     IF SQLCODE = gb_event.APP_ERROR THEN
                        twbkfrmt.p_storeapimessages(SQLERRM);
                        RAISE;
                     END IF;
                     k := k + 1;
                     err_term (k) := assoc_term (i);
                     err_crn (k)  := crn (i);
                     err_subj (k) := sftregs_rec.sftregs_sect_subj_code;
                     err_crse (k) := sftregs_rec.sftregs_sect_crse_numb;
                     err_sec (k)  := sftregs_rec.sftregs_sect_seq_numb;
                     err_code (k) := SQLCODE;
                     err_levl (k) := lcur_tab(1).r_levl_code;
                     err_cred (k) := TO_CHAR (sftregs_rec.sftregs_credit_hr);
                     err_gmod (k) := sftregs_rec.sftregs_gmod_code;
                  END;
            END IF;

            i := i + 1;
         EXCEPTION
            WHEN OTHERS
            THEN
               IF SQLCODE = gb_event.APP_ERROR THEN
                  twbkfrmt.p_storeapimessages(SQLERRM);
                  RAISE;
               END IF;
               i := i + 1;
         END;
      END LOOP;

--
-- Loop through the add table on the page.
-- ===================================================

      WHILE i <= regs_row + wait_row + add_row + 1
      LOOP
         DECLARE
         BEGIN
            IF     crn (i) IS NOT NULL
               AND LTRIM (RTRIM (rsts_in (i))) IS NOT NULL
            THEN
               sftregs_rec.sftregs_crn := crn (i);
               sftregs_rec.sftregs_rsts_code := rsts_in (i);
               sftregs_rec.sftregs_rsts_date := SYSDATE;
               sftregs_rec.sftregs_pidm := genpidm;
               sftregs_rec.sftregs_term_code := assoc_term (i);

               BEGIN
                  IF multi_term
                  THEN
--
-- Get the latest student and registration records.
-- ===================================================
                     bwckcoms.p_regs_etrm_chk (
                        genpidm,
                        assoc_term (i),
                        clas_code,
                        multi_term
                     );
                  ELSIF NOT etrm_done
                  THEN
                     bwckcoms.p_regs_etrm_chk (genpidm, term, clas_code);
                     etrm_done := TRUE;
                  END IF;
-- Do check for study path status also. Create sfrensp if it doesnt exist for the studypath
        IF bwckcoms.F_StudyPathEnabled = 'Y' THEN
          IF NOT multi_term THEN
          
            bwckcoms.p_regs_ensp_chk(
                        genpidm,
                        SUBSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),1, INSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),'|')-1),
                        twbkwbis.f_getparam (global_pidm, 'TERM')
                                   );
          ELSE
            IF twbkwbis.f_getparam (global_pidm, 'G_SP'||assoc_term (i)) <> 'NULL' THEN
              bwckcoms.p_regs_ensp_chk(
                          genpidm,
                          twbkwbis.f_getparam (global_pidm, 'G_SP'||assoc_term (i)),
                          assoc_term (i)
                                     );
            END IF;                       
          END IF;
          
        END IF;
     
--
--
-- Get the section information.
-- ===================================================
                  bwckregs.p_getsection (
                     sftregs_rec.sftregs_term_code,
                     sftregs_rec.sftregs_crn,
                     sftregs_rec.sftregs_sect_subj_code,
                     sftregs_rec.sftregs_sect_crse_numb,
                     sftregs_rec.sftregs_sect_seq_numb
                  );
--
-- Get the studyPath information
--====================================================
        IF bwckcoms.F_StudyPathEnabled = 'Y' THEN
          IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC' THEN
             genpidm := TO_NUMBER(twbkwbis.F_GetParam (global_pidm, 'STUPIDM'), 99999999);
          ELSE 
             genpidm := global_pidm;
          END IF;
          IF NOT multi_term THEN
            sftregs_rec.sftregs_stsp_key_sequence :=
                           SUBSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),1, INSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),'|')-1);
          ELSE
            IF twbkwbis.f_getparam (global_pidm, 'G_SP'||assoc_term (i)) <> 'NULL' THEN  
              sftregs_rec.sftregs_stsp_key_sequence :=twbkwbis.f_getparam (global_pidm, 'G_SP'||assoc_term (i));
            ELSE
              sftregs_rec.sftregs_stsp_key_sequence :=NULL;
            END IF;
          END IF;
        END IF;

-- Call the procedure to add a course.
-- ===================================================
                  bwckregs.p_addcrse (
                     sftregs_rec,
                     sftregs_rec.sftregs_sect_subj_code,
                     sftregs_rec.sftregs_sect_crse_numb,
                     sftregs_rec.sftregs_sect_seq_numb,
                     start_date (i),
                     end_date (i)
                  );
--
-- Create a batch fee assessment record.
-- ===================================================
                  sfrbtch_row.sfrbtch_term_code := assoc_term (i);
                  sfrbtch_row.sfrbtch_pidm := genpidm;
                  sfrbtch_row.sfrbtch_clas_code := clas_code;
                  sfrbtch_row.sfrbtch_activity_date := SYSDATE;
                  bwcklibs.p_add_sfrbtch (sfrbtch_row);
                  tbrcbrq_row.tbrcbrq_term_code := assoc_term (i);
                  tbrcbrq_row.tbrcbrq_pidm := genpidm;
                  tbrcbrq_row.tbrcbrq_activity_date := SYSDATE;
                  bwcklibs.p_add_tbrcbrq (tbrcbrq_row);
               EXCEPTION
                  WHEN OTHERS
                  THEN
                     IF SQLCODE = gb_event.APP_ERROR THEN
                        twbkfrmt.p_storeapimessages(SQLERRM);
                        RAISE;
                     END IF;
                     k := k + 1;
                     err_term (k) := assoc_term (i);
                     err_crn (k)  := crn (i);
                     err_subj (k) := sftregs_rec.sftregs_sect_subj_code;
                     err_crse (k) := sftregs_rec.sftregs_sect_crse_numb;
                     err_sec (k)  := sftregs_rec.sftregs_sect_seq_numb;
                     err_code (k) := SQLCODE;
                     err_levl (k) := lcur_tab(1).r_levl_code;
                     err_cred (k) := TO_CHAR (sftregs_rec.sftregs_credit_hr);
                     err_gmod (k) := sftregs_rec.sftregs_gmod_code;
               END;
            ELSE
               BEGIN
--
-- ?????
-- ===================================================
                  IF     subj (i) IS NOT NULL
                     AND crse (i) IS NOT NULL
                     AND sec (i) IS NOT NULL
                     AND LTRIM (RTRIM (rsts_in (i))) IS NOT NULL
                  THEN
                     BEGIN
                        sftregs_rec.sftregs_rsts_code := rsts_in (i);
                        sftregs_rec.sftregs_rsts_date := SYSDATE;
                        sftregs_rec.sftregs_pidm := genpidm;
                        sftregs_rec.sftregs_term_code := assoc_term (i);

                        IF multi_term
                        THEN
--
-- Get the latest student and registration records.
-- ===================================================
                           bwckcoms.p_regs_etrm_chk (
                              genpidm,
                              assoc_term (i),
                              clas_code,
                              multi_term
                           );
                        ELSIF NOT etrm_done
                        THEN
                           bwckcoms.p_regs_etrm_chk (genpidm, term, clas_code);
                           etrm_done := TRUE;
                        END IF;

                        bwckregs.p_addcrse (
                           sftregs_rec,
                           subj (i),
                           crse (i),
                           sec (i),
                           start_date (i),
                           end_date (i)
                        );
--                        COMMIT;
--                        commit_flag := 'Y';
                     EXCEPTION
                        WHEN OTHERS
                        THEN
                           IF SQLCODE = gb_event.APP_ERROR THEN
                              twbkfrmt.p_storeapimessages(SQLERRM);
                              RAISE;
                           END IF;
                           k := k + 1;
                           err_term (k) := assoc_term (i);
                           err_crn (k) := crn (i);
                           err_subj (k) := subj (i);
                           err_crse (k) := crse (i);
                           err_sec (k) := sec (i);
                           err_code (k) := SQLCODE;
                           err_levl (k) := lcur_tab(1).r_levl_code;
                           err_cred (k) :=
                                      TO_CHAR (sftregs_rec.sftregs_credit_hr);
                           err_gmod (k) := sftregs_rec.sftregs_gmod_code;
--                           ROLLBACK;
--                           commit_flag := 'N';
                     END;
                  END IF;
               EXCEPTION
                  WHEN OTHERS
                  THEN
                     IF SQLCODE = gb_event.APP_ERROR THEN
                        twbkfrmt.p_storeapimessages(SQLERRM);
                        RAISE;
                     END IF;
                     NULL;
               END;
            END IF;

            i := i + 1;
         EXCEPTION
            WHEN NO_DATA_FOUND
            THEN
               BEGIN
                  IF     subj (i) IS NOT NULL
                     AND crse (i) IS NOT NULL
                     AND sec (i) IS NOT NULL
                  THEN
                     BEGIN
                        sftregs_rec.sftregs_rsts_code := rsts_in (i);
                        sftregs_rec.sftregs_rsts_date := SYSDATE;
                        sftregs_rec.sftregs_pidm := genpidm;
                        sftregs_rec.sftregs_term_code := assoc_term (i);

                        IF multi_term
                        THEN
--
-- Get the latest student and registration records.
-- ===================================================
                           bwckcoms.p_regs_etrm_chk (
                              genpidm,
                              assoc_term (i),
                              clas_code,
                              multi_term
                           );
                        ELSIF NOT etrm_done
                        THEN
                           bwckcoms.p_regs_etrm_chk (genpidm, term, clas_code);
                           etrm_done := TRUE;
                        END IF;

                        bwckregs.p_addcrse (
                           sftregs_rec,
                           subj (i),
                           crse (i),
                           sec (i),
                           start_date (i),
                           end_date (i)
                        );
--                        COMMIT;
--                        commit_flag := 'Y';
                     EXCEPTION
                        WHEN OTHERS
                        THEN
                           IF SQLCODE = gb_event.APP_ERROR THEN
                              twbkfrmt.p_storeapimessages(SQLERRM);
                              RAISE;
                           END IF;
                           k := k + 1;
                           err_term (k) := assoc_term (i);
                           err_crn (k) := crn (i);
                           err_subj (k) := subj (i);
                           err_crse (k) := crse (i);
                           err_sec (k) := sec (i);
                           err_code (k) := SQLCODE;
                           err_levl (k) := lcur_tab(1).r_levl_code;
                           err_cred (k) :=
                                      TO_CHAR (sftregs_rec.sftregs_credit_hr);
                           err_gmod (k) := sftregs_rec.sftregs_gmod_code;
--                           ROLLBACK;
--                           commit_flag := 'N';
                     END;
                  END IF;
               EXCEPTION
                  WHEN OTHERS
                  THEN
                     IF SQLCODE = gb_event.APP_ERROR THEN
                        twbkfrmt.p_storeapimessages(SQLERRM);
                        RAISE;
                     END IF;
                     NULL;
               END;

               i := i + 1;
         END;
      END LOOP;

--
-- Do batch validation on all registration records.
-- ===================================================
-- 8.7
-- delete any rows in sfrrscm that may be left from a previous submission for this pidm

     delete from sfrrscm
     where sfrrscm_pidm = genpidm
     and sfrrscm_term_code = term;
     
   bwckcoms.p_group_edits( term_in, genpidm, etrm_done,
                           capp_tech_error,
                           drop_problems, drop_failures);

   If capp_tech_error is null
   then
          --C3SC  grs ADD
     IF sv_auth_addcodes_bp.f_is_auth_enabled(term_in(1))
     THEN    
       sv_auth_addcodes_bp.p_local_problems( term_in, 
                            err_term, err_crn, err_subj, 
                            err_crse, err_sec, err_code, 
                            err_levl, err_cred, err_gmod,
                            drop_problems, drop_failures,add_row,
                            'bwckcoms.p_regs',
                            crn ,
                            rsts_in);
          return;
      ELSE    
      bwckcoms.p_problems ( term_in,
                            err_term, err_crn, err_subj,
                            err_crse, err_sec, err_code,
                            err_levl, err_cred, err_gmod,
                            drop_problems, drop_failures);
       return;
   end if;
      --END C3SC
   end if;

--
-- Redisplay the add/drop page (after a capp error)
-- ===================================================
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
      NULL,
      drop_problems,
      drop_failures
   );

EXCEPTION


      WHEN OTHERS                                                             
      THEN        
        /*
        *  8.7.1.1 perform rollback if there is an error so student cannot come back in to registration 
        *  with partial updates 
        */
        rollback; 

         IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') =    
                                                                         'FAC'
         THEN                                                                 
/*                                                                            
 * FACWEB-specific code                                                       
 */                                                                           
            IF NOT bwlkilib.f_add_drp (term)                                  
            THEN                                                              
               NULL;                                                          
            END IF;                                                           
                                                                              
            bwckfrmt.p_open_doc (                                             
               called_by_proc_name,                                           
               term,                                                          
               NULL,                                                          
               multi_term,                                                    
               term_in (1)                                                    
            );                                                                
         ELSE                                                                 
/*                                                                            
 * STUWEB-specific code                                                       
 */                                                                           
            IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)      
            THEN                                                              
               NULL;                                                          
            END IF;                                                           
                                                                              
            bwckfrmt.p_open_doc (                                             
               called_by_proc_name,                                           
               term,                                                          
               NULL,                                                          
               multi_term,                                                    
               term_in (1)                                                    
            );                                                                
         END IF;                      /* END stu/fac specific code sections */
                                                                              

         IF SQLCODE = gb_event.APP_ERROR THEN
            twbkfrmt.p_storeapimessages(SQLERRM);
         ELSE
            IF SQLCODE  <= -20000 AND SQLCODE >= -20999 THEN
               twbkfrmt.p_printmessage (
                SUBSTR (SQLERRM, INSTR (SQLERRM, ':') + 1),
                'ERROR'
               );
            ELSE
              twbkfrmt.p_printmessage (
                 G$_NLS.Get ('BWCKCOM1-0028','SQL','Error occurred while processing registration changes.'),
                 'ERROR'
               );
            END IF;
         END IF;

   END p_regs;

   PROCEDURE p_disp_pin_prompt
   IS
      useridname   VARCHAR2 (500);
      useridlen    NUMBER;
      pinlen       NUMBER;
      pinname      VARCHAR2 (500);
   BEGIN
      twbkfrmt.P_TableOpen (
         'DATAENTRY',
         cattributes =>
            G$_NLS.Get ('BWCKCOM1-0029',
             'SQL',
             'summary="This table allows the user to enter a student Personal Identification Number."')
      );

      IF SUBSTR (UPPER (twbklibs.twgbldap_rec.twgbldap_protocol), 1, 4) = 'LDAP'
      THEN
         pinlen     := twbkwbis.F_FetchWTParam ('LDAPPWDLENGTH');
         pinname    := twbkwbis.F_FetchWTParam ('PINNAME');
         IF twbkwbis.F_FetchWTParam ('LDAPMAPUSER') = 'PROMPT'
         THEN
            useridlen  := twbkwbis.F_FetchWTParam ('USERIDLENGTH');
            useridname := twbkwbis.F_FetchWTParam ('USERIDNAME');

            twbkfrmt.P_TableRowOpen;
            twbkfrmt.P_TableDataLabel(
               twbkfrmt.F_FormLabel(twbkwbis.F_FetchWTParam ('USERIDNAME'),
                                    idname=>'userid_input'));
            twbkfrmt.P_TableData(
               twbkfrmt.f_formtext('ldap_userid',
                                   useridlen + 2,
                                   useridlen,
                                   cattributes =>'ID="userid_input"'));
            twbkfrmt.P_TableRowClose;
         END IF;
         twbkfrmt.P_TableRowOpen;
         twbkfrmt.P_TableDataLabel (
            twbkfrmt.f_formlabel (pinname, idname =>'student_pin'));
      ELSE
         pinlen := twbkwbis.F_FetchWTParam ('PINLENGTH');
         twbkfrmt.P_TableRowOpen;
         twbkfrmt.p_tabledatalabel (
            twbkfrmt.f_formlabel (
               G$_NLS.Get ('BWCKCOM1-0030', 'SQL', 'Student ') ||
               '<ACRONYM title = "' ||
               g$_nls.get ('BWCKCOM1-0031', 'SQL', 'Personal Identification Number') ||
               '">' ||
               g$_nls.get ('BWCKCOM1-0032', 'SQL', 'PIN') ||
               '</ACRONYM>' || ':',
               idname   => 'student_pin'
            )
         );
      END IF;
      twbkfrmt.P_TableData (HTF.formpassword ('PIN_NUMB', pinlen + 1, pinlen,
                                              cattributes =>'ID="student_pin"'));
      twbkfrmt.P_TableRowClose;
      twbkfrmt.P_TableClose;
      htp.br;

   END p_disp_pin_prompt;

   FUNCTION f_validatestudentpin (
      pidm   SPRIDEN.SPRIDEN_PIDM%TYPE,
      pin    GOBTPAC.GOBTPAC_PIN%TYPE,
      ldap_userid IN VARCHAR2 DEFAULT NULL
   )
      RETURN BOOLEAN
   IS
      pin_valid    VARCHAR2(1);
      pin_expired  VARCHAR2(1);
      pin_disabled VARCHAR2(1);
      use_ldap     VARCHAR2(10);

      lv_id        VARCHAR2(30);
      ldapparam    VARCHAR2(255);
      iden_id_cv   SYS_REFCURSOR;
      iden_id_rec  gb_identification.identification_rec;
   BEGIN

      IF SUBSTR(UPPER(twbklibs.twgbldap_rec.twgbldap_protocol), 1, 4) = 'LDAP' THEN
         ldapparam := twbkwbis.F_FetchWTParam ('LDAPMAPUSER');
      -- Use spriden id for the validation logic when using the DEFAULT option.
         IF nvl(ldapparam, ' ') = 'DEFAULT' THEN
            iden_id_cv := gb_identification.f_query_one(pidm,NULL);
            FETCH iden_id_cv INTO iden_id_rec;
            CLOSE iden_id_cv;
            lv_id := iden_id_rec.r_id;
         END IF;
      END IF;
      twbklogn.p_validate_pin(p_id   => nvl(ldap_userid,lv_id),
                              p_pidm => pidm,
                              p_pin  => pin,
                              p_pin_valid => pin_valid,
                              p_pin_expired => pin_expired,
                              p_pin_disabled => pin_disabled,
                              p_use_ldap     => use_ldap);

      IF pin_valid = 'Y'
      THEN
         RETURN TRUE;
      ELSE
         RETURN FALSE;
      END IF;

   END f_validatestudentpin;

--------------------------------------------------------------------------
---- Function which will check whether study paths are enabled by the institution
  FUNCTION F_StudyPathEnabled
  RETURN VARCHAR2
  IS
      sobctrl_row            sobctrl%ROWTYPE;
      CURSOR sobctrl_c IS
      select * from sobctrl;
  BEGIN
        OPEN sobctrl_c;
      FETCH sobctrl_c into sobctrl_row;
      CLOSE sobctrl_c;
      IF sobctrl_row.SOBCTRL_STUDY_PATH_IND = 'Y' THEN
        RETURN 'Y';
      ELSE
        RETURN 'N';
      END IF;
  
  END F_StudyPathEnabled;

---------------------------------------------------------------------------

--  Function which will check whether study paths are required for the term.

FUNCTION F_StudyPathReq (p_term STVTERM.STVTERM_CODE%TYPE)
RETURN VARCHAR2
IS
  CURSOR sobterm_c IS
  SELECT * 
    FROM sobterm
   WHERE sobterm_term_code = p_term;
  
  sobterm_row sobterm%ROWTYPE;    
BEGIN
  
  OPEN sobterm_c;
  FETCH sobterm_c into sobterm_row;
  CLOSE sobterm_c;
    
    IF sobterm_row.sobterm_study_path_ind = 'Y' THEN
      RETURN 'Y';
    ELSE
      RETURN 'N';
    END IF;
    
END F_StudyPathReq;    

---------------------------------------------------------------------------

-- Function which will give the name of the Learner Study Path based on the term code and 
-- Study Path Seq no

FUNCTION F_GetStudyPathName(p_pidm     SPRIDEN.SPRIDEN_PIDM%TYPE,
                            p_term     STVTERM.STVTERM_CODE%TYPE,
                            p_sp_seqno SGRSTSP.SGRSTSP_KEY_SEQNO%TYPE)
  RETURN VARCHAR2

 IS
  lv_sgrstsp_eff_term STVTERM.STVTERM_cODE%TYPE;
  lv_return_val       VARCHAR2(4000);
  CURSOR get_sgrstsp_eff_term_c(p_pidm SPRIDEN.SPRIDEN_PIDM%TYPE, p_term STVTERM.STVTERM_CODE%TYPE, p_sp_seqno SGRSTSP.SGRSTSP_KEY_SEQNO%TYPE) IS
    SELECT SGRSTSP_TERM_CODE_EFF
      FROM SGRSTSP
     WHERE SGRSTSP_PIDM = p_pidm
       AND SGRSTSP_KEY_SEQNO = p_sp_seqno
       AND SGRSTSP_TERM_CODE_EFF =
           (SELECT MAX(SGRSTSP_TERM_CODE_EFF)
              FROM SGRSTSP
             WHERE SGRSTSP_PIDM = p_pidm
               AND SGRSTSP_KEY_SEQNO = p_sp_seqno
               AND SGRSTSP_TERM_CODE_EFF <= p_term);

BEGIN
  OPEN get_sgrstsp_eff_term_c(p_pidm, p_term, p_sp_seqno);
  FETCH get_sgrstsp_eff_term_c
    INTO lv_sgrstsp_eff_term;
  CLOSE get_sgrstsp_eff_term_c;
  IF lv_sgrstsp_eff_term IS NOT NULL THEN
  
    lv_return_val := sb_studypath_name.f_learner_study_path_name(P_pidm,
                                                                 lv_sgrstsp_eff_term,
                                                                 p_sp_seqno);
  
  ELSE
    lv_return_val := (g$_nls.get('BWCKCOM1-0033', 'SQL', 'None'));
  END IF;

  RETURN lv_return_val;
END F_GetStudyPathName;                             
--
-- =====================================================================
-- This procedure displays the first part of the add/drop page.
-- Called when coming into add/drop from lookup classes page.
-- =====================================================================
   PROCEDURE p_add_drop_init (
      term_in     IN   OWA_UTIL.ident_arr
   )
   IS
      term         stvterm.stvterm_code%TYPE := NULL;
      multi_term   BOOLEAN                   := TRUE;

      CURSOR reg_check_c ( pidm_in SPRIDEN.SPRIDEN_PIDM%TYPE,
                           term_in stvterm.stvterm_code%TYPE)
      IS
      SELECT NVL (COUNT (*), 0)
        FROM sfrstcr
       WHERE sfrstcr_pidm = pidm_in
         AND sfrstcr_term_code = term_in;

   BEGIN
      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN;
      END IF;

      term := term_in (term_in.COUNT);

      IF term_in.COUNT = 1
      THEN
         multi_term := FALSE;
      END IF;

      IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
      THEN
         genpidm :=
           TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
      ELSE
         genpidm := global_pidm;
      END IF;

/* FACWEB-specific code */
      IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
      THEN
         IF NOT multi_term
         THEN
            IF NOT bwcksams.f_regsstu (
                  global_pidm,
                  term,
                  'bwlkfrad.P_FacAddDropCrse'
               )
            THEN
               RETURN;
            END IF;

            /* Check to see if add/drop activity is allowed for selected term */
            IF NOT bwlkilib.f_add_drp (term)
            THEN
               bwckfrmt.p_open_doc (
                  'bwlkfrad.P_FacAddDropCrse',
                  term,
                  NULL,
                  multi_term,
                  term_in (1)
               );
               twbkfrmt.p_printmessage (
                  g$_nls.get ('BWCKCOM1-0034',
                     'SQL',
                     'Registration is not allowed for this term at this time'),
                  'ERROR'
               );
               twbkwbis.p_closedoc (curr_release);
               RETURN;
            END IF;

            IF NOT sfkvars.add_allowed
            THEN
               OPEN reg_check_c ( genpidm, term);
               FETCH reg_check_c INTO row_count;
               CLOSE reg_check_c;

               IF row_count = 0
               THEN
                  bwckfrmt.p_open_doc (
                     'bwlkfrad.P_FacAddDropCrse',
                     term,
                     NULL,
                     multi_term,
                     term_in (1)
                  );
                  twbkfrmt.p_printmessage (
                     g$_nls.get ('BWCKCOM1-0035',
                        'SQL',
                        'Registration is not allowed at this time'),
                     'ERROR'
                  );
                  twbkwbis.p_closedoc (curr_release);
                  RETURN;
               END IF;
            END IF;

            IF NOT sfkvars.regs_allowed
            THEN
               bwckfrmt.p_open_doc (
                  'bwlkfrad.P_FacAddDropCrse',
                  term,
                  NULL,
                  multi_term,
                  term_in (1)
               );
               twbkfrmt.p_printmessage (
                  g$_nls.get ('BWCKCOM1-0036',
                     'SQL',
                     'Registration is not allowed at this time'),
                  'ERROR'
               );
               twbkwbis.p_closedoc (curr_release);
               RETURN;
            END IF;
         END IF;                                          /* not multi_term */

         bwckfrmt.p_open_doc (
            'bwlkfrad.P_FacAddDropCrse',
            term,
            NULL,
            multi_term,
            term_in (1)
         );

/* STUWEB-specific code */
      ELSE
         IF NOT multi_term
         THEN
--
-- Validate the term. If not valid, prompt for a new one.
-- ======================================================
            IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
            THEN
               bwskflib.p_seldefterm (term, 'bwskfreg.P_AddDropCrse');
               RETURN;
            END IF;

--
-- Check if the current user is eligible to register for
-- the current term.
-- ===================================================
            IF NOT bwcksams.f_regsstu (
                  global_pidm,
                  term,
                  'bwskfreg.P_AddDropCrse'
               )
            THEN
               RETURN;
            END IF;

            IF NOT sfkvars.regs_allowed
            THEN

               bwckfrmt.p_open_doc (
                  'bwskfreg.P_AddDropCrse',
                  term,
                  NULL,
                  multi_term,
                  term_in (1)
               );

               twbkfrmt.p_printmessage (
                  g$_nls.get ('BWCKCOM1-0037',
                     'SQL',
                     'Registration is not allowed at this time'),
                  'ERROR'
               );

               twbkwbis.p_closedoc (curr_release);
               RETURN;
            END IF;

--
-- Check if registration is allowed at the current time.
-- ===================================================
            IF NOT sfkvars.add_allowed
            THEN
               OPEN reg_check_c ( genpidm, term);
               FETCH reg_check_c INTO row_count;
               CLOSE reg_check_c;

               IF row_count = 0
               THEN

                  bwckfrmt.p_open_doc (
                     'bwskfreg.P_AddDropCrse',
                     term,
                     NULL,
                     multi_term,
                     term_in (1)
                  );
                  twbkfrmt.p_printmessage (
                     g$_nls.get ('BWCKCOM1-0038',
                        'SQL',
                        'Registration is not allowed at this time'),
                     'ERROR'
                  );
                  twbkwbis.p_closedoc (curr_release);
                  RETURN;
               END IF;
            END IF;
         END IF;                                          /* not multi_term */
         bwckfrmt.p_open_doc (
            'bwskfreg.P_AddDropCrse',
            term,
            NULL,
            multi_term,
            term_in (1)
         );

      END IF;                         /* END stu/fac specific code sections */
   END p_add_drop_init;

--------------------------------------------------------------------------

--
-- =====================================================================
-- This procedure displays the current registration portion of the
-- add/drop page.
-- =====================================================================
   PROCEDURE p_curr_sched_heading (
      heading_displayed   IN OUT   BOOLEAN,
      term                IN       STVTERM.STVTERM_CODE%TYPE DEFAULT NULL,
      multi_term_in       IN       BOOLEAN DEFAULT FALSE
   )
   IS
   BEGIN
      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN;
      END IF;

      IF NOT heading_displayed
      THEN
         IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') =
                                                                         'FAC'
         THEN
            genpidm :=
              TO_NUMBER (
                 twbkwbis.f_getparam (global_pidm, 'STUPIDM'),
                 '999999999'
              );
         ELSE
            genpidm := global_pidm;
         END IF;

--
-- Print student name subtitle for facweb.
-- ==================================================
         IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') =
                                                                         'FAC'
         THEN
--
-- Check if student is confidential.
-- Print subtitle, with or without link to address page.
-- ==================================================
            bwcklibs.P_ConfidStudInfo (genpidm, term);
         END IF;

         twbkfrmt.p_printheader (
            '3',
            g$_nls.get ('BWCKCOM1-0039', 'SQL', 'Current Schedule')
         );
         twbkfrmt.p_tableopen (
            'DATADISPLAY',
            cattributes   => 'SUMMARY="' ||
                                g$_nls.get ('BWCKCOM1-0040',
                                   'SQL',
                                   'Current Schedule') ||
                                '"'
         );
         twbkfrmt.p_tablerowopen;
         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-0041', 'SQL', 'Status')
         );
         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-0042', 'SQL', 'Action')
         );

         IF multi_term_in
         THEN
            twbkfrmt.p_tabledataheader (
               g$_nls.get ('BWCKCOM1-0043', 'SQL', 'Associated Term')
            );
         END IF;

         twbkfrmt.p_tabledataheader (
            twbkfrmt.f_printtext (
               '<ACRONYM title = "' ||
                  g$_nls.get ('BWCKCOM1-0044',
                     'SQL',
                     'Course Reference Number') ||
                  '">' ||
                  g$_nls.get ('BWCKCOM1-0045', 'SQL', 'CRN') ||
                  '</ACRONYM>'
            )
         );
         twbkfrmt.p_tabledataheader (
            twbkfrmt.f_printtext (
               '<ABBR title = ' || g$_nls.get ('BWCKCOM1-0046',
                                      'SQL',
                                      'Subject') ||
                  '>' ||
                  g$_nls.get ('BWCKCOM1-0047', 'SQL', 'Subj') ||
                  '</ABBR>'
            )
         );
         twbkfrmt.p_tabledataheader (
            twbkfrmt.f_printtext (
               '<ABBR title = ' || g$_nls.get ('BWCKCOM1-0048', 'SQL', 'Course') ||
                  '>' ||
                  g$_nls.get ('BWCKCOM1-0049', 'SQL', 'Crse') ||
                  '</ABBR>'
            )
         );
-- <<Coast>> -- start changes TRvvvv 9/30/2015
         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-9001', 'SQL', 'Part of Term')
         );
         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-9002', 'SQL', 'Dates')
         );
-- <<Coast>> -- end changes TR^^^^

-- <<Coast>> -- start changes
-- Remove Sec & Level column headings
--       twbkfrmt.p_tabledataheader (
--          twbkfrmt.f_printtext (
--             '<ABBR title = ' || g$_nls.get ('BWCKCOM1-0049',
--                                    'SQL',
--                                    'Section') ||
--                '>' ||
--                g$_nls.get ('BWCKCOM1-0050', 'SQL', 'Sec') ||
--                '</ABBR>'
--          )
--       );
--       twbkfrmt.p_tabledataheader (g$_nls.get ('BWCKCOM1-0051', 'SQL', 'Level'));
-- <<Coast>> -- end changes         
         twbkfrmt.p_tabledataheader (
            twbkfrmt.f_printtext (
               '<ABBR title = "' ||
                  g$_nls.get ('BWCKCOM1-0053', 'SQL', 'Credit Hours') ||
                  '">' ||
                  g$_nls.get ('BWCKCOM1-0054', 'SQL', 'Cred') ||
                  '</ABBR>'
            )
         );
         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-0055', 'SQL', 'Grade Mode')
         );
         twbkfrmt.p_tabledataheader (g$_nls.get ('BWCKCOM1-0056', 'SQL', 'Title'));
-- SP changes         
        IF bwckcoms.F_StudyPathEnabled  = 'Y' THEN
          twbkfrmt.p_tabledataheader (g$_nls.get ('BWCKCOM1-0057', 'SQL', 'Study Path'));
        END IF;
-- SP changes
         twbkfrmt.p_tablerowclose;
         heading_displayed := TRUE;
      END IF;
   END p_curr_sched_heading;

--------------------------------------------------------------------------

--
-- =====================================================================
-- After displaying the current schedule, display a few
-- summary data lines for credit hours, billing hours, etc.
-- ==================================================
   PROCEDURE p_summary (
      term            IN   STVTERM.STVTERM_CODE%TYPE DEFAULT NULL,
      tot_credit_hr   IN   SFTREGS.SFTREGS_CREDIT_HR%TYPE DEFAULT NULL,
      tot_bill_hr     IN   SFTREGS.SFTREGS_BILL_HR%TYPE DEFAULT NULL,
      tot_ceu         IN   SFTREGS.SFTREGS_CREDIT_HR%TYPE DEFAULT NULL,
      regs_count      IN   NUMBER DEFAULT NULL
   )
   IS
      max_hr   SFBETRM.SFBETRM_MHRS_OVER%TYPE := 0;
      min_hr   SFBETRM.SFBETRM_MIN_HRS%TYPE   := 0;
   BEGIN
      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN;
      END IF;

      IF regs_count > 0
      THEN
         IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') =
                                                                         'FAC'
         THEN
            genpidm :=
              TO_NUMBER (
                 twbkwbis.f_getparam (global_pidm, 'STUPIDM'),
                 '999999999'
              );
         ELSE
            genpidm := global_pidm;
         END IF;

         HTP.br;
         twbkfrmt.p_tableopen (
            'PLAIN',
            cattributes   => 'SUMMARY="' ||
                                g$_nls.get ('BWCKCOM1-0058',
                                   'SQL',
                                   'Schedule Summary') ||
                                '"'
         );
         twbkfrmt.p_tablerowopen;
         twbkfrmt.p_tabledata (
            g$_nls.get ('BWCKCOM1-0059', 'SQL', 'Total Credit Hours') || ': '
         );
         twbkfrmt.p_tabledata (
            RPAD (TO_CHAR (NVL (tot_credit_hr, 0), '9990D990'), 9)
         );
         twbkfrmt.p_tablerowclose;
         twbkfrmt.p_tablerowopen;
         twbkfrmt.p_tabledata (
            g$_nls.get ('BWCKCOM1-0060', 'SQL', 'Billing Hours') || ':'
         );
         twbkfrmt.p_tabledata (
            RPAD (TO_CHAR (NVL (tot_bill_hr, 0), '9990D990'), 9)
         );
         twbkfrmt.p_tablerowclose;

         IF NVL (tot_ceu, 0) > 0
         THEN
            twbkfrmt.p_tablerowopen;
            twbkfrmt.p_tabledata (
               twbkfrmt.f_printtext (
                  '<ACRONYM title = "' ||
                     g$_nls.get ('BWCKCOM1-0061',
                        'SQL',
                        'Continuing Education Units') ||
                     '">' ||
                     g$_nls.get ('BWCKCOM1-0062', 'SQL', 'CEU') ||
                     '</ACRONYM>:'
               )
            );
            twbkfrmt.p_tabledata (
               RPAD (TO_CHAR (NVL (tot_ceu, 0), '9990D990'), 9)
            );
            twbkfrmt.p_tablerowclose;
         END IF;

         FOR sfbetrm IN sfkcurs.sfbetrmc (genpidm, term)
         LOOP
-- <<Coast>> -- start changes TRvvvv 9/30/2015
--  Do not display the minimum, we don't use it
/*
            twbkfrmt.p_tablerowopen;
            min_hr := sfbetrm.sfbetrm_min_hrs;
            twbkfrmt.p_tabledata (
               g$_nls.get ('BWCKCOM1-0063', 'SQL', 'Minimum Hours') || ':'
            );
            twbkfrmt.p_tabledata (RPAD (TO_CHAR (min_hr, '999990D990'), 11));
            twbkfrmt.p_tablerowclose;
*/
-- <<Coast>> -- end changes TR^^^^ 9/30/2015
            --
            twbkfrmt.p_tablerowopen;

-- <<Coast>> -- start changes TRvvvv 9/30/2015
/*
            max_hr := sfbetrm.sfbetrm_mhrs_over;
            twbkfrmt.p_tabledata (
               g$_nls.get ('BWCKCOM1-0064', 'SQL', 'Maximum Hours') || ':'
            );
            twbkfrmt.p_tabledata (RPAD (TO_CHAR (max_hr, '999990D990'), 11));
*/
            -- Call Coast function(s) to get student's max hours
            IF SUBSTR(term, 6, 1) = '8' THEN -- Contract ed (military)
                max_hr := sfkfunc.f_max_units_contract_ed(global_pidm, term);
            ELSE
                max_hr := sfkfunc.f_max_units(global_pidm, term);
            END IF;
            twbkfrmt.p_tabledata (
               g$_nls.get ('BWCKCOM1-9003', 'SQL', 'Maximum Credit Hours') || ':'
            );
            twbkfrmt.p_tabledata (RPAD (TO_CHAR (max_hr, '999990D990'), 11));

            -- If term icludes intersession, display those hours
            IF sfkfunc.f_term_has_intersession(term) = 'Y' THEN 
                twbkfrmt.p_tabledata (
                   g$_nls.get ('BWCKCOM1-9004', 'SQL', 'Intersession Maximum Credit Hours') || ':'
                );
                twbkfrmt.p_tabledata(RPAD(TO_CHAR(sfkfunc.f_max_units_intersession(global_pidm, term), '999990D990'), 11));
            END IF;

-- <<Coast>> -- end changes TR^^^^ 9/30/2015
            twbkfrmt.p_tablerowclose;
         END LOOP;

-- <<Coast>> -- start changes TRvvvv 9/30/2015
    --  Add new row to display district hours and intersession hours if not contract ed term
         IF SUBSTR(term, 6, 1) <> '8' THEN -- Not contract ed (military)
            twbkfrmt.p_tablerowopen;
            max_hr := sfkfunc.f_district_units_full_term(global_pidm, term);
 
            twbkfrmt.p_tabledata (
               g$_nls.get ('BWCKCOM1-9005', 'SQL', 'Total Enrolled Credit Hours District') || ':');
            twbkfrmt.p_tabledata(RPAD(TO_CHAR(sfkfunc.f_district_units_full_term(global_pidm, term), '999990D990'), 11));
              -- Display intersession hours enrolled is term includes intersession
            IF sfkfunc.f_term_has_intersession(term) = 'Y' THEN 
                twbkfrmt.p_tabledata(g$_nls.get('BWCKCOM1-9006', 'SQL', 'Total Enrolled Intersession Credit Hours District') || ':');
                twbkfrmt.p_tabledata(RPAD(TO_CHAR(sfkfunc.f_district_units_intersession(global_pidm, term), '999990D990'), 11));
            END IF;
            twbkfrmt.p_tablerowclose;
         END IF;
-- <<Coast>> -- end changes TR^^^^ 9/30/2015

         twbkfrmt.p_tablerowopen;
         twbkfrmt.p_tabledata (g$_nls.get ('BWCKCOM1-0064', 'SQL', 'Date') || ':');
         twbkfrmt.p_tabledata (
            TO_CHAR (
               SYSDATE,
               twbklibs.date_display_fmt || ' ' ||
                  twbklibs.twgbwrul_rec.twgbwrul_time_fmt
            )
         );
         twbkfrmt.p_tablerowclose;
         twbkfrmt.p_tableclose;
         HTP.br;
      END IF;
   END p_summary;

--------------------------------------------------------------------------

--
-- =====================================================================
-- This procedure displays headings for the registration errors portion
-- of the add/drop page.
-- =====================================================================
   PROCEDURE p_reg_err_heading (
      call_type       IN   NUMBER DEFAULT NULL,
      multi_term_in   IN   BOOLEAN DEFAULT FALSE
   )
   IS
      table_type   VARCHAR2 (11);
   BEGIN
      twbkfrmt.p_printmessage (
         g$_nls.get ('BWCKCOM1-0066', 'SQL', 'Registration Add Errors'),
         'ERROR'
      );
      twbkfrmt.p_tableopen (
         'DATADISPLAY',
         cattributes   => 'SUMMARY="' ||
                             g$_nls.get ('BWCKCOM1-0067',
                                'SQL',
                                'This layout table is used to present Registration Errors') ||
                             '."'
      );
      twbkfrmt.p_tablerowopen;
      twbkfrmt.p_tabledataheader (g$_nls.get ('BWCKCOM1-0068', 'SQL', 'Status'));

      IF call_type = 1
      THEN
         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-0069', 'SQL', 'Action')
         );
      END IF;

      IF multi_term_in
      THEN
         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-0070', 'SQL', 'Associated Term')
         );
      END IF;

      twbkfrmt.p_tabledataheader (
         twbkfrmt.f_printtext (
            '<ACRONYM title = "' ||
               g$_nls.get ('BWCKCOM1-0071', 'SQL', 'Course Reference Number') ||
               '">' ||
               g$_nls.get ('BWCKCOM1-0072', 'SQL', 'CRN') ||
               '</ACRONYM>'
         )
      );
      twbkfrmt.p_tabledataheader (
         twbkfrmt.f_printtext (
            '<ABBR title = ' || g$_nls.get ('BWCKCOM1-0073', 'SQL', 'Subject') ||
               '>' ||
               g$_nls.get ('BWCKCOM1-0074', 'SQL', 'Subj') ||
               '</ABBR>'
         )
      );
      twbkfrmt.p_tabledataheader (
         twbkfrmt.f_printtext (
            '<ABBR title = ' || g$_nls.get ('BWCKCOM1-0075', 'SQL', 'Course') ||
               '>' ||
               g$_nls.get ('BWCKCOM1-0076', 'SQL', 'Crse') ||
               '</ABBR>'
         )
      );
-- <<Coast>> -- start changes TRvvvv 9/30/2015
         -- Add headers for intersession related columns
         twbkfrmt.p_tabledataheader(g$_nls.get('BWCKCOM1-9007', 'SQL', 'Part of Term'));
         twbkfrmt.p_tabledataheader(g$_nls.get('BWCKCOM1-9008', 'SQL', 'Dates'));
-- <<Coast>> -- end changes TR^^^^

-- <<Coast>> -- start changes      
-- Remove Section and Level      
--    twbkfrmt.p_tabledataheader (
--       twbkfrmt.f_printtext (
--          '<ABBR title = ' || g$_nls.get ('BWCKCOM1-0076', 'SQL', 'Section') ||
--             '>' ||
--             g$_nls.get ('BWCKCOM1-0077', 'SQL', 'Sec') ||
--             '</ABBR>'
--       )
--    );
--    twbkfrmt.p_tabledataheader (g$_nls.get ('BWCKCOM1-0078', 'SQL', 'Level'));
-- <<Coast>> -- end changes      
      twbkfrmt.p_tabledataheader (
         twbkfrmt.f_printtext (
            '<ABBR title = "' ||
               g$_nls.get ('BWCKCOM1-0080', 'SQL', 'Credit Hours') ||
               '">' ||
               g$_nls.get ('BWCKCOM1-0081', 'SQL', 'Cred') ||
               '</ABBR>'
         )
      );
      twbkfrmt.p_tabledataheader (
         g$_nls.get ('BWCKCOM1-0082', 'SQL', 'Grade Mode')
      );
      twbkfrmt.p_tabledataheader (g$_nls.get ('BWCKCOM1-0083', 'SQL', 'Title'));
      -- SP changes         
        IF bwckcoms.F_StudyPathEnabled  = 'Y' THEN
          twbkfrmt.p_tabledataheader (g$_nls.get ('BWCKCOM1-0084', 'SQL', 'Study Path'));
        END IF;
-- SP changes
      twbkfrmt.p_tablerowclose;
   END p_reg_err_heading;

--------------------------------------------------------------------------

--
-- =====================================================================
-- This procedure displays the new crn entry boxes on the add/drop page.
-- =====================================================================
   PROCEDURE p_add_drop_crn1 (
      call_type          IN   NUMBER DEFAULT NULL,
      sel_crn_count_in   IN   NUMBER DEFAULT 0
   )
   IS
      i   NUMBER;
      lv_sp_name VARCHAR2(4000);
   BEGIN
--
-- Display the add table.
-- ===================================================
      IF NOT sfkvars.add_allowed
      THEN
         RETURN;
      END IF;

      twbkfrmt.p_printheader (
         '3',
-- <<Coast>> -- start changes 09/21/2011
--       g$_nls.get ('BWCKCOM1-0084', 'SQL', 'Add Classes Worksheet')
         g$_nls.get ('BWCKCOM1-0084', 'SQL', 'Add Classes')
-- <<Coast>> -- end changes 09/21/2011
      );
--  Display info about the study path chosen
    IF bwckcoms.F_StudyPathEnabled = 'Y' THEN
        IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC' THEN
           genpidm := TO_NUMBER(twbkwbis.F_GetParam (global_pidm, 'STUPIDM'), 99999999);
        ELSE 
           genpidm := global_pidm;
        END IF;

        IF (twbkwbis.f_getparam (global_pidm, 'G_SP_SET') IS NOT NULL) THEN
          IF  (twbkwbis.f_getparam (global_pidm, 'G_SPATH') IS NOT NULL) THEN
            lv_sp_name := sb_studypath_name.f_learner_study_path_name(genpidm,
                                            SUBSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),INSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),'|')+1),
                                            SUBSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),1, INSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),'|')-1));
            twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse','SPINFO');
            twbkfrmt.p_printheader 
                     (
                     '4',
                     g$_nls.get ('BWCKCOM1-0086', 'SQL', 'Selected Study Path is %01%',lv_sp_name
                                )
                    );
          ELSE
             twbkwbis.p_dispinfo('bwckcoms.P_SelStudyPath', 'USE_NONE');
          END IF;      
        END IF;  
    END IF;    
      
      twbkfrmt.p_tableopen (
         'DATAENTRY',
         cattributes   => 'SUMMARY="' ||
                             g$_nls.get ('BWCKCOM1-0087',
                                'SQL',
                                'Add Classes Data Entry') ||
                             '" WIDTH="100%"'
      );
      twbkfrmt.p_tablerowopen;
      twbkfrmt.p_tableheader (
         twbkfrmt.f_printtext (
            '<ACRONYM title = "' ||
            g$_nls.get ('BWCKCOM1-0088', 'SQL', 'Course Reference Numbers') ||
            '">' ||
            g$_nls.get ('BWCKCOM1-0089', 'SQL', 'CRNs') ||
            '</ACRONYM>'
         ),
         ccolspan   => 10 + sel_crn_count_in
      );
      twbkfrmt.p_tablerowclose;
      twbkfrmt.p_tablerowopen;

      IF call_type = 1
      THEN
         RETURN;
      END IF;

      FOR i IN 1 .. 10
      LOOP
         twbkfrmt.p_tabledataopen;
         twbkfrmt.P_FormHidden ('RSTS_IN', SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2));
         twbkfrmt.p_formlabel (
            g$_nls.get ('BWCKCOM1-0090', 'SQL', 'CRN'),
            visible   => 'INVISIBLE',
            idname    => 'crn_id' || TO_CHAR (i)
         );
         twbkfrmt.p_formtext (
            'CRN_IN',
            '5',
            '5',
            cattributes   => 'ID="crn_id' || TO_CHAR (i) || '"'
         );
         twbkfrmt.P_FormHidden ('assoc_term_in', NULL);
         twbkfrmt.P_FormHidden ('start_date_in', NULL);
         twbkfrmt.P_FormHidden ('end_date_in', NULL);
         twbkfrmt.p_tabledataclose;
      END LOOP;

      twbkfrmt.p_tableclose;
   END p_add_drop_crn1;

--
-- =====================================================================
-- This procedure displays the new crn entry boxes on the add/drop page.
-- =====================================================================
   PROCEDURE p_add_drop_crn2 (
      add_row_number   IN   NUMBER DEFAULT NULL,
      regs_count       IN   NUMBER DEFAULT NULL,
      wait_count       IN   NUMBER DEFAULT NULL,
      multi_term       IN   VARCHAR2 DEFAULT NULL
   )
   IS
   BEGIN
      twbkfrmt.P_FormHidden ('regs_row', regs_count);
      twbkfrmt.P_FormHidden ('wait_row', wait_count);
      twbkfrmt.P_FormHidden ('add_row', add_row_number);
      twbkfrmt.p_printtext;
      HTP.br;
      HTP.formsubmit (
         'REG_BTN',
-- <<Coast>> -- start changes      
--       g$_nls.get ('BWCKCOM1-0090', 'SQL', 'Submit Changes')
         g$_nls.get ('BWCKCOM1-0090', 'SQL', 'Finalize Add/Drop')
-- <<Coast>> -- end changes         
      );

      IF sfkvars.add_allowed
      THEN
         HTP.formsubmit (
            'REG_BTN',
            g$_nls.get ('BWCKCOM1-0092', 'SQL', 'Class Search')
         );
      END IF;

-- <<Coast>> -- start changes
--    HTP.formreset (g$_nls.get ('BWCKCOM1-0092', 'SQL', 'Reset'));
-- <<Coast>> -- start changes 09/21/2011
--    HTP.formreset (g$_nls.get ('BWCKCOM1-0092', 'SQL', 'Reset Worksheet'));
      HTP.formreset (g$_nls.get ('BWCKCOM1-0092', 'SQL', 'Reset Page'));
-- <<Coast>> -- end changes 09/21/2011
-- <<Coast>> -- end changes      
      
      IF bwckcoms.F_StudyPathEnabled = 'Y' and NVL(multi_term,'N') <> 'Y' THEN      
        HTP.formsubmit (
            'REG_BTN',
            g$_nls.get ('BWCKCOM1-0094', 'SQL', 'Select Study Path')
         );
      END IF;
-- <<Coast>> -- start changes
-- Add image link, image must be available in graphic elements in WebTailor (In this case - Pay)
      htp.p(twbkfrmt.f_imagelink ('bwskoacc.P_ViewAcctTerm' ,'pay'));
-- <<Coast>> -- end changes      
   END p_add_drop_crn2;

--------------------------------------------------------------------------


PROCEDURE p_disp_start_date_confirm (
   term_in         IN   OWA_UTIL.ident_arr,
   rsts_in         IN   OWA_UTIL.ident_arr,
   assoc_term_in   IN   OWA_UTIL.ident_arr,
   crn_in          IN   OWA_UTIL.ident_arr,
   start_date_in   IN   OWA_UTIL.ident_arr,
   end_date_in     IN   OWA_UTIL.ident_arr,
   subj            IN   OWA_UTIL.ident_arr,
   crse            IN   OWA_UTIL.ident_arr,
   sec             IN   OWA_UTIL.ident_arr,
   levl            IN   OWA_UTIL.ident_arr,
   cred            IN   OWA_UTIL.ident_arr,
   gmod            IN   OWA_UTIL.ident_arr,
   title           IN   bwckcoms.varchar2_tabtype,
   mesg            IN   OWA_UTIL.ident_arr,
   reg_btn         IN   OWA_UTIL.ident_arr,
   regs_row             NUMBER,
   add_row              NUMBER,
   wait_row             NUMBER,
   next_proc_in    IN   VARCHAR2,
   msg_in          IN   VARCHAR2 DEFAULT NULL
)
IS
   term                stvterm.stvterm_code%TYPE := NULL;
   multi_term          BOOLEAN                   := TRUE;
   local_ssbsect_row   ssbsect%ROWTYPE;
   i                   INTEGER;
   temp_from_date      VARCHAR2 (100);
   temp_to_date        VARCHAR2 (100);
BEGIN
--
-- Initialize genpidm based on faculty/student.
-- =================================================

   IF NOT twbkwbis.f_validuser (global_pidm)
   THEN
      RETURN;
   END IF;

   term := term_in (term_in.COUNT);

   IF term_in.COUNT = 1
   THEN
      multi_term := FALSE;
   END IF;

   IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
   THEN
--
-- Start the web page for Faculty.
-- =================================================
      genpidm :=
          TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
      bwckfrmt.p_open_doc (
         'bwlkfrad.p_fac_disp_start_date_confirm',
         term,
         NULL,
         multi_term,
         term_in (1)
      );
      twbkwbis.p_dispinfo ('bwlkfrad.p_fac_disp_start_date_confirm');
   ELSE
--
-- Start the web page for Student.
-- =================================================
      genpidm := global_pidm;
      bwckfrmt.p_open_doc (
         'bwskfreg.p_disp_start_date_confirm',
         term,
         NULL,
         multi_term,
         term_in (1)
      );
    -- C3SC CL, 03/29/06   
    IF  sv_auth_addcodes_bp.f_is_auth_enabled(p_term_code  => term)
    THEN  
       twbkwbis.p_dispinfo ('bwskfreg.p_disp_start_date_confirm', 'AUTHTERM');  
     ELSE    
      twbkwbis.p_dispinfo ('bwskfreg.p_disp_start_date_confirm');
   END IF;
    -- End C3SC, 03/29/06                      

   END IF;

--
-- Initialize variables.
-- =================================================
   bwcklibs.p_initvalue (global_pidm, term, NULL, SYSDATE, NULL, NULL);

--
-- If error message passed in, something was wrong with
-- the data entered - display the message.
-- ==================================================

   IF msg_in IS NOT NULL
   THEN
      CASE substr(msg_in,1,1)
         WHEN '1' THEN
            twbkfrmt.p_printmessage ('<ACRONYM title = "' ||
               g$_nls.get ('BWCKCOM1-0095', 'SQL', 'Course Reference Number') ||
               '">' ||
               g$_nls.get ('BWCKCOM1-0096', 'SQL', 'CRN') ||
               '</ACRONYM> ' ||
               G$_NLS.Get ('BWCKCOM1-0097', 'SQL',
                  '%01%, date entered (%02%) is invalid.',
                  crn_in (to_number(substr(msg_in,2))),
                  start_date_in (to_number(substr(msg_in,2)))),
               'ERROR');

         WHEN '2' THEN
            twbkfrmt.p_printmessage ('<ACRONYM title = "' ||
               g$_nls.get ('BWCKCOM1-0098', 'SQL', 'Course Reference Number') ||
               '">' ||
               g$_nls.get ('BWCKCOM1-0099', 'SQL', 'CRN') ||
               '</ACRONYM> ' ||
               G$_NLS.Get ('BWCKCOM1-0100','SQL',
                  '%01%, date entered (%02%) is invalid.',
                  crn_in (to_number(substr(msg_in,2))),
                  end_date_in (to_number(substr(msg_in,2)))),
               'ERROR');
         WHEN '3' THEN
            twbkfrmt.p_printmessage ('<ACRONYM title = "' ||
               g$_nls.get ('BWCKCOM1-0101', 'SQL', 'Course Reference Number') ||
               '">' ||
               g$_nls.get ('BWCKCOM1-0102', 'SQL', 'CRN') ||
               '</ACRONYM> ' ||
               G$_NLS.Get ('BWCKCOM1-0103', 'SQL',
                  '%01%, only one of start or end dates may be entered.',
                  crn_in (to_number(substr(msg_in,2)))),
               'ERROR');
         WHEN '4' THEN
            twbkfrmt.p_printmessage ('<ACRONYM title = "' ||
               g$_nls.get ('BWCKCOM1-0104', 'SQL', 'Course Reference Number') ||
               '">' ||
               g$_nls.get ('BWCKCOM1-0105', 'SQL', 'CRN') ||
               '</ACRONYM> ' ||
               G$_NLS.Get ('BWCKCOM1-0106', 'SQL',
                  '%01%, start date (%02%) not within the permitted range.',
                  crn_in (to_number(substr(msg_in,2))),
                  start_date_in (to_number(substr(msg_in,2)))),
               'ERROR');
         WHEN '5' THEN
            twbkfrmt.p_printmessage ('<ACRONYM title = "' ||
               g$_nls.get ('BWCKCOM1-0107', 'SQL', 'Course Reference Number') ||
               '">' ||
               g$_nls.get ('BWCKCOM1-0108', 'SQL', 'CRN') ||
               '</ACRONYM> ' ||
               G$_NLS.Get ('BWCKCOM1-0109', 'SQL',
                  '%01%, start date (%02%) cannot be prior to today.',
                  crn_in (to_number(substr(msg_in,2))),
                  start_date_in (to_number(substr(msg_in,2)))),
               'ERROR');
         WHEN '6' THEN
            twbkfrmt.p_printmessage ('<ACRONYM title = "' ||
               g$_nls.get ('BWCKCOM1-0110', 'SQL', 'Course Reference Number') ||
               '">' ||
               g$_nls.get ('BWCKCOM1-0111', 'SQL', 'CRN') ||
               '</ACRONYM> ' ||
               G$_NLS.Get ('BWCKCOM1-0112', 'SQL',
                  '%01%, end date (%02%) is not within the permitted range.',
                  crn_in (to_number(substr(msg_in,2))),
                  end_date_in (to_number(substr(msg_in,2)))),
               'ERROR');
         WHEN '7' THEN
            twbkfrmt.p_printmessage ('<ACRONYM title = "' ||
               g$_nls.get ('BWCKCOM1-0113', 'SQL', 'Course Reference Number') ||
               '">' ||
               g$_nls.get ('BWCKCOM1-0114', 'SQL', 'CRN') ||
               '</ACRONYM> ' ||
               G$_NLS.Get ('BWCKCOM1-0115', 'SQL',
                  '%01%, start date (%02%), calculated from end date (%03%), cannot be prior to today.',
                  crn_in (to_number(substr(msg_in,2))),
                  TO_CHAR (
                    bwcklibs.f_olr_date (
                       assoc_term_in (to_number(substr(msg_in,2))),
                       crn_in (to_number(substr(msg_in,2))),
                       TO_DATE (end_date_in (to_number(substr(msg_in,2))), twbklibs.date_input_fmt),
                       'E'
                    ), twbklibs.date_input_fmt),
                  end_date_in (to_number(substr(msg_in,2)))),
               'ERROR');
         ELSE
            twbkfrmt.p_printmessage ('<ACRONYM title = "' ||
               g$_nls.get ('BWCKCOM1-0116', 'SQL', 'Course Reference Number') ||
               '">' ||
               g$_nls.get ('BWCKCOM1-0117', 'SQL', 'CRN') ||
               '</ACRONYM> ' ||
               G$_NLS.Get ('BWCKCOM1-0118', 'SQL', '%01%, start or end date must be entered.',
                  crn_in (to_number(substr(msg_in,2)))),
            'ERROR');
      END CASE;

   END IF;

--
--  Open Form, initialize form variables
-- ==================================================
   HTP.formopen (
      twbkwbis.f_cgibin || 'bwckcoms.p_proc_start_date_confirm',
      cattributes   => 'onSubmit="return checkSubmit()"'
   );

   FOR i IN 1 .. term_in.COUNT
   LOOP
      twbkfrmt.P_FormHidden ('term_in', term_in (i));
   END LOOP;

   twbkfrmt.P_FormHidden ('rsts_in', 'dummy');
   twbkfrmt.P_FormHidden ('assoc_term_in', 'dummy');
   twbkfrmt.P_FormHidden ('crn_in', 'dummy');
   twbkfrmt.P_FormHidden ('start_date_in', 'dummy');
   twbkfrmt.P_FormHidden ('end_date_in', 'dummy');
   twbkfrmt.P_FormHidden ('subj', 'dummy');
   twbkfrmt.P_FormHidden ('crse', 'dummy');
   twbkfrmt.P_FormHidden ('sec', 'dummy');
   twbkfrmt.P_FormHidden ('levl', 'dummy');
   twbkfrmt.P_FormHidden ('cred', 'dummy');
   twbkfrmt.P_FormHidden ('gmod', 'dummy');
   twbkfrmt.P_FormHidden ('title', 'dummy');
   twbkfrmt.P_FormHidden ('mesg', 'dummy');
   twbkfrmt.P_FormHidden ('reg_btn', 'dummy');
   twbkfrmt.P_FormHidden ('reg_btn', reg_btn (2));
   twbkfrmt.P_FormHidden ('reg_btn', 'dummy'); -- C3SC GRS 04/22/2009 ADD    
   twbkfrmt.P_FormHidden ('regs_row', regs_row);
   twbkfrmt.P_FormHidden ('add_row', add_row);
   twbkfrmt.P_FormHidden ('wait_row', wait_row);
   twbkfrmt.P_FormHidden ('start_date_from_in', 'dummy');
   twbkfrmt.P_FormHidden ('start_date_to_in', 'dummy');
   twbkfrmt.P_FormHidden ('end_date_from_in', 'dummy');
   twbkfrmt.P_FormHidden ('end_date_to_in', 'dummy');
   twbkfrmt.P_FormHidden ('next_proc_in', next_proc_in);

   FOR i IN 2 .. crn_in.COUNT
   LOOP
      EXIT WHEN crn_in (i) IS NULL;

--
-- Display course information.
-- =================================================
      IF i = 2
      THEN
         twbkfrmt.p_tableopen (
            'DATAENTRY',
            cattributes =>
               'WIDTH="100%" SUMMARY="' ||
               g$_nls.get ('BWCKCOM1-0119',
                           'SQL',
                           'This table allows the user to enter a start or end date for Open Learning courses.') ||
               '."'
         );
         twbkfrmt.p_tablerowopen;
         twbkfrmt.p_tabledataheader (
            twbkfrmt.f_printtext (
               '<ACRONYM title = "' ||
                  g$_nls.get ('BWCKCOM1-0120', 'SQL', 'Course Reference Number') ||
                  '">' ||
                  g$_nls.get ('BWCKCOM1-0121', 'SQL', 'CRN') ||
                  '</ACRONYM>'
            )
         );

         IF multi_term
         THEN
            twbkfrmt.p_tabledataheader (
               g$_nls.get ('BWCKCOM1-0122', 'SQL', 'Associated Term')
            );
         END IF;

         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-0123', 'SQL', 'Course')
         );
         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-0124', 'SQL', 'Course Title')
         );
         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-0125', 'SQL', 'Duration')
         );
         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-0126',
               'SQL',
               'Start Date (%01%)',
               g$_date.translate_format(twbklibs.date_input_fmt)
            )
         );
         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-0127',
               'SQL',
               'End Date (%01%)',
               g$_date.translate_format(twbklibs.date_input_fmt)
            )
         );
         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-0128', 'SQL', 'Permitted Start Dates')
         );
         twbkfrmt.p_tabledataheader (
            g$_nls.get ('BWCKCOM1-0129', 'SQL', 'Permitted End Dates')
         );
         twbkfrmt.p_tablerowclose;
      END IF;

      IF i < regs_row + 2
      THEN
--
-- pre-existing courses.
-- =================================================
         twbkfrmt.P_FormHidden ('rsts_in', rsts_in (i));
         twbkfrmt.P_FormHidden ('subj', subj (i));
         twbkfrmt.P_FormHidden ('crse', crse (i));
         twbkfrmt.P_FormHidden ('sec', sec (i));
         twbkfrmt.P_FormHidden ('levl', levl (i));
         twbkfrmt.P_FormHidden ('cred', cred (i));
         twbkfrmt.P_FormHidden ('gmod', gmod (i));
         twbkfrmt.P_FormHidden ('title', title (i));
         twbkfrmt.P_FormHidden ('mesg', mesg (i));
         twbkfrmt.P_FormHidden ('crn_in', crn_in (i));
         twbkfrmt.P_FormHidden ('assoc_term_in', assoc_term_in (i));
         twbkfrmt.P_FormHidden ('start_date_in', start_date_in (i));
         twbkfrmt.P_FormHidden ('end_date_in', end_date_in (i));
         twbkfrmt.P_FormHidden ('start_date_from_in', NULL);
         twbkfrmt.P_FormHidden ('start_date_to_in', NULL);
         twbkfrmt.P_FormHidden ('end_date_from_in', NULL);
         twbkfrmt.P_FormHidden ('end_date_to_in', NULL);
      ELSE
--
-- newly added course.
-- =================================================

         OPEN ssklibs.ssbsectc (crn_in (i), assoc_term_in (i));
         FETCH ssklibs.ssbsectc INTO local_ssbsect_row;
         CLOSE ssklibs.ssbsectc;
         twbkfrmt.p_tablerowopen;
         twbkfrmt.p_tabledata (
            twbkfrmt.F_FormHidden ('rsts_in', rsts_in (i)) ||
            twbkfrmt.F_FormHidden ('crn_in', crn_in (i)) || crn_in (i) ||
            twbkfrmt.F_FormHidden ('assoc_term_in', assoc_term_in (i))
         );

         IF multi_term
         THEN
            twbkfrmt.p_tabledata (bwcklibs.f_term_desc (assoc_term_in (i)));
         END IF;

         twbkfrmt.p_tabledata (
            local_ssbsect_row.ssbsect_subj_code || ' ' ||
               local_ssbsect_row.ssbsect_crse_numb
         );
         twbkfrmt.p_tabledata (
            bwcklibs.f_course_title (assoc_term_in (i), crn_in (i))
         );

         IF sfkolrl.f_open_learning_course (assoc_term_in (i), crn_in (i))
         THEN
            twbkfrmt.p_tabledata (
               local_ssbsect_row.ssbsect_number_of_units || ' ' ||
                  local_ssbsect_row.ssbsect_dunt_code
            );

            IF( (TRUNC (SYSDATE) BETWEEN local_ssbsect_row.ssbsect_reg_from_date
                   AND local_ssbsect_row.ssbsect_reg_to_date) AND
                   (TRUNC (SYSDATE) <= local_ssbsect_row.ssbsect_learner_regstart_tdate)
--C3SC GR 13/06/06 Added auth logic to allow enter start date or en date even when the date have expired.                   
            OR( sv_auth_addcodes_bp.F_process_with_auth(p_term_code => assoc_term_in (i),
                                                          p_rsts_code =>rsts_in (i)) 
                 )
              )
--END C3SC
            THEN
--
-- OLR course.
-- =================================================
               twbkfrmt.p_tabledataopen;
               twbkfrmt.p_formlabel (
                  g$_nls.get ('BWCKCOM1-0130', 'SQL', 'Start'),
                  visible   => 'INVISIBLE',
                  idname    => 'start' || TO_CHAR (i - 1) || '_id'
               );

               IF local_ssbsect_row.ssbsect_learner_regstart_fdate =
                             local_ssbsect_row.ssbsect_learner_regstart_tdate
               THEN
                  twbkfrmt.p_formtext (
                     'start_date_in',
                     '15',
                     '15',
                     cvalue        => TO_CHAR (
                                         local_ssbsect_row.ssbsect_learner_regstart_fdate,
                                         twbklibs.date_input_fmt
                                      ),
                     cattributes   => 'ID="start' || TO_CHAR (i - 1) || '_id"'
                  );
               ELSE
                  twbkfrmt.p_formtext (
                     'start_date_in',
                     '15',
                     '15',
                     cvalue        => start_date_in (i),
                     cattributes   => 'ID="start' || TO_CHAR (i - 1) || '_id"'
                  );
               END IF;

               twbkfrmt.p_tabledataclose;
               twbkfrmt.p_tabledataopen;
               twbkfrmt.p_formlabel (
                  g$_nls.get ('BWCKCOM1-0131', 'SQL', 'End'),
                  visible   => 'INVISIBLE',
                  idname    => 'end' || TO_CHAR (i - 1) || '_id'
               );
               twbkfrmt.p_formtext (
                  'end_date_in',
                  '15',
                  '15',
                  cvalue        => end_date_in (i),
                  cattributes   => 'ID="end' || TO_CHAR (i - 1) || '_id"'
               );
               twbkfrmt.p_tabledataclose;
            ELSE
--
-- OLR course with expired valid date range.
-- =================================================
               twbkfrmt.p_tabledata (
                  twbkfrmt.F_FormHidden ('start_date_in', 'NR') ||
                     g$_nls.get ('BWCKCOM1-0132', 'SQL', 'Not available')
               );
               twbkfrmt.p_tabledata (
                  twbkfrmt.F_FormHidden ('end_date_in', 'NR') ||
                     g$_nls.get ('BWCKCOM1-0133', 'SQL', 'Not available')
               );
            END IF;

            twbkfrmt.p_tabledata (
               twbkfrmt.F_FormHidden (
                  'start_date_from_in',
                  TO_CHAR (
                     local_ssbsect_row.ssbsect_learner_regstart_fdate,
                     twbklibs.date_input_fmt
                  )
               ) ||
                  TO_CHAR (
                     local_ssbsect_row.ssbsect_learner_regstart_fdate,
                     twbklibs.date_input_fmt
                  ) ||
                  g$_nls.get ('BWCKCOM1-0134', 'SQL', ' to ') ||
                  twbkfrmt.F_FormHidden (
                     'start_date_to_in',
                     TO_CHAR (
                        local_ssbsect_row.ssbsect_learner_regstart_tdate,
                        twbklibs.date_input_fmt
                     )
                  ) ||
                  TO_CHAR (
                     local_ssbsect_row.ssbsect_learner_regstart_tdate,
                     twbklibs.date_input_fmt
                  )
            );
            temp_from_date :=
              TO_CHAR (
                 bwcklibs.f_olr_date (
                    assoc_term_in (i),
                    crn_in (i),
                    local_ssbsect_row.ssbsect_learner_regstart_fdate,
                    'S'
                 ),
                 twbklibs.date_input_fmt
              );
            temp_to_date :=
              TO_CHAR (
                 bwcklibs.f_olr_date (
                    assoc_term_in (i),
                    crn_in (i),
                    local_ssbsect_row.ssbsect_learner_regstart_tdate,
                    'S'
                 ),
                 twbklibs.date_input_fmt
              );
            twbkfrmt.p_tabledata (
               twbkfrmt.F_FormHidden ('end_date_from_in', temp_from_date) ||
                  temp_from_date ||
                  g$_nls.get ('BWCKCOM1-0135', 'SQL', ' to ') ||
                  twbkfrmt.F_FormHidden ('end_date_to_in', temp_to_date) ||
                  temp_to_date
            );
         ELSE
--
-- Traditional course.
-- =================================================
            twbkfrmt.p_tabledatadead(twbkfrmt.F_FormHidden ('start_date_in', NULL));
            twbkfrmt.p_tabledatadead(twbkfrmt.F_FormHidden ('end_date_in', NULL));
            twbkfrmt.p_tabledatadead(twbkfrmt.F_FormHidden ('start_date_from_in', NULL));
            twbkfrmt.p_tabledatadead(twbkfrmt.F_FormHidden ('start_date_to_in', NULL));
            twbkfrmt.p_tabledatadead(twbkfrmt.F_FormHidden ('end_date_from_in', NULL) ||
                                     twbkfrmt.F_FormHidden ('end_date_to_in', NULL));
         END IF;

         twbkfrmt.p_tablerowclose;
      END IF;
   END LOOP;

   IF crn_in.COUNT > 1
   THEN
      twbkfrmt.p_tableclose;
   END IF;

--
-- Submit Button.
-- =================================================
   HTP.br;
   HTP.formsubmit (NULL, g$_nls.get ('BWCKCOM1-0136', 'SQL', 'Submit Changes'));
   --C3SC GR, 06/18/2006, ADDED
    IF  sv_auth_addcodes_bp.f_is_auth_enabled(p_term_code  => term)
    THEN    
      HTP.formsubmit ('REG_BTN', g$_nls.get ('BWCKCOM1-0137', 'SQL', 'Cancel'));
    END IF;
   --END C3SC    
   HTP.formclose;
   twbkwbis.p_closedoc (curr_release);
END p_disp_start_date_confirm;

--------------------------------------------------------------------------

PROCEDURE p_proc_start_date_confirm (
   term_in              IN   OWA_UTIL.ident_arr,
   rsts_in              IN   OWA_UTIL.ident_arr,
   assoc_term_in        IN   OWA_UTIL.ident_arr,
   crn_in               IN   OWA_UTIL.ident_arr,
   start_date_in        IN   OWA_UTIL.ident_arr,
   end_date_in          IN   OWA_UTIL.ident_arr,
   subj                 IN   OWA_UTIL.ident_arr,
   crse                 IN   OWA_UTIL.ident_arr,
   sec                  IN   OWA_UTIL.ident_arr,
   levl                 IN   OWA_UTIL.ident_arr,
   cred                 IN   OWA_UTIL.ident_arr,
   gmod                 IN   OWA_UTIL.ident_arr,
   title                IN   bwckcoms.varchar2_tabtype,
   mesg                 IN   OWA_UTIL.ident_arr,
   reg_btn              IN   OWA_UTIL.ident_arr,
   regs_row                  NUMBER,
   add_row                   NUMBER,
   wait_row                  NUMBER,
   start_date_from_in   IN   OWA_UTIL.ident_arr,
   start_date_to_in     IN   OWA_UTIL.ident_arr,
   end_date_from_in     IN   OWA_UTIL.ident_arr,
   end_date_to_in       IN   OWA_UTIL.ident_arr,
   next_proc_in         IN   VARCHAR2
)
IS
   term                stvterm.stvterm_code%TYPE := NULL;
   multi_term          BOOLEAN                   := TRUE;
   local_ssbsect_row   ssbsect%ROWTYPE;
   i                   INTEGER;
   test_date           DATE;
   start_date          OWA_UTIL.ident_arr        := start_date_in;
   end_date            OWA_UTIL.ident_arr        := end_date_in;
   msg                 VARCHAR2 (2);
--C3SC 24/02/2006 GRS  
  auth_term_active      VARCHAR2(1);
  lv_cancel             VARCHAR2(50):= g$_nls.get ('BWCKCOM1-0138', 'SQL', 'Cancel');
  --C3SC 24/02/2006 GRS                 
   
BEGIN
   IF NOT twbkwbis.f_validuser (global_pidm)
   THEN
      RETURN;
   END IF;

   term := term_in (term_in.COUNT);

   --C3SC GR 13/06/06 Added
  
  IF LTRIM (RTRIM (reg_btn (3))) = lv_cancel
   THEN
     sv_auth_addcodes_bp.P_Cancel_Process(term_in   =>term_in,
                                         p_pidm_in  =>global_pidm,
                                         crn_in     => crn_in,
                                         regs_row   =>regs_row,
                                         assoc_term_in  =>assoc_term_in
                                         ) ;
 
   RETURN;
  END IF; 
 --END C3SC     

   IF term_in.COUNT = 1
   THEN
      multi_term := FALSE;
   END IF;

   FOR i IN 2 .. crn_in.COUNT
   LOOP
      BEGIN
       --C3SC GR 13/06/06 Added
          auth_term_active:='N';
         IF sv_auth_addcodes_bp.F_process_with_auth(p_term_code => assoc_term_in (i),
                                                          p_rsts_code =>rsts_in (i)) 
         
         THEN
           auth_term_active:='Y';
         END IF;  
        -- END C3SC  
      
--
-- validate entered start date.
-- =================================================
         IF     start_date_in (i) IS NOT NULL
            AND start_date_in (i) <> 'NR'
         THEN
            test_date := TO_DATE (start_date_in (i), twbklibs.date_input_fmt);
         END IF;
      EXCEPTION
         /* check others to catch all date errors */
         WHEN OTHERS
         THEN
            msg := '1'||to_char(i);
            EXIT;
      END;

      BEGIN
--
-- validate entered end date.
-- =================================================
         IF     end_date_in (i) IS NOT NULL
            AND nvl(start_date_in (i),'XX') <> 'NR'
         THEN
            test_date := TO_DATE (end_date_in (i), twbklibs.date_input_fmt);
         END IF;
      EXCEPTION
         /* check others to catch all date errors */
         WHEN OTHERS
         THEN
            msg := '2'||to_char(i);
            EXIT;
      END;

--
-- calculate unentered start/end dates for new OLR course.
-- =======================================================
      IF     sfkolrl.f_open_learning_course (assoc_term_in (i), crn_in (i))
         AND NOT subj.EXISTS (i)
      THEN
         IF start_date_in (i) = 'NR'
         THEN
            start_date (i) := start_date_from_in (i);
            end_date (i) := end_date_from_in (i);
         ELSIF     start_date_in (i) IS NOT NULL
               AND end_date_in (i) IS NOT NULL
         THEN
            msg := '3'||to_char(i);
            EXIT;
         ELSIF start_date_in (i) IS NOT NULL
         THEN
            IF NOT TO_DATE (start_date_in (i), twbklibs.date_input_fmt) BETWEEN TO_DATE (
                                                                                   start_date_from_in (
                                                                                      i
                                                                                   ),
                                                                                   twbklibs.date_input_fmt
                                                                                )
                   AND TO_DATE (start_date_to_in (i), twbklibs.date_input_fmt)
            THEN
               msg := '4'||to_char(i);
               EXIT;
            ELSIF TO_DATE (start_date_in (i), twbklibs.date_input_fmt) <
                                                               TRUNC (SYSDATE)
                   AND auth_term_active <> 'Y'    --C3SC GR 02/25/2006 Added                                                                                                                                                                           
--                                                                
            THEN
               msg := '5'||to_char(i);
               EXIT;
            ELSE
               end_date (i) :=
                 TO_CHAR (
                    bwcklibs.f_olr_date (
                       assoc_term_in (i),
                       crn_in (i),
                       TO_DATE (start_date_in (i), twbklibs.date_input_fmt),
                       'S'
                    ),
                    twbklibs.date_input_fmt
                 );
            END IF;
         ELSIF     start_date_in (i) IS NULL
               AND end_date_in (i) IS NOT NULL
         THEN
            IF NOT TO_DATE (end_date_in (i), twbklibs.date_input_fmt) BETWEEN TO_DATE (
                                                                                 end_date_from_in (
                                                                                    i
                                                                                 ),
                                                                                 twbklibs.date_input_fmt
                                                                              )
                   AND TO_DATE (end_date_to_in (i), twbklibs.date_input_fmt)
            THEN
               msg := '6'||to_char(i);
               EXIT;
            ELSE
               start_date (i) :=
                 TO_CHAR (
                    bwcklibs.f_olr_date (
                       assoc_term_in (i),
                       crn_in (i),
                       TO_DATE (end_date_in (i), twbklibs.date_input_fmt),
                       'E'
                    ),
                    twbklibs.date_input_fmt
                 );

               IF TO_DATE (start_date (i), twbklibs.date_input_fmt) <
                                                               TRUNC (SYSDATE)

                   AND auth_term_active <> 'Y'    --C3SC GR 02/25/2006 Added                                                                                                                                                                            
--                                                                
               THEN
                  msg := '7'||to_char(i);
                  EXIT;
               END IF;
            END IF;
         ELSIF     start_date_in (i) IS NULL
               AND end_date_in (i) IS NULL
         THEN
            msg := '8'||to_char(i);
            EXIT;
         END IF;
      END IF;
   END LOOP;

   IF msg IS NOT NULL
   THEN
      p_disp_start_date_confirm (
         term_in,
         rsts_in,
         assoc_term_in,
         crn_in,
         start_date_in,
         end_date_in,
         subj,
         crse,
         sec,
         levl,
         cred,
         gmod,
         title,
         mesg,
         reg_btn,
         regs_row,
         add_row,
         wait_row,
         next_proc_in,
         msg
      );
      RETURN;
   END IF;

--
-- no date problems, so add/drop.
-- =================================================
   bwckcoms.p_regs (
      term_in,
      rsts_in,
      assoc_term_in,
      crn_in,
      start_date,
      end_date,
      subj,
      crse,
      sec,
      levl,
      cred,
      gmod,
      title,
      mesg,
      reg_btn,
      regs_row,
      add_row,
      wait_row
         );
END p_proc_start_date_confirm;

--------------------------------------------------------------------------
   PROCEDURE p_open_rstsrowc (
      rstsrowc            IN OUT   rstsrowc_typ,
      open_learning       OUT      BOOLEAN,
      term_in                      VARCHAR2,
      pidm_in                      NUMBER,
      crn_in                       VARCHAR2,
      rsts_in                      VARCHAR2,
      ptrm_in                      VARCHAR2,
      sdax_rsts_code_in            VARCHAR2
   )
   IS
   BEGIN
      IF sfkolrl.f_open_learning_course (term_in, crn_in)
      THEN
         open_learning := TRUE;
         OPEN rstsrowc FOR
            SELECT stvrsts_code, stvrsts_desc, ssrrsts_usage_cutoff_pct_from,
                   ssrrsts_usage_cutoff_pct_to, ssrrsts_usage_cutoff_dur_from,
                   ssrrsts_usage_cutoff_dur_to
              FROM stvrsts, ssrrsts
             WHERE stvrsts_code <>
                             DECODE (rsts_in, 'RE', sdax_rsts_code_in, rsts_in)
               AND ( NVL (stvrsts_voice_type, 'R') not in ('D','W')
                     OR NOT EXISTS (SELECT 'X'
                                      FROM stvrsts b
                                     WHERE b.stvrsts_code = rsts_in
                                       AND b.stvrsts_voice_type in ('D','W'))
                     )
               AND stvrsts_web_ind = 'Y'
               AND stvrsts_extension_ind <> 'Y'
               AND stvrsts_wait_ind <> 'Y'
               AND stvrsts_code = ssrrsts_rsts_code
               AND term_in = ssrrsts_term_code
               AND crn_in = ssrrsts_crn;
      ELSE
         open_learning := FALSE;
         OPEN rstsrowc FOR
            SELECT stvrsts_code, stvrsts_desc,
                   NULL ssrrsts_usage_cutoff_pct_from,
                   NULL ssrrsts_usage_cutoff_pct_to,
                   NULL ssrrsts_usage_cutoff_dur_from,
                   NULL ssrrsts_usage_cutoff_dur_to
              FROM stvrsts
             WHERE stvrsts_code <>
                             DECODE (rsts_in, 'RE', sdax_rsts_code_in, rsts_in)
               AND ( NVL (stvrsts_voice_type, 'R') not in ('D','W')
                     OR NOT EXISTS (SELECT 'X'
                                      FROM stvrsts b
                                     WHERE b.stvrsts_code = rsts_in
                                       AND b.stvrsts_voice_type in ('D','W'))
                     )
               AND stvrsts_web_ind = 'Y'
               AND stvrsts_extension_ind <> 'Y'
               AND stvrsts_wait_ind <> 'Y'
               AND EXISTS (SELECT 'X'
                             FROM sfrrsts
                            WHERE sfrrsts_rsts_code = stvrsts_code
                              AND sfrrsts_term_code = term_in
                              AND sfrrsts_ptrm_code = ptrm_in
                              AND TRUNC (SYSDATE) BETWEEN sfrrsts_start_date
                                      AND sfrrsts_end_date);
      END IF;
   END p_open_rstsrowc;

--------------------------------------------------------------------------
   PROCEDURE p_open_waitrstsrowc (
      waitrstsrowc    IN OUT   rstsrowc_typ,
      open_learning   OUT      BOOLEAN,
      term_in                  VARCHAR2,
      pidm_in                  NUMBER,
      crn_in                   VARCHAR2,
      ptrm_in                  VARCHAR2
   )
   IS
   BEGIN
      IF sfkolrl.f_open_learning_course (term_in, crn_in)
      THEN
         open_learning := TRUE;
         OPEN waitrstsrowc FOR
            SELECT stvrsts_code, stvrsts_desc, NULL, NULL, NULL, NULL
              FROM stvrsts, ssrrsts
             WHERE stvrsts_wait_ind = 'Y'
               AND stvrsts_extension_ind <> 'Y'
               AND stvrsts_web_ind = 'Y'
               AND stvrsts_code = ssrrsts_rsts_code
               AND term_in = ssrrsts_term_code
               AND crn_in = ssrrsts_crn;
      ELSE
         open_learning := FALSE;
         OPEN waitrstsrowc FOR
            SELECT stvrsts_code, stvrsts_desc, NULL, NULL, NULL, NULL
              FROM stvrsts
             WHERE stvrsts_wait_ind = 'Y'
               AND stvrsts_extension_ind <> 'Y'
               AND stvrsts_web_ind = 'Y'
               AND EXISTS (SELECT 'X'
                             FROM sfrrsts
                            WHERE sfrrsts_rsts_code = stvrsts_code
                              AND sfrrsts_term_code = term_in
                              AND sfrrsts_ptrm_code = ptrm_in
                              AND TRUNC (SYSDATE) BETWEEN sfrrsts_start_date
                                      AND sfrrsts_end_date);
      END IF;
   END p_open_waitrstsrowc;

--------------------------------------------------------------------------
   PROCEDURE p_build_action_pulldown (
      term_in              stvterm.stvterm_code%TYPE,
      pidm_in              spriden.spriden_pidm%TYPE,
      crn_in               ssbsect.ssbsect_crn%TYPE,
      start_date_in        sftregs.sftregs_start_date%TYPE,
      completion_date_in   sftregs.sftregs_completion_date%TYPE,
      regstatus_date_in    sftregs.sftregs_rsts_date%TYPE,
      dunt_in              sftregs.sftregs_dunt_code%TYPE,
      regs_count_in        NUMBER,
      rsts_code_in         stvrsts.stvrsts_code%TYPE,
      ptrm_code_in         ssbsect.ssbsect_ptrm_code%TYPE DEFAULT NULL,
      sdax_rsts_code_in    VARCHAR2 DEFAULT NULL,
      hold_rsts_in         VARCHAR2 DEFAULT NULL
   )
   IS
      gtvsdax_row           gtvsdax%ROWTYPE;
      current_stvrsts_row   stvrsts%ROWTYPE;
      rstsrowc              rstsrowc_typ;
      rsts_row              rstsrowc_rec_typ;
      row_count             NUMBER           := 0;
      open_learning         BOOLEAN          := FALSE;
     --C3SC ADDED GRS 08/04/2006
      calc_drop_code        stvrsts.stvrsts_code%TYPE;
      active_web_drop       VARCHAR2(1);
     --C3SC END  
  
   BEGIN
--
-- Build the Action pulldown. Loop through the
-- NOT waitlisted reg status records.
-- ==========================================
      p_open_rstsrowc (
         rstsrowc,
         open_learning,
         term_in,
         pidm_in,
         crn_in,
         rsts_code_in,
         ptrm_code_in,
         sdax_rsts_code_in
      );
      --C3SC STU32 ADDED GRS 08/04/2006
       active_web_drop:=sv_acad_calendar_bp.F_Calc_Drop_is_Active(p_term_code=>term_in);
       IF active_web_drop = 'Y'AND 
       NOT open_learning  
       THEN
          -- C3SC Defect 1-3JBJFC   GRS 04/10/2008
          IF sv_acad_calendar_bp.f_is_waitlisted (p_rsts=> rsts_code_in) = 'Y' THEN
            sfkcurs.p_get_gtvsdax ('WEBRSTSDRP', 'WEBREG', gtvsdax_row);
            calc_drop_code := gtvsdax_row.gtvsdax_external_code;
          ELSE 
                      /* C3SC ADD NP 03/22/2010 */
                      IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC' THEN
                       calc_drop_code:= sv_acad_calendar_bp.F_Calc_Fac_Drop_Code (term_in,
                                                                                  crn_in,
                                                                                  regstatus_date_in);     
                      ELSE
                       calc_drop_code:= sv_acad_calendar_bp.F_Calc_Drop_Code (term_in,
                                                                              crn_in,
                                                                             regstatus_date_in);     
                      END IF;                                                       
                      /* C3SC END */
           END IF;                                                        
       --C3SC Defect:1-3KGMWE 
       ELSIF active_web_drop = 'Y'
       AND open_learning  
       THEN
        
         sfkcurs.p_get_gtvsdax ('WEBRSTSDRP', 'WEBREG', gtvsdax_row);
            calc_drop_code := gtvsdax_row.gtvsdax_external_code;
       END IF;                                                                
       --C3SC STU32 ADDED GRS   08/04/2006  


      <<rstsrowc_loop>>
      LOOP
         FETCH rstsrowc INTO rsts_row;
         EXIT rstsrowc_loop WHEN rstsrowc%NOTFOUND;

--
-- Do not allow a "Web Registered" option in the Action
-- pulldown in the Current Schedule area.
-- ==================================================
         sfkcurs.p_get_gtvsdax ('WEBRSTSREG', 'WEBREG', gtvsdax_row);
        -- Defect 1-2ZYBH7
           OPEN stkrsts.stvrstsc (rsts_code_in);
           FETCH stkrsts.stvrstsc INTO current_stvrsts_row;
           CLOSE stkrsts.stvrstsc;

         IF (rsts_row.stvrsts_code = gtvsdax_row.gtvsdax_external_code)
         -- Defect :1-2ZYBH7
          AND (      (   sfkwlat.f_wl_automation_active( p_term => term_in, p_crn => crn_in ) = 'Y'
                         AND ( sfkwlat.f_student_is_notified( p_term => term_in, p_pidm => pidm_in, p_crn => crn_in ) = 'N'
                              OR sfkwlat.f_is_seats_available(p_term=>term_in,p_crn=>crn_in,p_pidm=>pidm_in) ='N'
                              )
                         AND current_stvrsts_row.stvrsts_wait_ind = 'Y'
                      )
                 OR sfkwlat.f_wl_automation_active( p_term => term_in, p_crn => crn_in ) = 'N'
              )
         -- C3SC 8.7 GRS 03/22/2013 Change f_is_auth_enabled function to use f_reg_auth_active to allow RW to be listed in Waitlisted courses.
         --AND NOT sv_auth_addcodes_bp.f_is_auth_enabled(p_term_code=>term_in)   -- C3SC GRS 04/16/2009              
           -- Only when authorization codes are not active . If authorization code 
           -- is not active works like baseline
           AND NOT sv_auth_addcodes_bp.f_reg_auth_active (p_term_code => term_in, p_crn => crn_in, p_start_date =>start_date_in,
                                                       p_reg_date=> regstatus_date_in )
          -- C3SC 8.7 GRS 03/22/2013 END                                                       
         THEN
            GOTO next_row;
         END IF;
        --C3SC STU32 ADDED  GRS 08/04/2006
         IF active_web_drop = 'Y' AND 
           NOT sv_auth_addcodes_bp.f_is_valid_rsts_code (p_rsts_code  => rsts_row.stvrsts_code) 
            AND calc_drop_code is NOT NULL AND
           calc_drop_code <> rsts_row.stvrsts_code 
          -- AND NOT open_learning      Commented Out by Defect:1-3KGMWE 
         THEN
           GOTO next_row;
         ELSIF active_web_drop= 'Y' AND
               calc_drop_code IS NULL and 
               NOT open_learning 
         THEN      
           GOTO next_row;
         END IF;         
         --C3SC STU32 END;

--
-- Web Drop is the only option displayed in Action pulldown
-- for a class that is waitlisted.
--
-- This code removed for defect 94298.
-- ==================================================
--         OPEN stkrsts.stvrstsc (rsts_code_in);
--         FETCH stkrsts.stvrstsc INTO current_stvrsts_row;
--         CLOSE stkrsts.stvrstsc;
--         sfkcurs.p_get_gtvsdax ('WEBRSTSDRP', 'WEBREG', gtvsdax_row);

--         IF     (rsts_row.stvrsts_code <> gtvsdax_row.gtvsdax_external_code)
--            AND (current_stvrsts_row.stvrsts_wait_ind = 'Y')
--         THEN
--            GOTO next_row;
--         END IF;

--
-- For OLR courses, check for ssrrsts usage cutoffs, if any exist.
-- ===============================================================

         IF open_learning
         THEN

            IF sfkolrl.f_cutoff_reached (
                  start_date_in,
                  completion_date_in,
                  regstatus_date_in,
                  dunt_in,
                  rsts_row.ssrrsts_usage_cutoff_pct_from,
                  rsts_row.ssrrsts_usage_cutoff_pct_to,
                  rsts_row.ssrrsts_usage_cutoff_dur_from,
                  rsts_row.ssrrsts_usage_cutoff_dur_to
               )
            THEN
               GOTO next_row;
            END IF;

         END IF;

--
-- Display the Action pulldown.
-- ==================================================
         row_count := row_count + 1;

         IF row_count = 1
         THEN
            twbkfrmt.p_tabledataopen;
            twbkfrmt.p_formlabel (
               g$_nls.get ('BWCKCOM1-0139', 'SQL', 'Action'),
               visible   => 'INVISIBLE',
               idname    => 'action_id' || TO_CHAR (regs_count_in)
            );
            HTP.formselectopen (
               'RSTS_IN',
               nsize         => 1,
               cattributes   => 'ID="action_id' || TO_CHAR (regs_count_in) ||
                                   '"'
            );

            IF hold_rsts_in IS NULL
            THEN
               twbkwbis.p_formselectoption (
                  g$_nls.get ('BWCKCOM1-0140', 'SQL', 'None'),
                  NULL,
                  'SELECTED'
               );
            ELSE
               twbkwbis.p_formselectoption (
                  g$_nls.get ('BWCKCOM1-0141', 'SQL', 'None'),
                  NULL
               );
            END IF;
         END IF;

         IF NVL (hold_rsts_in, '#') <> rsts_row.stvrsts_code
         THEN
            twbkwbis.p_formselectoption (
               rsts_row.stvrsts_desc,
               rsts_row.stvrsts_code
            );
         ELSE
            twbkwbis.p_formselectoption (
               rsts_row.stvrsts_desc,
               rsts_row.stvrsts_code,
               'SELECTED'
            );
         END IF;

         <<next_row>>
                 --C3SC STU32 ADDED  GRS 08/04/2006
         IF active_web_drop ='Y' THEN
           row_count := row_count + 1;

           IF row_count = 1
           THEN
             twbkfrmt.p_tabledataopen;
             twbkfrmt.p_formlabel (
               g$_nls.get ('BWCKCOM1-0142', 'SQL', 'Action'),
               visible   => 'INVISIBLE',
               idname    => 'action_id' || TO_CHAR (regs_count_in)
               );
              HTP.formselectopen (
               'RSTS_IN',
               nsize         => 1,
               cattributes   => 'ID="action_id' || TO_CHAR (regs_count_in) ||
                                   '"'
              );
 
             IF hold_rsts_in IS NULL
             THEN
               twbkwbis.p_formselectoption (
                  g$_nls.get ('BWCKCOM1-0143', 'SQL', 'None'),
                  NULL,
                  'SELECTED'
               );
             ELSE
               twbkwbis.p_formselectoption (
                  g$_nls.get ('BWCKCOM1-0144', 'SQL', 'None'),
                  NULL
               );
             END IF;
           END IF;
         ELSE 
         NULL;
       END IF;
       --C3SC STU32 END
      END LOOP rstsrowc_loop;

      CLOSE rstsrowc;

      IF row_count = 0
      THEN
         twbkfrmt.p_tabledata (twbkfrmt.F_FormHidden ('RSTS_IN', NULL));
      ELSE
         HTP.formselectclose;
         twbkfrmt.p_tabledataclose;
      END IF;
   END p_build_action_pulldown;

--------------------------------------------------------------------------
   PROCEDURE p_build_wait_action_pulldown (
      term_in               stvterm.stvterm_code%TYPE,
      pidm_in               spriden.spriden_pidm%TYPE,
      crn_in                ssbsect.ssbsect_crn%TYPE,
      wait_count_in         NUMBER,
      ptrm_code_in          ssbsect.ssbsect_ptrm_code%TYPE DEFAULT NULL,
      rsts_in               VARCHAR2 DEFAULT NULL,
      p_start_date_in       sftregs.sftregs_start_date%TYPE DEFAULT NULL,            --C3SC GRS 04/15/2009  ADD
      p_reg_date_in         sftregs.sftregs_rsts_date%TYPE DEFAULT NULL,             --C3SC GRS 04/15/2009  ADD
      row_count_out   OUT   NUMBER
   )
   IS
      waitrstsrowc    rstsrowc_typ;
      rsts_row        rstsrowc_rec_typ;
      open_learning   BOOLEAN          := FALSE;
      gtvsdax_row           gtvsdax%ROWTYPE;
      current_stvrsts_row   stvrsts%ROWTYPE;



   BEGIN
      row_count_out := 0;
      p_open_waitrstsrowc (
         waitrstsrowc,
         open_learning,
         term_in,
         pidm_in,
         crn_in,
         ptrm_code_in
      );


      <<waitrstsrowc_loop>>
      LOOP
         FETCH waitrstsrowc INTO rsts_row;
         EXIT waitrstsrowc_loop WHEN waitrstsrowc%NOTFOUND;
--
-- Do not check for ssrrsts usage cutoffs, as the course has never
-- been registered - so usage will always be zero.
-- ===============================================================

         row_count_out := row_count_out + 1;

         IF row_count_out = 1
         THEN
            twbkfrmt.p_tabledataopen;
            twbkfrmt.p_formlabel (
               g$_nls.get ('BWCKCOM1-0145', 'SQL', 'Action'),
               visible   => 'INVISIBLE',
               idname    => 'waitaction_id' || TO_CHAR (wait_count_in)
            );
            -- C3SC GRS 04/16/2009 ADD 
            IF sv_auth_addcodes_bp.f_is_auth_enabled(p_term_code=>term_in) AND
              sv_auth_addcodes_bp.f_reg_auth_active (p_term_code =>term_in,
                                 p_crn =>crn_in,
                                 p_start_date=>p_start_date_in,
                                 p_reg_date=>p_reg_date_in,
                                 p_check_value=>'CLOSE'
                                 )
            THEN                      
              GOTO next_row;
            END IF;  
            -- C3SC END 
            HTP.formselectopen (
               'RSTS_IN',
               nsize         => 1,
               cattributes   => 'ID="waitaction_id' || TO_CHAR (wait_count_in) ||
                                   '"'
            );

            IF rsts_in IS NULL
            THEN
               twbkwbis.p_formselectoption (
                  g$_nls.get ('BWCKCOM1-0146', 'SQL', 'None'),
                  NULL,
                  'SELECTED'
               );
            ELSE
               twbkwbis.p_formselectoption (
                  g$_nls.get ('BWCKCOM1-0147', 'SQL', 'None'),
                  NULL
               );
            END IF;
         END IF;

         IF rsts_in <> rsts_row.stvrsts_code
         OR rsts_in IS NULL
         THEN
            twbkwbis.p_formselectoption (
               rsts_row.stvrsts_desc,
               rsts_row.stvrsts_code
            );
         ELSE
            twbkwbis.p_formselectoption (
               rsts_row.stvrsts_desc,
               rsts_row.stvrsts_code,
               'SELECTED'
            );
         END IF;

         <<next_row>>
         NULL;
      END LOOP waitrstsrowc_loop;

      CLOSE waitrstsrowc;
-- 8.5.1.1
      IF row_count_out = 0
         THEN
            twbkfrmt.p_tabledataopen;
            twbkfrmt.p_formlabel (
               g$_nls.get ('BWCKCOM1-0148', 'SQL', 'Action'),
               visible   => 'INVISIBLE',
               idname    => 'waitaction_id' || TO_CHAR (wait_count_in)
            );
            HTP.formselectopen (
               'RSTS_IN',
               nsize         => 1,
               cattributes   => 'ID="waitaction_id' || TO_CHAR (wait_count_in) ||
                                   '"'
            );

            twbkwbis.p_formselectoption (
                  g$_nls.get ('BWCKCOM1-0149', 'SQL', 'None'),
                  NULL,
                  'SELECTED'
               );
      END IF;
--
--      IF row_count = 0
--      THEN
--         twbkfrmt.p_tabledata;
--      ELSE
         HTP.formselectclose;
         twbkfrmt.p_tabledataclose;
--      END IF;
   END p_build_wait_action_pulldown;

--
-- This procedure performs group edits for all selected terms
-- =====================================================================
   PROCEDURE p_group_edits (
   term_in                  IN       OWA_UTIL.ident_arr,
   pidm_in                  IN       spriden.spriden_pidm%TYPE,
   etrm_done_in_out         IN OUT   BOOLEAN,
   capp_tech_error_in_out   IN OUT   VARCHAR2,
   drop_problems_in_out     IN OUT   sfkcurs.drop_problems_rec_tabtype,
   drop_failures_in_out     IN OUT   sfkcurs.drop_problems_rec_tabtype
)
IS
   multi_term   BOOLEAN := TRUE;
   clas_code             sgrclsr.sgrclsr_clas_code%TYPE;
BEGIN
   IF term_in.COUNT = 1
   THEN
      multi_term := FALSE;
   END IF;
--
-- Do batch validation on all registration records.
-- ===================================================

   FOR i IN 1 .. term_in.COUNT
   LOOP
      IF multi_term
      THEN
--
-- Get the latest student and registration records.
-- ===================================================
         bwckcoms.p_regs_etrm_chk (
            pidm_in,
            term_in (i),
            clas_code,
            multi_term,
            create_sfbetrm_in   => FALSE
         );
      ELSIF NOT etrm_done_in_out
      THEN
         bwckcoms.p_regs_etrm_chk (
            pidm_in,
            term_in (i),
            clas_code,
            create_sfbetrm_in   => FALSE
         );
         etrm_done_in_out := TRUE;
      END IF;
      bwckregs.p_allcrsechk (
         term_in (i),
         'ADD_DROP',
         capp_tech_error_in_out,
         drop_problems_in_out,
         drop_failures_in_out,
         term_in
      );

      IF capp_tech_error_in_out IS NOT NULL
      THEN
         RETURN;
      END IF;
      -- Check for minimum hours error
      IF NVL (twbkwbis.f_getparam (genpidm, 'ERROR_FLAG'), 'N') = 'M'
      THEN
         sfkmods.p_delete_sftregs_by_pidm_term(genpidm, term_in (i));
         sfkmods.p_insert_sftregs_from_stcr(genpidm, term_in (i),SYSDATE);
      END IF;
   END LOOP;
END p_group_edits;

--
-- This procedure determines how to proceed, based on whether a problems
-- list has been created, and the client defined rule for what to do with
-- the problems list.
-- The rule options are:
--  'C'- The user can confirm/reject problems list related drops
--  'Y' - problems list related drops are automatically dropped
--  'N' - problems list related drops cannot be dropped.
-- ======================================================================
PROCEDURE p_problems (
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
   drop_failures_in   IN   sfkcurs.drop_problems_rec_tabtype
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
   bwcklibs.p_reset_failures (genpidm, drop_failures_in);
--
-- Transfer all changes in SFTREGS to SFRSTCR.
-- =====================================================================
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
   RETURN;
END p_problems;

--
-- This procedure displays the list of 'problems', that is, courses that
-- will be dropped as a result of dropping courses requested by the user.
-- The user can choose to reject or accept these changes.
-- ======================================================================
PROCEDURE p_disp_confirm_drops (
   term_in            IN   OWA_UTIL.ident_arr,
   err_term_in        IN   OWA_UTIL.ident_arr,
   err_crn_in         IN   OWA_UTIL.ident_arr,
   err_subj_in        IN   OWA_UTIL.ident_arr,
   err_crse_in        IN   OWA_UTIL.ident_arr,
   err_sec_in         IN   OWA_UTIL.ident_arr,
   err_code_in        IN   OWA_UTIL.ident_arr,
   err_levl_in        IN   OWA_UTIL.ident_arr,
   err_cred_in        IN   OWA_UTIL.ident_arr,
   err_gmod_in        IN   OWA_UTIL.ident_arr,
   drop_problems_in   IN   sfkcurs.drop_problems_rec_tabtype,
   drop_failures_in   IN   sfkcurs.drop_problems_rec_tabtype
)
IS
   term         stvterm.stvterm_code%TYPE := NULL;
   multi_term   BOOLEAN                   := TRUE;
   i            NUMBER;
BEGIN

--
-- Initialize genpidm based on faculty/student.
-- =================================================

   IF NOT twbkwbis.f_validuser (global_pidm)
   THEN
      RETURN;
   END IF;

   term := term_in (term_in.COUNT);

   IF term_in.COUNT = 1
   THEN
      multi_term := FALSE;
   END IF;

   IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
   THEN
--
-- Start the web page for Faculty.
-- =================================================
      genpidm :=
          TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
      bwckfrmt.p_open_doc (
         'bwlkfrad.p_fac_disp_confirm_drops',
         term,
         NULL,
         multi_term,
         term_in (1)
      );
      twbkwbis.p_dispinfo ('bwlkfrad.p_fac_disp_confirm_drops', 'DEFAULT');
   ELSE
--
-- Start the web page for Student.
-- =================================================
      genpidm := global_pidm;
      bwckfrmt.p_open_doc (
         'bwskfreg.p_disp_confirm_drops',
         term,
         NULL,
         multi_term,
         term_in (1)
      );
      twbkwbis.p_dispinfo ('bwskfreg.p_disp_confirm_drops', 'DEFAULT');
   END IF;

--
-- Initialize variables.
-- =================================================
   bwcklibs.p_initvalue (global_pidm, term, NULL, SYSDATE, NULL, NULL);

--
--  Open Form
-- ==================================================
   HTP.formopen (
      twbkwbis.f_cgibin || 'bwckcoms.p_proc_confirm_drops',
      cattributes   => 'onSubmit="return checkSubmit()"'
   );

--
-- Initialize form variables.
-- ==================================================
   FOR i IN 1 .. term_in.COUNT
   LOOP
      twbkfrmt.P_FormHidden ('term_in', term_in (i));
   END LOOP;

   FOR i IN 1 .. err_term_in.COUNT
   LOOP
      twbkfrmt.P_FormHidden ('err_term_in', err_term_in (i));
      twbkfrmt.P_FormHidden ('err_crn_in', err_crn_in (i));
      twbkfrmt.P_FormHidden ('err_subj_in', err_subj_in (i));
      twbkfrmt.P_FormHidden ('err_crse_in', err_crse_in (i));
      twbkfrmt.P_FormHidden ('err_sec_in', err_sec_in (i));
      twbkfrmt.P_FormHidden ('err_code_in', err_code_in (i));
      twbkfrmt.P_FormHidden ('err_levl_in', err_levl_in (i));
      twbkfrmt.P_FormHidden ('err_cred_in', err_cred_in (i));
      twbkfrmt.P_FormHidden ('err_gmod_in', err_gmod_in (i));
   END LOOP;

   IF NOT drop_problems_in.EXISTS(1)
   THEN
       twbkfrmt.P_FormHidden ('d_p_term_code', 'dummy');
       twbkfrmt.P_FormHidden ('d_p_crn', 'dummy');
       twbkfrmt.P_FormHidden ('d_p_subj', 'dummy');
       twbkfrmt.P_FormHidden ('d_p_crse', 'dummy');
       twbkfrmt.P_FormHidden ('d_p_sec', 'dummy');
       twbkfrmt.P_FormHidden ('d_p_ptrm_code', 'dummy');
       twbkfrmt.P_FormHidden ('d_p_rmsg_cde', 'dummy');
       twbkfrmt.P_FormHidden ('d_p_message', 'dummy');
       twbkfrmt.P_FormHidden ('d_p_start_date', 'dummy');
       twbkfrmt.P_FormHidden ('d_p_comp_date', 'dummy');
       twbkfrmt.P_FormHidden ('d_p_rsts_date', 'dummy');
       twbkfrmt.P_FormHidden ('d_p_dunt_code', 'dummy');
       twbkfrmt.P_FormHidden ('d_p_drop_code', 'dummy');
       twbkfrmt.P_FormHidden ('d_p_drop_conn', 'dummy');
   ELSE
       FOR i IN 1 .. drop_problems_in.COUNT
           LOOP
           twbkfrmt.P_FormHidden ('d_p_term_code', drop_problems_in (i).term_code);
           twbkfrmt.P_FormHidden ('d_p_crn', drop_problems_in (i).crn);
           twbkfrmt.P_FormHidden ('d_p_subj', drop_problems_in (i).subj);
           twbkfrmt.P_FormHidden ('d_p_crse', drop_problems_in (i).crse);
           twbkfrmt.P_FormHidden ('d_p_sec', drop_problems_in (i).sec);
           twbkfrmt.P_FormHidden ('d_p_ptrm_code', drop_problems_in (i).ptrm_code);
           twbkfrmt.P_FormHidden ('d_p_rmsg_cde', drop_problems_in (i).rmsg_cde);
           twbkfrmt.P_FormHidden ('d_p_message', drop_problems_in (i).message);
           twbkfrmt.P_FormHidden ('d_p_start_date', drop_problems_in (i).start_date);
           twbkfrmt.P_FormHidden ('d_p_comp_date', drop_problems_in (i).comp_date);
           twbkfrmt.P_FormHidden ('d_p_rsts_date', drop_problems_in (i).rsts_date);
           twbkfrmt.P_FormHidden ('d_p_dunt_code', drop_problems_in (i).dunt_code);
           twbkfrmt.P_FormHidden ('d_p_drop_code', drop_problems_in (i).drop_code);
           twbkfrmt.P_FormHidden ('d_p_drop_conn', drop_problems_in (i).connected_crns);
           END LOOP;
   END IF;

   IF NOT drop_failures_in.EXISTS(1)
   THEN
       twbkfrmt.P_FormHidden ('d_f_term_code', 'dummy');
       twbkfrmt.P_FormHidden ('d_f_crn', 'dummy');
       twbkfrmt.P_FormHidden ('d_f_subj', 'dummy');
       twbkfrmt.P_FormHidden ('d_f_crse', 'dummy');
       twbkfrmt.P_FormHidden ('d_f_sec', 'dummy');
       twbkfrmt.P_FormHidden ('d_f_ptrm_code', 'dummy');
       twbkfrmt.P_FormHidden ('d_f_rmsg_cde', 'dummy');
       twbkfrmt.P_FormHidden ('d_f_message', 'dummy');
       twbkfrmt.P_FormHidden ('d_f_start_date', 'dummy');
       twbkfrmt.P_FormHidden ('d_f_comp_date', 'dummy');
       twbkfrmt.P_FormHidden ('d_f_rsts_date', 'dummy');
       twbkfrmt.P_FormHidden ('d_f_dunt_code', 'dummy');
       twbkfrmt.P_FormHidden ('d_f_drop_code', 'dummy');
       twbkfrmt.P_FormHidden ('d_f_drop_conn', 'dummy');

   ELSE
       FOR i IN 1 .. drop_failures_in.COUNT
           LOOP
           twbkfrmt.P_FormHidden ('d_f_term_code', drop_failures_in (i).term_code);
           twbkfrmt.P_FormHidden ('d_f_crn', drop_failures_in (i).crn);
           twbkfrmt.P_FormHidden ('d_f_subj', drop_failures_in (i).subj);
           twbkfrmt.P_FormHidden ('d_f_crse', drop_failures_in (i).crse);
           twbkfrmt.P_FormHidden ('d_f_sec', drop_failures_in (i).sec);
           twbkfrmt.P_FormHidden ('d_f_ptrm_code', drop_failures_in (i).ptrm_code);
           twbkfrmt.P_FormHidden ('d_f_rmsg_cde', drop_failures_in (i).rmsg_cde);
           twbkfrmt.P_FormHidden ('d_f_message', drop_failures_in (i).message);
           twbkfrmt.P_FormHidden ('d_f_start_date', drop_failures_in (i).start_date);
           twbkfrmt.P_FormHidden ('d_f_comp_date', drop_failures_in (i).comp_date);
           twbkfrmt.P_FormHidden ('d_f_rsts_date', drop_failures_in (i).rsts_date);
           twbkfrmt.P_FormHidden ('d_f_dunt_code', drop_failures_in (i).dunt_code);
           twbkfrmt.P_FormHidden ('d_f_drop_code', drop_failures_in (i).drop_code);
           twbkfrmt.P_FormHidden ('d_f_drop_conn', drop_failures_in  (i).connected_crns);
           END LOOP;
   END IF;

   twbkfrmt.p_tableopen (
      'DATADISPLAY',
      cattributes   => 'SUMMARY="' ||
                          g$_nls.get ('BWCKCOM1-0150',
                             'SQL',
                             'Class Connections') ||
                          '"',
      ccaption      => g$_nls.get ('BWCKCOM1-0151', 'SQL',
                                   'Classes that will be Dropped')
      );
   twbkfrmt.p_tablerowopen;

   IF multi_term
   THEN
      twbkfrmt.p_tabledataheader (
         g$_nls.get ('BWCKCOM1-0152', 'SQL', 'Associated Term')
      );
   END IF;

   twbkfrmt.p_tabledataheader (
      twbkfrmt.f_printtext (
         '<ACRONYM title = "' ||
            g$_nls.get ('BWCKCOM1-0153', 'SQL', 'Course Reference Number') || '">' ||
            g$_nls.get ('BWCKCOM1-0154', 'SQL', 'CRN') ||
            '</ACRONYM>'
      )
   );
   twbkfrmt.p_tabledataheader (
      twbkfrmt.f_printtext (
         '<ABBR title = ' || g$_nls.get ('BWCKCOM1-0155', 'SQL', 'Subject') ||
         '>' || g$_nls.get ('BWCKCOM1-0156', 'SQL', 'Subj') ||
            '</ABBR>'
      )
   );
   twbkfrmt.p_tabledataheader (
      twbkfrmt.f_printtext (
         '<ABBR title = ' || g$_nls.get ('BWCKCOM1-0157', 'SQL', 'Course') || '>' ||
         g$_nls.get ('BWCKCOM1-0158', 'SQL', 'Crse') ||
            '</ABBR>'
      )
   );
   twbkfrmt.p_tabledataheader (
      twbkfrmt.f_printtext (
         '<ABBR title = ' || g$_nls.get ('BWCKCOM1-0159', 'SQL', 'Section') ||
         '>' || g$_nls.get ('BWCKCOM1-0160', 'SQL', 'Sec') ||
            '</ABBR>'
      )
   );
   twbkfrmt.p_tabledataheader (g$_nls.get ('BWCKCOM1-0161', 'SQL', 'Title'));

   twbkfrmt.p_tabledataheader (g$_nls.get ('BWCKCOM1-0162', 'SQL', 'Registration Issues'));

   twbkfrmt.p_tablerowclose;

   FOR i IN 1 .. drop_problems_in.COUNT
   LOOP
      twbkfrmt.p_tablerowopen;

      IF multi_term
      THEN
         twbkfrmt.p_tabledata (
            bwcklibs.f_term_desc (drop_problems_in (i).term_code)
         );
      END IF;
        
      twbkfrmt.p_tabledata (drop_problems_in (i).crn);
      twbkfrmt.p_tabledata (drop_problems_in (i).subj);
      twbkfrmt.p_tabledata (drop_problems_in (i).crse);
      twbkfrmt.p_tabledata (drop_problems_in (i).sec);
      twbkfrmt.p_tabledata (
         bwcklibs.f_course_title (
            drop_problems_in (i).term_code,
            drop_problems_in (i).crn
         )
      );
      twbkfrmt.p_tabledata (
         bwcklibs.f_connected_crn_message (
            drop_problems_in (i).rmsg_cde,
            drop_problems_in (i).message,
            drop_problems_in (i).connected_crns
         )
      );
      twbkfrmt.p_tablerowclose;
   END LOOP;

   twbkfrmt.p_tableclose;

--
--  Infotext at bottom of page to remind user to press a button
-- ============================================================
   IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
   THEN
      twbkwbis.p_dispinfo ('bwlkfrad.p_fac_disp_confirm_drops', 'REMIND');
   ELSE
      twbkwbis.p_dispinfo ('bwskfreg.p_disp_confirm_drops', 'REMIND');
   END IF;

--
-- Submit Buttons.
-- =================================================
   HTP.br;
   HTP.formsubmit ('submit_btn', g$_nls.get ('BWCKCOM1-0163', 'SQL', 'Drop'));
   HTP.formsubmit ('submit_btn', g$_nls.get ('BWCKCOM1-0164', 'SQL', 'Do not drop'));
   HTP.formclose;
   HTP.br;

   twbkwbis.p_closedoc (curr_release);
END p_disp_confirm_drops;

--
-- This procedure interprets the choice made by the user on whether to
-- reject or accept the 'problem list' drops.
-- =====================================================================
PROCEDURE p_proc_confirm_drops (
   term_in                   IN   OWA_UTIL.ident_arr,
   err_term_in               IN   OWA_UTIL.ident_arr,
   err_crn_in                IN   OWA_UTIL.ident_arr,
   err_subj_in               IN   OWA_UTIL.ident_arr,
   err_crse_in               IN   OWA_UTIL.ident_arr,
   err_sec_in                IN   OWA_UTIL.ident_arr,
   err_code_in               IN   OWA_UTIL.ident_arr,
   err_levl_in               IN   OWA_UTIL.ident_arr,
   err_cred_in               IN   OWA_UTIL.ident_arr,
   err_gmod_in               IN   OWA_UTIL.ident_arr,
   d_p_term_code             IN   OWA_UTIL.ident_arr,
   d_p_crn                   IN   OWA_UTIL.ident_arr,
   d_p_subj                  IN   OWA_UTIL.ident_arr,
   d_p_crse                  IN   OWA_UTIL.ident_arr,
   d_p_sec                   IN   OWA_UTIL.ident_arr,
   d_p_ptrm_code             IN   OWA_UTIL.ident_arr,
   d_p_rmsg_cde              IN   OWA_UTIL.ident_arr,
   d_p_message               IN   OWA_UTIL.ident_arr,
   d_p_start_date            IN   OWA_UTIL.ident_arr,
   d_p_comp_date             IN   OWA_UTIL.ident_arr,
   d_p_rsts_date             IN   OWA_UTIL.ident_arr,
   d_p_dunt_code             IN   OWA_UTIL.ident_arr,
   d_p_drop_code             IN   OWA_UTIL.ident_arr,
   d_p_drop_conn             IN   bwckcoms.varchar2_tabtype,
   d_f_term_code             IN   OWA_UTIL.ident_arr,
   d_f_crn                   IN   OWA_UTIL.ident_arr,
   d_f_subj                  IN   OWA_UTIL.ident_arr,
   d_f_crse                  IN   OWA_UTIL.ident_arr,
   d_f_sec                   IN   OWA_UTIL.ident_arr,
   d_f_ptrm_code             IN   OWA_UTIL.ident_arr,
   d_f_rmsg_cde              IN   OWA_UTIL.ident_arr,
   d_f_message               IN   OWA_UTIL.ident_arr,
   d_f_start_date            IN   OWA_UTIL.ident_arr,
   d_f_comp_date             IN   OWA_UTIL.ident_arr,
   d_f_rsts_date             IN   OWA_UTIL.ident_arr,
   d_f_dunt_code             IN   OWA_UTIL.ident_arr,
   d_f_drop_code             IN   OWA_UTIL.ident_arr,
   d_f_drop_conn             IN   bwckcoms.varchar2_tabtype,
   submit_btn                IN   VARCHAR2
)
IS
   drop_problems   sfkcurs.drop_problems_rec_tabtype;
   drop_failures   sfkcurs.drop_problems_rec_tabtype;
   last_term       stvterm.stvterm_code%type := 'dummy';
   lv_literal      VARCHAR2(30);
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

--
-- Regenerate the problems and failures list arrays from the repeating
-- html form variables created on the display page.
-- =====================================================================
   IF d_p_term_code (1) <> 'dummy'
   THEN
       FOR i IN 1 .. d_p_term_code.COUNT
           LOOP

-- In order to prevent students saving the drop confirm page while
-- registration is open, and re-using the saved page after it has closed
-- =====================================================================
           IF last_term <> d_p_term_code (i) THEN
              IF NOT bwskflib.f_validterm (d_p_term_code (i), stvterm_rec,
                                           sorrtrm_rec)
              THEN
                 twbkwbis.p_dispinfo ('bwskflib.P_SelDefTerm', 'BADTERM');
                 RETURN;
              END IF;
           END IF;
                     
           last_term := d_p_term_code (i);

           drop_problems (i).term_code := d_p_term_code (i);
           drop_problems (i).crn := d_p_crn (i);
           drop_problems (i).subj := d_p_subj (i);
           drop_problems (i).crse := d_p_crse (i);
           drop_problems (i).sec := d_p_sec (i);
           drop_problems (i).ptrm_code := d_p_ptrm_code (i);
           drop_problems (i).rmsg_cde := d_p_rmsg_cde (i);
           drop_problems (i).message := d_p_message (i);
           drop_problems (i).start_date := d_p_start_date (i);
           drop_problems (i).comp_date := d_p_comp_date (i);
           drop_problems (i).rsts_date := d_p_rsts_date (i);
           drop_problems (i).dunt_code := d_p_dunt_code (i);
           drop_problems (i).drop_code := d_p_drop_code (i);
           drop_problems (i).connected_crns := d_p_drop_conn (i);
           END LOOP;
   END IF;

   IF d_f_term_code (1) <> 'dummy'
   THEN
       FOR i IN 1 .. d_f_term_code.COUNT
           LOOP
           drop_failures (i).term_code := d_f_term_code (i);
           drop_failures (i).crn := d_f_crn (i);
           drop_failures (i).subj := d_f_subj (i);
           drop_failures (i).crse := d_f_crse (i);
           drop_failures (i).sec := d_f_sec (i);
           drop_failures (i).ptrm_code := d_f_ptrm_code (i);
           drop_failures (i).rmsg_cde := d_f_rmsg_cde (i);
           drop_failures (i).message := d_f_message (i);
           drop_failures (i).start_date := d_f_start_date (i);
           drop_failures (i).comp_date := d_f_comp_date (i);
           drop_failures (i).rsts_date := d_f_rsts_date (i);
           drop_failures (i).dunt_code := d_f_dunt_code (i);
           drop_failures (i).drop_code := d_f_drop_code (i);
           drop_failures (i).connected_crns := d_f_drop_conn (i);
           END LOOP;
   END IF;

--
-- Failures list courses changes are always rejected.
-- ======================================================================
   bwcklibs.p_reset_failures ( genpidm, drop_failures);

--
-- User accepts problem list drops - so drop them, and then transfer all
-- changes in SFTREGS to SFRSTCR.
-- =====================================================================
   lv_literal := g$_nls.get ('BWCKCOM1-0165', 'SQL', 'Drop');
   IF submit_btn = lv_literal
   THEN
      bwcklibs.p_confirm_drops (genpidm, term_in, drop_problems);
      bwckregs.p_final_updates (
         term_in,
         err_term_in,
         err_crn_in,
         err_subj_in,
         err_crse_in,
         err_sec_in,
         err_code_in,
         err_levl_in,
         err_cred_in,
         err_gmod_in,
         'DROPCONFIRMED',
         drop_problems,
         drop_failures
      );
      RETURN;
   ELSE
--
-- User rejects problem list drops - so cancel ALL drops related to
-- them, and then transfer all remaining changes in SFTREGS to SFRSTCR.
-- =====================================================================
      bwcklibs.p_reset_drops ( genpidm, drop_problems);
      bwckregs.p_final_updates (
         term_in,
         err_term_in,
         err_crn_in,
         err_subj_in,
         err_crse_in,
         err_sec_in,
         err_code_in,
         err_levl_in,
         err_cred_in,
         err_gmod_in,
         'DROPREJECTED',
         drop_problems,
         drop_failures
      );
      RETURN;
   END IF;
END p_proc_confirm_drops;

--
-- This procedure opens the html display table and generates the column
-- headings, for the problems and/or failures list to be shown when the
-- add/drop page is redisplayed.
-- =====================================================================
PROCEDURE p_drop_problems_table_open (multi_term_in IN BOOLEAN DEFAULT FALSE)
IS
BEGIN

    twbkfrmt.p_printmessage (
       g$_nls.get ('BWCKCOM1-0166', 'SQL', 'Registration Update Errors'),
       'ERROR'
    );

   twbkfrmt.p_tableopen (
      'DATADISPLAY',
      cattributes   => 'SUMMARY="' ||
                          g$_nls.get ('BWCKCOM1-0167',
                             'SQL',
                             'This layout table is used to present Drop or Withdrawal Errors') ||
                          '."'
   );
   twbkfrmt.p_tablerowopen;

   IF multi_term_in
   THEN
      twbkfrmt.p_tabledataheader (
         g$_nls.get ('BWCKCOM1-0168', 'SQL', 'Associated Term')
      );
   END IF;

   twbkfrmt.p_tabledataheader (
      twbkfrmt.f_printtext (
         '<ACRONYM title = "' ||
            g$_nls.get ('BWCKCOM1-0169', 'SQL', 'Course Reference Number') ||
            '">' ||
            g$_nls.get ('BWCKCOM1-0170', 'SQL', 'CRN') ||
            '</ACRONYM>'
      )
   );
   twbkfrmt.p_tabledataheader (
      twbkfrmt.f_printtext (
         '<ABBR title = ' || g$_nls.get ('BWCKCOM1-0171', 'SQL', 'Subject') ||
         '>' || g$_nls.get ('BWCKCOM1-0172', 'SQL', 'Subj') ||
            '</ABBR>'
      )
   );
   twbkfrmt.p_tabledataheader (
      twbkfrmt.f_printtext (
         '<ABBR title = ' || g$_nls.get ('BWCKCOM1-0173', 'SQL', 'Course') ||
         '>' || g$_nls.get ('BWCKCOM1-0174', 'SQL', 'Crse') ||
            '</ABBR>'
      )
   );
   twbkfrmt.p_tabledataheader (
      twbkfrmt.f_printtext (
         '<ABBR title = ' || g$_nls.get ('BWCKCOM1-0175', 'SQL', 'Section') ||
         '>' || g$_nls.get ('BWCKCOM1-0176', 'SQL', 'Sec') ||
            '</ABBR>'
      )
   );
   twbkfrmt.p_tabledataheader (g$_nls.get ('BWCKCOM1-0177', 'SQL', 'Status'));
   twbkfrmt.p_tablerowclose;
END p_drop_problems_table_open;

--
-- This procedure displays the problems list - shown when the add/drop
-- page is redisplayed.
-- =====================================================================
PROCEDURE p_drop_problems_list (
   drop_problems_in    IN       sfkcurs.drop_problems_rec_tabtype,
   multi_term_in       IN       BOOLEAN DEFAULT FALSE,
   table_open_in_out   IN OUT   BOOLEAN
)
IS
BEGIN
   IF drop_problems_in.EXISTS (1)
   THEN
      FOR i IN 1 .. drop_problems_in.COUNT
      LOOP
         IF     i = 1
            AND NOT table_open_in_out
         THEN
            bwckcoms.p_drop_problems_table_open (multi_term_in);
            table_open_in_out := TRUE;
         END IF;

         twbkfrmt.p_tablerowopen;

         IF multi_term_in
         THEN
            twbkfrmt.p_tabledata (
               bwcklibs.f_term_desc (drop_problems_in (i).term_code)
            );
         END IF;

         twbkfrmt.p_tabledata (drop_problems_in (i).crn);
         twbkfrmt.p_tabledata (drop_problems_in (i).subj);
         twbkfrmt.p_tabledata (drop_problems_in (i).crse);
         twbkfrmt.p_tabledata (drop_problems_in (i).sec);
         twbkfrmt.p_tabledata (
            bwcklibs.f_connected_crn_message (
               drop_problems_in (i).rmsg_cde,
               drop_problems_in (i).message,
               drop_problems_in (i).connected_crns
            )
         );
         twbkfrmt.p_tablerowclose;
      END LOOP;
   END IF;
END p_drop_problems_list;

--
-- This procedure displays the failures list - shown when the add/drop
-- page is redisplayed.
-- =====================================================================
PROCEDURE p_drop_failures_list (
   drop_failures_in    IN       sfkcurs.drop_problems_rec_tabtype,
   multi_term_in       IN       BOOLEAN DEFAULT FALSE,
   table_open_in_out   IN OUT   BOOLEAN
)
IS
BEGIN
   IF drop_failures_in.EXISTS (1)
   THEN
      FOR i IN 1 .. drop_failures_in.COUNT
      LOOP
         IF     i = 1
            AND NOT table_open_in_out
         THEN
            bwckcoms.p_drop_problems_table_open (multi_term_in);
            table_open_in_out := TRUE;
         END IF;

         twbkfrmt.p_tablerowopen;

         IF multi_term_in
         THEN
            twbkfrmt.p_tabledata (
               bwcklibs.f_term_desc (drop_failures_in (i).term_code)
            );
         END IF;

         twbkfrmt.p_tabledata (drop_failures_in (i).crn);
         twbkfrmt.p_tabledata (drop_failures_in (i).subj);
         twbkfrmt.p_tabledata (drop_failures_in (i).crse);
         twbkfrmt.p_tabledata (drop_failures_in (i).sec);
         twbkfrmt.p_tabledata (
            bwcklibs.f_connected_crn_message (
               drop_failures_in (i).rmsg_cde,
               drop_failures_in (i).message,
               drop_failures_in (i).connected_crns
            )
         );
         twbkfrmt.p_tablerowclose;
      END LOOP;
   END IF;
END p_drop_failures_list;

-- Study Paths Changes
-- Procedure which will be used to display the available study paths and select one
-- It will be stored in the global parameter G_SPATH
PROCEDURE P_SelStudyPath(term_in           stvterm.stvterm_code%TYPE DEFAULT NULL,
                         calling_proc_name IN VARCHAR2 DEFAULT NULL,
                         st_path_req       IN VARCHAR2 DEFAULT NULL,
                         MSG               IN VARCHAR2 DEFAULT NULL,
                         p_term_in         IN OWA_UTIL.ident_arr,
                         assoc_term_in     IN OWA_UTIL.ident_arr,
                         sel_crn           IN OWA_UTIL.ident_arr,
                         rsts              IN OWA_UTIL.ident_arr,
                         crn               IN OWA_UTIL.ident_arr,
                         start_date_in     IN OWA_UTIL.ident_arr,
                         end_date_in       IN OWA_UTIL.ident_arr,
                         subj              IN OWA_UTIL.ident_arr,
                         crse              IN OWA_UTIL.ident_arr,
                         sec               IN OWA_UTIL.ident_arr,
                         levl              IN OWA_UTIL.ident_arr,
                         cred              IN OWA_UTIL.ident_arr,
                         gmod              IN OWA_UTIL.ident_arr,
                         title             IN bwckcoms.varchar2_tabtype,
                         mesg              IN OWA_UTIL.ident_arr,
                         regs_row          NUMBER,
                         add_row           NUMBER,
                         wait_row          NUMBER
                         ) IS
  -- cursor selects all study paths for which the sfrensp records exists and whose status permits registration
  --  plus the study paths for which no sfrensp records exists.
  CURSOR sgvstsp_all_c(pidm SPRIDEN.SPRIDEN_PIDM%TYPE, term_code STVTERM.STVTERM_CODE%TYPE) IS
    SELECT sgvstsp_name, sgvstsp_key_seqno, sgvstsp_term_code_eff
      FROM sovlcur, sgvstsp
     WHERE sgvstsp_pidm = pidm
       AND (sgvstsp_astd_code IS NULL OR EXISTS
            (SELECT 'X'
               FROM stvastd
              WHERE stvastd_code = sgvstsp_astd_code
                AND NVL(stvastd_prevent_reg_ind, 'N') = 'N'))
       AND (sgvstsp_cast_code IS NULL OR EXISTS
            (SELECT 'X'
               FROM stvcast
              WHERE stvcast_code = sgvstsp_cast_code
                AND stvcast_prevent_reg_ind = 'N'))
       AND sgvstsp_term_code_eff =
           (SELECT MAX(m.sgrstsp_term_code_eff)
              FROM sgrstsp m
             WHERE m.sgrstsp_pidm = sgvstsp_pidm
               AND m.sgrstsp_key_seqno = sgvstsp_key_seqno
               AND m.sgrstsp_term_code_eff <= term_code)
       AND sgvstsp_enroll_ind = 'Y'
       AND sovlcur_current_ind = 'Y'
       AND sovlcur_active_ind = 'Y'
       AND sovlcur_pidm = sgvstsp_pidm
       AND sovlcur_key_seqno = sgvstsp_key_seqno
       AND sovlcur_lmod_code = sb_curriculum_str.f_learner
       AND NOT EXISTS (SELECT 'X'
              FROM stvests, sfrensp
             WHERE sfrensp_pidm = sgvstsp_pidm
               AND sfrensp_term_code = term_code
               AND sfrensp_key_seqno = sgvstsp_key_seqno
               AND sfrensp_ests_code = stvests_code
               AND stvests_prev_reg = 'Y')
     order by sgvstsp_term_code_eff, sgvstsp_key_seqno;
  lv_st_path_req   VARCHAR2(1);
  lv_sp_name       VARCHAR2(4000);
  lv_sp_key_seq_no SGRSTSP.SGRSTSP_KEY_SEQNO%TYPE;
  row_count        NUMBER := 0;
  lv_term_in       STVTERM.STVTERM_CODE%TYPE;
  lv_sgvstsp_term  STVTERM.STVTERM_CODE%TYPE;
  lv_crn           OWA_UTIL.ident_arr ;  
BEGIN
  IF NOT twbkwbis.F_ValidUser(global_pidm) THEN
    RETURN;
  END IF;

  IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC' THEN
     genpidm := TO_NUMBER(twbkwbis.F_GetParam (global_pidm, 'STUPIDM'), 99999999);
  ELSE 
     genpidm := global_pidm;
  END IF;
  
  IF term_in IS NULL THEN
    lv_term_in := twbkwbis.f_getparam(global_pidm, 'TERM');
  ELSE
    twbkwbis.p_setparam(global_pidm, 'TERM', term_in);
    lv_term_in := term_in;
  END IF;

  IF lv_term_in IS NULL THEN
    bwskflib.p_seldefterm(lv_term_in, 'bwckcoms.P_SelStudyPath');
    RETURN;
  END IF;
  
  IF st_path_req IS NOT NULL THEN
    lv_st_path_req := st_path_req;
  ELSE
    lv_st_path_req := bwckcoms.F_StudyPathReq(lv_term_in);
  END IF;  
  soklcur.p_create_sotvcur(p_pidm    => genpidm,
                           p_term_code => lv_term_in,
                         p_lmod_code => sb_curriculum_str.f_learner); 

  OPEN sgvstsp_all_c(genpidm, lv_term_in);
  LOOP
    FETCH sgvstsp_all_c
      INTO lv_sp_name, lv_sp_key_seq_no, lv_sgvstsp_term;
    EXIT WHEN sgvstsp_all_c%NOTFOUND;
  
    row_count := row_count + 1;
  
    IF row_count = 1 THEN

    IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC' THEN
           bwckfrmt.p_open_doc('bwckcoms.P_SelStudyPathFac', lv_term_in);                          
    ELSE 
           bwckfrmt.p_open_doc('bwckcoms.P_SelStudyPath', lv_term_in);                          
    END IF;



    
      HTP.formOpen('bwckcoms.P_StoreStudyPath');
-- Store the parameter values for use in next page
      FOR i IN 1 .. p_term_in.COUNT LOOP
        twbkfrmt.p_formhidden('p_term_in', p_term_in(i));
      END LOOP;
      --twbkfrmt.p_formhidden('sel_crn', 'dummy');
      --FOR i IN NVL(regs_row,1)+2 .. sel_crn.COUNT LOOP
      FOR i IN 1 .. sel_crn.COUNT LOOP       
        twbkfrmt.p_formhidden('sel_crn', sel_crn(i));
      END LOOP;
      FOR i IN 1 .. assoc_term_in.COUNT LOOP
        twbkfrmt.p_formhidden('assoc_term_in', assoc_term_in(i));
      END LOOP;
      
      IF regs_row = 0 THEN
        FOR i IN 1 .. 1 LOOP
          twbkfrmt.p_formhidden('crn', crn(i));
          lv_crn(1) := crn(i);
        END LOOP;
      ELSE
        FOR i IN 1 .. NVL(regs_row,1) LOOP
          twbkfrmt.p_formhidden('crn', crn(i));
          lv_crn(1) := crn(i);
        END LOOP;
      END IF;
      FOR i IN 1 .. start_date_in.COUNT LOOP
        twbkfrmt.p_formhidden('start_date_in', start_date_in(i));
      END LOOP;
      FOR i IN 1 .. end_date_in.COUNT LOOP
        twbkfrmt.p_formhidden('end_date_in', end_date_in(i));
      END LOOP;
      FOR i IN 1 .. rsts.COUNT LOOP
        twbkfrmt.p_formhidden('rsts', rsts(i));
      END LOOP;
      FOR i IN 1 .. subj.COUNT LOOP
        twbkfrmt.p_formhidden('subj', subj(i));
      END LOOP;
      FOR i IN 1 .. crse.COUNT LOOP
        twbkfrmt.p_formhidden('crse', crse(i));
      END LOOP;
      FOR i IN 1 .. sec.COUNT LOOP
        twbkfrmt.p_formhidden('sec', sec(i));
      END LOOP;
      FOR i IN 1 .. levl.COUNT LOOP
        twbkfrmt.p_formhidden('levl', levl(i));
      END LOOP;
      FOR i IN 1 .. cred.COUNT LOOP
        twbkfrmt.p_formhidden('cred', cred(i));
      END LOOP;
      FOR i IN 1 .. gmod.COUNT LOOP
        twbkfrmt.p_formhidden('gmod', gmod(i));
      END LOOP;
      FOR i IN 1 .. title.COUNT LOOP
        twbkfrmt.p_formhidden('title', title(i));
      END LOOP;
      FOR i IN 1 .. mesg.COUNT LOOP
        twbkfrmt.p_formhidden('mesg', mesg(i));
      END LOOP;
      
        twbkfrmt.p_formhidden('add_row', add_row);
      
      
        twbkfrmt.p_formhidden('wait_row', wait_row);
      
      
        twbkfrmt.p_formhidden('regs_row', regs_row);
      IF calling_proc_name = 'bwskfreg.p_adddropcrse' THEN
        --SELECT 1  INTO lv_st_path_req FROM DUAL  where 1=2;
        null;
      END IF;
      

-- Store the parameter values for use in next page
      IF lv_st_path_req = 'Y' THEN
        twbkwbis.p_dispinfo('bwckcoms.P_SelStudyPath', 'REQUIRED');
      ELSE
        twbkwbis.p_dispinfo('bwckcoms.P_SelStudyPath', 'OPTIONAL');
      END IF;
    
      IF MSG = 'REQ_ERROR' THEN
      
        twbkwbis.p_dispinfo('bwckcoms.P_SelStudyPath', 'REQ_ERROR');
      
      END IF;
    
      twbkfrmt.P_TableOpen('DATAENTRY',
                           cattributes => 'SUMMARY="' ||
                                          G$_NLS.Get('BWCKCOM1-0178',
                                                     'SQL',
                                                     'This table allows
                                        the user to select a
                                       valid study path for
                                       registration
                                       processing') || '."');
    
      twbkfrmt.p_tablerowopen;
      twbkfrmt.p_tabledataopen;
      twbkfrmt.P_FormHidden('calling_proc_name', calling_proc_name);
      twbkfrmt.P_FormHidden('st_path_req', lv_st_path_req);
      twbkfrmt.p_tabledataclose;
      twbkfrmt.p_tablerowclose;
    
      twbkfrmt.p_tablerowopen();
      twbkfrmt.p_tabledataopen;
    
      twbkfrmt.p_formlabel(g$_nls.get('BWCKCOM1-0179',
                                      'SQL',
                                      'Select a Study Path:'));
      htp.formSelectOpen('st_path',
                         NULL,
                         1,
                         cattributes => 'ID="st_path_id"');
      twbkwbis.P_FormSelectOption(G$_NLS.Get('BWCKCOM1-0180',
                                             'SQL',
                                             'None'),
                                  NULL);
    END IF;
    twbkwbis.P_FormSelectOption(G$_NLS.FormatMsg('BWGKCCR1-0044',
                                           'SQL',
                                           lv_sp_name),
                                lv_sp_key_seq_no || '|' || lv_sgvstsp_term);
  END LOOP;
  CLOSE sgvstsp_all_c;

  IF row_count = 0 THEN
    IF lv_st_path_req = 'Y' THEN
      bwckfrmt.p_open_doc('bwckcoms.P_SelStudyPath', lv_term_in);
      twbkwbis.p_dispinfo('bwckcoms.P_SelStudyPath', 'REQUIRED');
      twbkwbis.p_dispinfo('bwckcoms.P_SelStudyPath', 'NO_SPATH');
      twbkwbis.P_CloseDoc(curr_release);
      RETURN;
    ELSE
      
      P_StoreStudyPath(NULL,
                           calling_proc_name,
                           'N',
                           p_term_in         ,
                           assoc_term_in     ,
                           sel_crn           ,
                           rsts              ,
                           lv_crn               ,
                           start_date_in     ,
                           end_date_in       ,
                           subj              ,
                           crse              ,
                           sec               ,
                           levl              ,
                           cred              ,
                           gmod              ,
                           title             ,
                           mesg              ,
                           regs_row          ,
                           add_row           ,
                           wait_row          );
      RETURN;
    END IF;
  END IF;

  htp.formSelectClose;

  twbkfrmt.p_tabledataclose;
  twbkfrmt.p_tablerowclose;

  twbkfrmt.p_tablerowopen();
  twbkfrmt.p_tabledataseparator(' ', ccolspan => 11);
  twbkfrmt.p_tablerowclose;
  twbkfrmt.P_TableClose;
  HTP.formsubmit(NULL, G$_NLS.Get('BWCKCOM1-0181', 'SQL', 'Submit'));

  HTP.formClose;
  twbkwbis.P_CloseDoc(curr_release);

END P_SelStudyPath;

      
PROCEDURE P_StoreStudyPath(st_path           VARCHAR2 DEFAULT NULL,
                           calling_proc_name VARCHAR2 DEFAULT NULL,
                           st_path_req       VARCHAR2 DEFAULT NULL,
                           p_term_in         IN OWA_UTIL.ident_arr,
                           assoc_term_in     IN OWA_UTIL.ident_arr,
                           sel_crn           IN OWA_UTIL.ident_arr,
                           rsts              IN OWA_UTIL.ident_arr,
                           crn               IN OWA_UTIL.ident_arr,
                           start_date_in     IN OWA_UTIL.ident_arr,
                           end_date_in       IN OWA_UTIL.ident_arr,
                           subj              IN OWA_UTIL.ident_arr,
                           crse              IN OWA_UTIL.ident_arr,
                           sec               IN OWA_UTIL.ident_arr,
                           levl              IN OWA_UTIL.ident_arr,
                           cred              IN OWA_UTIL.ident_arr,
                           gmod              IN OWA_UTIL.ident_arr,
                           title             IN bwckcoms.varchar2_tabtype,
                           mesg              IN OWA_UTIL.ident_arr,
                           regs_row          NUMBER,
                           add_row           NUMBER,
                           wait_row          NUMBER)         IS
  lv_count            NUMBER := 0;
  lv_term_arr         OWA_UTIL.ident_arr;
  lv_sel_crn         OWA_UTIL.ident_arr;
  stupidm_encoded     VARCHAR2(1000);
  hold_stupidm_char   VARCHAR2(30);
  lv_term             stvterm.stvterm_code%TYPE;
  lv_stupin           VARCHAR2(30);
  cnt                 NUMBER;
BEGIN
  IF NOT twbkwbis.f_validuser (global_pidm) 
  THEN
    RETURN;
  END IF;
  
  IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC' THEN
     genpidm := TO_NUMBER(twbkwbis.F_GetParam (global_pidm, 'STUPIDM'), 99999999);
  ELSE 
     genpidm := global_pidm;
  END IF;

  lv_term_arr(1) := twbkwbis.f_getparam (global_pidm, 'TERM');
  IF st_path_req = 'Y' AND st_path IS NULL THEN
    bwckcoms.P_SelStudyPath(NULL,
                            calling_proc_name,
                            st_path_req,
                            'REQ_ERROR',
                            p_term_in,
                            assoc_term_in,
                            sel_crn,                            
                            rsts,
                            crn,                            
                            start_date_in,
                            end_date_in,                            
                            subj,
                            crse,
                            sec,
                            levl,
                            cred,
                            gmod,
                            title,
                            mesg,
                            regs_row,
                            add_row,
                            wait_row);
    RETURN;
  END IF;
  
  lv_sel_crn(1) := 'dummy';
  cnt:=1;
  FOR i IN NVL(regs_row,1)+2 .. sel_crn.COUNT LOOP  
    cnt:= cnt+1;
    lv_sel_crn(cnt) := sel_crn(i);
  END LOOP;
  
  twbkwbis.p_setparam (global_pidm, 'G_SP_TERM',twbkwbis.f_getparam (global_pidm, 'TERM') );    
  twbkwbis.p_setparam (global_pidm, 'G_SPATH', st_path);  
  twbkwbis.p_setparam (global_pidm, 'G_SP_SET', 'Y');        
  twbkwbis.p_setparam (global_pidm, 'G_FROM_SP', 'Y');        -- tells whether you came from this page. will be first deleted in the successive page
     
  IF calling_proc_name = 'bwskfreg.p_adddropcrse' THEN
    bwskfreg.p_adddropcrse(NULL);
  ELSIF calling_proc_name = 'bwskfreg.p_adddropcrseCRN' OR
        calling_proc_name = 'bwlkfrad.P_FacAddDropCrseCRN' THEN  
    
    bwckcoms.p_adddrop1 (
            p_term_in,
            lv_sel_crn,
            assoc_term_in,
            crn,
            start_date_in,
            end_date_in,
            rsts,
            subj,
            crse,
            sec,
            levl,
            cred,
            gmod,
            title,
            mesg,
            regs_row,
            add_row,
            wait_row
         );  

  ELSIF calling_proc_name = 'P_CrseSearch' THEN
    bwskfcls.P_CrseSearch (lv_term_arr, FALSE);  
  ELSIF calling_proc_name = 'bwlkfrad.P_FacAddDropCrse' THEN
    lv_term := twbkwbis.f_getparam (global_pidm, 'TERM');
    lv_stupin := twbkwbis.f_getparam (global_pidm, 'STUPIN');
    hold_stupidm_char := twbkwbis.f_getparam (global_pidm, 'STUPIDM');
    stupidm_encoded := twbkbssf.f_encode(hold_stupidm_char);
    bwlkfrad.P_FacAddDropCrse(lv_term, stupidm_encoded,lv_stupin);
  ELSIF calling_proc_name = 'bwlkffcs.P_FacCrseSearch' THEN
    lv_stupin := twbkwbis.f_getparam (global_pidm, 'STUPIN');
    hold_stupidm_char := twbkwbis.f_getparam (global_pidm, 'STUPIDM');
    stupidm_encoded := twbkbssf.f_encode(hold_stupidm_char);
    bwlkffcs.P_FacCrseSearch(lv_term_arr, FALSE, stupidm_encoded, lv_stupin);
  END IF;
  
END P_StoreStudyPath;                            


--  Procedure which will delete all the global parameters related to studypath
PROCEDURE P_DeleteStudyPath
IS
BEGIN
  IF NOT twbkwbis.f_validuser (global_pidm)
  THEN
    RETURN;
  END IF;
  
  IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC' THEN
     genpidm := TO_NUMBER(twbkwbis.F_GetParam (global_pidm, 'STUPIDM'), 99999999);
  ELSE 
     genpidm := global_pidm;
  END IF;
  twbkmods.p_delete_twgrwprm_pidm_name (global_pidm, 'G_SPATH');    
  twbkmods.p_delete_twgrwprm_pidm_name (global_pidm, 'G_SP_TERM');
  twbkmods.p_delete_twgrwprm_pidm_name (global_pidm, 'G_SP_SET');    
  twbkmods.p_delete_twgrwprm_pidm_name (global_pidm, 'G_FROM_SP');    
END P_DeleteStudyPath;    

-- Procedure which will create SFRENSP for the studypath if one doesnt exist already
PROCEDURE p_regs_ensp_chk( p_pidm              SPRIDEN.SPRIDEN_PIDM%TYPE,
                           p_sp_seq_no         SFRENSP.SFRENSP_KEY_SEQNO%TYPE,
                           p_term_code     STVTERM.STVTERM_CODE%TYPE,
                           create_sfrensp      BOOLEAN                          DEFAULT TRUE)  
IS
  lv_rowid            gb_common.internal_record_id_type;
  lv_etrm_ests_code   STVESTS.STVESTS_CODE%TYPE;
  CURSOR get_sfbetrm_ests_c IS
  SELECT SFBETRM_ESTS_CODE
    FROM SFBETRM
   WHERE SFBETRM_PIDM = p_pidm
     AND SFBETRM_TERM_CODE = p_term_code;
BEGIN
    IF p_sp_seq_no IS NULL THEN
      RETURN;
    END IF;
    
  IF sb_studypath_enrollment.f_exists(p_pidm,p_term_code,p_sp_seq_no) = 'Y' THEN
    NULL;
  ELSE
    IF create_sfrensp THEN

      OPEN get_sfbetrm_ests_c;
      FETCH get_sfbetrm_ests_c INTO lv_etrm_ests_code;
      CLOSE get_sfbetrm_ests_c;
      
      sb_studypath_enrollment.p_create(
        p_term_code   => p_term_code,
        p_pidm        => p_pidm,
        p_key_seqno   => p_sp_seq_no,
        p_ests_code   => lv_etrm_ests_code,
        p_ests_date   => SYSDATE,  
        p_add_date    => SYSDATE,  
        p_user        => goksels.f_get_ssb_id_context,
        p_data_origin => gb_common.data_origin, 
        p_rowid_out   => lv_rowid); 
      
      COMMIT;
    END IF;
  END IF;
                
END p_regs_ensp_chk;

-- Procedure which will display the crns selected from MULTIPLE terms
-- and allow you to select a dropdown for each one of the terms.
-- Called from p_addfromsearch
PROCEDURE P_SelMultiStudyPath
  (   
      term_in         IN   OWA_UTIL.ident_arr,
      assoc_term_in   IN   OWA_UTIL.ident_arr,
      sel_crn         IN   OWA_UTIL.ident_arr,
      rsts            IN   OWA_UTIL.ident_arr,
      crn             IN   OWA_UTIL.ident_arr,
      start_date_in   IN   OWA_UTIL.ident_arr,
      end_date_in     IN   OWA_UTIL.ident_arr,
      subj            IN   OWA_UTIL.ident_arr,
      crse            IN   OWA_UTIL.ident_arr,
      sec             IN   OWA_UTIL.ident_arr,
      levl            IN   OWA_UTIL.ident_arr,
      cred            IN   OWA_UTIL.ident_arr,
      gmod            IN   OWA_UTIL.ident_arr,
      title           IN   bwckcoms.varchar2_tabtype,
      mesg            IN   OWA_UTIL.ident_arr,
      regs_row             NUMBER,
      add_row              NUMBER,
      wait_row             NUMBER,
      add_btn         IN   OWA_UTIL.ident_arr,
      MSG             IN VARCHAR2 DEFAULT NULL ,
      calling_proc_name IN VARCHAR2 DEFAULT NULL
  ) IS
  print_header BOOLEAN := TRUE;
  old_term     stvterm.stvterm_code%TYPE := NULL;
  lv_reg       BOOLEAN := FALSE;
  CURSOR sgvstsp_all_c(pidm spriden.spriden_pidm%TYPE, term_code stvterm.stvterm_code%TYPE) IS
    SELECT sgvstsp_name, sgvstsp_key_seqno, sgvstsp_term_code_eff
      FROM sovlcur, sgvstsp
     WHERE sgvstsp_pidm = pidm
       AND (sgvstsp_astd_code IS NULL OR EXISTS
            (SELECT 'X'
               FROM stvastd
              WHERE stvastd_code = sgvstsp_astd_code
                AND nvl(stvastd_prevent_reg_ind, 'N') = 'N'))
       AND (sgvstsp_cast_code IS NULL OR EXISTS
            (SELECT 'X'
               FROM stvcast
              WHERE stvcast_code = sgvstsp_cast_code
                AND stvcast_prevent_reg_ind = 'N'))
       AND sgvstsp_term_code_eff =
           (SELECT MAX(m.sgrstsp_term_code_eff)
              FROM sgrstsp m
             WHERE m.sgrstsp_pidm = sgvstsp_pidm
               AND m.sgrstsp_key_seqno = sgvstsp_key_seqno
               AND m.sgrstsp_term_code_eff <= term_code)
       AND sgvstsp_enroll_ind = 'Y'
       AND sovlcur_current_ind = 'Y'
       AND sovlcur_active_ind = 'Y'
       AND sovlcur_pidm = sgvstsp_pidm
       AND sovlcur_key_seqno = sgvstsp_key_seqno
       AND sovlcur_lmod_code = sb_curriculum_str.f_learner
       AND NOT EXISTS (SELECT 'X'
              FROM stvests, sfrensp
             WHERE sfrensp_pidm = sgvstsp_pidm
               AND sfrensp_term_code = term_code
               AND sfrensp_key_seqno = sgvstsp_key_seqno
               AND sfrensp_ests_code = stvests_code
               AND stvests_prev_reg = 'Y')
     ORDER BY sgvstsp_term_code_eff, sgvstsp_key_seqno;

CURSOR section_info_c (p_term_code STVTERM.STVTERM_CODE%TYPE,
                       p_crn       SSBSECT.SSBSECT_CRN%TYPE)
    IS
SELECT SSBSECT_SUBJ_CODE,
       SSBSECT_CRSE_NUMB,
       SSBSECT_SEQ_NUMB,
       SSBSECT_CAMP_CODE,
       NVL(TO_CHAR(SSBSECT_CREDIT_HRS, '9990D990'),
           DECODE(scbcrse_credit_hr_ind,
                  'TO',
                  NVL(LTRIM(TO_CHAR(scbcrse_credit_hr_low,
                                    '9990D990')),
                      TO_CHAR(0, '0D99')) || '-' ||
                  LTRIM(TO_CHAR(scbcrse_credit_hr_high, '9990D990')),
                  'OR',
                  NVL(LTRIM(TO_CHAR(scbcrse_credit_hr_low,
                                    '9990D990')),
                      TO_CHAR(0, '0D99')) || '/' ||
                  LTRIM(TO_CHAR(scbcrse_credit_hr_high, '9990D990')),
                  TO_CHAR(scbcrse_credit_hr_low, '9990D990'))) SSBSECT_CREDIT_HRS,
       bwcklibs.f_course_title(SSBSECT_TERM_CODE, SSBSECT_CRN) TITLE
  FROM SSBSECT, SCBCRSE
 WHERE SSBSECT_TERM_CODE = p_term_code
   AND SSBSECT_CRN = p_crn
   AND SSBSECT_SUBJ_CODE = SCBCRSE_SUBJ_CODE
   AND SSBSECT_CRSE_NUMB = SCBCRSE_CRSE_NUMB
   AND SCBCRSE_EFF_TERM =
       (SELECT MAX(SCBCRSE_EFF_TERM)
          FROM SCBCRSE
         WHERE SCBCRSE_SUBJ_CODE = SSBSECT_SUBJ_CODE
           AND SCBCRSE_CRSE_NUMB = SSBSECT_CRSE_NUMB
           AND SCBCRSE_EFF_TERM <= p_term_code);

  lv_sp_name       VARCHAR2(4000);
  lv_sp_key_seq_no sgrstsp.sgrstsp_key_seqno%TYPE;
  lv_sgvstsp_term  stvterm.stvterm_code%TYPE;
  row_count        NUMBER := 0;
  st_path_req      VARCHAR2(1);
  section_info_rec   section_info_c%ROWTYPE;

BEGIN

  IF NOT twbkwbis.f_validuser(global_pidm) THEN
    RETURN;
  END IF;
  IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC' THEN
     genpidm := TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
  ELSE
     genpidm := global_pidm;
  END IF;
  
  
  IF sel_crn.COUNT < 2 THEN   -- If no CRN has been selected in the previous page go to add drop page.
  
    twbkwbis.p_setparam (genpidm, 'G_FROM_MSP', 'Y');    
    bwckcoms.p_addfromsearch(term_in,
                    assoc_term_in,
                    sel_crn,
                    add_btn);
                    
    RETURN;                    
    
  END IF;
  
  bwckfrmt.p_open_doc('bwckcoms.P_SelMultiStudyPath');
  htp.formopen('bwckcoms.P_StoreMultiStudyPath');
  twbkfrmt.p_formhidden('calling_proc_name', calling_proc_name);
  twbkfrmt.P_FormHidden ('p_st_path', 'DUMMY');
  FOR i IN 1 .. term_in.COUNT LOOP
    twbkfrmt.p_formhidden('p_term_in', term_in(i));
  END LOOP;
  FOR i IN 1 .. assoc_term_in.COUNT LOOP
    twbkfrmt.p_formhidden('p_assoc_term_in', assoc_term_in(i));
  END LOOP;
    
  FOR i IN 1 .. rsts.COUNT LOOP
    twbkfrmt.p_formhidden('p_rsts', rsts(i));
  END LOOP;
  FOR i IN 1 .. crn.COUNT LOOP
    twbkfrmt.p_formhidden('p_crn', crn(i));
  END LOOP;
  FOR i IN 1 .. start_date_in.COUNT LOOP
    twbkfrmt.p_formhidden('p_start_date_in', start_date_in(i));
  END LOOP;
  FOR i IN 1 .. end_date_in.COUNT LOOP
    twbkfrmt.p_formhidden('p_end_date_in', end_date_in(i));
  END LOOP;
    FOR i IN 1 .. subj.COUNT LOOP
    twbkfrmt.p_formhidden('p_subj', subj(i));
  END LOOP;
  FOR i IN 1 .. crse.COUNT LOOP
    twbkfrmt.p_formhidden('p_crse', crse(i));
  END LOOP;
  FOR i IN 1 .. sec.COUNT LOOP
    twbkfrmt.p_formhidden('p_sec', sec(i));
  END LOOP;
    FOR i IN 1 .. levl.COUNT LOOP
    twbkfrmt.p_formhidden('p_levl', levl(i));
  END LOOP;
    FOR i IN 1 .. cred.COUNT LOOP
    twbkfrmt.p_formhidden('p_cred', cred(i));
  END LOOP;
  FOR i IN 1 .. gmod.COUNT LOOP
    twbkfrmt.p_formhidden('p_gmod', gmod(i));
  END LOOP;
  FOR i IN 1 .. title.COUNT LOOP
    twbkfrmt.p_formhidden('p_title', title(i));
  END LOOP;
  FOR i IN 1 .. mesg.COUNT LOOP
    twbkfrmt.p_formhidden('p_mesg', mesg(i));
  END LOOP;
  twbkfrmt.p_formhidden('p_sel_crn', 'dummy');
  twbkfrmt.p_formhidden('p_regs_row', regs_row);
  twbkfrmt.p_formhidden('p_add_row', add_row);
  twbkfrmt.p_formhidden('p_wait_row', wait_row);
  
  
  --SELECT 1 INTO row_count FROM DUAL WHERE 1 = 2;
    
  FOR i IN 1 .. add_btn.COUNT LOOP
    twbkfrmt.p_formhidden('p_add_btn', add_btn(i));
  END LOOP;
  
  FOR i IN 2 .. sel_crn.COUNT LOOP
    BEGIN
    
      IF old_term = f_trim_sel_crn_term(sel_crn(i)) THEN

        
        print_header := FALSE;
      ELSE
        lv_reg := FALSE;
        IF i > 2 THEN
          twbkfrmt.p_tableclose; -- Close the CRNs table after each term.
        END IF;
        print_header := TRUE;
        old_term     := f_trim_sel_crn_term(sel_crn(i));
      END IF;

      IF print_header THEN

        twbkfrmt.p_tableopen;
        twbkfrmt.p_tablerowopen;
        twbkfrmt.p_tabledatalabel(twbkfrmt.f_printtext(gb_stvterm.f_get_description(old_term),
                                                       class_in => 'fieldOrangetextbold'));

        twbkfrmt.p_tablerowopen;
        twbkfrmt.p_tableclose;

        soklcur.p_create_sotvcur(p_pidm      => genpidm,
                                 p_term_code => old_term,
                                 p_lmod_code => sb_curriculum_str.f_learner);

        OPEN sgvstsp_all_c(genpidm, old_term);
        row_count := 0;
        LOOP

          FETCH sgvstsp_all_c
            INTO lv_sp_name, lv_sp_key_seq_no, lv_sgvstsp_term;
          EXIT WHEN sgvstsp_all_c%NOTFOUND;

          row_count := row_count + 1;

          IF row_count = 1 THEN
            lv_reg := TRUE;
            st_path_req := bwckcoms.f_studypathreq(old_term);
            IF st_path_req = 'Y' THEN
                twbkwbis.p_dispinfo('bwckcoms.P_SelStudyPath', 'REQUIRED');
            ELSE
              twbkwbis.p_dispinfo('bwckcoms.P_SelStudyPath', 'OPTIONAL');
            END IF;
         
          IF twbkwbis.f_getparam(global_pidm, 'G_SP' || old_term) =
                   'NULL'  AND  bwckcoms.f_studypathreq(old_term) = 'Y' THEN
          
                  twbkwbis.p_dispinfo('bwckcoms.P_SelStudyPath', 'REQ_ERROR');
          END IF;
            twbkfrmt.p_tableopen('DATAENTRY',
                                 cattributes => 'SUMMARY="' ||
                                                g$_nls.get('BWCKCOM1-0182',
                                                           'SQL',
                                                           'This table allows
                                        the user to select a
                                       valid study path for
                                       registration
                                       processing') || '."');

            twbkfrmt.p_tablerowopen();
            twbkfrmt.p_tabledataopen;

            twbkfrmt.p_formlabel(g$_nls.get('BWCKCOM1-0183',
                                            'SQL',
                                            'Select a Study Path:'));
            htp.formselectopen('p_st_path',
                               NULL,
                               1,
                               cattributes => 'ID="st_path_id"');
            IF msg IS NOT NULL THEN
              IF twbkwbis.f_getparam(global_pidm, 'G_SP' || old_term) =
                 'NULL' THEN
                twbkwbis.p_formselectoption(g$_nls.get('BWCKCOM1-0184',
                                                       'SQL',
                                                       'None'),
                                            '|' || old_term,
                                            'SELECTED');
              ELSE
                twbkwbis.p_formselectoption(g$_nls.get('BWCKCOM1-0185',
                                                       'SQL',
                                                       'None'),
                                            '|' || old_term);
              END IF;
            ELSE
              twbkwbis.p_formselectoption(g$_nls.get('BWCKCOM1-0186',
                                                     'SQL',
                                                     'None'),
                                          '|' || old_term);
            END IF;
          END IF;

          IF msg IS NOT NULL THEN
            IF twbkwbis.f_getparam(global_pidm, 'G_SP' || old_term) =
               to_char(lv_sp_key_seq_no) THEN
              twbkwbis.p_formselectoption(g$_nls.FormatMsg('BWGKCCR1-0044',
                                                     'SQL',
                                                     lv_sp_name),
                                          lv_sp_key_seq_no || '|' ||
                                          old_term,
                                          'SELECTED');
            ELSE
              twbkwbis.p_formselectoption(g$_nls.FormatMsg('BWGKCCR1-0044',
                                                     'SQL',
                                                     lv_sp_name),
                                          lv_sp_key_seq_no || '|' ||
                                          old_term);
            END IF;
          ELSE
            twbkwbis.p_formselectoption(g$_nls.FormatMsg('BWGKCCR1-0044',
                                                   'SQL',
                                                   lv_sp_name),
                                        lv_sp_key_seq_no || '|' || old_term);
          END IF;

        END LOOP;

        CLOSE sgvstsp_all_c;
        
        IF row_count = 0 THEN
            st_path_req := bwckcoms.f_studypathreq(old_term);
            IF st_path_req = 'Y' THEN
                twbkwbis.p_dispinfo('bwckcoms.P_SelMultiStudyPath', 'NO_REG');
            ELSE
                row_count := row_count + 1;
                lv_reg := TRUE;
                twbkfrmt.p_tableopen('DATAENTRY',
                                     cattributes => 'SUMMARY="' ||
                                                    g$_nls.get('BWCKCOM1-0187',
                                                               'SQL',
                                                               'This table allows
                                            the user to select a
                                           valid study path for
                                           registration
                                           processing') || '."');
    
                twbkfrmt.p_tablerowopen();
                twbkfrmt.p_tabledataopen;
    
                twbkfrmt.p_formlabel(g$_nls.get('BWCKCOM1-0188',
                                                'SQL',
                                                'Select a Study Path:'));
                htp.formselectopen('p_st_path',
                                   NULL,
                                   1,
                                   cattributes => 'ID="st_path_id"');
                twbkwbis.p_formselectoption(g$_nls.get('BWCKCOM1-0189',
                                                     'SQL',
                                                     'None'),
                                                     '|' || old_term);                   
            END IF;
        END IF;   
        IF row_count > 0 THEN
          htp.formselectclose;
        END IF;   
        
        twbkmods.p_delete_twgrwprm_pidm_name(global_pidm, 'G_SP' || old_term);
        twbkfrmt.p_tablerowclose;
        twbkfrmt.p_tableclose;
        twbkfrmt.p_tableopen('DATADISPLAY',
                             cattributes   => 'SUMMARY="' ||
                                            g$_nls.get ('BWCKCOM1-0190',
                                               'SQL',
                                               'This table will display the CRNs selected for a term.') ||
                                            '" width="70%"');

        twbkfrmt.p_tablerowopen;

                     twbkfrmt.p_tabledataheader (
                        cvalue =>twbkfrmt.f_printtext (
                           '<ACRONYM title = "' ||
                              g$_nls.get ('BWCKCOM1-0191',
                                 'SQL',
                                 'Course Reference Number') ||
                              '">' ||
                              g$_nls.get ('BWCKCOM1-0192', 'SQL', 'CRN') ||
                              '</ACRONYM>'
                        ),
                        cattributes =>'WIDTH="10%"'
                     );
                     twbkfrmt.p_tabledataheader (
                        cvalue =>twbkfrmt.f_printtext (
                           '<ABBR title = ' ||
                              g$_nls.get ('BWCKCOM1-0193', 'SQL', 'Subject') ||
                              '>' ||
                              g$_nls.get ('BWCKCOM1-0194', 'SQL', 'Subj') ||
                              '</ABBR>'
                        ),
                        cattributes =>'WIDTH="10%"'
                     );
                     twbkfrmt.p_tabledataheader (
                        cvalue =>twbkfrmt.f_printtext (
                           '<ABBR title = ' ||
                              g$_nls.get ('BWCKCOM1-0195', 'SQL', 'Course') ||
                              '>' ||
                              g$_nls.get ('BWCKCOM1-0196', 'SQL', 'Crse') ||
                              '</ABBR>'
                        ),
                        cattributes =>'WIDTH="10%"'
                     );
                     twbkfrmt.p_tabledataheader (
                        cvalue =>twbkfrmt.f_printtext (
                           '<ABBR title = ' ||
                              g$_nls.get ('BWCKCOM1-0197', 'SQL', 'Section') ||
                              '>' ||
                              g$_nls.get ('BWCKCOM1-0198', 'SQL', 'Sec') ||
                              '</ABBR>'
                        ),
                        cattributes =>'WIDTH="10%"'
                     );
                     twbkfrmt.p_tabledataheader (
                        cvalue =>twbkfrmt.f_printtext (
                           '<ABBR title = ' ||
                              g$_nls.get ('BWCKCOM1-0199', 'SQL', 'Campus') ||
                              '>' ||
                              g$_nls.get ('BWCKCOM1-0200', 'SQL', 'Cmp') ||
                              '</ABBR>'
                        ),
                        cattributes =>'WIDTH="10%"'
                     );
                     twbkfrmt.p_tabledataheader (
                        cvalue =>twbkfrmt.f_printtext (
                           '<ABBR title = "' ||
                              g$_nls.get ('BWCKCOM1-0201',
                                 'SQL',
                                 'Credit Hours') ||
                              '">' ||
                              g$_nls.get ('BWCKCOM1-0202', 'SQL', 'Cred') ||
                              '</ABBR>'
                        ),
                        cattributes =>'WIDTH="20%"'
                     );
                     twbkfrmt.p_tabledataheader (
                        cvalue =>g$_nls.get ('BWCKCOM1-0203', 'SQL', 'Title'),
                        cattributes =>'WIDTH="30%"'
                     );
                     
        twbkfrmt.p_tablerowclose;

      END IF;
      IF lv_reg THEN
        twbkfrmt.p_formhidden('p_sel_crn', sel_crn(i));
      END IF;  
      twbkfrmt.p_tablerowopen;
      twbkfrmt.p_tabledata(f_trim_sel_crn(sel_crn(i)));
      section_info_rec := NULL;
      OPEN section_info_c(f_trim_sel_crn_term(sel_crn(i)),f_trim_sel_crn(sel_crn(i)));
      FETCH section_info_c INTO section_info_rec;
      CLOSE section_info_c ;
      twbkfrmt.p_tabledata(section_info_rec.SSBSECT_SUBJ_CODE);
      twbkfrmt.p_tabledata(section_info_rec.SSBSECT_CRSE_NUMB);
      twbkfrmt.p_tabledata(section_info_rec.SSBSECT_SEQ_NUMB);
      twbkfrmt.p_tabledata(section_info_rec.SSBSECT_CAMP_CODE);
      twbkfrmt.p_tabledata(section_info_rec.SSBSECT_CREDIT_HRS);
      twbkfrmt.p_tabledata(section_info_rec.TITLE);

      twbkfrmt.p_tablerowclose;

    END;

  END LOOP;
  twbkfrmt.p_tableclose;
  twbkfrmt.P_FormHidden ('REG_BTN', 'DUMMY');
  HTP.BR;
  

    htp.formsubmit('REG_BTN', g$_nls.get('BWCKCOM1-0204', 'SQL', 'Register'));

  HTP.formsubmit (
            'REG_BTN',
            g$_nls.get ('BWCKCOM1-0205', 'SQL', 'Class Search')
         );
  HTP.BR;
  htp.formclose;
  twbkwbis.p_closedoc (curr_release);
END p_selmultistudypath;


-- Procedure which will store the studypaths selected in global variables
PROCEDURE P_StoreMultiStudyPath  
  ( 
   p_st_path       IN   OWA_UTIL.ident_arr,
   p_term_in       IN OWA_UTIL.ident_arr,  -- the 4 parameters are to preserve these variables.
   p_assoc_term_in IN OWA_UTIL.ident_arr,
   p_sel_crn       IN OWA_UTIL.ident_arr,
   p_rsts            IN   OWA_UTIL.ident_arr,
   p_crn             IN   OWA_UTIL.ident_arr,
   p_start_date_in   IN   OWA_UTIL.ident_arr,
   p_end_date_in     IN   OWA_UTIL.ident_arr,
   p_subj            IN   OWA_UTIL.ident_arr,
   p_crse            IN   OWA_UTIL.ident_arr,
   p_sec             IN   OWA_UTIL.ident_arr,
   p_levl            IN   OWA_UTIL.ident_arr,
   p_cred            IN   OWA_UTIL.ident_arr,
   p_gmod            IN   OWA_UTIL.ident_arr,
   p_title           IN   bwckcoms.varchar2_tabtype,
   p_mesg            IN   OWA_UTIL.ident_arr,
   p_regs_row             NUMBER,
   p_add_row              NUMBER,
   p_wait_row             NUMBER,
   p_add_btn       IN OWA_UTIL.ident_arr,
   calling_proc_name IN VARCHAR2 DEFAULT NULL,
   reg_btn         IN   OWA_UTIL.ident_arr
  ) IS
  lv_error_exists BOOLEAN := FALSE;
  lv_term         stvterm.stvterm_code%TYPE;
  lv_spath        VARCHAR2(100);
  lv_literal      VARCHAR(30);
BEGIN
  IF NOT twbkwbis.f_validuser(global_pidm) THEN
    RETURN;
  END IF;
  --SELECT 1 INTO lv_literal FROM DUAL WHERE 1 = 2;
  lv_literal := g$_nls.get ('BWCKCOM1-0206',
                   'SQL',
                   'Class Search');
  IF LTRIM (RTRIM (reg_btn (2))) = lv_literal
  THEN
         IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') =
                                                                         'FAC'
         THEN
            /* FACWEB */
            bwlkffcs.P_FacCrseSearch(p_term_in);
         ELSE
            /* STUWEB */
            bwskfcls.P_CrseSearch(p_term_in);
         END IF;
         RETURN;
  END IF;
      
  
  FOR i IN 2 .. p_st_path.COUNT LOOP
    BEGIN
      lv_term  := substr(p_st_path(i), instr(p_st_path(i), '|') + 1);
      lv_spath := substr(p_st_path(i), 1, instr(p_st_path(i), '|') - 1);
      twbkwbis.p_setparam(global_pidm,
                          'G_SP' || lv_term,
                          nvl(lv_spath, 'NULL'));
      twbkwbis.p_setparam(global_pidm,
                          'G_FROM_MSP' ,
                          'Y');                           
    
      IF bwckcoms.f_studypathreq(lv_term) = 'Y' AND lv_spath IS NULL THEN
        lv_error_exists := TRUE;
      END IF;
    
    EXCEPTION
      WHEN OTHERS THEN
        htp.print(SQLERRM);
    END;
  END LOOP;

  IF lv_error_exists THEN
    p_selmultistudypath(p_term_in,
                        p_assoc_term_in,
                        p_sel_crn,
                        p_rsts            ,
                                          p_crn             ,
                                          p_start_date_in   ,
                                          p_end_date_in     ,
                                          p_subj            ,
                                          p_crse            ,
                                          p_sec             ,
                                          p_levl            ,
                                          p_cred            ,
                                          p_gmod            ,
                                          p_title           ,
                                          p_mesg            ,
                                          p_regs_row        ,
                                          p_add_row         ,
                                          p_wait_row        ,
                        p_add_btn,
                        'ERROR',
                        calling_proc_name);
    RETURN;
  END IF;
  
  IF (calling_proc_name = 'bwskfreg.P_AddDropCrse') OR
     (calling_proc_name = 'bwlkfrad.P_FacAddDropCrse')
  THEN
    bwckcoms.p_addfromsearch(p_term_in,
                    p_assoc_term_in,
                    p_sel_crn,
                    p_add_btn);
    RETURN;
  ELSIF calling_proc_name =   'bwckcoms.p_addfromsearch1' THEN
    bwckcoms.p_addfromsearch1 (
      p_term_in         ,
      p_assoc_term_in   ,
      p_sel_crn         ,
      p_rsts            ,
      p_crn             ,
      p_start_date_in   ,
      p_end_date_in     ,
      p_subj            ,
      p_crse            ,
      p_sec             ,
      p_levl            ,
      p_cred            ,
      p_gmod            ,
      p_title           ,
      p_mesg            ,
      p_regs_row        ,
      p_add_row         ,
      p_wait_row        ,
      p_add_btn         
   );
  END IF; 

END p_storemultistudypath;  

--  Procedure which will delete all the global parameters related to studypath set for multiple terms
PROCEDURE P_DeleteMultiStudyPath (p_pidm SPRIDEN.SPRIDEN_PIDM%TYPE)
IS
  
BEGIN
    twbkmods.p_del_twgrwprm_pidm_like_name (p_pidm,'G_SP');
    twbkmods.p_delete_twgrwprm_pidm_name (global_pidm, 'G_FROM_MSP');    
END P_DeleteMultiStudyPath;    


BEGIN                                         /* initialization starts here */
   NULL;
END bwckcoms;
