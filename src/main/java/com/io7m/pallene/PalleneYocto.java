/*
 * Copyright © 2017 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.pallene;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.DefaultConsole;
import jnr.ffi.LibraryLoader;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Web server.
 */

public final class PalleneYocto
{
  private static final Logger LOG = LoggerFactory.getLogger(PalleneYocto.class);

  private PalleneYocto()
  {

  }

  private static final class CommandLineOptions
  {
    CommandLineOptions()
    {

    }

    @Parameter(
      names = "--chroot",
      description = "Chroot to this directory after starting the server.",
      required = true)
    private Path chroot_path;

    @Parameter(
      names = "--user-id",
      description = "Switch to this user ID after starting the server.",
      required = true)
    private int user_id;

    @Parameter(
      names = "--group-id",
      description = "Switch to this group ID after starting the server.",
      required = true)
    private int group_id;

    @Parameter(
      names = "--mime-type",
      description = "The MIME type that will be used for the served file.")
    private String mime_type = "text/html";

    @Parameter(
      names = "--file",
      description = "The file that will be served for any request.",
      required = true)
    private Path file;

    @Parameter(
      names = "--port",
      description = "The port to which to bind the server.")
    private int port = 80;

    @Parameter(
      names = "--address",
      description = "The address to which to bind the server.",
      required = true)
    private String address;

    @Parameter(
      names = "--help",
      description = "Show this help message.",
      help = true)
    private boolean help;
  }

  /**
   * Main entry point.
   * @param args Command line arguments
   */

  public static void main(
    final String[] args)
  {
    System.exit(go(args));
  }

