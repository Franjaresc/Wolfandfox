#!/bin/sh
# Checks a running full stack over real HTTP. Used by CI, and useful after `docker compose up --build`.
set -eu

FRONTEND_URL="http://localhost:${FRONTEND_PORT:-8081}"
BACKEND_URL="http://localhost:${BACKEND_PORT:-8080}"
CLOSURES="/api/v1/route-closures"

body=$(mktemp)
headers=$(mktemp)
trap 'rm -f "$body" "$headers"' EXIT

fail() {
  echo "FAILED: $1" >&2
  cat "$body" >&2
  exit 1
}

# Usage: check <description> <expected status> <curl arguments...>
check() {
  description=$1
  expected=$2
  shift 2
  status=$(curl -sS -o "$body" -D "$headers" -w '%{http_code}' "$@")
  [ "$status" = "$expected" ] || fail "$description: expected $expected, got $status"
  echo "ok: $description ($status)"
}

contains() {
  grep -q "$1" "$body" || fail "$2"
}

# Unique per run, so the script can be repeated against the same database.
route="RT-$(date +%s | tail -c 9)"
closure="{\"routeCode\":\"$route\",\"closureDate\":\"2026-01-15\",\"vehicleCode\":\"VAN-0042\",\"driverCode\":\"DRV-0007\",\"packagesDelivered\":42,\"packagesFailed\":3,\"packagesReturned\":1,\"kmDriven\":87}"
post() {
  check "$1" "$2" -X POST -H 'Content-Type: application/json' -d "$3" "$FRONTEND_URL$CLOSURES"
}

check "frontend serves the application" 200 "$FRONTEND_URL/"
contains 'id="root"' "the page has no application root"

check "frontend serves client-side routes" 200 "$FRONTEND_URL/closures/new"
contains 'id="root"' "the client-side route did not load the application"

post "create a closure through the frontend" 201 "$closure"
contains '"totalPackages":46' "the created closure has the wrong total"
location=$(tr -d '\r' <"$headers" | sed -n 's/^[Ll]ocation: //p')
[ -n "$location" ] || fail "the response has no Location header"

check "read it back through the frontend" 200 "$FRONTEND_URL$location"
contains "\"routeCode\":\"$route\"" "the closure read back is a different one"

post "reject a duplicate" 409 "$closure"
contains '"code":"ROUTE_CLOSURE_DUPLICATE"' "the duplicate has no stable code"
grep -qi '^content-type: application/problem+json' "$headers" || fail "the duplicate is not problem+json"

post "reject invalid data" 400 "{\"routeCode\":\"$route\"}"
contains '"code":"VALIDATION_FAILED"' "the validation error has no stable code"

check "list directly from the backend" 200 "$BACKEND_URL$CLOSURES?routeCode=$route"
contains "\"routeCode\":\"$route\"" "the backend does not list the new closure"

check "backend reports healthy" 200 "$BACKEND_URL/actuator/health"
contains '"status":"UP"' "the backend is not up"

# nginx forwards only /api, so this is the application page rather than Actuator's JSON.
check "frontend does not expose Actuator" 200 "$FRONTEND_URL/actuator/health"
contains 'id="root"' "Actuator is reachable through the frontend"

echo "All checks passed."
