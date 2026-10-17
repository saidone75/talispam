# syntax=docker/dockerfile:1

ARG GRAALVM_IMAGE=ghcr.io/graalvm/native-image-community:25i4
FROM ${GRAALVM_IMAGE} AS builder

ARG CLOJURE_CLI_VERSION=1.12.5.1664
ARG NATIVE_OPTIMIZATION=s
ARG NATIVE_MARCH=compatibility
WORKDIR /application

RUN curl -fsSL "https://download.clojure.org/install/clojure-tools-${CLOJURE_CLI_VERSION}.tar.gz" \
        -o /tmp/clojure-tools.tar.gz \
    && tar xzf /tmp/clojure-tools.tar.gz -C /tmp \
    && mkdir -p /usr/local/lib/clojure/libexec \
    && cp /tmp/clojure-tools/deps.edn /tmp/clojure-tools/example-deps.edn /tmp/clojure-tools/tools.edn /usr/local/lib/clojure/ \
    && cp /tmp/clojure-tools/*.jar /usr/local/lib/clojure/libexec/ \
    && sed 's@PREFIX@/usr/local/lib/clojure@g' /tmp/clojure-tools/clojure > /usr/local/bin/clojure \
    && chmod +x /usr/local/bin/clojure \
    && rm -rf /tmp/clojure-tools /tmp/clojure-tools.tar.gz

COPY deps.edn build.clj ./
RUN --mount=type=cache,target=/root/.m2 clojure -P -M \
    && clojure -P -T:build

COPY src ./src
COPY resources ./resources
COPY reflectconfig.json ./
RUN --mount=type=cache,target=/root/.m2 \
    clojure -T:build uber \
    && mkdir -p /out \
    && set -- target/uberjar/*-standalone.jar \
    && TALISPAM_BINARY="$(basename "$1" -standalone.jar)" \
    && native-image -J-XX:MaxRAMPercentage=60.0 \
        -O${NATIVE_OPTIMIZATION} -march=${NATIVE_MARCH} \
        -H:ReflectionConfigurationFiles=./reflectconfig.json \
        --features=clj_easy.graal_build_time.InitClojureClasses \
        -jar "$1" "/out/$TALISPAM_BINARY" \
    && test -x "/out/$TALISPAM_BINARY"

# Export only the executable with Docker BuildKit's local output.
FROM scratch AS artifact
COPY --from=builder /out/ /
