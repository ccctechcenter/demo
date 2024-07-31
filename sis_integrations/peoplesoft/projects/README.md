# Peoplesoft Projects

The College Adaptor supports multiple SIS back-ends. This folder houses the Peoplesoft
code/logic/objects that implements the communication channel between the College Adaptor and a given
peoplesoft install as well as the PeopleCode (PeopleCode is a Java-based Proprietary language) method/functions needed
to maintain data in a peoplesoft environment.

* This is the meat/potatoes of the Peoplesoft implementation/support of College Adaptor.
  Most logic is here within peoplesoft (as opposed to in the groovy code) as it:
    * abstracts the db from calling programs to have a db-agnostic implementation
      (peoplesoft supports installations on either oracle, postgres, or ms sql databases)
    * provides the Colleges with the most transparency of what College adaptor is doing so that they can code-review
      it and make more informed decisions/justifications for releases/deployments
    * stays consistent with other custom modifications that each college may have.
      e.g. if a college has a peoplecode trigger on enrollments, direct db access/manipulation could potentially
      bypass this trigger and the college's other systems would be impacted.

## Maintenance

These files should not be edited directly. Instead, The project files (xml and ini)
should be imported into a Peoplesoft Instance using App Designer.

## List of Projects

### convenience/CCTC_GLOBAL -- CAv4.5.0
This project contains the entire CCTC integration presence (including deprecated objects) and should *NOT* be deployed
nor imported into any environment other than a development one. Its sole purpose is to
help a developer keep track of all the objects we have built. Its a convenience mechanism.

Note: While Permission Lists, Roles, and Query Trees are in the project, they must be maintained with
the appropriate tool (not app designer). The CCTC User is maintained separately as well and should be
given the appropriate CCTC role(s) manually.


### convenience/CCTC_HEALTH -- Not Released: See feature/CONDUCTOR-224-PS branch
This project is a convenience mechanism for simulating the Database being down for testing purposes.
While deploying this project to a College and/or Production is fine; it wont break anything and all it does is
a single query from dual, it isn't used by the College adaptor at the moment. (If it were to be used, it would be used
by the /health endpoint). It does not contain any RPC references nor any other feature references/dependencies.

It is meant to be a micro-service around System Health Metrics. While this project can be installed without
the CCTC_RPC core project, it is intended to be utilized through the CCTC_RPC tunnel.


### deprecated/CCTC_EDEX -- Pre CAv2.6.0
This project contains the monolithic API approach (we moved away from monolithic and went towards microservices) when trying
to implement Online Education Initiative requirements as known as the Course Exchange project. Rumor has it that the
project got de-funded after a good chunk of code was written for it. Thus, the project is here even though it
should *NOT* ever be deployed anywhere anymore.
*Note*: It utilizes query objects and a deprecated version of the component interface. Trying to resurrect this ancient
Mummy may come with many curse-words.


### deprecated/CCTC_OEI_V2 -- Pre CAv2.6.0
This project contains the monolithic API approach (we moved away from monolithic and went towards microservices) when trying
to implement Online Education Initiative (for the 2nd time) requirements as known as the Course Exchange project.
Rumor has it that the project got de-funded after a good chunk of code was written for it. Thus, the project is here even
though it should *NOT* ever be deployed anywhere anymore.
*Note*: It utilizes query objects and a deprecated version of the component interface. Trying to resurrect this ancient
Mummy may come with many curse-words.

### deprecated/CCTC_ASSESSMENT -- Pre CAv2.6.0
This project contains the monolithic API approach (we moved away from monolithic and went towards microservices) when trying
to implement Assessments. Assessments and all of the functionality behind it are deprecated and replaced by the Mulitple
Measures Placement Status (MMPS) project/functionality. See the CCTC_STDNTPLCMNT project.

Thus, the project is here even though it should *NOT* ever be deployed anywhere anymore.
*Note*: It utilizes query objects and a deprecated version of the component interface. Trying to resurrect this ancient
Mummy may come with many curse-words.


### CCTC_RPC (CCTC Remote Procedure Call) -- CAv3.6.0
This is part of the core functionality of the College Adapter Integration.
It contains all peoplecode and objects needed to allow our Java/Groovy App to execute
Peoplecode methods through this "tunnel". It does not contain any peoplecode methods that our
app would actually want to run as that is out of scope for this project and in scope for each other project.
While RPC is powerful, we try to limit ourselves a bit by only allowing the tunnel to execute CCTC_ code,
e.g. the method must be in a class that is prefixed with CCTC_ and that class must be in a package prefixed with CCTC_
this is so we don't execute arbitrary non-cctc methods.

