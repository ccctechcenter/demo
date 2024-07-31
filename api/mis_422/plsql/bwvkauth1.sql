create or replace PACKAGE BODY bwvkauth
AS
--
-- FILE NAME..: bwvkaut1.sql
-- RELEASE....: 8.8 C3SC
-- OBJECT NAME: BWVKAUTH
-- PRODUCT....: SCOMWEB  - California Community College Solution Center (C3SC)
-- USAGE......: Registration Add Authorization Page
-- COPYRIGHT..: Copyright 2006 - 2014 Ellucian Company L.P. and its affiliates.

--
-- DESCRIPTION:
--
-- This is a description of what this object does.

   curr_release   CONSTANT VARCHAR2 (30)             := '8.8 C3SC';
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
   cancel_btn     CONSTANT VARCHAR2(20):= g$_nls.get('X','SQL','Cancel');
   subm_btn       CONSTANT VARCHAR2(50):= g$_nls.get('X','SQL','Submit Changes');
   validate_btn   CONSTANT VARCHAR2(50):= g$_nls.get('X','SQL','Validate');
   temp_drop_problems     sfkcurs.drop_problems_rec_tabtype;
   temp_drop_failures     sfkcurs.drop_problems_rec_tabtype;
   sfbetrm_row             sfbetrm%ROWTYPE;
   sgrclsr_clas_code       stvclas.stvclas_code%TYPE;
   sgbstdn_row             sgbstdn%ROWTYPE;
   atmp_hrs                VARCHAR2 (1);
   clas_desc               stvclas.stvclas_desc%TYPE;
   /*C3SC 8.7.1.1 GRS 04/OCT/2013 ADDED */
   --Internal procedure to get the override data
  PROCEDURE p_getoverride;

PROCEDURE P_DispAutCode (term_in           IN   OWA_UTIL.ident_arr,
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
                         drop_failures_in     IN sfkcurs.drop_problems_rec_tabtype,
                         auth_code_in         IN OWA_UTIL.ident_arr,
                         auth_msg_code        IN OWA_UTIL.ident_arr,
                         num_attempts_in         IN OWA_UTIL.ident_arr,
                         add_row          NUMBER,
                         orig_crn           IN    OWA_UTIL.ident_arr,
                         orig_rsts          IN    OWA_UTIL.ident_arr
                        )
IS
   j NUMBER:=0;
   term                        stvterm.stvterm_code%TYPE              := NULL;
   multi_term                  BOOLEAN                                := TRUE;
   crse_title                  ssrsyln.ssrsyln_long_course_title%TYPE;
   errs_count                  NUMBER:=0;
   lv_authmsg_ref              sv_regauth_msgs.regauth_msgs_ref;
   lv_authmsg_rec              sv_regauth_msgs.regauth_msgs_rec;
   message                     gb_common_strings.err_type;
   status                      sv_auth_addcodes_strings.error_status_type;
   lv_course_auth_status_ref   sv_auth_addcodes_bp.course_auth_status_ref;
   lv_auth_status_rec          sv_auth_addcodes_bp.course_auth_status_rec;
   lv_section_ref              sb_section.section_ref;
   lv_section_rec              sb_section.section_rec;
   show_sub_btn                VARCHAR2(1):='N';
   temp_auth_code             svbauth.svbauth_auth_code%TYPE;
   temp_num_attempts           NUMBER;
   lv_new_class                VARCHAR2(1);

BEGIN
  IF NOT twbkwbis.f_validuser (global_pidm)
  THEN
    RETURN;
  END IF;

  IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
  THEN
    genpidm :=TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
  ELSE
    genpidm := global_pidm;
  END IF;

  term := term_in (term_in.COUNT);

  IF term_in.COUNT = 1
  THEN
    multi_term := FALSE;
  END IF;

--
-- Open doc and pass the term description to be printed
-- at top of page.
-- ==================================================
  bwckfrmt.p_open_doc ('bwvkauth.P_DispAutCode',
                        term,
                        NULL,
                        multi_term,
                        term_in (1)
                         );
  HTP.br;



