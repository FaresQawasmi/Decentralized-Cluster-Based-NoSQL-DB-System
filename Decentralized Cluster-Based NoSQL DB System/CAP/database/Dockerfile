# Use the official Java 17 base image
FROM openjdk:17

# Set the working directory in docker
WORKDIR /app

# Copy the jar file into the container
COPY target/database-0.0.1-SNAPSHOT.jar /app/database.jar

# Command to run the application
CMD ["java", "-jar", "/app/database.jar"]
