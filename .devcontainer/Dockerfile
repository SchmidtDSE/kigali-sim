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

# Chrome
RUN apt-get update && apt-get install -y chromium \
    && rm -rf /var/lib/apt/lists/*

# Node
RUN apt-get update && apt-get install -y nodejs npm \
    && rm -rf /var/lib/apt/lists/*

RUN npm install -g pnpm@10.20.0

# Check java
RUN java -version && node --version && pnpm --version

# Prepare puppeteer for front-end tests
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium

# Create user or reuse if UID/GID already exist
ARG USERNAME=devuser
ARG USER_UID=1000
ARG USER_GID=1000

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

# Expose ports for development server
EXPOSE 8000 8080

CMD ["/bin/bash"]