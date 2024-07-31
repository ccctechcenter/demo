# College Adaptor Host Setup

This document explains how to add an on-prem host to our College Adaptor cluster and Rancher 2 for management.

### Firewall
 - Incoming - N/A
 - Outgoing - 443/TCP

### Operating System
- Ubuntu 18.04 - 22.04

### Installation

The following script installs any software and binaries needed for K3s to operate.

Set variables and install
```
K3S_VERSION="v1.27.7+k3s2" # See https://bitbucket.org/cccnext/eks-clusters/src/develop/college-adaptor/stack.json#lines-2 for currently deployed version (master branch for pilot/prod)
MISCODE=002
NAME=002-south-orange-prod
SECRET=ThisIsNotTheActualSecret
HOST=k3s-college-adaptor.ci.ccctechcenter.org

curl -sfL https://get.k3s.io | INSTALL_K3S_VERSION="$K3S_VERSION" INSTALL_K3S_EXEC="agent \
    --server https://$HOST \
    --node-label miscode=$MISCODE \
    --node-taint miscode=$MISCODE:NoSchedule \
    --node-name $NAME \
    --token=$SECRET" \
    sh -s -
```