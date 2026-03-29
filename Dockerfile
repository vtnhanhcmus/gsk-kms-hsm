# Build SDK + Payara Micro (bundle), chạy bằng JRE 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
COPY sdk/pom.xml sdk/
COPY sdk/src sdk/src
COPY examples/payara-envelope-demo/pom.xml examples/payara-envelope-demo/
COPY examples/payara-envelope-demo/src examples/payara-envelope-demo/src

RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# finalName=ROOT → ROOT.war → context root "/", JAR tên ROOT-microbundle.jar
COPY --from=build /build/examples/payara-envelope-demo/target/ROOT-microbundle.jar ./app.jar

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# --nocluster: tránh Hazelcast/cluster trong môi trường container đơn giản
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--nocluster"]
