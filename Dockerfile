# 1) Build the WAR with Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package

# 2) Stock Payara Micro image, it already contains the Agentic AI implementation.
#    ROOT.war is deployed at http://localhost:8080/
FROM payara/micro:7.2026.9
COPY --from=build /app/target/ROOT.war /opt/payara/deployments/ROOT.war
