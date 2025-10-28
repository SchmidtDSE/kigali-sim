# Use Eclipse Temurin JDK 21 as base image
FROM eclipse-temurin:21-jre

# Install wget for downloading the jar file
RUN apt-get update && apt-get install -y wget

# Download the latest Kigali Sim jar
RUN wget https://kigalisim.org/kigalisim-fat.jar -O /app/kigalisim-fat.jar

# Set working directory
WORKDIR /working

# Default command to show help
ENTRYPOINT ["java", "-jar", "/app/kigalisim-fat.jar"]
CMD ["--help"]
