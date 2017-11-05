#!/bin/sh -ex

cgcreate -g memory:/pallene
echo $((64 * 1024 * 1024)) > /sys/fs/cgroup/memory/pallene/memory.memsw.limit_in_bytes
echo $((64 * 1024 * 1024)) > /sys/fs/cgroup/memory/pallene/memory.limit_in_bytes

exec cgexec -g memory:/pallene "$@"
