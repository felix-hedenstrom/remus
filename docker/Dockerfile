# syntax=docker/dockerfile:1

FROM sbtscala/scala-sbt:eclipse-temurin-jammy-21.0.4_7_1.10.1_3.5.1 AS build
WORKDIR /build

COPY project project
COPY build.sbt .
COPY modules modules

# Links the Scala.js client, copies the bundle into the server's resources,
# then packages the server jar and its full runtime classpath as plain jars
# (no sbt-assembly/native-packager: neither is published for sbt 2.x yet).
RUN sbt server/copyClientAssets server/Compile/packageBin server/copyRuntimeDeps

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /build/target/out/jvm/scala-*/server/server_*.jar app.jar
COPY --from=build /build/target/out/jvm/scala-*/server/docker-libs/ lib/

ENV PORT=8080
ENV DB_PATH=/data/pellegrino.db
VOLUME ["/data"]
EXPOSE 8080

CMD ["sh", "-c", "java -cp app.jar:lib/* dnd.server.Main"]
