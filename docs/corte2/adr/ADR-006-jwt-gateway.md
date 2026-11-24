# ADR-006 — JWT HS256 emitido por user-service, validado en gateway y servicios

**Contexto.** RNF-06/07 exigen autenticación y autorización por rol. Con microservicios no hay sesión compartida.

**Decisión.** `user-service` emite un JWT HS256 (claims `sub`, `login`, `role`, `name`, 60 min). El gateway valida firma y expiración, aplica reglas gruesas por ruta, elimina cabeceras `X-User-*` del cliente y las reinyecta desde el token. Cada servicio vuelve a validar el JWT como *resource server* (defensa en profundidad) y aplica reglas finas. Secreto por variable de entorno `JWT_SECRET`. Contraseñas con BCrypt (RNF-08). El registro público ya no permite elegir rol (corrección del Corte 1).

**Alternativas.** RS256 con JWKS (mejor para rotar claves, más configuración; previsto para Corte 3); Keycloak (sobredimensionado para el curso); validar solo en el gateway (un servicio expuesto por error quedaría abierto).

**Consecuencias.** + Sin estado en servidor, escalable. − Revocar un token antes de que expire no es posible en este corte; el secreto compartido debe protegerse.
