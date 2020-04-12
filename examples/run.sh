#!/bin/sh

# -Dcom.sun.management.jmxremote.port=3333 \
# -Dcom.sun.management.jmxremote.ssl=false \
# -Dcom.sun.management.jmxremote.authenticate=false \

exec
   java \
  -Xmx16m \
  -Xms16m \
  -XX:MetaspaceSize=32m \
  -XX:MaxMetaspaceSize=32m \
  -XX:-UseCompressedClassPointers \
  -XX:TieredStopAtLevel=2 \
  -XX:+UseSerialGC \
  -jar target/com.io7m.pallene-0.0.1-main.jar \
  --address "::1" \
  --chroot "/pallene" \
  --file "src/main/resources/com/io7m/pallene/index.xhtml" \
  --group-id 996 \
  --user-id 996