--
-- Start a form.
-- ==================================================
  HTP.formopen (twbkwbis.f_cgibin || 'bwvkauth.P_ProcAuthCode',
                cattributes   =>'onSubmit="return checkSubmit()"'
                );
  /*HTP.formopen (twbkwbis.f_cgibin || 'bwvkauth.P_ProcAuthCode','POST');*/

   twbkfrmt.P_FormHidden('add_row',add_row);
  --Internal Defect: 1943 STU16:GRS 30/06/2006
  FOR i IN 1 .. orig_crn.count
  LOOP
    twbkfrmt.P_FormHidden ('orig_crn',orig_crn(i));
    twbkfrmt.P_FormHidden ('orig_rsts',orig_rsts(i));
  END LOOP;

   FOR i IN 1 .. term_in.COUNT
   LOOP
      twbkfrmt.P_FormHidden ('term_in', term_in (i));
   END LOOP;


  FOR i IN 1 .. err_term.COUNT
  LOOP
   twbkfrmt.P_FormHidden ('err_term', err_term (i));
   twbkfrmt.P_FormHidden ('err_crn', err_crn (i));
   twbkfrmt.P_FormHidden ('err_subj', err_subj (i));
   twbkfrmt.P_FormHidden ('err_crse', err_crse (i));
   twbkfrmt.P_FormHidden ('err_sec', err_sec (i));
   twbkfrmt.P_FormHidden ('err_code', err_code (i));
   twbkfrmt.P_FormHidden ('err_levl', err_levl (i));
   twbkfrmt.P_FormHidden ('err_cred', err_cred (i));
   twbkfrmt.P_FormHidden ('err_gmod', err_gmod (i));
  END LOOP;

   twbkfrmt.P_FormHidden ('capp_tech_error_in_out', capp_tech_error_in_out);
   twbkfrmt.P_FormHidden ('drop_result_label_in', drop_result_label_in);


   FOR i IN 1 .. drop_problems_in.COUNT
   LOOP
     twbkfrmt.P_FormHidden ('drop_problems_term_code',drop_problems_in(i).term_code);
     twbkfrmt.P_FormHidden ('drop_problems_pidm',drop_problems_in(i).pidm);
     twbkfrmt.P_FormHidden ('drop_problems_crn',drop_problems_in(i).crn);
     twbkfrmt.P_FormHidden ('drop_problems_subj',drop_problems_in(i).subj);
     twbkfrmt.P_FormHidden ('drop_problems_crse',drop_problems_in(i).crse);
     twbkfrmt.P_FormHidden ('drop_problems_sec',drop_problems_in(i).sec);
     twbkfrmt.P_FormHidden ('drop_problems_ptrm_code',drop_problems_in(i).ptrm_code);
     twbkfrmt.P_FormHidden ('drop_problems_rmsg_cde',drop_problems_in(i).rmsg_cde);  -- C3SC  GRS 03/25/09 ADD
     twbkfrmt.P_FormHidden ('drop_problems_message',drop_problems_in(i).message);
     twbkfrmt.P_FormHidden ('drop_problems_start_date',drop_problems_in(i).start_date);
     twbkfrmt.P_FormHidden ('drop_problems_comp_date',drop_problems_in(i).comp_date);
     twbkfrmt.P_FormHidden ('drop_problems_rsts_date',drop_problems_in(i).rsts_date);
     twbkfrmt.P_FormHidden ('drop_problems_dunt_code',drop_problems_in(i).dunt_code);
     twbkfrmt.P_FormHidden ('drop_problems_drop_code',drop_problems_in(i).drop_code);
     twbkfrmt.P_FormHidden ('drop_problems_connected_crns',drop_problems_in(i).connected_crns);

   END LOOP;
   IF drop_problems_in.COUNT = 0 THEN
     twbkfrmt.P_FormHidden ('drop_problems_term_code','Dummy');
     twbkfrmt.P_FormHidden ('drop_problems_pidm','Dummy');
     twbkfrmt.P_FormHidden ('drop_problems_crn','Dummy');
     twbkfrmt.P_FormHidden ('drop_problems_subj','Dummy');
     twbkfrmt.P_FormHidden ('drop_problems_crse','Dummy');
     twbkfrmt.P_FormHidden ('drop_problems_sec','Dummy');
     twbkfrmt.P_FormHidden ('drop_problems_ptrm_code','Dummy');
     twbkfrmt.P_FormHidden ('drop_problems_rmsg_cde','Dummy'); 	  -- C3SC  GRS 03/25/09 ADD
     twbkfrmt.P_FormHidden ('drop_problems_message','Dummy');
     twbkfrmt.P_FormHidden ('drop_problems_start_date','Dummy');
     twbkfrmt.P_FormHidden ('drop_problems_comp_date','Dummy');
     twbkfrmt.P_FormHidden ('drop_problems_rsts_date','Dummy');
     twbkfrmt.P_FormHidden ('drop_problems_dunt_code','Dummy');
     twbkfrmt.P_FormHidden ('drop_problems_drop_code','Dummy');
     twbkfrmt.P_FormHidden ('drop_problems_connected_crns','Dummy');
   END IF;
   --Drop failures


   FOR i IN 1 .. drop_failures_in.COUNT
   LOOP
    twbkfrmt.P_FormHidden ('drop_failures_term_code',drop_failures_in(i).term_code);
     twbkfrmt.P_FormHidden ('drop_failures_pidm',drop_failures_in(i).pidm);
     twbkfrmt.P_FormHidden ('drop_failures_crn',drop_failures_in(i).crn);
     twbkfrmt.P_FormHidden ('drop_failures_subj',drop_failures_in(i).subj);
     twbkfrmt.P_FormHidden ('drop_failures_crse',drop_failures_in(i).crse);
     twbkfrmt.P_FormHidden ('drop_failures_sec',drop_failures_in(i).sec);
     twbkfrmt.P_FormHidden ('drop_failures_ptrm_code',drop_failures_in(i).ptrm_code);
     twbkfrmt.P_FormHidden ('drop_failures_rmsg_cde',drop_failures_in(i).rmsg_cde);  -- C3SC  GRS 03/25/09 ADD
     twbkfrmt.P_FormHidden ('drop_failures_message',drop_failures_in(i).message);
     twbkfrmt.P_FormHidden ('drop_failures_start_date',drop_failures_in(i).start_date);
     twbkfrmt.P_FormHidden ('drop_failures_comp_date',drop_failures_in(i).comp_date);
     twbkfrmt.P_FormHidden ('drop_failures_rsts_date',drop_failures_in(i).rsts_date);
     twbkfrmt.P_FormHidden ('drop_failures_dunt_code',drop_failures_in(i).dunt_code);
     twbkfrmt.P_FormHidden ('drop_failures_drop_code',drop_failures_in(i).drop_code);
     twbkfrmt.P_FormHidden ('drop_failures_connected_crns',drop_failures_in(i).connected_crns);
   END LOOP;
   IF drop_failures_in.COUNT = 0 THEN
      twbkfrmt.P_FormHidden ('drop_failures_term_code','Dummy');
     twbkfrmt.P_FormHidden ('drop_failures_pidm','Dummy');
     twbkfrmt.P_FormHidden ('drop_failures_crn','Dummy');
     twbkfrmt.P_FormHidden ('drop_failures_subj','Dummy');
     twbkfrmt.P_FormHidden ('drop_failures_crse','Dummy');
     twbkfrmt.P_FormHidden ('drop_failures_sec','Dummy');
     twbkfrmt.P_FormHidden ('drop_failures_ptrm_code','Dummy');
     twbkfrmt.P_FormHidden ('drop_failures_rmsg_cde','Dummy');  -- C3SC  GRS 03/25/09 ADD
     twbkfrmt.P_FormHidden ('drop_failures_message','Dummy');
     twbkfrmt.P_FormHidden ('drop_failures_start_date','Dummy');
     twbkfrmt.P_FormHidden ('drop_failures_comp_date','Dummy');
     twbkfrmt.P_FormHidden ('drop_failures_rsts_date','Dummy');
     twbkfrmt.P_FormHidden ('drop_failures_dunt_code','Dummy');
     twbkfrmt.P_FormHidden ('drop_failures_drop_code','Dummy');
     twbkfrmt.P_FormHidden ('drop_failures_connected_crns','Dummy');
   END IF;

--
--
-- Print info text.
-- ==================================================
  twbkwbis.p_dispinfo ('bwvkauth.P_DispAutCode', 'DEFAULT');

