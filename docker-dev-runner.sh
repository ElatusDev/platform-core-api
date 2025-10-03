#!/bin/bash

# --- Configuration ---
COMPOSE_FILE="docker-compose.dev.yml"
DB_INIT_DIR="./db_init"
APP_SERVICE_NAME="platform-core-api"
DB_SERVICE_NAME="multi_tenant_db"
REDIS_SERVICE_NAME="platform-core-redis"
CA_SERVICE_NAME="ca-service"
ENV_FILE=".env";

# --- Functions ---

setup_local() {
  SPRING_PROFILES_ACTIVE="local"
    if [ -f "$ENV_FILE" ]; then
        echo "Loading environment variables from $ENV_FILE..."

        # Read each line from the .env file
        # grep -v '^#' ignores lines that start with a '#' (comments)
        # xargs ensures that the export command handles lines with whitespace correctly
        export $(grep -v '^#' "$ENV_FILE" | xargs)

        echo "Environment variables loaded."
    else
        echo "Error: $ENV_FILE file not found. Please create it or check the path."
    fi
}
run_local() {

  # Check for DB container
    if ! docker-compose -f "$COMPOSE_FILE" ps "$DB_SERVICE_NAME" | grep -q "$DB_SERVICE_NAME"; then
        _start_db
    fi

    # Check for Redis container
    if ! docker-compose -f "$COMPOSE_FILE" ps "$REDIS_SERVICE_NAME" | grep -q "$REDIS_SERVICE_NAME"; then
        _start_redis
    fi

    # Check for CA container
    if ! docker-compose -f "$COMPOSE_FILE" ps "$CA_SERVICE_NAME" | grep -q "$CA_SERVICE_NAME"; then
        _start_ca
    fi
}

_start_db() {
    local volume_name="$APP_SERVICE_NAME'_db_data'"
    # Check and remove existing DB container and volume
    if docker-compose -f "$COMPOSE_FILE" ps -q "$DB_SERVICE_NAME" | grep -q .; then
        echo "Stopping and removing existing container: $DB_SERVICE_NAME"
        docker-compose -f "$COMPOSE_FILE" stop "$DB_SERVICE_NAME"
        docker-compose -f "$COMPOSE_FILE" rm -f "$DB_SERVICE_NAME"
    fi
    if docker volume ls --filter name="$volume_name" | grep -q "$volume_name"; then
        echo "Removing existing DB volume: $volume_name"
        docker volume rm "$volume_name"
    fi

    docker-compose -f "$COMPOSE_FILE" up --build --force-recreate -d "$DB_SERVICE_NAME"
    echo "Waiting for MariaDB to be ready (up to 10 seconds)..."
    sleep 10
    # --- CRITICAL FIX: Add a log command here to display the initialization process ---
    echo "--- Displaying MariaDB initialization logs (this may take a moment) ---"
    docker-compose -f "$COMPOSE_FILE" logs -f "$DB_SERVICE_NAME" & # Run in the background
    LOG_PID=$! # Capture the PID of the logs command
    sleep 5
    kill "$LOG_PID"
}

_start_redis() {
    echo "--- Starting Redis service ---"
    local volume_name="makani-helpdesk-api_redis_data"
    if docker-compose -f "$COMPOSE_FILE" ps -q "$REDIS_SERVICE_NAME" | grep -q .; then
        echo "Stopping and removing existing container: $REDIS_SERVICE_NAME"
        docker-compose -f "$COMPOSE_FILE" stop "$REDIS_SERVICE_NAME"
        docker-compose -f "$COMPOSE_FILE" rm -f "$REDIS_SERVICE_NAME"
    fi
    if docker volume ls --filter name="$volume_name" | grep -q "$volume_name"; then
        echo "Removing existing Redis volume: $volume_name"
        docker volume rm "$volume_name"
    fi
    docker-compose -f "$COMPOSE_FILE" up --build --force-recreate -d "$REDIS_SERVICE_NAME"
    echo "Redis service is up."
}

_start_ca() {
    echo "--- Starting Certificate Authority service ---"
    local volume_name="makani-helpdesk-api_ca_certs"

    # The 'up --force-recreate' flag handles removing the old container,
    # so we only need to check for and remove the volume explicitly.
    if docker volume ls --filter name="$volume_name" -q | grep -q .; then
        echo "Removing existing CA volume: $volume_name"
        docker volume rm "$volume_name"
    fi

    # Start the CA service, forcing a rebuild and recreation
    docker-compose -f "$COMPOSE_FILE" up --build --force-recreate -d "$CA_SERVICE_NAME"

    echo "Waiting for CA service to generate certificate..."

    # Use a while loop to wait for the specific log message.
    # The `grep -m 1` flag is crucial; it tells grep to exit after the first match.
    # The timeout adds robustness in case of failure.
    local timeout_seconds=5
    local start_time=$(date +%s)

    while true; do
      if docker-compose -f "$COMPOSE_FILE" logs "$CA_SERVICE_NAME" | grep -m 1 "certificate and key have been generated" > /dev/null; then
        echo "CA service is up and certificate is ready."
        break
      fi

      current_time=$(date +%s)
      elapsed_time=$((current_time - start_time))
      if [ "$elapsed_time" -ge "$timeout_seconds" ]; then
        echo "Error: Timed out waiting for the CA service to generate a certificate."
        docker-compose -f "$COMPOSE_FILE" logs "$CA_SERVICE_NAME"
        return 1
      fi

      sleep 1
    done
}

# --- Main Functions for runner actions ---

run_dev() {
  SPRING_PROFILES_ACTIVE="dev"

    # Check for DB container
    if ! docker-compose -f "$COMPOSE_FILE" ps "$DB_SERVICE_NAME" | grep -q "$DB_SERVICE_NAME"; then
        _start_db
    fi

    # Check for Redis container
    if ! docker-compose -f "$COMPOSE_FILE" ps "$REDIS_SERVICE_NAME" | grep -q "$REDIS_SERVICE_NAME"; then
        _start_redis
    fi

    # Check for CA container
    if ! docker-compose -f "$COMPOSE_FILE" ps "$CA_SERVICE_NAME" | grep -q "$CA_SERVICE_NAME"; then
        _start_ca
    fi

    # Rebuild the application image
    echo "Rebuilding the application image: $APP_SERVICE_NAME"
    docker-compose -f "$COMPOSE_FILE" build "$APP_SERVICE_NAME"

    # Start the application service, which depends on the others
    echo "Starting application service: $APP_SERVICE_NAME"
    docker-compose -f "$COMPOSE_FILE" up -d "$APP_SERVICE_NAME"
}

# --- Main Script ---
echo "Choose an action:"
echo "1. Run in DEV mode"
echo "2. Start DB or Update schema"
echo "3. Setup Local Env"
echo "4. Run in local mode (debugging)"
echo "q. Quit"
read -p "Enter your choice: " choice

case "$choice" in
    1)
      echo "--- Running app in DEV mode ---"
      run_dev
      ;;
   2)
      echo "--- Use when database schema has changed (DATA LOSS WARNING) ---"
      _start_db
      ;;
   3)
     setup_local
     ;;
   4)
     echo "---- Run app in LOCAL mode (for IDE debugging)"
     run_local
     ;;
   q)
      echo "Quitting."
      exit 0
      ;;
   *)
      echo "Invalid choice."
      exit 1
      ;;
esac

echo "Done."