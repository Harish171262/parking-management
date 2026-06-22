FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY *.java ./
COPY public/ ./public/
RUN javac *.java
EXPOSE 8080
CMD ["java", "ParkingServer"]