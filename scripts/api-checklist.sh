#!/usr/bin/env bash

set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
STUDENT_ID="${STUDENT_ID:-}"
STUDENT_PASSWORD="${STUDENT_PASSWORD:-}"
ADMIN_ID="${ADMIN_ID:-}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"

REGISTER_STUDENT_ID="${REGISTER_STUDENT_ID:-}"
REGISTER_NAME="${REGISTER_NAME:-Postman Test User}"
REGISTER_PASSWORD="${REGISTER_PASSWORD:-password123}"
REGISTER_COURSE="${REGISTER_COURSE:-BSCS}"
REGISTER_YEAR_LEVEL="${REGISTER_YEAR_LEVEL:-3}"

REPORT_FILE="${REPORT_FILE:-docs/api-testing-checklist-report.md}"

for cmd in curl jq mktemp; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: $cmd"
    exit 1
  fi
done

if [[ -z "$STUDENT_ID" || -z "$STUDENT_PASSWORD" || -z "$ADMIN_ID" || -z "$ADMIN_PASSWORD" ]]; then
  echo "Set STUDENT_ID, STUDENT_PASSWORD, ADMIN_ID, ADMIN_PASSWORD first."
  exit 1
fi

RESULTS=()

record_pass() {
  RESULTS+=("[x] $1")
}

record_fail() {
  RESULTS+=("[ ] $1")
}

record_skip() {
  RESULTS+=("[-] $1")
}

request() {
  local method="$1"
  local url="$2"
  local token="$3"
  local data="${4:-}"
  local file="${5:-}"
  local content_type="${6:-application/json}"

  local body_file
  body_file="$(mktemp)"

  local code
  if [[ -n "$file" ]]; then
    if [[ -n "$token" ]]; then
      code="$(curl -sS -o "$body_file" -w "%{http_code}" -X "$method" -H "Authorization: Bearer $token" -F "file=@$file" "$url")"
    else
      code="$(curl -sS -o "$body_file" -w "%{http_code}" -X "$method" -F "file=@$file" "$url")"
    fi
  else
    if [[ -n "$data" ]]; then
      if [[ -n "$token" ]]; then
        code="$(curl -sS -o "$body_file" -w "%{http_code}" -X "$method" -H "Content-Type: $content_type" -H "Authorization: Bearer $token" -d "$data" "$url")"
      else
        code="$(curl -sS -o "$body_file" -w "%{http_code}" -X "$method" -H "Content-Type: $content_type" -d "$data" "$url")"
      fi
    else
      if [[ -n "$token" ]]; then
        code="$(curl -sS -o "$body_file" -w "%{http_code}" -X "$method" -H "Authorization: Bearer $token" "$url")"
      else
        code="$(curl -sS -o "$body_file" -w "%{http_code}" -X "$method" "$url")"
      fi
    fi
  fi

  echo "$code|$body_file"
}

status_ok() {
  local expected="$1"
  local got="$2"
  [[ "$expected" == "$got" ]]
}

json_get() {
  local file="$1"
  local expr="$2"
  jq -r "$expr // empty" "$file" 2>/dev/null
}

tmpfile="$(mktemp)"
echo "ping" > "$tmpfile"

# Setup reachability
if curl -sS "$BASE_URL" >/dev/null 2>&1; then
  record_pass "Setup: app reachable at $BASE_URL"
else
  record_fail "Setup: app not reachable at $BASE_URL"
fi

# Auth: register (optional)
if [[ -n "$REGISTER_STUDENT_ID" ]]; then
  reg_payload="{\"studentId\":\"$REGISTER_STUDENT_ID\",\"name\":\"$REGISTER_NAME\",\"password\":\"$REGISTER_PASSWORD\",\"course\":\"$REGISTER_COURSE\",\"yearLevel\":$REGISTER_YEAR_LEVEL}"
  out="$(request POST "$BASE_URL/api/auth/register" "" "$reg_payload")"
  reg_code="${out%%|*}"
  reg_body="${out##*|}"
  if status_ok 201 "$reg_code"; then
    record_pass "Auth: POST /api/auth/register returns 201"
  else
    record_fail "Auth: POST /api/auth/register expected 201, got $reg_code"
  fi
  rm -f "$reg_body"
