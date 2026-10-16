#!/usr/bin/env bash
# "Hoy" se calcula con ArgentinaClock.today(): LocalDate.now() depende de la zona horaria del
# teléfono (ADR-26). Falla si LocalDate.now() aparece en la app fuera de core/time y de la lista de
# excepciones, o si un archivo de la lista ya no lo usa: así la lista solo se achica.
# Uso: .github/scripts/check-local-date-now.sh (desde cualquier carpeta del repo)
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
allowlist=.github/scripts/local-date-now-allowlist.txt

allowed=$(tr -d '\r' < "$allowlist" | grep -v '^[[:space:]]*#' | sed '/^[[:space:]]*$/d' | sort -u)
found=$(grep -rl --include='*.kt' 'LocalDate\.now()' app/app/src/main | grep -v '/core/time/' | sort -u || true)

new=$(comm -13 <(echo "$allowed") <(echo "$found") | sed '/^$/d')
fixed=$(comm -23 <(echo "$allowed") <(echo "$found") | sed '/^$/d')

for file in $new; do
  echo "::error file=$file::Usá ArgentinaClock.today() en lugar de LocalDate.now() (ADR-26)"
done
for file in $fixed; do
  echo "::error file=$allowlist::$file ya no usa LocalDate.now(): sacalo de la lista de excepciones"
done

if [ -n "$new$fixed" ]; then
  exit 1
fi
echo "Sin LocalDate.now() nuevos. Quedan $(echo "$allowed" | sed '/^$/d' | wc -l) archivos en la lista de excepciones."
