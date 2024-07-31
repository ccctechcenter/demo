create or replace PACKAGE BODY bwskfreg
AS
--AUDIT_TRAIL_MSGKEY_UPDATE
-- PROJECT : MSGKEY
-- MODULE  : BWSKFRE1
-- SOURCE  : enUS
-- TARGET  : I18N
-- DATE    : Mon Dec 29 01:24:20 2014
-- MSGSIGN : #1cb24fe39f96b4fa
--TMI18N.ETR DO NOT CHANGE--
--
-- FILE NAME..: bwskfre1.sql
-- RELEASE....: 8.7.1
-- OBJECT NAME: BWSKFREG
-- PRODUCT....: STUWEB
-- COPYRIGHT..: Copyright 2015 Ellucian Company L.P. and its affiliates.
--
--   curr_release   VARCHAR2 (10)             := '8.7.1';
-- local05
   curr_release   VARCHAR2 (10)             := '8.7.1A';
   global_pidm    spriden.spriden_pidm%TYPE;
   row_count      NUMBER;
   stvterm_rec    stvterm%ROWTYPE;
   sorrtrm_rec    sorrtrm%ROWTYPE;
   apin           sprapin.sprapin_pin%TYPE;
   aterm          stvterm.stvterm_code%TYPE;
   term           stvterm.stvterm_code%TYPE;
   twgbwses_rec   twgbwses%ROWTYPE;
   sprapin_rec    sprapin%ROWTYPE;

-- This procedure determines if alternate PINs are being used for the
-- selected term. This procedure gets called when the user selects the Add/Drop
-- page from the web menu.
-- =========================================================================
   PROCEDURE p_altpin (term_in IN stvterm.stvterm_code%TYPE DEFAULT NULL)
   IS
      pinlen          NUMBER;
      last_access     DATE;
      msg             VARCHAR2 (2000);
      ask_alt_pin     VARCHAR2 (1);
      internal_code   gtvsdax.gtvsdax_internal_code%TYPE;
      gtvsdax_group   gtvsdax.gtvsdax_internal_code_group%TYPE;
  --local05 added following cursor to suport demographic verification
        durtpile Varchar2(1) := '';
        Cursor c_szrdurt Is
            Select 'X'
            From   saturn.szrdurt
            Where  szrdurt_pidm = global_pidm
                   And szrdurt_term_code = term
                   And szrdurt_action_taken = 'V';  --local08
        --END LOCAL05
       --local07
        Cursor c_sgbstdn Is
            Select 'X'
            From   sgbstdn
            Where  sgbstdn_pidm = global_pidm
                   And sgbstdn_term_code_eff = term;
        areuthere Varchar2(1) := '';
        --end local07        

   BEGIN

   -- Validate CHAR/VARCHAR2 post variables
    twbksecr.p_chk_parms_05(term_in);   /*080701-1*/
    
-- Validate the current user.
-- ====================================================
      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN;
      END IF;

--
-- Check for Valid Term
-- =====================================================
      IF term_in IS NOT NULL
      THEN
         IF bwskflib.f_validterm (term_in, stvterm_rec, sorrtrm_rec)
         THEN
            twbkwbis.p_setparam (global_pidm, 'TERM', term_in);
         /* 080700-1 START */
         ELSE
            bwskflib.p_seldefterm (term_in, 'bwskfreg.P_AltPin');
            RETURN;
         /* 080700-1 END */
         END IF;

         term := term_in;
      -- Luminus must always ask for term to be selected
      ELSIF twbkwbis.cp_integrated_mode
      THEN
         bwskflib.p_seldefterm (term, 'bwskfreg.P_AltPin');
         RETURN;
      -- end luminus handler
      ELSE
         term := twbkwbis.f_getparam (global_pidm, 'TERM');

         IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
         THEN
            bwskflib.p_seldefterm (term, 'bwskfreg.P_AltPin');
            RETURN;
         END IF;
      END IF;
-----------------------------------------------------------------------
-- 
      --local09 - do not allow reg if readmit required.
      If pw_register.f_readmit_req(global_pidm, term) = 'N'
      Then
--local05  added logic to determine if student has verified demographics
        Open c_szrdurt;
        Fetch c_szrdurt
            Into durtpile;
        Close c_szrdurt;
        If durtpile Is Null -- They've not been here for this term before.
        Then
            --local07
            Begin
                Open c_sgbstdn;
                Fetch c_sgbstdn
                    Into areuthere;
                Close c_sgbstdn;
                If areuthere Is Null
                Then
                    pw_register.p_calc_primary_camp(global_pidm, term);
                End If;
            End;
            --end local07        
            --local01 removed....
            pw_update_demo.p_start_here(term,
                                        global_pidm); --send to verification package
            Return;
        End If;
        --end of local05 mod
        --==== SIG =======================================================
      End If;
    