--
--
-- ==================================================
  bwcklibs.p_initvalue (global_pidm, term, NULL, NULL, NULL, NULL);

  twbkfrmt.p_tableopen ('DATAENTRY',
                        cattributes   => 'SUMMARY="' ||
                             g$_nls.get ('X',
                                         'SQL',
                                         'Classes with Add Auth Code message Related'
                                         ) ||
                                     '"'
                     );
    twbkfrmt.p_tablerowopen;
      twbkfrmt.p_tabledataheader (g$_nls.get ('X', 'SQL', 'Status'));
      twbkfrmt.p_tabledataheader (g$_nls.get ('X', 'SQL', 'Registration Add Auth Code'));
      twbkfrmt.p_tabledataheader (twbkfrmt.f_printtext ('<ACRONYM title = "' ||
                                                        g$_nls.get ('X','SQL','Course Reference Number') ||
                                                        '">' ||
                                                        g$_nls.get ('X', 'SQL', 'CRN') ||
                                                        '</ACRONYM>'
                                                       )
                                  );
      twbkfrmt.p_tabledataheader (twbkfrmt.f_printtext ('<ABBR title = ' ||
                                                        g$_nls.get ('X','SQL','Subject') ||
                                                        '>' ||
                                                        g$_nls.get ('X', 'SQL', 'Subj') ||
                                                       '</ABBR>'
                                                        )
                                    );
      twbkfrmt.p_tabledataheader (twbkfrmt.f_printtext ('<ABBR title = ' ||
                                                        g$_nls.get ('X', 'SQL', 'Course') ||
                                                        '>' ||
                                                        g$_nls.get ('X', 'SQL', 'Crse') ||
                                                        '</ABBR>'
                                                         )
                                   );
      twbkfrmt.p_tabledataheader (twbkfrmt.f_printtext ('<ABBR title = ' ||
                                                         g$_nls.get ('X','SQL','Section') ||
                                                         '>' ||
                                                         g$_nls.get ('X', 'SQL', 'Sec') ||
                                                        '</ABBR>'
                                                        )
                                  );
      twbkfrmt.p_tabledataheader (twbkfrmt.f_printtext ('<ABBR title = "' ||
                                                        g$_nls.get ('X', 'SQL', 'Credit Hours') ||
                                                        '">' ||
                                                        g$_nls.get ('X', 'SQL', 'Cred') ||
                                                        '</ABBR>'
                                                          )
                                    );
      twbkfrmt.p_tabledataheader (g$_nls.get ('X', 'SQL', 'Title'));
      twbkfrmt.p_tabledataheader (g$_nls.get ('X', 'SQL', 'Reason'));
    twbkfrmt.p_tablerowclose;
    errs_count := 0;
    FOR i IN 1 .. term_in.COUNT
    LOOP
      show_sub_btn:='N';
      sv_auth_addcodes_bp.p_course_auth_status(p_term_code=>term_in(i),
                                               p_pidm=>genpidm,
                                               p_course_auth_status_data=> lv_course_auth_status_ref
                                               );


      LOOP
      	j:=j+1;
      	BEGIN
      	temp_auth_code:=auth_code_in(j);
      	EXCEPTION
        WHEN NO_DATA_FOUND THEN
      	temp_auth_code:=NULL;
      	END;

      	FETCH  lv_course_auth_status_ref INTO lv_auth_status_rec;
      	EXIT WHEN lv_course_auth_status_ref%NOTFOUND;

        lv_section_ref:=sb_section.f_query_one(p_term_code=>lv_auth_status_rec.r_term_code,
                                               p_crn =>lv_auth_status_rec.r_crn
                                               );
        FETCH lv_section_ref INTO lv_section_rec;
        CLOSE lv_section_ref;
--
-- Open an html table for the errors section.
-- ==================================================
        twbkfrmt.p_tablerowopen;
        lv_authmsg_ref:=sv_regauth_msgs.f_query_one(lv_auth_status_rec.r_term_code,lv_auth_status_rec.r_crn,genpidm);
        FETCH lv_authmsg_ref INTO lv_authmsg_rec;
        IF lv_authmsg_ref%NOTFOUND THEN
           lv_authmsg_rec.r_msg_code:='AUTH2'; --Internal Defect:1721  STU16:GRS 04/04/2006
         CLOSE lv_authmsg_ref;
        ELSE
          IF lv_authmsg_rec.r_msg_code = 'AUTH1' THEN
           show_sub_btn:='Y';
          END IF;
        END IF;

        IF lv_authmsg_rec.r_msg_code = 'AUTH2' THEN
         twbkfrmt.P_FormHidden('num_attempts_in', 0);
         twbkfrmt.P_FormHidden('auth_code_in',NULL);
         twbkfrmt.P_FormHidden('crn_in',lv_auth_status_rec.r_crn);
         twbkfrmt.P_FormHidden('auth_msg_code',lv_authmsg_rec.r_msg_code);
         twbkfrmt.P_FormHidden('associated_term',lv_auth_status_rec.r_term_code);

         --GOTO NEXT_REC;
        ELSE



    --Internal Defect:1721  STU16:GRS 04/04/2006
         message:= sv_auth_addcodes_strings.f_get_error(lv_authmsg_rec.r_msg_code);
         status := sv_auth_addcodes_strings.f_get_error_status(lv_authmsg_rec.r_msg_code);
    --Internal Defect:1721  STU16:GRS 04/04/2006
          twbkfrmt.P_FormHidden('auth_msg_code',lv_authmsg_rec.r_msg_code);
          twbkfrmt.P_FormHidden('associated_term',lv_auth_status_rec.r_term_code);

         BEGIN
            IF num_attempts_in(j) IS NULL THEN
            temp_num_attempts:=0;
            ELSE
            temp_num_attempts:=num_attempts_in(j);
            END IF;
         EXCEPTION WHEN NO_DATA_FOUND THEN
             temp_num_attempts:=0;
         END;
          twbkfrmt.P_FormHidden('num_attempts_in', temp_num_attempts);

          twbkfrmt.p_tabledata (status);

          IF( lv_authmsg_rec.r_msg_code = 'AUTH7' OR
              lv_authmsg_rec.r_msg_code = 'AUTH8' OR
              lv_authmsg_rec.r_msg_code = 'AUTH1'
             )
          THEN

            twbkfrmt.p_tabledata (temp_auth_code);
            twbkfrmt.P_FormHidden('auth_code_in',temp_auth_code);
          ELSIF temp_num_attempts <= 3 THEN
            twbkfrmt.p_tabledataopen;
            twbkfrmt.p_formlabel (g$_nls.get ('X', 'SQL', 'Registration Add Auth Code'),
                                  visible   => 'INVISIBLE',
                                 idname    => 'addCode_id' || TO_CHAR (i)
                                 );
            twbkfrmt.p_formtext ('auth_code_in',
                                 '4',
                                 '4',
                                 temp_auth_code,
                                 cattributes   => 'ID="auth_code_in' || TO_CHAR (i) || '"'
                                 );
            twbkfrmt.p_tabledataclose;
          ELSE

            twbkfrmt.p_tabledata('');
            twbkfrmt.P_FormHidden('auth_code_in',temp_auth_code);
          END IF;

          twbkfrmt.p_tabledata (lv_auth_status_rec.r_crn);
          twbkfrmt.P_FormHidden('crn_in',lv_auth_status_rec.r_crn);

