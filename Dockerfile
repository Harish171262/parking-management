FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Install wget to download the required MongoDB driver dependency
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Download the driver uber-JAR (contains bson, driver-core, and driver-sync)
RUN wget https://repo1.maven.org/maven2/org/mongodb/mongodb-driver-sync/4.11.1/mongodb-driver-sync-4.11.1.jar -O mongodb-driver.jar

COPY *.java ./
COPY public/ ./public/

# Compile with the downloaded JAR added to the classpath
RUN javac -cp ".:mongodb-driver.jar" *.java

EXPOSE 8080

# Run the server while keeping the JAR on the execution classpath
CMD ["java", "-cp", ".:mongodb-driver.jar", "ParkingServer"]