--
-- Check if Alternate PIN is used by institution. If not,
-- go directly to add/drop page. If so, continue...
-- =====================================================
      internal_code := 'WEBALTPINA';
      gtvsdax_group := 'WEBREG';
      OPEN sfkcurs.get_gtvsdaxc (internal_code, gtvsdax_group);
      FETCH sfkcurs.get_gtvsdaxc INTO ask_alt_pin;

      IF sfkcurs.get_gtvsdaxc%NOTFOUND
      THEN
         CLOSE sfkcurs.get_gtvsdaxc;
         bwskfreg.p_adddropcrse;
         RETURN;
      END IF;

      CLOSE sfkcurs.get_gtvsdaxc;
--
-- Check if an Alternate PIN exists for the student/term.
-- If not, then go directly to add/drop page. If so, continue...
-- =====================================================
      OPEN sfkcurs.getaltpinc (global_pidm, term);
      FETCH sfkcurs.getaltpinc INTO sprapin_rec;

      IF sfkcurs.getaltpinc%NOTFOUND
      THEN
         CLOSE sfkcurs.getaltpinc;
         bwskfreg.p_adddropcrse;
         RETURN;
      END IF;

      CLOSE sfkcurs.getaltpinc;
--
-- Check if alt pin has been entered already. If so,
-- validate it and go directly to add/drop page. If
-- not, continue...
-- =====================================================
      apin := twbkwbis.f_getparam (global_pidm, 'AP ' || term);
      term := twbkwbis.f_getparam (global_pidm, 'TERM');

      IF apin IS NOT NULL
      THEN
         OPEN sfkcurs.getaltpinc (global_pidm, term, apin);
         FETCH sfkcurs.getaltpinc INTO sprapin_rec;

         IF sfkcurs.getaltpinc%FOUND
         THEN
            CLOSE sfkcurs.getaltpinc;
            bwskfreg.p_adddropcrse;
            RETURN;
         END IF;

         CLOSE sfkcurs.getaltpinc;
      END IF;

--
-- Generate the Alternate PIN form. When user clicks on
-- submit, run the p_checkaltpin procedure.
-- =====================================================
      pinlen := goklibs.f_pinlength;
      bwckfrmt.p_open_doc ('bwskfreg.P_AltPin', term);
      twbkwbis.p_dispinfo ('bwskfreg.P_AltPin');

      IF msg IS NOT NULL
      THEN
         twbkfrmt.p_printtext (msg);
      END IF;

      HTP.formopen (
         twbkwbis.f_cgibin || 'bwskfreg.P_CheckAltPin',
         cattributes   => 'onSubmit="return checkSubmit()"'
      );
      twbkfrmt.p_tableopen (
         'DATAENTRY',
         cattributes   => 'SUMMARY="' ||
                             g$_nls.get ('BWSKFRE1-0000',
                                'SQL',
                                'This table allows the user to enter their alternate PIN for registration processing.') ||
                             '"'
      );
      twbkfrmt.p_tablerowopen;
      twbkfrmt.p_tabledatalabel (
         twbkfrmt.f_formlabel (
            g$_nls.get ('BWSKFRE1-0001', 'SQL', 'Alternate PIN') || ':',
            idname   => 'apin_id'
         )
      );
      twbkfrmt.p_tabledata (
         HTF.formpassword (
            'pin',
            pinlen,
            pinlen,
            cattributes   => 'ID="apin_id"'
         )
      );
      twbkfrmt.p_tablerowclose;
      twbkfrmt.p_tableclose;
      HTP.br;
      HTP.formsubmit (NULL, g$_nls.get ('BWSKFRE1-0002', 'SQL', 'Submit'));
      ----
      HTP.formclose;
      twbkwbis.p_closedoc (curr_release);
      
   /*080701-2 Start*/
    EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = gb_event.APP_ERROR THEN
         twbkfrmt.p_storeapimessages(SQLERRM);
      ELSE
         IF SQLCODE  <= -20000 AND SQLCODE >= -20999 THEN
            twbkfrmt.p_printmessage ( SUBSTR (SQLERRM, INSTR (SQLERRM, ':') + 1),'ERROR');
         ELSE
           twbkfrmt.p_printmessage (G$_NLS.Get ('BWSKFRE1-0003','SQL','Error occurred while processing registration changes.ERROR: %01%',LTRIM(REPLACE( SUBSTR (SQLERRM, INSTR (SQLERRM, ':') + 1),'PL/SQL:'))), 'ERROR');
      END IF;
     END IF;
    /*080701-2 End*/
   END p_altpin;

