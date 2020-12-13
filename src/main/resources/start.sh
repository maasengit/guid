#! /bin/bash

BASEDIR=$(cd "$(dirname "$0")"; pwd)
echo current path:$BASEDIR

# run uuid config script
if [ -f "${BASEDIR}/start_guid.sh" ];then
. ${BASEDIR}/start_guid.sh
fi
IP_ADDRESS=`hostname -i`
echo GUID_DATACENTER_ID=$GUID_DATACENTER_ID
# get IP_127_0_0_1_GUID_WORKER_ID=1
GUID_WORKER_ID_NAME=IP_${IP_ADDRESS//\./\_}_GUID_WORKER_ID
export GUID_WORKER_ID=$(eval echo '$'"$GUID_WORKER_ID_NAME")
echo GUID_WORKER_ID=$GUID_WORKER_ID