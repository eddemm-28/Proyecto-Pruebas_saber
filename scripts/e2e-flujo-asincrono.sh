#!/usr/bin/env bash
# B17: prueba end-to-end del flujo asíncrono contra el sistema levantado con docker compose:
#   Publicar Pregunta → (PreguntaPublicada) → Generar/Finalizar Simulacro → (SimulacroFinalizado) → Actualizar Seguimiento
# Uso: ./scripts/e2e-flujo-asincrono.sh      (API=http://host:8080 para otro servidor)
source "$(dirname "$0")/lib.sh"

RUN=$(date +%s)
COMP="E2E Competencia $RUN"     # competencia única para no mezclar con datos previos
START=$(date +%s)

log "1. Preparar usuarios"
ADMIN=$(login "$ADMIN_LOGIN" "$ADMIN_PASSWORD"); expect 200 "login admin"
ensure_user autor1 AUTOR "$ADMIN"
ensure_user revisor1 REVISOR "$ADMIN"
ensure_user docente1 DOCENTE "$ADMIN"
ensure_user "est$RUN" ESTUDIANTE "$ADMIN"
AUTOR=$(login autor1 "$DEFAULT_PASSWORD")
REVISOR=$(login revisor1 "$DEFAULT_PASSWORD")
DOCENTE=$(login docente1 "$DEFAULT_PASSWORD")
EST=$(login "est$RUN" "$DEFAULT_PASSWORD")
ok "Tokens obtenidos a través del api-gateway"

log "2. Seguridad: un estudiante no accede al banco; sin token → 401"
api GET /api/questions "$EST" >/dev/null; expect 403 "estudiante en /api/questions"
api GET /api/questions "" >/dev/null;     expect 401 "sin token"
ok "Reglas de rol del gateway aplicadas"

log "3. Validaciones estructurales (RF-08..RF-13)"
bad=$(question_json "Inválida" "$COMP" "Tema" MEDIA A | jq '.options[3]="Todas las anteriores" | .context=""')
api POST /api/questions "$AUTOR" "$bad" >/dev/null; expect 422 "pregunta inválida"
ok "Pregunta inválida rechazada con 422: $(jq -r '.errors | length' /tmp/saberpro_body) errores"

log "4. Ciclo de vida y publicación de 3 preguntas (State + A19 + PreguntaPublicada)"
IDS=()
for r in A B C; do
  IDS+=("$(publish_question "$AUTOR" "$REVISOR" "$ADMIN" "$(question_json "E2E $r" "$COMP" "Tema E2E" MEDIA "$r")")")
done
ok "Publicadas: ${IDS[*]}"

log "5. Esperar a que la réplica del simulation-service reciba los eventos"
catalog_ready() {
  local n
  n=$(api GET /api/simulations/catalog "$EST" | jq --arg c "$COMP" '[.[] | select(.competence==$c) | .available] | add // 0')
  [[ "$n" -ge 3 ]]
}
wait_until "PreguntaPublicada en simulation-service" 30 catalog_ready
ok "Réplica actualizada en $(( $(date +%s) - START )) s desde el inicio"

log "6. Generar simulacro (Strategy por competencia) y responder"
SIM=$(api POST /api/simulations "$EST" "{\"strategy\":\"COMPETENCE\",\"competence\":\"$COMP\",\"questionCount\":3,\"timeLimitMinutes\":5}")
expect 201 "crear simulacro"
SIM_ID=$(jq -r .id <<<"$SIM")
[[ $(jq '[.items[] | has("correctOption")] | any' <<<"$SIM") == false ]] || fail "el simulacro expone respuestas correctas"
# Responde A a todo: 1 de 3 correcta (las respuestas correctas son A, B y C)
for q in $(jq -r '.items[].questionId' <<<"$SIM"); do
  api PUT "/api/simulations/$SIM_ID/answers/$q" "$EST" '{"selectedOption":"A"}' >/dev/null; expect 200 "responder"
done
RESULT=$(api POST "/api/simulations/$SIM_ID/finish" "$EST"); expect 200 "finalizar"
SCORE=$(jq .globalScore <<<"$RESULT")
[[ "$SCORE" == "33.33" ]] || fail "puntaje esperado 33.33, obtenido $SCORE"
ok "Simulacro $SIM_ID calificado: $SCORE / 100"

log "7. Esperar SimulacroFinalizado en el tracking-service"
history_ready() {
  [[ $(api GET /api/tracking/students/me/history "$EST" | jq .totalElements) -ge 1 ]]
}
wait_until "SimulacroFinalizado en tracking-service" 30 history_ready
STATS=$(api GET /api/tracking/students/me/stats "$EST")
CLASS=$(jq -r --arg c "$COMP" '.competences[] | select(.competence==$c) | .averageScore' <<<"$STATS")
[[ "$CLASS" == "33.33" ]] || fail "estadística esperada 33.33, obtenida $CLASS"
ok "Historial y estadísticas actualizados (promedio $CLASS)"

log "8. Reporte agregado para el docente (RF-28)"
api GET "/api/tracking/reports/competences" "$DOCENTE" | jq -e --arg c "$COMP" '.[] | select(.competence==$c)' >/dev/null
expect 200 "reporte docente"
ok "Reporte por competencia disponible"

log "9. Archivar una pregunta publicada → PreguntaArchivada la retira de nuevos simulacros"
api POST "/api/questions/${IDS[0]}/archive" "$ADMIN" >/dev/null; expect 200 "archivar"
catalog_shrunk() {
  local n
  n=$(api GET /api/simulations/catalog "$EST" | jq --arg c "$COMP" '[.[] | select(.competence==$c) | .available] | add // 0')
  [[ "$n" -eq 2 ]]
}
wait_until "PreguntaArchivada en simulation-service" 30 catalog_shrunk
ok "Catálogo con 2 preguntas disponibles"

ok "E2E completo en $(( $(date +%s) - START )) s"