-- Display the data cells that show subject, course,
-- section, level, credit hours, grade mode, title,
-- and error message.
-- ==================================================
          twbkfrmt.p_tabledata (lv_section_rec.r_subj_code);
          twbkfrmt.p_tabledata (lv_section_rec.r_crse_numb);
          twbkfrmt.p_tabledata (lv_section_rec.r_seq_numb);
          twbkfrmt.p_tabledata (TO_CHAR (lv_section_rec.r_credit_hrs, '9990D990'));
          crse_title :=bwcklibs.f_course_title (lv_auth_status_rec.r_term_code, lv_auth_status_rec.r_crn);
          twbkfrmt.p_tabledata (crse_title);
         IF temp_num_attempts >3 THEN
            twbkfrmt.p_tabledata(g$_nls.get ('X', 'SQL','MAXIMUM ATTEMPTS REACHED'));
         ELSE
           twbkfrmt.p_tabledata (message);
         END IF;
         twbkfrmt.p_tablerowclose;
       END IF;
      <<NEXT_REC>>
        NULL;
      END LOOP;
      CLOSE lv_course_auth_status_ref;
   END LOOP;
   twbkfrmt.p_tableclose;
   htp.br;
   htp.br;

   HTP.formsubmit ('sub_btn_in',validate_btn);
   IF show_sub_btn = 'Y' THEN
     HTP.formsubmit ('sub_btn_in',subm_btn);
   END IF;
   HTP.formsubmit ('sub_btn_in',cancel_btn);

   htp.formclose;
   twbkwbis.p_closedoc (curr_release);
END P_DispAutCode;

Procedure P_ProcAuthCode(
       term_in                 IN   OWA_UTIL.ident_arr,
       err_term                IN   OWA_UTIL.ident_arr,
       err_crn                 IN   OWA_UTIL.ident_arr,
       err_subj                IN   OWA_UTIL.ident_arr,
       err_crse                IN   OWA_UTIL.ident_arr,
       err_sec                 IN   OWA_UTIL.ident_arr,
       err_code                IN   OWA_UTIL.ident_arr,
       err_levl                IN   OWA_UTIL.ident_arr,
       err_cred                IN   OWA_UTIL.ident_arr,
       err_gmod                IN   OWA_UTIL.ident_arr,
       crn_in                  IN   OWA_UTIL.ident_arr,
       auth_code_in            IN   OWA_UTIL.ident_arr,
       auth_msg_code           IN   OWA_UTIL.ident_arr  ,
       associated_term         IN   OWA_UTIL.ident_arr  ,
       sub_btn_in              IN   VARCHAR2   DEFAULT g$_nls.get('X','SQL','Validate') ,
       capp_tech_error_in_out  IN OUT  VARCHAR2,
       drop_result_label_in    IN twgrinfo.twgrinfo_label%TYPE DEFAULT NULL,
       drop_problems_term_code IN   OWA_UTIL.ident_arr,
       drop_problems_pidm      IN   OWA_UTIL.ident_arr,
       drop_problems_crn       IN   OWA_UTIL.ident_arr,
       drop_problems_subj      IN   OWA_UTIL.ident_arr,
       drop_problems_crse      IN   OWA_UTIL.ident_arr,
       drop_problems_sec      IN   OWA_UTIL.ident_arr,
       drop_problems_ptrm_code IN   OWA_UTIL.ident_arr,
       drop_problems_rmsg_cde   IN   OWA_UTIL.ident_arr,    -- C3SC  GRS 03/25/09 ADD
       drop_problems_message       IN   OWA_UTIL.ident_arr,
       drop_problems_start_date    IN   OWA_UTIL.ident_arr,
       drop_problems_comp_date    IN   OWA_UTIL.ident_arr,
       drop_problems_rsts_date    IN   OWA_UTIL.ident_arr,
       drop_problems_dunt_code    IN   OWA_UTIL.ident_arr,
       drop_problems_drop_code    IN   OWA_UTIL.ident_arr,
       drop_problems_connected_crns    IN   OWA_UTIL.ident_arr,
       drop_failures_term_code IN   OWA_UTIL.ident_arr,
       drop_failures_pidm      IN   OWA_UTIL.ident_arr,
       drop_failures_crn      IN   OWA_UTIL.ident_arr,
       drop_failures_subj      IN   OWA_UTIL.ident_arr,
       drop_failures_crse      IN   OWA_UTIL.ident_arr,
       drop_failures_sec      IN   OWA_UTIL.ident_arr,
       drop_failures_ptrm_code IN   OWA_UTIL.ident_arr,
       drop_failures_rmsg_cde  IN   OWA_UTIL.ident_arr,     -- C3SC  GRS 03/25/09 ADD
       drop_failures_message       IN   OWA_UTIL.ident_arr,
       drop_failures_start_date    IN   OWA_UTIL.ident_arr,
       drop_failures_comp_date    IN   OWA_UTIL.ident_arr,
       drop_failures_rsts_date    IN   OWA_UTIL.ident_arr,
       drop_failures_dunt_code    IN   OWA_UTIL.ident_arr,
       drop_failures_drop_code    IN   OWA_UTIL.ident_arr,
       drop_failures_connected_crns    IN   OWA_UTIL.ident_arr,
       num_attempts_in         IN OWA_UTIL.ident_arr       ,
       add_row                   NUMBER,
       orig_crn           IN    OWA_UTIL.ident_arr,
       orig_rsts          IN    OWA_UTIL.ident_arr
       )
IS

  aut_code_temp                    VARCHAR2(4);
  i                     INTEGER                        := 2;
  j                                 NUMBER;
  k                                 NUMBER;
 lv_auth_ref                       sv_auth_addcodes.auth_addcodes_ref;
 lv_auth_rec                       sv_auth_addcodes.auth_addcodes_rec;
 lv_pidm                           SPRIDEN.SPRIDEN_PIDM%TYPE;
 lv_course_auth_status_data_ref    sv_auth_addcodes_bp.course_auth_status_ref;
 lv_auth_status_rec                sv_auth_addcodes_bp.course_auth_status_rec;
 lv_err_code                       SVTAUTM.SVTAUTM_MSG_CODE%TYPE;
 go_addauthpage                    VARCHAR2(1):='N';
