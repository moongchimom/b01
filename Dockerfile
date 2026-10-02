FROM eclipse-temurin:17-jdk-jammy
COPY build/libs/b01-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8380
CMD ["java" , "-jar" , "/app.jar"]