-- This procedure determines if alternate PINs are being used for the
-- selected term. P_altpin1 gets called when the user has selected
-- Register, Add to Worksheet, or Back to Worksheet from the Lookup Classes
-- to Add Results Page.
-- =========================================================================
   PROCEDURE p_altpin1 (
      term_in         IN   OWA_UTIL.ident_arr,
      assoc_term_in   IN   OWA_UTIL.ident_arr,
      sel_crn         IN   OWA_UTIL.ident_arr,
      add_btn         IN   OWA_UTIL.ident_arr,
      crn   IN   OWA_UTIL.ident_arr,
      rsts   IN   OWA_UTIL.ident_arr
   )
   IS
   
   		   
      pinlen          NUMBER;
      i               NUMBER;
      j               NUMBER;
      last_access     DATE;
      msg             VARCHAR2 (2000);
      ask_alt_pin     VARCHAR2 (1);
      internal_code   gtvsdax.gtvsdax_internal_code%TYPE;
      gtvsdax_group   gtvsdax.gtvsdax_internal_code_group%TYPE;
      altpin_found    BOOLEAN                                  := FALSE;
      hold_sel_term   stvterm.stvterm_code%TYPE;
      sel_term        stvterm.stvterm_code%TYPE;
      sel_term_arr    OWA_UTIL.ident_arr;
      apin_arr        OWA_UTIL.ident_arr;
      missing_apin    BOOLEAN                                  := FALSE;
      term                      stvterm.stvterm_code%TYPE      := NULL;
      multi_term                BOOLEAN                        := TRUE;
      update_term_select_c_rec  soklibs.update_term_select_c%ROWTYPE;
      lv_btn          varchar2(100);


   BEGIN
   
    /*080701-1 Start */
    twbksecr.p_chk_parms_tab_05(term_in, assoc_term_in, sel_crn, add_btn, crn);
    twbksecr.p_chk_parms_tab_05(rsts);
    /*080701-1 End */
    
-- Validate the current user.
-- ====================================================
      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN;
      END IF;
    	   
      term := term_in (term_in.COUNT);

      IF term_in.COUNT = 1
      THEN
         multi_term := FALSE;
      END IF;
  -- 8.5.1
      lv_btn := g$_nls.get ('BWSKFRE1-0004',
                                 'SQL',
--                              'Class Search');
                                 'New Search');
      IF LTRIM (RTRIM (add_btn (2))) = lv_btn
                              
      THEN
          bwskfcls.p_disp_term_date('P_CrseSearch');
         RETURN;
      END IF;

--
-- Check if Alternate PIN is used by institution. If not,
-- go directly to add/drop page. If so, continue...
-- =====================================================
      internal_code := 'WEBALTPINA';
      gtvsdax_group := 'WEBREG';
      OPEN sfkcurs.get_gtvsdaxc (internal_code, gtvsdax_group);
      FETCH sfkcurs.get_gtvsdaxc INTO ask_alt_pin;

      IF sfkcurs.get_gtvsdaxc%NOTFOUND
      THEN
         CLOSE sfkcurs.get_gtvsdaxc;
         bwckcoms.p_addfromsearch (term_in, assoc_term_in, sel_crn, add_btn);
         RETURN;
      END IF;

      CLOSE sfkcurs.get_gtvsdaxc;
--
-- Check if an Alternate PIN exists for the student/term.
-- If not, then go directly to add/drop page. If so, continue...
-- =====================================================

      hold_sel_term := 'dummy';
      j := 1;

      FOR i IN 2 .. sel_crn.COUNT
      LOOP
         sel_term := bwckcoms.f_trim_sel_crn_term (sel_crn (i));

         IF sel_term = hold_sel_term
         THEN
            GOTO end_altpin_loop;
         END IF;

         hold_sel_term := sel_term;
         OPEN sfkcurs.getaltpinc (global_pidm, sel_term);
         FETCH sfkcurs.getaltpinc INTO sprapin_rec;

         IF sfkcurs.getaltpinc%FOUND
         THEN
            altpin_found := TRUE;
            sel_term_arr (j) := sel_term;
            j := j + 1;
         END IF;

         CLOSE sfkcurs.getaltpinc;

         <<end_altpin_loop>>
         NULL;
      END LOOP;
--
-- If no CRNs were selected, check alt PIN requirement for all terms.
-- ==================================================================
      IF sel_crn.COUNT = 1
      THEN
         FOR i IN 1 .. term_in.COUNT
         LOOP

            OPEN soklibs.update_term_select_c(term_in(i));
            FETCH soklibs.update_term_select_c INTO update_term_select_c_rec;
            IF soklibs.update_term_select_c%FOUND
            THEN

               OPEN sfkcurs.getaltpinc (global_pidm, term_in(i));
               FETCH sfkcurs.getaltpinc INTO sprapin_rec;

               IF sfkcurs.getaltpinc%FOUND
               THEN
                  altpin_found := TRUE;
                  sel_term_arr (j) := term_in(i);
                  j := j + 1;
               END IF;
               CLOSE sfkcurs.getaltpinc;

            END IF;
            CLOSE soklibs.update_term_select_c;

         END LOOP;
      END IF;
      --
      IF NOT altpin_found
      THEN
         bwckcoms.p_addfromsearch (term_in, assoc_term_in, sel_crn, add_btn);
         RETURN;
      END IF;