Additionally, this project contains the Permission Lists necessary to run College Adaptor. However, note that since
Roles are not a supported type when doing a 'Copy to File', the role is not in this repo. and must be created manually.

This project is meant to be a stand alone project with no dependencies to establish a execution/communication tunnel.


### CCTC_EVENTS (CCTC Data Change Events) -- CAv4.0.0
This is one step above the core functionality of the College Adapter Integration.
It contains a base class CCTC_EventHandlers which contains a list of event handlers (just regular peoplesoft methods) that
are called/triggered/fire when said event has occurred by one of the other feature packages.
For example, there is a method name onStudentPlacementInserted, that is called, when a Student Placement record is inserted
using the CCTC_STDNTPLCMNT project/package/code.

This base listing of events provides the foundation for the College to extend/implement these event handlers to run/execute
whatever arbitrary code they want/require when such events happen.

##### List of Events currently supported:
* onExampleChanged
* onStudentPlacementInserted
* onStandardAppInserted
* onInternationalAppInserted
* onPromiseGrantAppInserted
* onSharedAppInserted
* onEnrollmentRequestSubmitted
* onRequestEnrollmentDropped
* onFinAidUnitsInserted
* onFinAidUnitsRemoved
* onStudentCohortInserted
* onStudentCohortRemoved
* onStudentOrientationUpdated

Note: just because we have the above events supported, does not mean each college will utilize every event. Also, the triggering
action of each event is dependent on the associated project being installed and supported by the college. If the associated project
is NOT installed, that's fine, the CCTC_EVENTS project will still work just fine, but the event will just never be triggered.

This project is not directly called by the College Adaptor, instead a College should extend this code (using the below
CCTC_COLLEGE project) to implement their own handlers.


### CCTC_COLLEGE (Custom College Code; Event Handlers) -- CAv3.6.0
Provides hooks for the College to implement/execute their own code per each CCTC data-change event (as described in the CCTC_EVENTS project)
This Project should be installed Once and Only Once per College installation. It is intended for the College's Techs to
take ownership of this project to maintain and add to as needed per event. AGAIN: Once the College Tech's take ownership,
CCCTC should NOT re-deploy this package/project as it will wipe out any changes made by the College Techs.
This project contains a class that extends/implements the CCTC_EVENTS_PKG:CCTC_EventHandlers class as a sub/child class named
CCTC_COLLEGE_PKG:CCTC_CollegeEventHandlers. The College Adaptor directly utilizes this package through CCTC_RPC tunnel
when a successful data-change event is reported.


### CCTC_IDENTITY (Identity Translations) -- CAv4.0.0
This is a utility project to translate between internal and external person identifiers. e.g. translate between the CCCID
and the EMPLID as well as lookup EMPLID by email/oprId. The mapping is stored in a standard/built-in Peoplesoft table. Translations/Gets
are done via SQL objects and associations (inserts) are done through a CCTC CI (that wraps a standard/built-in Component).

Also, in this project is a Person class for retrieving basic name details as well as associated emails.
Further, in this project is a Student class for retrieving a number of various fields/status about a student.

While this project can be installed without the CCTC_RPC core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.
* Note: An External System must be setup in the PS Web interface. The id of the external system is then set in the college-adaptor-config
  and passed into as a parameter to the translation functions.

### CCTC_COHORT (Student Cohorts) -- CAv3.6.0
This is a feature project to get/set Student Cohort data into/from a custom staging table.
It contains four peoplecode functions for:
* get a listing of cohorts by student and term
* get a cohort record by student, term, and cohort (useful for existence checks)
* set (insert a new) student cohort record
* remove a student cohort record

It does not contain any RPC references nor any other feature references/dependencies. However, Student Cohorts do have
associated Data-Change Events and so can be enhanced by installing the CCTC_COLLEGE and CCTC_EVENTS projects.

This Project is meant to be a micro-service around Student Cohorts. While this project can be installed without the CCTC_RPC
core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.