else
  record_skip "Auth: POST /api/auth/register skipped (set REGISTER_STUDENT_ID to enable)"
fi

# Auth: student login
student_login_payload="{\"studentId\":\"$STUDENT_ID\",\"password\":\"$STUDENT_PASSWORD\"}"
out="$(request POST "$BASE_URL/api/auth/login" "" "$student_login_payload")"
student_code="${out%%|*}"
student_body="${out##*|}"

STUDENT_TOKEN=""
if status_ok 200 "$student_code"; then
  STUDENT_TOKEN="$(json_get "$student_body" '.data.accessToken')"
  STUDENT_REFRESH="$(json_get "$student_body" '.data.refreshToken')"
  if [[ -n "$STUDENT_TOKEN" && -n "$STUDENT_REFRESH" ]]; then
    record_pass "Auth: POST /api/auth/login returns 200 with accessToken + refreshToken"
  else
    record_fail "Auth: POST /api/auth/login missing token fields"
  fi
else
  record_fail "Auth: POST /api/auth/login expected 200, got $student_code"
fi

# Auth: admin login
admin_login_payload="{\"studentId\":\"$ADMIN_ID\",\"password\":\"$ADMIN_PASSWORD\"}"
out="$(request POST "$BASE_URL/api/auth/login" "" "$admin_login_payload")"
admin_code="${out%%|*}"
admin_body="${out##*|}"

ADMIN_TOKEN=""
if status_ok 200 "$admin_code"; then
  ADMIN_TOKEN="$(json_get "$admin_body" '.data.accessToken')"
  if [[ -n "$ADMIN_TOKEN" ]]; then
    record_pass "Auth: admin login works for protected admin routes"
  else
    record_fail "Auth: admin login returned 200 but no access token"
  fi
else
  record_fail "Auth: admin login expected 200, got $admin_code"
fi

# Auth: forgot password (smoke)
forgot_payload="{\"studentId\":\"$STUDENT_ID\"}"
out="$(request POST "$BASE_URL/api/auth/forgot-password" "" "$forgot_payload")"
forgot_code="${out%%|*}"
forgot_body="${out##*|}"
if [[ "$forgot_code" == "200" || "$forgot_code" == "400" ]]; then
  record_pass "Auth: POST /api/auth/forgot-password reachable (200/400 depending on email verification)"
else
  record_fail "Auth: POST /api/auth/forgot-password unexpected status $forgot_code"
fi

if [[ -z "$STUDENT_TOKEN" || -z "$ADMIN_TOKEN" ]]; then
  record_fail "Protected routes skipped because login failed"