--
-- Check if alt pin has been entered already. If so,
-- validate it and go directly to add/drop page. If
-- not, continue...
-- =====================================================
      missing_apin := TRUE;

      FOR i IN 1 .. sel_term_arr.COUNT
      LOOP
         apin_arr (i) :=
                   twbkwbis.f_getparam (global_pidm, 'AP ' || sel_term_arr (i));

         IF apin_arr (i) IS NOT NULL
         THEN
            OPEN sfkcurs.getaltpinc (
               global_pidm,
               sel_term_arr (i),
               apin_arr (i)
            );
            FETCH sfkcurs.getaltpinc INTO sprapin_rec;

            IF sfkcurs.getaltpinc%FOUND
            THEN
               missing_apin := FALSE;
            ELSE
               missing_apin := TRUE;
               EXIT;
            END IF;

            CLOSE sfkcurs.getaltpinc;
         ELSE
            missing_apin := TRUE;
            EXIT;
         END IF;
      END LOOP;

      FOR i IN 1 .. sel_term_arr.COUNT
      LOOP
         apin_arr (i) :=
                   twbkwbis.f_getparam (global_pidm, 'AP ' || sel_term_arr (i));
      END LOOP;

      aterm := twbkwbis.f_getparam (global_pidm, 'TERM');

      IF NOT missing_apin
      THEN
         bwckcoms.p_addfromsearch (term_in, assoc_term_in, sel_crn, add_btn);
         RETURN;
      END IF;

--
-- Generate the Alternate PIN form. When user clicks on
-- submit, run the p_checkaltpin1 procedure.
-- ======================================================
      pinlen := goklibs.f_pinlength;
      bwckfrmt.p_open_doc ('bwskfreg.P_AltPin', term, NULL, multi_term, term_in (1));
      twbkwbis.p_dispinfo ('bwskfreg.P_AltPin');

      IF msg IS NOT NULL
      THEN
         twbkfrmt.p_printtext (msg);
      END IF;

      HTP.formopen (
         twbkwbis.f_cgibin || 'bwskfreg.P_CheckAltPin1',
         cattributes   => 'onSubmit="return checkSubmit()"'
      );

      FOR i IN 1 .. term_in.COUNT
      LOOP
         twbkfrmt.P_FormHidden ('term_in', term_in (i));
      END LOOP;

      BEGIN
         i := 1;

         WHILE sel_crn (i) IS NOT NULL
         LOOP
            twbkfrmt.P_FormHidden ('assoc_term_in', assoc_term_in (i));
            twbkfrmt.P_FormHidden ('sel_crn', sel_crn (i));
            i := i + 1;
         END LOOP;
      EXCEPTION
         WHEN NO_DATA_FOUND
         THEN
            i := 1;
      END;

      twbkfrmt.P_FormHidden ('add_btn', add_btn (1));
      twbkfrmt.P_FormHidden ('add_btn', add_btn (2));
      twbkfrmt.p_tableopen (
         'DATAENTRY',
         cattributes   => 'SUMMARY="' ||
                             g$_nls.get ('BWSKFRE1-0005',
                                'SQL',
                                'This table allows the user to enter their alternate PIN for registration processing.') ||
                             '"'
      );

      FOR i IN 1 .. sel_term_arr.COUNT
      LOOP
         IF apin_arr (i) IS NULL
         THEN
            twbkfrmt.p_tablerowopen;
            twbkfrmt.p_tabledatalabel (
               twbkfrmt.f_formlabel (
                  g$_nls.get ('BWSKFRE1-0006',
                     'SQL',
                     'Alternate PIN for %01%:',
                     bwcklibs.f_term_desc(sel_term_arr (i))
                  ),
                  idname   => 'apin_id' || i
               )
            );
            twbkfrmt.P_FormHidden ('sel_term_arr', sel_term_arr (i));
            twbkfrmt.p_tabledata (
               HTF.formpassword (
                  'pin',
                  pinlen,
                  pinlen,
                  cattributes   => 'ID="apin_id' || i || '"'
               )
            );
            twbkfrmt.p_tablerowclose;
         END IF;
      END LOOP;

      twbkfrmt.p_tableclose;
      HTP.br;
      HTP.formsubmit (NULL, g$_nls.get ('BWSKFRE1-0007', 'SQL', 'Submit'));
      ----
      HTP.formclose;
      twbkwbis.p_closedoc (curr_release);
   END p_altpin1;

-- =========================================================================
-- This procedure checks the validity of an entered alternate PIN
-- =========================================================================
   PROCEDURE p_checkaltpin (pin IN VARCHAR2 DEFAULT NULL)
   IS
      pinlen        NUMBER;
      last_access   DATE;
      msg           VARCHAR2 (2000);
      term          OWA_UTIL.ident_arr;
   BEGIN
