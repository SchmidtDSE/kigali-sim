# Use Eclipse Temurin JDK 21 as base image as specified in requirements
FROM quay.io/lib/eclipse-temurin:21

# Install system dependencies needed for development
RUN apt-get update && apt-get install -y \
    curl \
    git \
    wget \
    unzip \
    build-essential \
    python3 \
    python3-pip \
    chromium-browser \
    ca-certificates \
    sudo \
    nodejs \
    npm \
    && rm -rf /var/lib/apt/lists/* \
    && update-ca-certificates

# Verify installations
RUN java -version && node --version && npm --version

# Create a non-root user for development
RUN useradd -m -s /bin/bash devuser && \
    usermod -aG sudo devuser && \
    echo "devuser ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers

# Set up workspace directory
WORKDIR /workspace

# Set ownership of workspace to devuser
RUN chown -R devuser:devuser /workspace

# Switch to devuser
USER devuser

# Set environment variables for Java tools
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH=$JAVA_HOME/bin:$PATH

# Expose ports typically used for the development server or automated tests
EXPOSE 8000 8080

# Default command
CMD ["/bin/bash"]