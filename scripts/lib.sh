#!/usr/bin/env bash
# Funciones comunes para los scripts de semilla y E2E. Requiere: curl y jq.
set -euo pipefail

API="${API:-http://localhost:8080}"
ADMIN_LOGIN="${ADMIN_LOGIN:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin123*}"
DEFAULT_PASSWORD="${DEFAULT_PASSWORD:-Clave123*}"

command -v jq >/dev/null || { echo "Se requiere jq (https://jqlang.github.io/jq/)"; exit 1; }

log()  { printf '\033[1;34m▶ %s\033[0m\n' "$*"; }
ok()   { printf '\033[1;32m✔ %s\033[0m\n' "$*"; }
fail() { printf '\033[1;31m✘ %s\033[0m\n' "$*"; exit 1; }

# api METHOD PATH [TOKEN] [JSON] → imprime el cuerpo; deja el código HTTP en $HTTP_STATUS
api() {
  local method=$1 path=$2 token=${3:-} body=${4:-}
  local args=(-s -o /tmp/saberpro_body -w '%{http_code}' -X "$method" "$API$path" -H 'Accept: application/json')
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$body" ]] && args+=(-H 'Content-Type: application/json' --data "$body")
  HTTP_STATUS=$(curl "${args[@]}")
  # Se guarda también en archivo: api suele llamarse dentro de $(...) (subshell) y la variable se perdería
  echo "$HTTP_STATUS" > /tmp/saberpro_status
  cat /tmp/saberpro_body
}

expect() {
  local expected=$1 what=$2
  HTTP_STATUS=$(cat /tmp/saberpro_status 2>/dev/null || echo 0)
  [[ "$HTTP_STATUS" == "$expected" ]] || { echo; cat /tmp/saberpro_body; fail "$what: se esperaba HTTP $expected y llegó $HTTP_STATUS"; }
}

login() { # login password → token
  api POST /api/auth/login "" "{\"login\":\"$1\",\"password\":\"$2\"}" | jq -r .accessToken
}

# ensure_user login rol adminToken → crea (si no existe) y asigna rol
ensure_user() {
  local login=$1 role=$2 admin=$3
  api POST /api/auth/register "" "{\"login\":\"$login\",\"fullName\":\"Usuario $login\",\"email\":\"$login@saberpro.test\",\"password\":\"$DEFAULT_PASSWORD\"}" >/dev/null
  HTTP_STATUS=$(cat /tmp/saberpro_status)
  [[ "$HTTP_STATUS" == 201 || "$HTTP_STATUS" == 409 ]] || fail "registro de $login (HTTP $HTTP_STATUS)"
  if [[ "$role" != ESTUDIANTE ]]; then
    local id
    id=$(api GET "/api/users?q=$login&size=50" "$admin" | jq -r --arg l "$login" '.content[] | select(.login==$l) | .id')
    api PATCH "/api/users/$id/role" "$admin" "{\"role\":\"$role\"}" >/dev/null
    expect 200 "cambio de rol de $login"
  fi
}

# question_json título competencia tema dificultad respuesta
question_json() {
  jq -n --arg t "$1" --arg c "$2" --arg tema "$3" --arg d "$4" --arg r "$5" '{
    title: $t, type: "MULTIPLE_CHOICE",
    context: "Un equipo de desarrollo de la Universidad del Cauca analiza un caso práctico relacionado con \($tema).",
    statement: "¿Cuál de las siguientes afirmaciones es correcta sobre \($tema)?",
    options: ["Primera afirmación sobre \($tema)", "Segunda afirmación sobre \($tema)",
              "Tercera afirmación sobre \($tema)", "Cuarta afirmación sobre \($tema)"],
    correctOption: $r,
    justification: "La opción \($r) es la única consistente con el contexto planteado.",
    bibliography: "ICFES (2024). Guía de orientación Saber Pro.",
    competence: $c, topic: $tema, subtopic: "General", difficulty: $d }'
}

# publish_question autorToken revisorToken adminToken json → id de la pregunta publicada
publish_question() {
  local autor=$1 revisor=$2 admin=$3 json=$4 id
  id=$(api POST /api/questions "$autor" "$json" | jq -r .id); expect 201 "crear pregunta"
  api POST "/api/questions/$id/submit" "$autor" >/dev/null;        expect 200 "enviar a revisión"
  api POST "/api/questions/$id/start-review" "$revisor" >/dev/null; expect 200 "iniciar revisión"
  api POST "/api/questions/$id/approve" "$revisor" '{"comment":"Cumple el formato"}' >/dev/null; expect 200 "aprobar"
  api POST "/api/questions/$id/publish" "$admin" >/dev/null;       expect 200 "publicar"
  echo "$id"
}

# wait_until "descripción" segundos comando...
wait_until() {
  local what=$1 timeout=$2; shift 2
  for ((i = 0; i < timeout; i++)); do
    if "$@"; then return 0; fi
    sleep 1
  done
  fail "Tiempo agotado esperando: $what"
}
