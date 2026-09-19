# Multi-stage image for the packaged WAR.
# Stage 1 compiles and packages with Maven, stage 2 is a Tomcat 10.1 runtime holding only the WAR.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy the poms first so Docker caches the dependency download until a pom changes.
COPY pom.xml ./
COPY core/pom.xml core/pom.xml
COPY core/common/pom.xml core/common/pom.xml
COPY core/auth/pom.xml core/auth/pom.xml
COPY core/catalog/pom.xml core/catalog/pom.xml
COPY core/customer/pom.xml core/customer/pom.xml
COPY core/order/pom.xml core/order/pom.xml
COPY ws/pom.xml ws/pom.xml
RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY core ./core
COPY ws ./ws
# Tests are skipped here: the DAO tests need Testcontainers, i.e. a Docker daemon that a build
# stage does not have. They run in CI with mvn verify instead.
RUN mvn -B -q package -DskipTests

FROM tomcat:10.1-jre17-temurin
# The README documents the API under /spring-jdbc-tutorial/api/v1: the WAR name is the context path.
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /build/ws/target/ws.war /usr/local/tomcat/webapps/spring-jdbc-tutorial.war

# Run as a non-root user so a compromised process has no rights on the container.
RUN useradd --system --no-create-home spring \
    && chown -R spring /usr/local/tomcat/webapps /usr/local/tomcat/logs /usr/local/tomcat/temp /usr/local/tomcat/work /usr/local/tomcat/conf
USER spring

EXPOSE 8080
CMD ["catalina.sh", "run"]