-- sftregs_rec                       sftregs%ROWTYPE;             /*C3SC 8.7.1.1 GRS 04/OCT/2013 MODIFIED */
 etrm_done             BOOLEAN                        := FALSE;
 multi_term            BOOLEAN                        := TRUE;
 clas_code             SGRCLSR.SGRCLSR_CLAS_CODE%TYPE;
 term                  stvterm.stvterm_code%TYPE      := NULL;
 sfrbtch_row             sfrbtch%ROWTYPE;
 tbrcbrq_row             tbrcbrq%ROWTYPE;
 drop_problems         sfkcurs.drop_problems_rec_tabtype;
 drop_failures         sfkcurs.drop_problems_rec_tabtype;
 capp_tech_error       VARCHAR2 (4);
 agn_flag              VARCHAR2(1);
 tempt_num_atempts     OWA_UTIL.ident_arr;
 drop_msg           VARCHAR2 (15) := NULL;
 autodrop_setting   VARCHAR2 (1);
 regs_count         NUMBER(10);
 wait_count         NUMBER(10);
 stcr_err_ind       sftregs.sftregs_error_flag%TYPE;
 appr_err           sftregs.sftregs_error_flag%TYPE;
 old_sftregs_row    sftregs%ROWTYPE;
 loop_sftregs_row  sftregs%ROWTYPE;
 lv_err_count       number;
 k NUMBER;


 BEGIN
  IF NOT twbkwbis.f_validuser (global_pidm)
  THEN
    RETURN;
  END IF;

-- Defect 1694
-- uses Student ID , if Faculty Advisor is logged in
--
  IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
      THEN
         global_pidm :=
           TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
  END IF;
-- end Defect

  term := term_in (term_in.COUNT);

  IF term_in.COUNT = 1
  THEN
     multi_term := FALSE;
  END IF;

  bwcklibs.p_initvalue (global_pidm, term, '', SYSDATE, '', '');
  IF drop_failures_term_code(1) <>'Dummy'
  THEN
    FOR i IN 1 .. drop_failures_term_code.COUNT
    LOOP
	 temp_drop_failures(i).term_code:= drop_failures_term_code(i);
     temp_drop_failures(i).pidm:=drop_failures_pidm(i);
     temp_drop_failures(i).crn := drop_failures_crn(i);
     temp_drop_failures(i).subj:=drop_failures_subj(i);
     temp_drop_failures(i).crse:=drop_failures_crse(i);
     temp_drop_failures(i).sec:=drop_failures_sec(i);
     temp_drop_failures(i).ptrm_code:=drop_failures_ptrm_code(i);
     temp_drop_failures(i).rmsg_cde:=drop_failures_rmsg_cde(i);   -- C3SC GRS 03/25/2009
     temp_drop_failures(i).message:=drop_failures_message(i);
     temp_drop_failures(i).start_date:=drop_failures_start_date(i);
     temp_drop_failures(i).comp_date:=drop_failures_comp_date(i);
     temp_drop_failures(i).rsts_date:=drop_failures_rsts_date(i);
     temp_drop_failures(i).dunt_code:=drop_failures_dunt_code(i);
     temp_drop_failures(i).drop_code:=drop_failures_drop_code(i);
     temp_drop_failures(i).connected_crns:=drop_failures_connected_crns(i);
    END LOOP;
  END IF;
  IF drop_problems_term_code(1)<>'Dummy' THEN
  FOR i IN 1 .. drop_problems_term_code.COUNT
  LOOP
     temp_drop_problems(i).term_code:= drop_problems_term_code(i);
     temp_drop_problems(i).pidm:=drop_problems_pidm(i);
     temp_drop_problems(i).crn:=drop_problems_crn(i);
     temp_drop_problems(i).subj:=drop_problems_subj(i);
     temp_drop_problems(i).crse:=drop_problems_crse(i);
     temp_drop_problems(i).sec:=drop_problems_sec(i);
     temp_drop_problems(i).ptrm_code:=drop_problems_ptrm_code(i);
     temp_drop_problems(i).rmsg_cde:=drop_problems_rmsg_cde(i);   -- C3SC GRS 03/25/2009
     temp_drop_problems(i).message:=drop_problems_message(i);
     temp_drop_problems(i).start_date:=drop_problems_start_date(i);
     temp_drop_problems(i).comp_date:=drop_problems_comp_date(i);
     temp_drop_problems(i).rsts_date:=drop_problems_rsts_date(i);
     temp_drop_problems(i).dunt_code:=drop_problems_dunt_code(i);
     temp_drop_problems(i).drop_code:=drop_problems_drop_code(i);
     temp_drop_problems(i).connected_crns:=drop_problems_connected_crns(i);
  END LOOP;
 END IF;

  IF sub_btn_in = validate_btn THEN
    FOR j IN 1 .. crn_in.COUNT
    LOOP
      agn_flag:='N';
      IF auth_code_in(j) IS NOT NULL AND auth_msg_code(j) <> 'AUTH1' THEN
        lv_course_auth_status_data_ref := sv_auth_addcodes_bp.f_get_course_auth_status(p_term_code  => associated_term(j),
                                                                                       p_crn        =>crn_in(j) ,
                                                                                       p_pidm       => global_pidm);
        FETCH lv_course_auth_status_data_ref INTO lv_auth_status_rec;
        CLOSE lv_course_auth_status_data_ref;
        IF  lv_auth_status_rec.r_auth_code IS NULL              -- no auth code found for crn
          OR auth_code_in(j) <> lv_auth_status_rec.r_auth_code  -- or entered auth code is not = to assigned auth code
        THEN
          IF sv_auth_addcodes_bp.f_exists_active(associated_term(j), crn_in(j),auth_code_in(j) ) = 'Y' THEN
            lv_auth_ref := sv_auth_addcodes.f_query_one(associated_term(j), crn_in(j), auth_code_in(j));
            FETCH lv_auth_ref INTO lv_auth_rec;
            IF lv_auth_ref%NOTFOUND THEN
	      CLOSE lv_auth_ref;
	      lv_pidm:= NULL;
            END IF; --END IF NOT FOUND LV_AUTH_REF
            CLOSE lv_auth_ref;
            lv_pidm:= lv_auth_rec.r_pidm;
            IF lv_pidm IS NOT NULL THEN
              tempt_num_atempts(j):=num_attempts_in(j)+1;
              lv_err_code := 'AUTH6'; --Invalid Auth Code - Assigned to Different Student ID
            ELSIF lv_auth_status_rec.r_auth_code IS NULL THEN
              lv_err_code := 'AUTH1'; --APPROVED - ID Validated  */
              agn_flag:='Y';
            ELSE
       	      tempt_num_atempts(j):=num_attempts_in(j)+1;
       	      lv_err_code := 'AUTH9'; --Invalid Auth Code - Diff. from Student�s Active Assigned Code
    	    END IF;  --END IF LV_PIDM IS NOT NULL
	  ELSE
	    tempt_num_atempts(j):=num_attempts_in(j)+1;
	    lv_err_code := 'AUTH5'; --Invalid Auth Code - Does not exist for CRN*/
          END IF; --END IF F_EXISTS_ACTIVE
	ELSE
	  lv_err_code := 'AUTH1'; --APPROVED - ID Validated*/
	END IF; --END IF AUTH_CODE IS NULL
	go_addauthpage:='Y';
	IF agn_flag='Y' THEN
  	  sv_auth_addcodes.p_update(p_term_code    => associated_term(j),
                                    p_crn          => crn_in(j),
                                    p_auth_code    => auth_code_in(j),
                                    p_pidm         => global_pidm,
                                    p_active_ind   => 'Y',
                                    p_assign_date  => SYSDATE,
                                    p_user_id      => USER);
	END IF; -- END IF AGN_FLAG= Y




	  sv_regauth_msgs.p_update(p_term_code =>associated_term(j),
                                   p_crn =>  crn_in(j),
                                   p_pidm=>global_pidm,
                                   p_msg_code=>lv_err_code
                                  );

      ELSE
        IF( auth_msg_code(j) != 'AUTH2')  AND
          ( auth_msg_code(j) != 'AUTH1')
        THEN
          tempt_num_atempts(j):=num_attempts_in(j)+1;
        END IF;    	-- message diff auth2 and auth1
        go_addauthpage:='Y';
      END IF;	--END IF MESSAGE CODE IS NOT NULL AND DIF FROM AUTH1
    END LOOP;