### CCTC_MATRIC (Student Matriculation) -- CAv4.0.0
This is a feature project to set student matriculation fields such as Orientation Status.
It contains one peoplecode function for:
* set a students orientation status

It does not contain any RPC references nor any other feature references/dependencies. However, Student Matriculation does have
an associated Data-Change Event and so can be enhanced by installing the CCTC_COLLEGE and CCTC_EVENTS projects.

This Project is meant to be a micro-service around Student Matriculation. While this project can be installed without the CCTC_RPC
core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.


### CCTC_STDNTPLCMNT (Student Placement) -- CAv3.6.0
This is a feature project to get/set Student Placement data into/from a custom staging table.
It contains three peoplecode functions for:
* get a student's most recent placement record
* set (insert a new) student's placement record
* remove a student placement record

It does not contain any RPC references nor any other feature references/dependencies. However, Student Placements do have
associated Data-Change Events and so can be enhanced by installing the CCTC_COLLEGE and CCTC_EVENTS projects.

This Project is meant to be a micro-service around Student Placements. While this project can be installed without the CCTC_RPC
core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.


### CCTC_APPLY (CCCApply Standard Applications) -- CAv4.6.0
This is a feature project to get/set Standard Application data into/from custom staging tables.
It contains five peoplecode functions for:
* get a standard application
* set a standard application
* get the standard application's supplemental questions
* set the standard application's supplemental questions
* remove, for a given appId, the standard application and its supplemental questions record

NOTE: The NonCredit Application is, actually, just a Standard Application with some fields set in a particular way.
e.g. the nonCredit field will be true and the confirmation number will start with "NC-" etc.

It does not contain any RPC references nor any other feature references/dependencies. However, Applications do have
associated Data-Change Events and so can be enhanced by installing the CCTC_COLLEGE and CCTC_EVENTS projects.

This Project is meant to be a microservice around Student Applications. While this project can be installed without the CCTC_RPC
core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.


### CCTC_INTL (CCCApply International Applications) -- CAv4.6.0
This is a feature project to get/set International Application data into/from custom staging tables.
It contains five peoplecode functions for:
* get an international application
* set an international application
* get the international application's supplemental questions
* set the international application's supplemental questions
* remove, for a given appId, the international application and its supplemental questions record

It does not contain any RPC references nor any other feature references/dependencies. However, Applications do have
associated Data-Change Events and so can be enhanced by installing the CCTC_COLLEGE and CCTC_EVENTS projects.

This Project is meant to be a microservice around International Student Applications. While this project can be installed without the CCTC_RPC
core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.


### CCTC_CCPG (CCCApply CCPromise Grant Applications) -- CAv4.6.0
This is a feature project to get/set CCPromise Grant Application data into/from custom staging tables.
It contains two peoplecode functions for:
* get an existing CCPromise Grant Application details
* set/insert a CCPromise Grant Application

It does not contain any RPC references nor any other feature references/dependencies. However, Financial Aid does have
associated Data-Change Events and so can be enhanced by installing the CCTC_COLLEGE and CCTC_EVENTS projects.

This Project is meant to be a microservice around Financial Aid features. While this project can be installed without
the CCTC_RPC core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.

### CCTC_SHAREDAPPS (Shared Applications) -- CAv4.6.0
This is a feature project to get/set Shared Application data into/from custom staging tables.
It contains 3 peoplecode functions for:
* get a shared application
* set a shared application
* remove, for a given appId and teaching mis code, the shared application record

It does not contain any RPC references nor any other feature references/dependencies. However, Applications do have
associated Data-Change Events and so can be enhanced by installing the CCTC_COLLEGE and CCTC_EVENTS projects.

This Project is meant to be a micro-service around Shared Applications. While this project can be installed without the CCTC_RPC
core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.

### CCTC_FRAUD_REPORTS (Reported Fraudulent Activities) -- CAv4.11.0
This is a feature project to get/set Fraud Report data into/from a custom staging table.
It contains 5 peoplecode functions for:
* get a fraud report by id
* get a list of matching fraud reports by appId and/or cccId
* set/create a fraud report
* remove, for a given sisFraudReportId, the fraud report record
* rescind, for a given sisFraudReportId, the fraud report record (updates rescind timestamp)

It does not contain any RPC references nor any other feature references/dependencies. It does not have
any associated Data-Change Events.