--
-- Validate the current user.
-- ====================================================

   -- Validate CHAR/VARCHAR2 post variables
       twbksecr.p_chk_parms_05(pin);    /*080701-1*/
    
      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN;
      END IF;

      pinlen := goklibs.f_pinlength;
      term (1) := twbkwbis.f_getparam (global_pidm, 'TERM');
      OPEN sfkcurs.getaltpinc (global_pidm, term (1), pin);
      FETCH sfkcurs.getaltpinc INTO sprapin_rec;

      IF    sfkcurs.getaltpinc%NOTFOUND
         OR (pin IS NULL)
      THEN
         OPEN twbklibs.getsessionc (global_pidm);
         FETCH twbklibs.getsessionc INTO twgbwses_rec;
         CLOSE twbklibs.getsessionc;

         /* Determine if login attempts are exceeded */
         IF twgbwses_rec.twgbwses_login_attempts < 2
         THEN
            /* Increment login attempt counter */
            UPDATE twgbwses
               SET twgbwses_login_attempts =
                      twgbwses_rec.twgbwses_login_attempts + 1
             WHERE twgbwses_pidm = global_pidm;

            COMMIT;
         ELSE
            bwckfrmt.p_open_doc ('bwskfreg.P_CheckAltPin', term (1));
            twbkwbis.p_dispinfo ('bwskfreg.P_AltPin','ALTPINMAX');
            CLOSE sfkcurs.getaltpinc;                                  -- CLF
            twbkwbis.p_closedoc (curr_release);
            RETURN;
         END IF;

         bwckfrmt.p_open_doc ('bwskfreg.P_CheckAltPin', term (1));
         twbkwbis.p_dispinfo ('bwskfreg.P_AltPin');
         twbkwbis.p_dispinfo ('bwskfreg.P_AltPin','ALTPINERROR');
         HTP.formopen (
            twbkwbis.f_cgibin || 'bwskfreg.P_CheckAltPin',
            cattributes   => 'onSubmit="return checkSubmit()"'
         );
         twbkfrmt.p_tableopen (
            'DATAENTRY',
            cattributes   => 'SUMMARY="' ||
                                g$_nls.get ('BWSKFRE1-0008',
                                   'SQL',
                                   'Alternate PIN Data Entry') ||
                                '"'
         );
         twbkfrmt.p_tablerowopen;
         twbkfrmt.p_tabledatalabel (
            twbkfrmt.f_formlabel (
               g$_nls.get ('BWSKFRE1-0009', 'SQL', 'Alternate PIN') || ':',
               idname   => 'apin_id'
            )
         );
         twbkfrmt.p_tabledata (
            HTF.formpassword (
               'pin',
               pinlen,
               pinlen,
               cattributes   => 'ID="apin_id"'
            )
         );
         twbkfrmt.p_tablerowclose;
         twbkfrmt.p_tableclose;
         HTP.br;
         HTP.formsubmit (NULL, g$_nls.get ('BWSKFRE1-0010', 'SQL', 'Submit'));
         ----
         HTP.formclose;
         twbkwbis.p_closedoc (curr_release);
         CLOSE sfkcurs.getaltpinc;
         RETURN;
      ELSE
         UPDATE twgbwses
            SET twgbwses_login_attempts = 0
          WHERE twgbwses_pidm = global_pidm;

         COMMIT;
         twbkwbis.p_setparam (global_pidm, 'AP ' || term (1), pin);
         CLOSE sfkcurs.getaltpinc;
         bwskfreg.p_adddropcrse;
         RETURN;
      END IF;
   END p_checkaltpin;

-- =========================================================================
--
--
   PROCEDURE p_checkaltpin1 (
      term_in         IN   OWA_UTIL.ident_arr,
      assoc_term_in   IN   OWA_UTIL.ident_arr,
      sel_crn         IN   OWA_UTIL.ident_arr,
      add_btn         IN   OWA_UTIL.ident_arr,
      sel_term_arr    IN   OWA_UTIL.ident_arr,
      pin             IN   OWA_UTIL.ident_arr
   )
   IS
      pinlen        NUMBER;
      i             NUMBER;
      last_access   DATE;
      msg           VARCHAR2 (2000);
      missing_pin   BOOLEAN                   := FALSE;
      failed_term   stvterm.stvterm_code%TYPE;
      default_pin   VARCHAR2 (20);
   BEGIN
    
    /*080701-1 Start */
    -- Validate PL/SQL TABLE post variables
    twbksecr.p_chk_parms_tab_05(term_in, assoc_term_in, sel_crn, add_btn, sel_term_arr);
    twbksecr.p_chk_parms_tab_05(pin);
    /*080701-1 End */
    
