#!/usr/bin/env bash
# El wrapper de Gradle tiene que verificar la distribución que descarga: sin distributionSha256Sum,
# Gradle baja el ZIP sin comprobar que sea el oficial. Android Studio borra esa línea cuando
# regenera el wrapper, por ejemplo al proponer bajar la versión de AGP o de Gradle (#128). Un
# checksum equivocado no hace falta controlarlo: Gradle rechaza el ZIP y el build falla solo.
# Uso: .github/scripts/check-gradle-wrapper.sh <carpeta>... (app, backend o shared)
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

if [ "$#" -eq 0 ]; then
  echo "Uso: $0 <carpeta>... (app, backend o shared)" >&2
  exit 2
fi

failed=0
for dir in "$@"; do
  file="$dir/gradle/wrapper/gradle-wrapper.properties"
  if [ ! -f "$file" ]; then
    echo "::error::No existe $file"
    failed=1
  elif ! tr -d '\r' < "$file" | grep -qE '^distributionSha256Sum=[0-9a-f]{64}$'; then
    echo "::error file=$file::Falta distributionSha256Sum: el wrapper bajaría Gradle sin verificarlo. Copiá el checksum \"Binary-only\" de la versión de distributionUrl desde https://gradle.org/release-checksums/ y, si Android Studio propuso bajar AGP o Gradle, actualizá Android Studio"
    failed=1
  fi
done

if [ "$failed" -ne 0 ]; then
  exit 1
fi
echo "El wrapper de Gradle verifica la distribución que descarga ($*)."
