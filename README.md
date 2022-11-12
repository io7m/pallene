pallene
===

[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.pallene/com.io7m.pallene.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.pallene%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/com.io7m.pallene/com.io7m.pallene.svg?style=flat-square)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/io7m/pallene/)
[![Codecov](https://img.shields.io/codecov/c/github/io7m/pallene.svg?style=flat-square)](https://codecov.io/gh/io7m/pallene)

![pallene](./src/site/resources/pallene.jpg?raw=true)

| JVM | Platform | Status |
|-----|----------|--------|
| OpenJDK (Temurin) Current | Linux | [![Build (OpenJDK (Temurin) Current, Linux)](https://img.shields.io/github/workflow/status/io7m/pallene/main.linux.temurin.current)](https://github.com/io7m/pallene/actions?query=workflow%3Amain.linux.temurin.current)|
| OpenJDK (Temurin) LTS | Linux | [![Build (OpenJDK (Temurin) LTS, Linux)](https://img.shields.io/github/workflow/status/io7m/pallene/main.linux.temurin.lts)](https://github.com/io7m/pallene/actions?query=workflow%3Amain.linux.temurin.lts)|
| OpenJDK (Temurin) Current | Windows | [![Build (OpenJDK (Temurin) Current, Windows)](https://img.shields.io/github/workflow/status/io7m/pallene/main.windows.temurin.current)](https://github.com/io7m/pallene/actions?query=workflow%3Amain.windows.temurin.current)|
| OpenJDK (Temurin) LTS | Windows | [![Build (OpenJDK (Temurin) LTS, Windows)](https://img.shields.io/github/workflow/status/io7m/pallene/main.windows.temurin.lts)](https://github.com/io7m/pallene/actions?query=workflow%3Amain.windows.temurin.lts)|
## Usage

```
pallene.jar \
  --address a \
  --port p \
  --chroot c \
  --file f \
  --group-id g \
  --user-id u \
  --mime-type m
```

The `pallene.jar` command reads the contents of `f` into memory, opens
a socket `s` and binds it to address `a`, port `p`. It then chroots
to directory `c` and switches to group ID `g` and user ID `u`. Then,
for each client connection `k` on `s`, it reads and discards at most
`1024` octets from `k` and then responds with an HTTP `HTTP/1.0 200
OK` response, serving the contents of `f` with MIME type `m`. It then
closes `k`.

The command does NOT fork into the background; it is designed to run
under a process supervision system and simply aborts with exit code
`1` on any error. The command logs the addresses of connecting clients,
but does not log any data or headers the clients send. The command only
works on POSIX-compatible systems.

See the `examples` directory in this source repository for example
scripts that can, for example, run the server in a memory-restricted
Linux `cgroup` and run with GC settings optimized for tiny heaps.