-- Validate the current user.
-- ====================================================
      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN;
      END IF;

      pinlen := goklibs.f_pinlength;

      FOR i IN 1 .. pin.COUNT
      LOOP
         OPEN sfkcurs.getaltpinc (global_pidm, sel_term_arr (i), pin (i));
         FETCH sfkcurs.getaltpinc INTO sprapin_rec;

         IF    sfkcurs.getaltpinc%NOTFOUND
            OR (pin (i) IS NULL)
         THEN
            missing_pin := TRUE;
            failed_term := sel_term_arr (i);
         END IF;

         CLOSE sfkcurs.getaltpinc;
      END LOOP;

      IF missing_pin
      THEN
         OPEN twbklibs.getsessionc (global_pidm);
         FETCH twbklibs.getsessionc INTO twgbwses_rec;
         CLOSE twbklibs.getsessionc;

         /* Determine if login attempts are exceeded */
         IF twgbwses_rec.twgbwses_login_attempts < 2
         THEN
            /* Increment login attempt counter */
            UPDATE twgbwses
               SET twgbwses_login_attempts =
                      twgbwses_rec.twgbwses_login_attempts + 1
             WHERE twgbwses_pidm = global_pidm;

            COMMIT;
         ELSE
            bwckfrmt.p_open_doc ('bwskfreg.P_CheckAltPin', failed_term);
            twbkwbis.p_dispinfo ('bwskfreg.P_AltPin','ALTPINMAX');
            twbkwbis.p_closedoc (curr_release);
            RETURN;
         END IF;

         bwckfrmt.p_open_doc ('bwskfreg.P_CheckAltPin', failed_term);
         twbkwbis.p_dispinfo ('bwskfreg.P_AltPin');
         twbkwbis.p_dispinfo ('bwskfreg.P_AltPin','ALTPINERROR');
         HTP.formopen (
            twbkwbis.f_cgibin || 'bwskfreg.P_CheckAltPin1',
            cattributes   => 'onSubmit="return checkSubmit()"'
         );

         FOR i IN 1 .. term_in.COUNT
         LOOP
            twbkfrmt.P_FormHidden ('term_in', term_in (i));
         END LOOP;

         BEGIN
            i := 1;

            WHILE sel_crn (i) IS NOT NULL
            LOOP
               twbkfrmt.P_FormHidden ('assoc_term_in', assoc_term_in (i));
               twbkfrmt.P_FormHidden ('sel_crn', sel_crn (i));
               i := i + 1;
            END LOOP;
         EXCEPTION
            WHEN NO_DATA_FOUND
            THEN
               i := 1;
         END;

         twbkfrmt.P_FormHidden ('add_btn', add_btn (1));
         twbkfrmt.P_FormHidden ('add_btn', add_btn (2));
         twbkfrmt.p_tableopen (
            'DATAENTRY',
            cattributes   => 'SUMMARY="' ||
                                g$_nls.get ('BWSKFRE1-0011',
                                   'SQL',
                                   'Alternate PIN Data Entry') ||
                                '"'
         );

         FOR i IN 1 .. sel_term_arr.COUNT
         LOOP
            twbkfrmt.p_tablerowopen;
            twbkfrmt.p_tabledatalabel (
               twbkfrmt.f_formlabel (
                  g$_nls.get ('BWSKFRE1-0012',
                     'SQL',
                     'Alternate PIN for %01%:',
                     bwcklibs.f_term_desc(sel_term_arr (i))
                  ),
                  idname   => 'apin_id' || i
               )
            );
            twbkfrmt.P_FormHidden ('sel_term_arr', sel_term_arr (i));

            IF sel_term_arr (i) = failed_term
            THEN
               default_pin := NULL;
            ELSE
               default_pin := pin (i);
            END IF;

            twbkfrmt.p_tabledata (
               HTF.formpassword (
                  'pin',
                  pinlen,
                  pinlen,
                  default_pin,
                  cattributes   => 'ID="apin_id' || i || '"'
               )
            );
            twbkfrmt.p_tablerowclose;
         END LOOP;

         twbkfrmt.p_tableclose;
         HTP.br;
         HTP.formsubmit (NULL, g$_nls.get ('BWSKFRE1-0013', 'SQL', 'Submit'));
         ----
         HTP.formclose;
         twbkwbis.p_closedoc (curr_release);
         RETURN;
      ELSE
         UPDATE twgbwses
            SET twgbwses_login_attempts = 0
          WHERE twgbwses_pidm = global_pidm;

         COMMIT;

         FOR i IN 1 .. sel_term_arr.COUNT
         LOOP
            twbkwbis.p_setparam (
               global_pidm,
               'AP ' || sel_term_arr (i),
               pin (i)
            );
         END LOOP;

         bwckcoms.p_addfromsearch (term_in, assoc_term_in, sel_crn, add_btn);
         RETURN;
      END IF;
   END p_checkaltpin1;