ELSIF sub_btn_in = subm_btn THEN
       -- Get sobterm repeat indicator value.
        bwcklibs.p_getsobterm(term_in(1),sobterm_row);   --C3SC 8.7 GRS 04/16/2013 ADDED

       FOR i IN 1 .. crn_in.COUNT
       LOOP
       IF auth_msg_code(i)='AUTH1' THEN

         DECLARE
            my_subj   SSBSECT.SSBSECT_SUBJ_CODE%TYPE := NULL;
            my_crse   SSBSECT.SSBSECT_CRSE_NUMB%TYPE := NULL;
            my_seq    SSBSECT.SSBSECT_SEQ_NUMB%TYPE  := NULL;
        BEGIN
         sftregs_rec.sftregs_crn :=crn_in(i);
         sftregs_rec.sftregs_pidm :=global_pidm;
         sftregs_rec.sftregs_term_code :=associated_term (i);
         FOR j IN 1..orig_crn.COUNT
         LOOP
            IF crn_in(i) = orig_crn(j) THEN
			 sftregs_rec.sftregs_rsts_code :=orig_rsts(j);
           END IF;
         END LOOP;

         sftregs_rec.sftregs_rmsg_cde:=NULL; --C3SC GRS 03/25/2009 ADD
         sftregs_rec.sftregs_message:=NULL;
         sftregs_rec.sftregs_capc_over :='Y';
         sftregs_rec.sftregs_rsts_date:=SYSDATE;               /*Defect:1-C79YI6 C3SC CHANGE AS 08/09/2010*/
        -- BEGIN
           IF multi_term
           THEN
             --
             -- Get the latest student and registration records.
             -- ===================================================
               bwckcoms.p_regs_etrm_chk (global_pidm,
                                        associated_term(i),
                                        clas_code,
                                        multi_term
                                        );

             ELSIF NOT etrm_done
             THEN
               NULL;
               bwckcoms.p_regs_etrm_chk (global_pidm, term, clas_code);
              etrm_done := TRUE;
             END IF;


          --
