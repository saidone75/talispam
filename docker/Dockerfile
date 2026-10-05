# syntax=docker/dockerfile:1

ARG GRAALVM_IMAGE=ghcr.io/graalvm/native-image-community:25i4
FROM ${GRAALVM_IMAGE} AS builder

ARG LEIN_VERSION=2.12.0
ENV LEIN_ROOT=1
WORKDIR /application

RUN curl -fsSL "https://raw.githubusercontent.com/technomancy/leiningen/${LEIN_VERSION}/bin/lein" \
        -o /usr/local/bin/lein \
    && chmod +x /usr/local/bin/lein \
    && lein version

COPY project.clj ./
RUN --mount=type=cache,target=/root/.m2 lein deps

COPY src ./src
COPY reflectconfig.json ./
RUN --mount=type=cache,target=/root/.m2 \
    lein uberjar \
    && mkdir -p /out \
    && native-image -J-XX:MaxRAMPercentage=60.0 -march=compatibility \
        -H:ReflectionConfigurationFiles=./reflectconfig.json \
        --features=clj_easy.graal_build_time.InitClojureClasses \
        -jar target/uberjar/*-standalone.jar /out/talispam \
    && test -x /out/talispam

# Export only the executable with Docker BuildKit's local output.
FROM scratch AS artifact
COPY --from=builder /out/talispam /talispam
