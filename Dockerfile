FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Install wget to download dependencies
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Download the complete legacy Uber-JAR that contains Sync, Core, and BSON together
RUN wget https://repo1.maven.org/maven2/org/mongodb/mongo-java-driver/3.12.14/mongo-java-driver-3.12.14.jar -O mongodb-driver.jar

COPY *.java ./
COPY public/ ./public/

# Compile using the complete uber-jar
RUN javac -cp ".:mongodb-driver.jar" *.java

EXPOSE 8080

# Run the server with the correct uber-jar classpath
CMD ["java", "-cp", ".:mongodb-driver.jar", "ParkingServer"]