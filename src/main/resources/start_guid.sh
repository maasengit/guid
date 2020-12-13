#! /bin/bash
# ====================================
# The following arguments are required
# ====================================
export GUID_DATACENTER_ID=1
export IP_192_168_1_100_GUID_WORKER_ID=1
export IP_192_168_1_101_GUID_WORKER_ID=2


# ====================
# optional arguments, max is 2^41=69年
# ====================
# start time: 2020-01-01 00:00:00
# export GUID_START_EPOCH=1577836800000
# allow max anticlock
# export GUID_CLOCK_BACK_THRESHOLD=50
# ====================
# sum of the following MUST be 63
# ====================
### timestamp's length
# export GUID_TIMESTAMP_LEN=41
### datacenter id's length, max is 2^3=8
# export GUID_DATACENTER_ID_LEN=3
### worker id's length, max is 2^7=128
# export GUID_WORKER_ID_LEN=7
### remain length, max is 2^2 - 1=3
# export GUID_REMAIN_LEN=2
### sequence's length, max is 2^10=1024
# export GUID_SEQUENCE_LEN=10