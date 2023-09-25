# Use the official Java 17 base image
FROM openjdk:17

# Set the working directory in docker
WORKDIR /app

# Copy the jar file into the container
COPY target/*.jar /app/bootstrapnode.jar

# Command to run the application
ENTRYPOINT ["java", "-jar", "/app/bootstrapnode.jar"]
