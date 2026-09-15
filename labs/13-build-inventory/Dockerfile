FROM eclipse-temurin:21-jre-alpine@sha256:974b08960c5d96694c780e65b2d5705268ab1e1ca1a0dd0caf4ba6c3fe34d699
WORKDIR /opt/book21
COPY app.jar app.jar
COPY export-helper.jar tools/export-helper.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/opt/book21/app.jar"]
