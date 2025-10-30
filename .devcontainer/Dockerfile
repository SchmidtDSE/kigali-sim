# Use Debian 12 (bookworm) as base and copy JDK from Eclipse Temurin
# Debian has proper chromium packages that work in Docker (no snap)
FROM debian:bookworm-slim AS base

# Copy JDK from Eclipse Temurin
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:21-jdk $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Install system dependencies needed for development
RUN apt-get update && apt-get install -y \
    curl \
    git \
    wget \
    unzip \
    build-essential \
    python3 \
    python3-pip \
    ca-certificates \
    sudo \
    && rm -rf /var/lib/apt/lists/* \
    && update-ca-certificates

# Install Chromium browser from official Debian repositories
# Debian provides chromium as a proper .deb package (no snap issues)
RUN apt-get update && apt-get install -y chromium \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js from official Debian repositories (Node.js 18.x LTS)
RUN apt-get update && apt-get install -y nodejs npm \
    && rm -rf /var/lib/apt/lists/*

# Install pnpm using npm (npm is from official Debian repos)
# Use pnpm 10.20.0 to match the packageManager field in package.json
RUN npm install -g pnpm@10.20.0

# Verify installations
RUN java -version && node --version && pnpm --version

# Set environment variables for Puppeteer to use system Chromium
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium

# Create a non-root user for development
# VS Code's updateRemoteUserUID will automatically remap the UID/GID to match the host user
ARG USERNAME=devuser
ARG USER_UID=1000
ARG USER_GID=1000

# Create user. If UID/GID already exist, reuse them (common in base images)
RUN if getent group $USER_GID; then \
        GROUP_NAME=$(getent group $USER_GID | cut -d: -f1); \
    else \
        groupadd --gid $USER_GID $USERNAME; \
        GROUP_NAME=$USERNAME; \
    fi \
    && if getent passwd $USER_UID; then \
        # If user exists, just modify it \
        usermod -l $USERNAME -d /home/$USERNAME -m $(getent passwd $USER_UID | cut -d: -f1) || true; \
    else \
        useradd --uid $USER_UID --gid $USER_GID -m -s /bin/bash $USERNAME; \
    fi \
    && echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME

# Set up workspace directory
WORKDIR /workspace

# Switch to non-root user
USER $USERNAME

# Set environment variables for Java tools
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH=$JAVA_HOME/bin:$PATH

# Expose ports typically used for the development server or automated tests
EXPOSE 8000 8080

# Default command
CMD ["/bin/bash"]