This Project is meant to be a micro-service around Fraud Reports. While this project can be installed without the CCTC_RPC
core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.

### CCTC_FINAID (Financial Aids) -- CAv4.1.0
This is a project to get/set Financial Aid related features. It contains the ability to get/set/remove
Course Exchange Financial Aid Unit Records for a student and term.
It contains four peoplecode functions for:
* get a List of FA Units for a given student and term
* get a Record for a given student, term, mis, and C-Id
* set/insert a FA Unit record for a given student, term, mis, and C-Id
* remove a FA Unit record for a given student, term, mis, and C-Id

It does not contain any RPC references nor any other feature references/dependencies. However, Financial Aid does have
associated Data-Change Events and so can be enhanced by installing the CCTC_COLLEGE and CCTC_EVENTS projects.

This Project is meant to be a micro-service around Financial Aid features. While this project can be installed without
the CCTC_RPC core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.


### CCTC_TERMS (Term and Session) -- CAv4.0.0
This is a project to get Term and Session data from the built-in Peoplesoft tables.
It contains two functions for:
* retrieving term data (for all terms or a given term).
* retrieving term data (for all terms containing the given date).

It does not contain any RPC references nor any other feature references/dependencies.

This Project is meant to be a micro-service around Term and Session features. While this project can be installed without
the CCTC_RPC core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.


### CCTC_CRSE_SCTN (Courses and Sections) -- CAv3.6.0
This is a project to get Course data and Section data from the built-in Peoplesoft tables.
It contains 10 functions for:
* retrieving a single course header detail record
* retrieving total course fees for a given course
* retrieving the components (aka contacts) for a given course
* retrieving the requisistes (both prereqs and coreqs) for a given course

* retrieving a single section header detail record
* retrieving a list of section header detail records for a given course
* retrieving a list of section header detail records per search words
* retrieving a list of instructors for a given section
* retrieving a list of meeting patterns (meeting times) for a given section
* retrieving a list of cross listed sections for a given section

It does not contain any RPC references nor any other feature references/dependencies.

This Project is meant to be a micro-service around Course and Section features. While this project can be installed without
the CCTC_RPC core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.

### CCTC_Enrollments (Student Enrollments) -- CAv3.6.0
This is a project to get/set student enrollment requests using the built-in Peoplesoft component interfaces to legit enrollment tables.
It contains six peoplecode functions for:
* Getting current enrollments (by either student or section)
* requesting a student enrollment be added
* requesting a student enrollment be dropped
* checking the request status
* getting request errors (aka denial reasons)
* getting a student prerequisite status for a course and term (date)

It does not contain any RPC references nor any other feature references/dependencies. However, Enrollment does have
associated Data-Change Events and so can be enhanced by installing the CCTC_COLLEGE and CCTC_EVENTS projects.

This Project is meant to be a micro-service around Enrollment features. While this project can be installed without
the CCTC_RPC core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.

### CCTC_Transcripts (Course Exchange PESC Transcripts) -- CAv3.6.0
This is a project to get a student PESC Transcript data from the built-in peoplesoft tables.
It contains ten peoplecode functions for:
* Getting Institution details
* Getting General Grading Basis details for an institution
* Getting a Student's Overall Academic Summary
* Getting a Student's Course History
* Getting the degrees awarded to a student
* Getting the plans associated with the degrees awarded to a student
* Getting the honors associated with the degrees awarded to a student
* Getting the students External Careers (current usage for High School data)
* Getting a Student's Academic Summaries per enrolled term
* Getting any honors associated with the enrolled terms for a student

It does not contain any RPC references nor any other feature references/dependencies.

This Project is meant to be a micro-service around Transcript data. While this project can be installed without
the CCTC_RPC core project, it is intended to be utilized by the java/groovy app through the CCTC_RPC tunnel.

### MisCodeSpecific
##### 050/CCTC_College (MCC Custom Event Handlers) -- CAv3.6.0
Because MCC is our development partner, we know that they needed to execute specific COBOL program when the
onEnrollmentRequestSubmitted event triggered, so we added their code here for them. This is unusual and we normally would
not want to house the College Specific CCTC_COLLEGE project as each college should have ownership of it once it is
delivered/installed. But as of now, there is no current/immediate harm in backing it up here for them since they have not
yet taking ownership of it and they are our development partner. We wouldn't want to export/deploy this project to any
other college.
