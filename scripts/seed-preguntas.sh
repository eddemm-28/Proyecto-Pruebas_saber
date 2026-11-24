#!/usr/bin/env bash
# Crea usuarios de prueba (autor1, revisor1, docente1) y publica preguntas de ejemplo
# en varias competencias para poder generar simulacros. Uso: ./scripts/seed-preguntas.sh [N_POR_COMPETENCIA]
source "$(dirname "$0")/lib.sh"

N=${1:-8}
COMPETENCES=("Lectura crítica" "Razonamiento cuantitativo" "Inglés" "Diseño de software")
TOPICS=("Inferencia" "Estadística" "Gramática" "Patrones de diseño")
DIFFS=(FACIL MEDIA DIFICIL)
ANSWERS=(A B C D)

log "Login de administrador en $API"
ADMIN=$(login "$ADMIN_LOGIN" "$ADMIN_PASSWORD"); expect 200 "login admin"
ensure_user autor1 AUTOR "$ADMIN"
ensure_user revisor1 REVISOR "$ADMIN"
ensure_user docente1 DOCENTE "$ADMIN"
AUTOR=$(login autor1 "$DEFAULT_PASSWORD")
REVISOR=$(login revisor1 "$DEFAULT_PASSWORD")
ok "Usuarios autor1 / revisor1 / docente1 (clave $DEFAULT_PASSWORD)"

total=0
for c in "${!COMPETENCES[@]}"; do
  for ((i = 1; i <= N; i++)); do
    d=${DIFFS[$(( (i - 1) % 3 ))]}
    r=${ANSWERS[$(( (i + c) % 4 ))]}
    json=$(question_json "${COMPETENCES[$c]} #$i" "${COMPETENCES[$c]}" "${TOPICS[$c]}" "$d" "$r")
    publish_question "$AUTOR" "$REVISOR" "$ADMIN" "$json" >/dev/null
    total=$((total + 1))
  done
  ok "${COMPETENCES[$c]}: $N preguntas publicadas"
done
ok "$total preguntas publicadas. El simulation-service las recibe por RabbitMQ en unos segundos."
