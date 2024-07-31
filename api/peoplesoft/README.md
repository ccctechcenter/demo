# College Adaptor's Peoplesoft adaptor

## College Adaptor Layout

College Adaptor is split into two layers:

### API Layer

The API java layer provides a WebAPI (controllers) that interfaces a college to conductor (through the service workers).
The service workers provide the communication between conductor and CA with a goal of having each college able to
receive work (data retrievals/updates) with only outgoing connections

### Service Layer
The service layer provides the Student Information System (SIS) back end interface

* Written in groovy and ran dynamically through reflection-like loading at runtime (see GroovyServiceImpl class)
* This layer is divided by each SIS Type supported: Banner, Peoplesoft, Colleague, etc
* Each SIS Type is responsible for adapting the WebAPI (java layer) to the specific SIS implementation
* Each SIS does this differently within the constraints/freedoms/capabilities of that SIS


## Peoplesoft Specific External Resources

### Peoplesoft Overview
See: https://cccnext.jira.com/wiki/spaces/INFDA/pages/846463259

### Integration Architecture
See: https://cccnext.jira.com/wiki/spaces/INFDA/pages/936346076

### Developer Resources
See: https://cccnext.jira.com/wiki/spaces/INFDA/pages/939163658

### Features / Implementations
See CCC-Glue Peoplesoft Confluence and the Peoplesoft-Projects [Bitbucket Repository Readme](https://bitbucket.org/cccnext/peoplesoft-projects/src/develop/)
