# Multi-stage build: compile the WAR with Maven, then run it on Tomcat.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage — Tomcat 10 (Jakarta EE, matches jakarta.servlet-api in pom.xml)
FROM tomcat:10.1-jdk17-temurin
# Deploy as ROOT so the app answers at "/" instead of "/shortnx"
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /app/target/shortnx.war /usr/local/tomcat/webapps/ROOT.war

# Render provides the PORT env var and expects the app to listen on it.
# Tomcat's default HTTP connector is hardcoded to 8080 in server.xml, so we
# rewrite it at container start using Render's PORT value.
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