-- =========================================================================
-- This procedure displays the add/drop page.
-- This procedure is called when "Add/Drop Classes" is selected from the
-- registration menu.
-- =========================================================================
   PROCEDURE p_adddropcrse (term_in IN stvterm.stvterm_code%TYPE DEFAULT NULL)
   IS
      errs_count           NUMBER;
      regs_count           NUMBER;
      wait_count           NUMBER;
      term                 OWA_UTIL.ident_arr;
      reg_access_allowed   BOOLEAN            := TRUE;
      drop_problems        sfkcurs.drop_problems_rec_tabtype;
      drop_failures        sfkcurs.drop_problems_rec_tabtype;
      capp_tech_error      VARCHAR2 (4) := NULL;
      minh_admin_error     VARCHAR2 (1) := 'N';
      
      --  Study path Changes      
      lv_st_path_req  VARCHAR2(1);
      p_sp_dummy             OWA_UTIL.ident_arr;
      title          bwckcoms.varchar2_tabtype;
      sobctrl_row            sobctrl%ROWTYPE;
      CURSOR sobctrl_c IS
      select * from sobctrl;

   BEGIN

   -- Validate CHAR/VARCHAR2 post variables
    twbksecr.p_chk_parms_05(term_in);   /*080701-1*/
    
-- Validate the current user.
-- ====================================================
      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN;
      END IF;
      
      p_sp_dummy(1):= 'dummy';
      title(1) := 'dummy';
--
--
-- ====================================================
      twbkwbis.p_setparam (global_pidm, 'STUFAC_IND', 'STU');

--
-- Use the term that was passed in and update the parameter
-- table (gorwprm) for the current user. Or use the existing
-- term in the parameter table.
-- ====================================================
      IF term_in IS NOT NULL
      THEN
         twbkwbis.p_setparam (global_pidm, 'TERM', term_in);
         term (1) := term_in;
      ELSE
         term (1) := twbkwbis.f_getparam (global_pidm, 'TERM');
      END IF;

--
-- Validate the term. If not valid, prompt for a new one.
-- ====================================================
      IF NOT bwskflib.f_validterm (term (1), stvterm_rec, sorrtrm_rec)
      THEN
         bwskflib.p_seldefterm (term (1), 'bwskfreg.P_AddDropCrse');
         RETURN;
      END IF;

      IF NOT bwcksams.F_RegsStu (
            global_pidm,
            term (1),
            'bwskfreg.P_AddDropCrse'
         )
      THEN
         RETURN;
      END IF;

--
-- Check if the current user is eligible to register for
-- the current term.
-- ====================================================
      IF NOT sfkvars.regs_allowed
      THEN
         bwckfrmt.p_open_doc ('bwskfreg.P_AddDropCrse', term (1));
         twbkfrmt.p_printmessage (
            g$_nls.get ('BWSKFRE1-0014',
               'SQL',
               'Registration is not allowed at this time'),
            'ERROR'
         );
         twbkwbis.p_closedoc (curr_release);
         RETURN;
      END IF;

--
-- Check if registration is allowed at the current time.
-- ====================================================
      IF NOT sfkvars.add_allowed
      THEN
         SELECT NVL (COUNT (*), 0)
           INTO row_count
           FROM sfrstcr
          WHERE sfrstcr_pidm = global_pidm
            AND sfrstcr_term_code = term (1);

         IF row_count = 0
         THEN
            bwckfrmt.p_open_doc ('bwskfreg.P_AddDropCrse', term (1));
            twbkfrmt.p_printmessage (
               g$_nls.get ('BWSKFRE1-0015',
                  'SQL',
                  'Registration is not allowed at this time'),
               'ERROR'
            );
            twbkwbis.p_closedoc (curr_release);
            RETURN;
         END IF;
      END IF;
--  Study Path Changes
--  Redirect to studypaths select page if studypaths are enabled
  OPEN sobctrl_c;
  FETCH sobctrl_c into sobctrl_row;
  CLOSE sobctrl_c;
  
  lv_st_path_req := bwckcoms.F_StudyPathReq(term (1));
  IF sobctrl_row.SOBCTRL_STUDY_PATH_IND = 'Y' THEN
    IF (twbkwbis.f_getparam (global_pidm, 'G_FROM_SP') IS NULL) THEN
      bwckcoms.P_DeleteStudyPath ;
      
          bwckcoms.P_SelStudyPath(term_in,
                            'bwskfreg.p_adddropcrse',
                            lv_st_path_req,
                            NULL,
                            p_sp_dummy,
                            p_sp_dummy,
                            p_sp_dummy,
                            p_sp_dummy,
                            p_sp_dummy,
                            p_sp_dummy,
                            p_sp_dummy,
                            p_sp_dummy,
                            p_sp_dummy,
                            p_sp_dummy,
                            p_sp_dummy,
                            p_sp_dummy,
                            p_sp_dummy,
                            title,
                            p_sp_dummy,
                            NULL,
                            NULL,
                            NULL);

      RETURN;
    ELSE
      twbkmods.p_delete_twgrwprm_pidm_name (global_pidm, 'G_FROM_SP');	
    END IF;
  END IF;

