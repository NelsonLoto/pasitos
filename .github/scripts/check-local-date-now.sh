#!/usr/bin/env bash
# "Hoy" se calcula con ArgentinaClock.today(): las fechas no pueden depender de la zona horaria
# del teléfono (ADR-26). Falla si la app la usa fuera de core/time y de la lista de excepciones, o
# si un archivo de la lista ya no la usa: así la lista solo se achica.
# Uso: .github/scripts/check-local-date-now.sh (desde cualquier carpeta del repo)
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
allowlist=.github/scripts/local-date-now-allowlist.txt

# Toman la zona horaria del teléfono. LocalDate.now(clock), con un reloj explícito, se permite
device_zone='(LocalDate|LocalDateTime|LocalTime|ZonedDateTime|OffsetDateTime)\.now\(\)|ZoneId\.systemDefault\(\)|TimeZone\.getDefault\(\)'

allowed=$(tr -d '\r' < "$allowlist" | grep -v '^[[:space:]]*#' | sed '/^[[:space:]]*$/d' | sort -u)
found=$(grep -rlE --include='*.kt' "$device_zone" app/app/src/main | grep -v '/core/time/' | sort -u || true)

new=$(comm -13 <(echo "$allowed") <(echo "$found") | sed '/^$/d')
fixed=$(comm -23 <(echo "$allowed") <(echo "$found") | sed '/^$/d')

for file in $new; do
  echo "::error file=$file::Usás la zona horaria del teléfono. Para \"hoy\" usá ArgentinaClock.today() (ADR-26)"
done
for file in $fixed; do
  echo "::error file=$allowlist::$file ya no usa la zona horaria del teléfono: sacalo de la lista de excepciones"
done

if [ -n "$new$fixed" ]; then
  exit 1
fi
echo "La app no usa la zona horaria del teléfono fuera de la lista de excepciones ($(echo "$allowed" | sed '/^$/d' | wc -l) archivos)."
