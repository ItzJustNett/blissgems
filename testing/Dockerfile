# BlissGems test container: Paper server + two headless Fabric clients (the auk-control mod
# drives them over HTTP) + the test runner, all in one container. Everything listens on
# 127.0.0.1 only. Dependencies come from testing/.cache, filled by deps.sh (pinned in deps.lock).

# --- the bot mod, built from the pinned source ---
FROM eclipse-temurin:21-jdk-jammy AS mod
COPY .cache/aukcontrol-src /src
WORKDIR /src
RUN ./gradlew --no-daemon -q build -x test && cp build/libs/aukcontrol-1.0.0.jar /aukcontrol.jar

# --- the test image ---
FROM eclipse-temurin:21-jdk-jammy
RUN apt-get update && apt-get install -y --no-install-recommends \
      xvfb xauth libgl1-mesa-dri libglx-mesa0 libgl1 libegl1 libxrandr2 libxxf86vm1 libxi6 libxtst6 \
      libxcursor1 libxinerama1 libpulse0 libopenal1 python3 python3-pip python3-yaml maven git zip unzip \
      procps curl ca-certificates \
  && rm -rf /var/lib/apt/lists/* && pip3 install --no-cache-dir portablemc==4.4.1 pillow==10.4.0

# Minecraft 1.21.1 + Fabric (downloaded once into the image; the bots share it)
ARG CLIENT_VERSION=fabric:1.21.1:0.16.10
RUN python3 -m portablemc --main-dir /opt/mc --work-dir /opt/mc start "$CLIENT_VERSION" --dry --jvm "$(command -v java)"
COPY .cache/client/fabric-api.jar /opt/botmods/fabric-api.jar
COPY --from=mod /aukcontrol.jar /opt/botmods/aukcontrol.jar

# Paper, patched at build time so the server starts without internet
COPY .cache/server/ /opt/deps/
RUN mkdir -p /opt/paper && cp /opt/deps/paper-*.jar /opt/paper/paper.jar \
  && cd /opt/paper && java -Dpaperclip.patchonly=true -jar paper.jar >/dev/null

ENV LIBGL_ALWAYS_SOFTWARE=1
ENTRYPOINT ["/repo/testing/container/entrypoint.sh"]
