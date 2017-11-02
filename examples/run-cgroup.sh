#!/bin/sh -ex

cgcreate -g memory:/pallene
echo $((32 * 1024 * 1024)) > /sys/fs/cgroup/memory/pallene/memory.limit_in_bytes
echo $((32 * 1024 * 1024)) > /sys/fs/cgroup/memory/pallene/memory.memsw.limit_in_bytes

