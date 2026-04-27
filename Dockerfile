# syntax=docker/dockerfile:1.7

FROM sbtscala/scala-sbt:eclipse-temurin-21.0.8_9_1.12.9_3.8.3 AS builder
WORKDIR /build

COPY project/build.properties project/build.properties
COPY project/plugins.sbt project/plugins.sbt
COPY build.sbt .
RUN sbt -Dsbt.color=false -Dsbt.log.noformat=true update

COPY src src
RUN sbt -Dsbt.color=false -Dsbt.log.noformat=true package

RUN mkdir -p /dist/lib \
 && cp target/scala-*/*.jar /dist/lib/ \
 && sbt --error -Dsbt.color=false -Dsbt.log.noformat=true \
        'export Runtime / dependencyClasspath' \
    | tail -n 1 \
    | tr ':' '\n' \
    | grep '\.jar$' \
    | xargs -I {} cp {} /dist/lib/

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /dist/lib /app/lib
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/app/lib/*", "cz.dusanrychnovsky.lifemap.Main"]