--  Study Path Changes

--
-- Display the current registration information.
-- ====================================================
      reg_access_allowed := TRUE;
      bwcksams.P_RegsResult (
         term,
         errs_count,
         regs_count,
         wait_count,
         TRUE,
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

      IF reg_access_allowed = FALSE
      THEN
         minh_admin_error := sfkfunc.f_get_minh_admin_error(global_pidm, term(1));
         IF minh_admin_error = 'Y' THEN
            bwckfrmt.p_open_doc ('bwskfreg.P_AddDropCrse', term (1));
            twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'SEE_ADMIN');
            twbkwbis.p_closedoc (curr_release);
            RETURN;
         ELSE      
            bwckfrmt.p_open_doc ('bwskfreg.P_AddDropCrse', term (1));
            twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'SESSIONBLOCKED');
            twbkwbis.p_closedoc (curr_release);
            RETURN;
         END IF; 
      END IF;

      IF    errs_count > 0
         OR wait_count > 0
      THEN
         twbkfrmt.p_tableclose;
         HTP.br;
      END IF;

--
-- Display the add table.
-- ====================================================
      bwckcoms.p_add_drop_crn1;
--
-- Keep track of counts. Setup buttons.
-- ====================================================
      bwckcoms.p_add_drop_crn2 (10, regs_count, wait_count);
      HTP.formclose;
      twbkwbis.p_closedoc (curr_release);
   END p_adddropcrse;

--
-- =========================================================================
-- This procedure displays the Change Course Options page
-- =========================================================================
   PROCEDURE P_ChangeCrseOpt (term_in IN stvterm.stvterm_code%TYPE
            DEFAULT NULL)
   IS
      crn        OWA_UTIL.ident_arr;
      cred       OWA_UTIL.ident_arr;
      gmod       OWA_UTIL.ident_arr;
      levl       OWA_UTIL.ident_arr;
      err_code   OWA_UTIL.ident_arr;
      err_msg    twbklibs.error_msg_tabtype;
      term       stvterm.stvterm_code%TYPE;
      spath      OWA_UTIL.ident_arr;  
   BEGIN
   
      -- Validate CHAR/VARCHAR2 post variables
       twbksecr.p_chk_parms_05(term_in);   /*080701-1*/
       
      IF NOT twbkwbis.f_validuser (global_pidm)
      THEN
         RETURN;
      END IF;

      twbkwbis.p_setparam (global_pidm, 'STUFAC_IND', 'STU');

      IF term_in IS NOT NULL
      THEN
         twbkwbis.p_setparam (global_pidm, 'TERM', term_in);
         term := term_in;
      -- Luminus must always ask for term to be selected
      ELSIF twbkwbis.cp_integrated_mode
      THEN
         bwskflib.p_seldefterm (term, 'bwskfreg.P_ChangeCrseOpt');
         RETURN;
      -- end luminus handler
      ELSE
         term := twbkwbis.f_getparam (global_pidm, 'TERM');
      END IF;

      IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
      THEN
         bwskflib.p_seldefterm (term, 'bwskfreg.P_ChangeCrseOpt');
         RETURN;
      END IF;

      IF NOT bwcksams.F_RegsStu (global_pidm, term, 'bwskfreg.P_ChangeCrseOpt')
      THEN
         RETURN;
      END IF;

      crn (1) := 'dummy';
      cred (1) := 'dummy';
      gmod (1) := 'dummy';
      levl (1) := 'dummy';
      err_code (1) := 'dummy';
      err_msg (1) := 'dummy';
      spath (1) := 'dummy';       -- SP changes
      bwcksams.P_DispChgCrseOpt (
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
   /*080701-2 Start*/
    EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = gb_event.APP_ERROR THEN
         twbkfrmt.p_storeapimessages(SQLERRM);
      ELSE
         IF SQLCODE  <= -20000 AND SQLCODE >= -20999 THEN
            twbkfrmt.p_printmessage ( SUBSTR (SQLERRM, INSTR (SQLERRM, ':') + 1),'ERROR');
         ELSE
           twbkfrmt.p_printmessage (G$_NLS.Get ('BWSKFRE1-0016','SQL','Error occurred while displaying change class options.ERROR: %01%',LTRIM(REPLACE( SUBSTR (SQLERRM, INSTR (SQLERRM, ':') + 1),'PL/SQL:'))), 'ERROR');
      END IF;
     END IF;
    /*080701-2 End*/
   END P_ChangeCrseOpt;
END bwskfreg;
