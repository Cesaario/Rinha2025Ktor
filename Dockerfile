FROM alpine/java:21-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file into the container
COPY build/libs/rinhabackend-all.jar app.jar

# Define the command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]