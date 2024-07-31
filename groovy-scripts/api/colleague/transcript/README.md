# Colleague Transcripts

## Overview

Colleague Transcripts are loaded via a series of services that are defined as "abstract". This allows for a high degree
of customization.

## Customization

Customizing a transcript for a college involves extending `api.colleague.base.Transcript` in the custom script folder for
the MIS code. An example is included for mis code 111 that shows what steps were done for that college.

Notes:

- Each Service is declared `abstract` so methods used in the class to read and process data can be overriden without having
  to change the methods that call them.
- Each Service contains a copy constructor so that a customized version of the service can easily be instantiated with the
  same initial values as the one it's replacing.
- Entities in each Service are declared at the class level and can be replaced with a class that extends it. This allows 
  adding extra fields. 

> See the 111 customization for examples of each of the notes above