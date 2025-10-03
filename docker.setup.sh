#!/bin/bash
# ========================================================================
# OPTION 1: Docker Desktop + Docker Contexts (RECOMMENDED)
# ========================================================================

# Install Docker Desktop (separate from Lima)
# Download from: https://www.docker.com/products/docker-desktop

# Create different Docker contexts
docker context create personal --description "Personal Docker" \
    --docker "host=unix:///var/run/docker.sock"

docker context create work --description "Work Docker (Lima)" \
    --docker "host=unix://${HOME}/.lima/docker/sock/docker.sock"

# Switch between contexts
docker context use personal  # For personal projects
docker context use work      # For work projects

# List all contexts
docker context ls

# Set default context in your shell profile
echo 'export DOCKER_CONTEXT=personal' >> ~/.zshrc  # or ~/.bashrc

# ========================================================================
# OPTION 2: Use Colima (Lightweight, Mac-specific)
# ========================================================================

# Install Colima (Container runtimes on macOS)
brew install colima

# Start Colima with a specific profile for personal use
colima start --profile personal --cpu 2 --memory 4

# Start another profile for work (if needed)
colima start --profile work --cpu 2 --memory 4

# Switch between profiles
colima stop personal
colima start personal

# Use with Docker
docker context use colima-personal

# List Colima instances
colima list