-- Get the section information.
-- ===================================================
               bwckregs.p_getsection (
                     sftregs_rec.sftregs_term_code,
                     sftregs_rec.sftregs_crn,
                     my_subj,
                     my_crse,
                     my_seq
                  );


          sv_reg_inprogress.p_update(p_term_code  => sftregs_rec.sftregs_term_code,
                                     p_pidm       => sftregs_rec.sftregs_pidm,
                                     p_crn        => sftregs_rec.sftregs_crn,
                                     p_rsts_code  => sftregs_rec.sftregs_rsts_code,
                                     p_rsts_date  => sftregs_rec.sftregs_rsts_date,
                                     p_error_flag => 'O',
                                     p_rmsg_cde=>sftregs_rec.sftregs_rmsg_cde,
                                     p_message    =>sftregs_rec.sftregs_message,
                                     p_hold_rmsg_cde=>sftregs_rec.sftregs_rmsg_cde,
                                     p_hold_message=>sftregs_rec.sftregs_message,
                                     p_vr_status_type=>sfkfunc.f_get_rsts_type(sftregs_rec.sftregs_rsts_code),
                                     p_remove_ind =>'N'
                                     );


  /* CALB 8.7 04/16/2013 ADDED AFTER THE AUTH REQUIRED MESSAGE IS REMOVED WE STILL NEED TO CHECK FOR REPEATING (ITS NEEDED FOR FAMILY REPEAT)*/
          -- Get sftregs values
          OPEN sfkcurs.sftregsc (pidm =>sftregs_rec.sftregs_pidm,
                  		        term =>sftregs_rec.sftregs_term_code,
								crn =>sftregs_rec.sftregs_crn);
	        FETCH  sfkcurs.sftregsc INTO old_sftregs_row;
			CLOSE  sfkcurs.sftregsc;

           OPEN sfkcurs.sftregsc (sftregs_rec.sftregs_pidm, sftregs_rec.sftregs_term_code,sftregs_rec.sftregs_crn);
           FETCH sfkcurs.sftregsc INTO old_sftregs_row;
           CLOSE sfkcurs.sftregsc;

           sftregs_rec.sftregs_sect_subj_code := my_subj;
	       sftregs_rec.sftregs_sect_crse_numb := my_crse;
	       sftregs_rec.sftregs_sect_seq_numb  := my_seq;
	       sftregs_rec.sftregs_rsts_code :=  old_sftregs_row.sftregs_rsts_code;
	       sftregs_rec.sftregs_levl_code := old_sftregs_row.sftregs_levl_code;
	       sftregs_rec.sftregs_sect_schd_code := old_sftregs_row.sftregs_sect_schd_code;
	       sftregs_rec.sftregs_crse_title :=  old_sftregs_row.sftregs_crse_title;
	       sftregs_rec.sftregs_credit_hr :=  old_sftregs_row.sftregs_credit_hr;
	       sftregs_rec.sftregs_gmod_code := old_sftregs_row.sftregs_gmod_code;

	    p_getoverride;	   /*C3SC 8.7.1.1 GRS 04/OCT/2013 ADDED */



		 sfkedit.p_pre_edit (
            sftregs_rec,
            stcr_err_ind,
            appr_err,
            'Y',
            'N',  -- wait indicator
            'Y',
            'N',
            'N',
            'N',
            'N',
            'N',
            'N',
            'N',
            'N',
            'N',
            'N',
            sobterm_row.sobterm_rept_severity,  -- repeat logic  i need to get sobterm value in here.
            'N',
            'N',
            'N',
            'N',
            'XX',--sgrclsr_clas_code,
            0,--scbcrse_row.scbcrse_max_rpt_units,
            0,--scbcrse_row.scbcrse_repeat_limit,
            'XX',--ssbsect_row.ssbsect_sapr_code,
            'N',
            0,--ssbsect_row.ssbsect_seats_avail,
            0,--ssbsect_row.ssbsect_wait_count,
            0,--ssbsect_row.ssbsect_wait_capacity,
            0,--ssbsect_row.ssbsect_wait_avail,
            'WC',
            auth_over_census => null,
            auth_over_closed =>NULL,
            auth_over_started =>NULL
			);
		    --C3SC 8.8 ADDED GRS 01/10/2014
           IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
           THEN
            global_pidm :=
            TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
           END IF;
		   -- C3SC 8.8 END

           IF stcr_err_ind = 'Y'
           THEN
             sftregs_rec.sftregs_rsts_code :=
                                     SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2);
              sftregs_rec.sftregs_vr_status_type :=
             sfkfunc.f_get_rsts_type(sftregs_rec.sftregs_rsts_code);

             sftregs_rec.sftregs_remove_ind := 'Y';

           sv_reg_inprogress.p_update(p_term_code  => sftregs_rec.sftregs_term_code,
                                     p_pidm       => sftregs_rec.sftregs_pidm,
                                     p_crn        => sftregs_rec.sftregs_crn,
                                     p_rsts_code  => sftregs_rec.sftregs_rsts_code,
                                     p_rsts_date  => sftregs_rec.sftregs_rsts_date,
                                     p_error_flag => sftregs_rec.sftregs_error_flag,
                                     p_rmsg_cde=>sftregs_rec.sftregs_rmsg_cde,
                                     p_message    =>sftregs_rec.sftregs_message,
                                     p_hold_rmsg_cde=>sftregs_rec.sftregs_rmsg_cde,
                                     p_hold_message=>sftregs_rec.sftregs_message,
                                     p_vr_status_type=>sfkfunc.f_get_rsts_type(sftregs_rec.sftregs_rsts_code),
                                     p_remove_ind =>'Y'
                                     );


           END IF;
           -- CALB 8.7 04/16/2013 ADDED END
  		   --
           -- Create a batch fee assessment record.
           -- ===================================================
            sfrbtch_row.sfrbtch_term_code := associated_term(i);
            sfrbtch_row.sfrbtch_pidm := global_pidm;
            sfrbtch_row.sfrbtch_clas_code := clas_code;
            sfrbtch_row.sfrbtch_activity_date := SYSDATE;
            bwcklibs.p_add_sfrbtch (sfrbtch_row);
            tbrcbrq_row.tbrcbrq_term_code := associated_term(i);
            tbrcbrq_row.tbrcbrq_pidm := global_pidm;
            tbrcbrq_row.tbrcbrq_activity_date := SYSDATE;
            bwcklibs.p_add_tbrcbrq (tbrcbrq_row);
            null;
           END;
         END IF;
     END LOOP;
        --
       -- Do batch validation on all registration records.
       -- ===================================================
        bwckcoms.p_group_edits( term_in, global_pidm, etrm_done,
                                capp_tech_error,
                                temp_drop_problems, temp_drop_failures);

         p_create_problem_list (p_pidm_in =>   global_pidm,
                               p_term_in =>  term_in(1),
                               p_temp_failures_in_out =>  temp_drop_failures);


       If capp_tech_error is null
       then
          sv_auth_addcodes_bp.p_local_problems( term_in,
                            err_term, err_crn, err_subj,
                            err_crse, err_sec, err_code,
                            err_levl, err_cred, err_gmod,
                            temp_drop_problems, temp_drop_failures,add_row,
                            'bwvkauth.P_ProcAuthCode',
                             orig_crn     ,
                             orig_rsts
                            );
	   return;
      end if;
       go_addauthpage:='N';
    ELSE
      go_addauthpage:='N';
      NUll; -- cancel

     bwcklibs.p_reset_failures (global_pidm, temp_drop_failures);

--
-- Transfer all changes in SFTREGS to SFRSTCR.
-- =====================================================================
   --SC_Defect:2496   GRS 11/28/2006 BEGINS
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
        temp_drop_problems,
       temp_drop_failures
     );


/*
     bwckcoms.p_adddrop2 (term_in,
                          err_term,
                          err_crn,
                          err_subj,
                          err_crse,
                          err_sec,
                          err_code,
                          err_levl,
                          err_cred,
                          err_gmod,
                          capp_tech_error_in_out,
                          drop_result_label_in,
                          temp_drop_problems,
                          temp_drop_failures
                  );
  */
 --SC_Defect:2496   GRS 11/28/2006 END
      RETURN;
    END IF;

  IF go_addauthpage = 'Y' THEN
     P_DispAutCode (term_in      ,
                   err_term     ,
                   err_crn      ,
                   err_subj     ,
                   err_crse     ,
                   err_sec      ,
                   err_code     ,
                   err_levl     ,
                   err_cred                ,
                   err_gmod                ,
                   capp_tech_error_in_out  ,
                   drop_result_label_in    ,
                   temp_drop_problems        ,
                   temp_drop_failures        ,
                   auth_code_in         ,
                   auth_msg_code        ,
                   tempt_num_atempts   ,
                   add_row        ,
                   orig_crn,
                   orig_rsts
                      );
    END IF;
END P_ProcAuthCode;

