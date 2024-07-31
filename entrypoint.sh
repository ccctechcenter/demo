#!/bin/bash

set -e 

echo "command: $@"

# Run the "real" Docker entrypoint 
exec $@
