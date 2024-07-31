
## Colleague Communication Tunnel

Our Colleague Integration is done through an application server; not directly to some database.
We interface to that app server using the DataTel Messaging Interface (DMI) client. 
While the DMI protocol is standard, it's not very friendly. So we (CCCTC) have abstracted it into a client.
The code for this abstraction layer can be found in bitbucket here: https://bitbucket.org/cccnext/colleague-dmi-client/src/master/

## Colleague Workspace

The DMI client allows the college adaptor to communicate with the App server.
The App Server is where we all the metadata of our colleague objects are stored. 
Most of our integration uses records called an 'Entity', but we also have a few functions called 'Transactions'

The Colleague Objects are maintained in the App Server using a program called 'Colleague Studio'

Colleague Studio organizes the different colleague objects into 'Projects'
These 'Projects' are loosely associated into a 'Workspace'

Thus, the Workspace is the development environment (and code) for our Colleague Integration.

Colleague is VERY proprietary and thus doesn't store the code and metadata in plain text.
Instead, it is stored in an encoded format split across literally hundreds (if not thousands) of
tiny files for any given colleague object. Including those many, many, files into this repo is troublesome.

Thus, instead of nesting the colleague workspace into this repository, we keep it separate in 
bitbucket here: https://bitbucket.org/cccnext/colleague-api-envision/src/develop/