else
  # Users
  out="$(request GET "$BASE_URL/api/users/me" "$STUDENT_TOKEN")"
  code="${out%%|*}"; body="${out##*|}"
  [[ "$code" == "200" ]] && record_pass "Users: GET /api/users/me returns 200" || record_fail "Users: GET /api/users/me expected 200, got $code"
  rm -f "$body"

  out="$(request PUT "$BASE_URL/api/users/me" "$STUDENT_TOKEN" '{"name":"Updated Student","course":"BSIT","yearLevel":4}')"
  code="${out%%|*}"; body="${out##*|}"
  [[ "$code" == "200" ]] && record_pass "Users: PUT /api/users/me returns 200" || record_fail "Users: PUT /api/users/me expected 200, got $code"
  rm -f "$body"

  out="$(request PUT "$BASE_URL/api/users/me/email" "$STUDENT_TOKEN" '{"email":"student@example.edu"}')"
  code="${out%%|*}"; body="${out##*|}"
  [[ "$code" == "200" || "$code" == "400" ]] && record_pass "Users: PUT /api/users/me/email reachable (200/400 allowed by current account state)" || record_fail "Users: PUT /api/users/me/email unexpected $code"
  rm -f "$body"

  # Categories
  out="$(request GET "$BASE_URL/api/categories" "$STUDENT_TOKEN")"
  code="${out%%|*}"; body="${out##*|}"
  [[ "$code" == "200" ]] && record_pass "Categories: GET /api/categories returns 200" || record_fail "Categories: GET /api/categories expected 200, got $code"
  CATEGORY_ID="$(json_get "$body" '.data[0].id')"
  rm -f "$body"

  out="$(request POST "$BASE_URL/api/categories" "$ADMIN_TOKEN" '{"name":"Automation Temp Category"}')"
  code="${out%%|*}"; body="${out##*|}"
  TEMP_CATEGORY_ID="$(json_get "$body" '.data.id')"
  [[ "$code" == "201" ]] && record_pass "Categories: POST /api/categories (admin) returns 201" || record_fail "Categories: POST /api/categories expected 201, got $code"
  rm -f "$body"

  if [[ -n "$TEMP_CATEGORY_ID" ]]; then
    out="$(request PUT "$BASE_URL/api/categories/$TEMP_CATEGORY_ID" "$ADMIN_TOKEN" '{"name":"Automation Temp Category Updated"}')"
    code="${out%%|*}"; body="${out##*|}"
    [[ "$code" == "200" ]] && record_pass "Categories: PUT /api/categories/{id} (admin) returns 200" || record_fail "Categories: PUT /api/categories/{id} expected 200, got $code"
    rm -f "$body"

    out="$(request DELETE "$BASE_URL/api/categories/$TEMP_CATEGORY_ID" "$ADMIN_TOKEN")"
    code="${out%%|*}"; body="${out##*|}"
    [[ "$code" == "204" || "$code" == "409" ]] && record_pass "Categories: DELETE /api/categories/{id} reachable (204/409 depending on references)" || record_fail "Categories: DELETE /api/categories/{id} unexpected $code"
    rm -f "$body"
  else
    record_fail "Categories: could not get temp category id for update/delete"
  fi

  # Tickets
  if [[ -n "$CATEGORY_ID" ]]; then
    out="$(request POST "$BASE_URL/api/tickets" "$STUDENT_TOKEN" "{\"categoryId\":$CATEGORY_ID,\"title\":\"Automation Ticket\",\"description\":\"Automation description\"}")"
    code="${out%%|*}"; body="${out##*|}"
    TICKET_ID="$(json_get "$body" '.data.id')"
    [[ "$code" == "201" || "$code" == "403" ]] && record_pass "Tickets: POST /api/tickets reachable (201/403 depending on email verification)" || record_fail "Tickets: POST /api/tickets unexpected $code"
    rm -f "$body"
  else
    TICKET_ID=""
    record_fail "Tickets: missing category id; cannot create ticket"
  fi

  out="$(request GET "$BASE_URL/api/tickets" "$STUDENT_TOKEN")"
  code="${out%%|*}"; body="${out##*|}"
  [[ "$code" == "200" ]] && record_pass "Tickets: GET /api/tickets returns 200" || record_fail "Tickets: GET /api/tickets expected 200, got $code"
  rm -f "$body"

  if [[ -n "$TICKET_ID" ]]; then
    out="$(request GET "$BASE_URL/api/tickets/$TICKET_ID" "$STUDENT_TOKEN")"
    code="${out%%|*}"; body="${out##*|}"
    [[ "$code" == "200" ]] && record_pass "Tickets: GET /api/tickets/{id} returns 200" || record_fail "Tickets: GET /api/tickets/{id} expected 200, got $code"
    rm -f "$body"

    out="$(request PATCH "$BASE_URL/api/tickets/$TICKET_ID/status" "$ADMIN_TOKEN" '{"status":"IN_PROGRESS"}')"
    code="${out%%|*}"; body="${out##*|}"
    [[ "$code" == "200" ]] && record_pass "Tickets: PATCH /api/tickets/{id}/status (admin) returns 200" || record_fail "Tickets: PATCH /api/tickets/{id}/status expected 200, got $code"
    rm -f "$body"

    out="$(request PATCH "$BASE_URL/api/tickets/$TICKET_ID/status" "$ADMIN_TOKEN" '{"status":"RESOLVED"}')"
    code="${out%%|*}"; body="${out##*|}"
    rm -f "$body"

    out="$(request PATCH "$BASE_URL/api/tickets/$TICKET_ID/confirm-resolved" "$STUDENT_TOKEN")"
    code="${out%%|*}"; body="${out##*|}"
    [[ "$code" == "200" ]] && record_pass "Tickets: PATCH /api/tickets/{id}/confirm-resolved (student) returns 200" || record_fail "Tickets: PATCH /api/tickets/{id}/confirm-resolved expected 200, got $code"
    rm -f "$body"

    # Responses
    out="$(request POST "$BASE_URL/api/tickets/$TICKET_ID/responses" "$ADMIN_TOKEN" '{"message":"Automation response"}')"
    code="${out%%|*}"; body="${out##*|}"
    [[ "$code" == "201" ]] && record_pass "Responses: POST /api/tickets/{id}/responses (admin) returns 201" || record_fail "Responses: POST /api/tickets/{id}/responses expected 201, got $code"
    rm -f "$body"

    out="$(request GET "$BASE_URL/api/tickets/$TICKET_ID/responses" "$STUDENT_TOKEN")"
    code="${out%%|*}"; body="${out##*|}"
    [[ "$code" == "200" ]] && record_pass "Responses: GET /api/tickets/{id}/responses returns 200" || record_fail "Responses: GET /api/tickets/{id}/responses expected 200, got $code"
    rm -f "$body"

    # Attachments
    out="$(request POST "$BASE_URL/api/tickets/$TICKET_ID/attachments" "$STUDENT_TOKEN" "" "$tmpfile")"
    code="${out%%|*}"; body="${out##*|}"
    [[ "$code" == "201" || "$code" == "403" ]] && record_pass "Attachments: POST /api/tickets/{id}/attachments reachable (201/403 by account state)" || record_fail "Attachments: POST /api/tickets/{id}/attachments unexpected $code"
    rm -f "$body"

    out="$(request GET "$BASE_URL/api/tickets/$TICKET_ID/attachments" "$STUDENT_TOKEN")"
    code="${out%%|*}"; body="${out##*|}"
    [[ "$code" == "200" ]] && record_pass "Attachments: GET /api/tickets/{id}/attachments returns 200" || record_fail "Attachments: GET /api/tickets/{id}/attachments expected 200, got $code"
    rm -f "$body"
  else
    record_skip "Tickets/Responses/Attachments: skipped dependent checks (ticket creation failed)"
  fi

  # Notifications
  out="$(request GET "$BASE_URL/api/notifications" "$STUDENT_TOKEN")"
  code="${out%%|*}"; body="${out##*|}"
  NOTIF_ID="$(json_get "$body" '.data[0].id')"
  [[ "$code" == "200" ]] && record_pass "Notifications: GET /api/notifications returns 200" || record_fail "Notifications: GET /api/notifications expected 200, got $code"
  rm -f "$body"

  if [[ -n "$NOTIF_ID" ]]; then
    out="$(request PATCH "$BASE_URL/api/notifications/$NOTIF_ID/read" "$STUDENT_TOKEN")"
    code="${out%%|*}"; body="${out##*|}"
    [[ "$code" == "200" ]] && record_pass "Notifications: PATCH /api/notifications/{id}/read returns 200" || record_fail "Notifications: PATCH /api/notifications/{id}/read expected 200, got $code"
    rm -f "$body"
  else
    record_skip "Notifications: PATCH /api/notifications/{id}/read skipped (no notification found)"
  fi

  out="$(request PATCH "$BASE_URL/api/notifications/read-all" "$STUDENT_TOKEN")"
  code="${out%%|*}"; body="${out##*|}"
  [[ "$code" == "200" ]] && record_pass "Notifications: PATCH /api/notifications/read-all returns 200" || record_fail "Notifications: PATCH /api/notifications/read-all expected 200, got $code"
  rm -f "$body"

  # Analytics
  out="$(request GET "$BASE_URL/api/analytics/summary" "$ADMIN_TOKEN")"
  code="${out%%|*}"; body="${out##*|}"
  [[ "$code" == "200" ]] && record_pass "Analytics: GET /api/analytics/summary returns 200" || record_fail "Analytics: GET /api/analytics/summary expected 200, got $code"
  rm -f "$body"

  out="$(request GET "$BASE_URL/api/analytics/by-category" "$ADMIN_TOKEN")"
  code="${out%%|*}"; body="${out##*|}"
  [[ "$code" == "200" ]] && record_pass "Analytics: GET /api/analytics/by-category returns 200" || record_fail "Analytics: GET /api/analytics/by-category expected 200, got $code"
  rm -f "$body"

  out="$(request GET "$BASE_URL/api/analytics/unresolved" "$ADMIN_TOKEN")"
  code="${out%%|*}"; body="${out##*|}"
  [[ "$code" == "200" ]] && record_pass "Analytics: GET /api/analytics/unresolved returns 200" || record_fail "Analytics: GET /api/analytics/unresolved expected 200, got $code"
  rm -f "$body"

  # Negative checks
  out="$(request GET "$BASE_URL/api/analytics/summary" "$STUDENT_TOKEN")"
  code="${out%%|*}"; body="${out##*|}"
  [[ "$code" == "403" ]] && record_pass "Access control: student rejected on admin route (403)" || record_fail "Access control: expected 403 on admin route with student token, got $code"
  rm -f "$body"

  out="$(request GET "$BASE_URL/api/users/me" "")"
  code="${out%%|*}"; body="${out##*|}"
  [[ "$code" == "401" ]] && record_pass "Access control: missing token returns 401" || record_fail "Access control: expected 401 without token, got $code"
  rm -f "$body"

  out="$(request PUT "$BASE_URL/api/users/me" "$STUDENT_TOKEN" '{"name":""}')"
  code="${out%%|*}"; body="${out##*|}"
  [[ "$code" == "400" ]] && record_pass "Validation: invalid payload returns 400" || record_fail "Validation: expected 400 for invalid payload, got $code"
  rm -f "$body"
fi

rm -f "$student_body" "$admin_body" "$forgot_body" "$tmpfile"

pass_count=0
fail_count=0
skip_count=0

for row in "${RESULTS[@]}"; do
  case "$row" in
    "[x]"*) pass_count=$((pass_count + 1)) ;;
    "[ ]"*) fail_count=$((fail_count + 1)) ;;
    "[-]"*) skip_count=$((skip_count + 1)) ;;
  esac
done

{
  echo "# UCMS API Testing Checklist Report"
  echo
  echo "- Base URL: \\`$BASE_URL\\`"
  echo "- Passed: $pass_count"
  echo "- Failed: $fail_count"
  echo "- Skipped: $skip_count"
  echo
  for row in "${RESULTS[@]}"; do
    echo "- $row"
  done
} > "$REPORT_FILE"

echo "Generated report: $REPORT_FILE"
echo "Passed: $pass_count | Failed: $fail_count | Skipped: $skip_count"

if [[ $fail_count -gt 0 ]]; then
  exit 1
fi

exit 0
