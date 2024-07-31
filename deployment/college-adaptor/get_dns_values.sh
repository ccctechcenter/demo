#!/bin/bash

# Usage:
# ./get_dns_values.sh <env> <miscode>
# ./get_dns_values.sh ci 002
# 
ENV=$1
MISCODE=$2

export DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
export VALUES_FILE="$DIR/values-dns.yaml"

CONFIG=$(cat $DIR/college-adaptor-config/dns-config.yaml | yq e ".$ENV.$MISCODE" -)
rm $VALUES_FILE || true
touch $VALUES_FILE

if [[ "$CONFIG" != "null" ]]; then
  cat <<EOT > $VALUES_FILE
service:
  dnsConfig:
EOT
  cat $DIR/college-adaptor-config/dns-config.yaml | yq e ".$ENV.$MISCODE" - | sed 's/^/    /' >> $VALUES_FILE
fi
cat $VALUES_FILE