PROCEDURE p_final_upadates2(
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
   add_row                         NUMBER
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
   minh_error        VARCHAR2 (1) := 'N';

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
         p_init_final_update_vars2 (genpidm, term_in (i));

      -- descomentar temporalmente para prueba
      /*   IF drop_problems IS NOT NULL OR
             drop_failures IS NOT NULL
         THEN
             /* re-run group edits to find any conflicts with drops */
             /* that have been reset */
   /*          group_error_flag := NULL;
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

/*                UPDATE sftregs
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
  */
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

END  p_final_upadates2;

PROCEDURE p_init_final_update_vars2 (pidm_in IN sfrracl.sfrracl_pidm%TYPE,
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
END p_init_final_update_vars2;


PROCEDURE p_create_problem_list (p_pidm_in                    IN        spriden.spriden_pidm%type,
                                 p_term_in                    IN        sfrstcr.sfrstcr_term_code%TYPE,
                                 p_temp_failures_in_out   IN OUT       sfkcurs.drop_problems_rec_tabtype)
IS
	i NUMBER;
	j NUMBER :=1;
	approved_ind varchar2(1);
	lv_local_failures  sfkcurs.drop_problems_rec_tabtype;
  temp_crn           ssbsect.ssbsect_crn%type;
  temp_rmsg_cde       sftregs.sftregs_rmsg_cde%type;   --C3SC GRS 03/25/2009 ADD
  temp_message       sftregs.sftregs_message%type;


 CURSOR CRN_Approved(crn ssbsect.ssbsect_crn%type)
 IS
  SELECT 'Y'
  FROM SVTAUTM
  WHERE SVTAUTM_PIDM = p_pidm_in
    AND SVTAUTM_TERM_CODE = p_term_in
    AND SVTAUTM_CRN = crn
    AND SVTAUTM_MSG_CODE = 'AUTH2';

 BEGIN

   IF p_temp_failures_in_out.EXISTS(1)
   THEN
      FOR i IN 1..p_temp_failures_in_out.COUNT
      LOOP
         IF INSTR(p_temp_failures_in_out(i).message,'Authorization required') <> 0 THEN  --C3SC GRS 03/25/2009 CHANGED
          OPEN CRN_Approved(p_temp_failures_in_out(i).crn);
          FETCH CRN_Approved INTO approved_ind;
          CLOSE CRN_Approved;
          IF ( approved_ind = 'Y' OR
              (temp_crn IS NOT NULL AND
              temp_message IS NOT NULL AND
              temp_crn = p_temp_failures_in_out(i).crn AND
              temp_message = p_temp_failures_in_out(i).message))
         THEN
            NULL;
          ELSE

            lv_local_failures (j).term_code := p_temp_failures_in_out(i).term_code;
            lv_local_failures (j).pidm := p_temp_failures_in_out(i).pidm ;
            lv_local_failures (j).crn := p_temp_failures_in_out(i).crn;
            lv_local_failures (j).subj := p_temp_failures_in_out(i).subj;
            lv_local_failures (j).crse :=p_temp_failures_in_out(i).crse;
            lv_local_failures (j).sec := p_temp_failures_in_out(i).sec;
            lv_local_failures (j).ptrm_code := p_temp_failures_in_out(i).ptrm_code;
            lv_local_failures (j).rmsg_cde :=  p_temp_failures_in_out(i).rmsg_cde;  --C3SC GRS 03/25/2009 ADD
            lv_local_failures (j).message := p_temp_failures_in_out(i).message;
            lv_local_failures (j).start_date := p_temp_failures_in_out(i).start_date;
            lv_local_failures (j).comp_date := p_temp_failures_in_out(i).comp_date;
            lv_local_failures (j).rsts_date := p_temp_failures_in_out(i).rsts_date;
            lv_local_failures (j).dunt_code := p_temp_failures_in_out(i).dunt_code;
            lv_local_failures (j).drop_code := p_temp_failures_in_out(i).drop_code;
            lv_local_failures (j).connected_crns := p_temp_failures_in_out(i).connected_crns;
             j := j + 1;
         END IF;
        ELSE

            lv_local_failures (j).term_code := p_temp_failures_in_out(i).term_code;
            lv_local_failures (j).pidm := p_temp_failures_in_out(i).pidm ;
            lv_local_failures (j).crn := p_temp_failures_in_out(i).crn;
            lv_local_failures (j).subj := p_temp_failures_in_out(i).subj;
            lv_local_failures (j).crse :=p_temp_failures_in_out(i).crse;
            lv_local_failures (j).sec := p_temp_failures_in_out(i).sec;
            lv_local_failures (j).ptrm_code := p_temp_failures_in_out(i).ptrm_code;
            lv_local_failures (j).rmsg_cde   := p_temp_failures_in_out(i).rmsg_cde;   -- C3SC GRS 03/25/2009
            lv_local_failures (j).message := p_temp_failures_in_out(i).message;
            lv_local_failures (j).start_date := p_temp_failures_in_out(i).start_date;
            lv_local_failures (j).comp_date := p_temp_failures_in_out(i).comp_date;
            lv_local_failures (j).rsts_date := p_temp_failures_in_out(i).rsts_date;
            lv_local_failures (j).dunt_code := p_temp_failures_in_out(i).dunt_code;
            lv_local_failures (j).drop_code := p_temp_failures_in_out(i).drop_code;
            lv_local_failures (j).connected_crns := p_temp_failures_in_out(i).connected_crns;
             j := j + 1;
        END IF;
       TEMP_CRN:=p_temp_failures_in_out(i).crn;
       TEMP_RMSG_CDE := p_temp_failures_in_out(i).rmsg_cde;    -- C3SC GRS 03/25/2009  ADD
       TEMP_MESSAGE:= p_temp_failures_in_out(i).message;
      END LOOP;
      p_temp_failures_in_out:= lv_local_failures;
   END IF;
 END p_create_problem_list;

/*C3SC 8.7.1.1 04/OCT/2013 ADDED*/
 PROCEDURE p_getoverride
   IS
      genpidm   spriden.spriden_pidm%TYPE;

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
                        sftregs_rec.sftregs_crn,
                        sftregs_rec.sftregs_term_code
                     )
      LOOP
         FOR over_rec IN overridec (
                            genpidm,
                            sftregs_rec.sftregs_term_code,
                            sftregs_rec.sftregs_crn,
                            ssbsect.ssbsect_subj_code,
                            ssbsect.ssbsect_crse_numb,
                            ssbsect.ssbsect_seq_numb
                         )
         LOOP
            IF over_rec.sfrrovr_rept_over = 'Y'
            THEN
               sftregs_rec.sftregs_rept_over := 'Y';
               sftregs_rec.sftregs_error_flag := 'O';
            END IF;

            IF over_rec.sfrrovr_rpth_over = 'Y'
            THEN
               sftregs_rec.sftregs_rpth_over := 'Y';
               sftregs_rec.sftregs_error_flag := 'O';
            END IF;
         END LOOP;
      END LOOP;
   END p_getoverride;
/*C3SC 8.7.1.1 04/OCT/2013 ADDED*/


END BWVKAUTH;