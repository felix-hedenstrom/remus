# syntax=docker/dockerfile:1

FROM sbtscala/scala-sbt:eclipse-temurin-jammy-21.0.4_7_1.10.1_3.5.1 AS build
WORKDIR /build

COPY project project
COPY build.sbt .
COPY modules modules

# packageBin pulls in the Scala.js client build automatically (it's wired into
# Compile/resourceGenerators), so this one task links the client, bundles it
# into the jar's resources, and packages the server jar. copyRuntimeDeps then
# copies the runtime classpath as plain jars (no sbt-assembly/native-packager:
# neither is published for sbt 2.x yet).
RUN sbt server/Compile/packageBin server/copyRuntimeDeps

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /build/target/out/jvm/scala-*/server/server_*.jar app.jar
COPY --from=build /build/target/out/jvm/scala-*/server/docker-libs/ lib/

ENV PORT=8080
ENV DB_PATH=/data/remus.db
VOLUME ["/data"]
EXPOSE 8080

CMD ["sh", "-c", "java -cp app.jar:lib/* nu.fxh.remus.server.Main"]