  // CHECKSTYLE:OFF
  private static int go(
    final String[] args)
  {
    final CommandLineOptions options =
      new CommandLineOptions();

    final JCommander commander =
      JCommander.newBuilder()
        .addObject(options)
        .programName("pallene")
        .build();

    try {
      commander.parse(args);
    } catch (final ParameterException e) {
      LOG.error("parameter error: {}", e.getMessage());
      commander.setConsole(new DefaultConsole(System.err));
      commander.usage();
      return 1;
    }

    final POSIX posix = POSIXFactory.getNativePOSIX();
    final PalleneChrootType chroot = loadChrootFunction();

    final byte[] file_data;
    try {
      file_data = Files.readAllBytes(options.file);
    } catch (final IOException e) {
      LOG.error("Could not read file: ", e);
      return 1;
    }

    final ExecutorService exec =
      Executors.newSingleThreadExecutor(r -> {
        final Thread th = new Thread(r);
        th.setName("com.io7m.pallene." + th.getId());
        th.setDaemon(true);
        return th;
      });

    final Sandbox sandbox =
      new Sandbox(
        posix,
        options.group_id,
        options.user_id,
        chroot,
        options.chroot_path);

    final InetSocketAddress local_address =
      new InetSocketAddress(options.address, options.port);

    final ServerSocket socket;
    try {
      socket = new ServerSocket();
      socket.setReuseAddress(true);
    } catch (final IOException e) {
      LOG.error("Could not create server socket: ", e);
      return 1;
    }

    try {
      socket.bind(local_address);
    } catch (final IOException e) {
      LOG.error("Could not bind server socket: ", e);
      return 1;
    }

    try {
      sandbox.sandbox();
    } catch (final IOException e) {
      LOG.error("Could not sandbox: ", e);
      return 1;
    }

    while (true) {
      try {
        final Socket client = socket.accept();
        client.setTcpNoDelay(true);
        client.setKeepAlive(false);
        client.setSoTimeout(5000);
        final Future<?> future =
          exec.submit(() -> serveClient(options, file_data, client));
        LOG.trace("submitted: {}", future);
      } catch (final IOException e) {
        LOG.error("Could not accept client: ", e);
        try {
          Thread.sleep(1000L);
        } catch (final InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  private static void serveClient(
    final CommandLineOptions options,
    final byte[] file_data,
    final Socket client)
  {
    try {
      if (LOG.isInfoEnabled()) {
        LOG.info("client: {}", client.getRemoteSocketAddress());
      }

      try {
        final byte[] buffer = new byte[1024];
        final InputStream in = client.getInputStream();
        final int read = in.read(buffer);
        LOG.trace("read: {}", Integer.valueOf(read));
      } catch (final IOException e) {
        LOG.error("Could not read from client: ", e);
        return;
      }

      final OutputStream out;
      try {
        out = client.getOutputStream();
      } catch (final IOException e) {
        LOG.error("Could not retrieve client output stream: ", e);
        return;
      }

      try {
        final BufferedWriter writer =
          new BufferedWriter(
            new OutputStreamWriter(out, StandardCharsets.US_ASCII));

        writer.append("HTTP/1.0 200 OK");
        writer.append("\r\n");
        writer.append("Content-Type: ");
        writer.append(options.mime_type);
        writer.append("\r\n");
        writer.append("Content-Length: ");
        writer.append(Integer.toUnsignedString(file_data.length));
        writer.append("\r\n");
        writer.append("Connection: close");
        writer.append("\r\n");
        writer.append("\r\n");
        writer.flush();
      } catch (final IOException e) {
        LOG.error("Could not write header: ", e);
        return;
      }

      try {
        out.write(file_data);
        out.flush();
      } catch (final IOException e) {
        LOG.error("Could not write body: ", e);
        return;
      }

    } finally {
      try {
        client.close();
      } catch (final IOException e) {
        LOG.error("Error closing client socket: ", e);
      }
    }
  }
  // CHECKSTYLE:ON

  private static PalleneChrootType loadChrootFunction()
  {
    final LibraryLoader<PalleneChrootType> c_loader =
      LibraryLoader.create(PalleneChrootType.class);
    c_loader.failImmediately();
    return c_loader.load("c");
  }

  private static final class Sandbox
  {
    private final POSIX posix;
    private final int sandbox_gid;
    private final int sandbox_uid;
    private final Path sandbox_chroot_path;
    private final PalleneChrootType sandbox_chroot_func;

    Sandbox(
      final POSIX in_posix,
      final int gid,
      final int uid,
      final PalleneChrootType chroot_func,
      final Path chroot)
    {
      this.posix = Objects.requireNonNull(in_posix, "POSIX");
      this.sandbox_gid = gid;
      this.sandbox_uid = uid;
      this.sandbox_chroot_func =
        Objects.requireNonNull(chroot_func, "chroot_func");
      this.sandbox_chroot_path =
        Objects.requireNonNull(chroot, "chroot");
    }

    void sandbox()
      throws IOException
    {
      LOG.debug("sandboxing");

      chroot(this.posix, this.sandbox_chroot_path, this.sandbox_chroot_func);
      setGid(this.posix, this.sandbox_gid);
      setUid(this.posix, this.sandbox_uid);
    }

    private static void setUid(
      final POSIX posix,
      final int sandbox_uid)
      throws IOException
    {
      LOG.debug("setuid {}", Integer.valueOf(sandbox_uid));
      final int e = posix.setuid(sandbox_uid);
      if (e == -1) {
        final String message =
          posix.strerror(posix.errno());
        throw new IOException(
          "Could not setuid to " + sandbox_uid + ": " + message);
      }
    }

    private static void setGid(
      final POSIX posix,
      final int sandbox_gid)
      throws IOException
    {
      LOG.debug("setgid {}", Integer.valueOf(sandbox_gid));
      final int e = posix.setgid(sandbox_gid);
      if (e == -1) {
        final String message =
          posix.strerror(posix.errno());
        throw new IOException(
          "Could not setgid to " + sandbox_gid + ": " + message);
      }
    }

    private static void chroot(
      final POSIX posix,
      final Path sandbox_chroot_path,
      final PalleneChrootType sandbox_chroot_func)
      throws IOException
    {
      {
        LOG.debug("chrooting to {}", sandbox_chroot_path);
        final int e = sandbox_chroot_func.chroot(sandbox_chroot_path.toString());
        if (e == -1) {
          throw new IOException(
            "sandbox_chroot_path failed: " + posix.strerror(posix.errno()));
        }
      }

      {
        LOG.debug("chdir to /");
        final int e = posix.chdir("/");
        if (e == -1) {
          throw new IOException(
            "chdir failed: " + posix.strerror(posix.errno()));
        }
      }
    }
  }
}

