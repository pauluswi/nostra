#!/usr/bin/env bash
set -euo pipefail

DATA_DIR="${1:-$PWD/.local-postgres}"
SOCKET_DIR="${DATA_DIR}/.sockets"
PORT="5432"
DB_NAME="nostra"
DB_USER="nostra"
DB_PASSWORD="nostra"

if ! command -v pg_ctl >/dev/null 2>&1; then
  echo "PostgreSQL server tools are not installed locally."
  echo "Install PostgreSQL first, for example:"
  echo "  macOS (Homebrew): brew install postgresql@16"
  echo "  Ubuntu/Debian: sudo apt-get install postgresql"
  exit 1
fi

mkdir -p "$DATA_DIR" "$SOCKET_DIR"

if [ ! -f "$DATA_DIR/PG_VERSION" ]; then
  echo "Initializing PostgreSQL data directory at $DATA_DIR"
  initdb -D "$DATA_DIR" --username=postgres --auth=trust >/dev/null
fi

if ! pg_ctl -D "$DATA_DIR" -l "$DATA_DIR/server.log" -o "-p $PORT -k $SOCKET_DIR" status >/dev/null 2>&1; then
  echo "Starting PostgreSQL on port $PORT"
  pg_ctl -D "$DATA_DIR" -l "$DATA_DIR/server.log" -o "-p $PORT -k $SOCKET_DIR" start
fi

export PGHOST="$SOCKET_DIR"
export PGUSER="postgres"
export PGDATABASE="postgres"

if ! psql -h "$SOCKET_DIR" -U postgres -d postgres -tc "SELECT 1 FROM pg_roles WHERE rolname='${DB_USER}'" | grep -q 1; then
  createuser -h "$SOCKET_DIR" -U postgres --superuser "$DB_USER" >/dev/null 2>&1 || true
fi

if ! psql -h "$SOCKET_DIR" -U postgres -d postgres -tc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" | grep -q 1; then
  createdb -h "$SOCKET_DIR" -U postgres "$DB_NAME" >/dev/null 2>&1 || true
fi

psql -h "$SOCKET_DIR" -U postgres -d postgres -v ON_ERROR_STOP=1 <<SQL >/dev/null
ALTER USER "$DB_USER" WITH PASSWORD '$DB_PASSWORD';
SQL

echo "PostgreSQL local parity database is ready."
echo "JDBC URL: jdbc:postgresql://localhost:5432/$DB_NAME"
echo "Credentials: $DB_USER / $DB_PASSWORD"
echo "Run: APP_PROFILE=postgres DB_URL='jdbc:postgresql://localhost:5432/$DB_NAME' DB_USERNAME=$DB_USER DB_PASSWORD=$DB_PASSWORD ./mvnw spring-boot:run"
