pallene
=====

[![Travis](https://img.shields.io/travis/io7m/pallene.svg?style=flat-square)](https://travis-ci.org/io7m/pallene)
[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.pallene/com.io7m.pallene.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.pallene%22)
[![Codacy grade](https://img.shields.io/codacy/grade/f638fbf97971498f9dd2b5d6e9a5f586.svg?style=flat-square)](https://www.codacy.com/app/github_79/pallene)

An ultra-minimal single-file static web server.

![pallene](./src/site/resources/pallene.jpg?raw=true)

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

The `pallene.jar` command reads the contents of `f` into memory,
opens a socket and binds it to address `a`, port `p`. It then chroots
to directory `c` and switches to group ID `g` and user ID `u`. Then,
for each client connection 'k', it reads and discards at most `1024`
octets from `k` and then responds with an HTTP `HTTP/1.0 200 OK`
response, serving the contents of `f` with MIME type `m`. It then
closes 'k'.

The command does NOT fork into the background; it is designed to run
under a process supervision system and simply aborts with exit code
`1` on any error. The command logs the addresses of connecting clients,
but does not log any data or headers the clients send.

See the `examples` directory in this source repository for example
scripts that can, for example, run the server in a memory-restricted
Linux `cgroup` and run with GC settings optimized for tiny heaps.

