"""Verifica los originales OMS de T-02. Python 3.10+, solo biblioteca estándar."""

import argparse
from decimal import Decimal, InvalidOperation
from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path, PurePosixPath
import sys
from urllib.parse import urlsplit
from urllib.request import Request, urlopen
from zipfile import BadZipFile, ZipFile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
SD_COLUMNS = ["SD4neg", "SD3neg", "SD2neg", "SD1neg", "SD0", "SD1", "SD2", "SD3", "SD4"]
# Cobertura observada en los originales expandidos, sin recortar sus filas a la edad de uso de la app.
TABLES = {
    "WEIGHT_FOR_AGE": ("WFA", "Day", 0, 1856, 1, "kg"),
    "LENGTH_FOR_AGE": ("LFA", "Day", 0, 1856, 1, "cm"),
    "HEAD_CIRC_FOR_AGE": ("HCFA", "Day", 0, 1856, 1, "cm"),
    "BMI_FOR_AGE": ("BFA", "Day", 0, 1856, 1, "kg/m2"),
    "WEIGHT_FOR_LENGTH": ("WFL", "Length", 45, 110, "0.1", "kg"),
    "WEIGHT_FOR_HEIGHT": ("WFH", "Height", 65, 120, "0.1", "kg"),
}
MAX_BYTES = 4 * 1024 * 1024


def require(condition, message):
    if not condition:
        raise ValueError(message)


def read_manifest():
    manifest = json.loads((ROOT / "manifest.json").read_text(encoding="utf-8"))
    require(manifest["schema_version"] == 1, "Versión de manifiesto desconocida")
    files = manifest["files"]
    expected = {(indicator, sex) for indicator in TABLES for sex in ("FEMALE", "MALE")}
    actual = [(entry["indicator"], entry["sex"]) for entry in files]
    require(len(actual) == len(expected) and set(actual) == expected, "Faltan tablas o hay indicadores/sexos duplicados")
    require(len({entry["path"] for entry in files}) == len(files), "Rutas duplicadas en el manifiesto")
    for entry in files:
        relative = PurePosixPath(entry["path"])
        require(len(relative.parts) == 2 and relative.parts[0] == "originals" and relative.suffix == ".xlsx", "Ruta de original inválida")
        require("\\" not in entry["path"] and relative.name not in (".", ".."), "Ruta de original inválida")
        url = urlsplit(entry["download_url"])
        require(url.scheme == "https" and url.hostname == "cdn.who.int" and url.username is None and url.password is None, "La descarga debe ser HTTPS de cdn.who.int")
        require(PurePosixPath(url.path).name == relative.name, "El nombre local no coincide con el original")
        require(0 < entry["bytes"] <= MAX_BYTES, "Tamaño de original inválido")
    return files


def check_bytes(body, entry):
    require(len(body) == entry["bytes"], "Tamaño distinto al original registrado")
    require(sha256(body).hexdigest() == entry["sha256"], "SHA-256 distinto al original registrado")


def original_bytes(entry, download_missing=False):
    target = ROOT / entry["path"]
    if target.exists():
        body = target.read_bytes()
    else:
        require(download_missing, "Falta el original; usar --download-missing para recuperarlo")
        request = Request(entry["download_url"], headers={"User-Agent": "Pasitos-T02-reference-data/1.0"})
        with urlopen(request, timeout=45) as response:
            require(urlsplit(response.url).scheme == "https" and urlsplit(response.url).hostname == "cdn.who.int", "Redirección fuera de la fuente OMS")
            body = response.read(MAX_BYTES + 1)
        check_bytes(body, entry)
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("xb") as output:
            output.write(body)
    check_bytes(body, entry)
    return body


def table_rows(body, sheet_name):
    """Lee el formato concreto de estos originales; no importa ni reescribe planillas."""
    with ZipFile(BytesIO(body)) as archive:
        require(archive.testzip() is None, "XLSX corrupto")
        workbook = ET.fromstring(archive.read("xl/workbook.xml"))
        sheets = workbook.findall("s:sheets/s:sheet", NS)
        require(len(sheets) == 1 and sheets[0].get("name") == sheet_name, "Hoja distinta del indicador/sexo esperado")
        strings = []
        if "xl/sharedStrings.xml" in archive.namelist():
            strings = ["".join(node.itertext()) for node in ET.fromstring(archive.read("xl/sharedStrings.xml")).findall("s:si", NS)]
        sheet = ET.fromstring(archive.read("xl/worksheets/sheet1.xml"))
        rows = []
        for row in sheet.findall("s:sheetData/s:row", NS):
            cells = []
            for cell in row.findall("s:c", NS):
                require(cell.find("s:f", NS) is None, "El original contiene fórmulas inesperadas")
                require(cell.get("t") != "e", "El original contiene un error de Excel")
                value = cell.find("s:v", NS)
                if value is not None:
                    text = value.text
                    if cell.get("t") == "s":
                        text = strings[int(text)]
                else:
                    inline = cell.find("s:is", NS)
                    text = "".join(inline.itertext()) if inline is not None else None
                expected_address = f"{chr(65 + len(cells))}{len(rows) + 1}"
                require(cell.get("r") == expected_address, "Celdas faltantes o desordenadas")
                cells.append(text)
            require(len(cells) == 13, "Cantidad de columnas distinta a A:M")
            rows.append(cells)
    return rows


def check_table(body, entry):
    prefix, column, start, end, step, unit = TABLES[entry["indicator"]]
    step = Decimal(str(step))
    sex = "girls" if entry["sex"] == "FEMALE" else "boys"
    sheet_name = f"{prefix}_{sex}_z_exp"
    rows = table_rows(body, sheet_name)
    require(rows[0] == [column, "L", "M", "S", *SD_COLUMNS], "Columnas distintas a las tablas expandidas LMS")
    data = rows[1:]
    count = int((Decimal(end) - Decimal(start)) / step) + 1
    require(len(data) == count, f"Se esperaban {count} filas de datos")
    for index, row in enumerate(data):
        numbers = [Decimal(value) for value in row]
        require(all(value.is_finite() for value in numbers), f"Fila {index + 2}: valor no finito")
        expected_axis = Decimal(start) + index * step
        require(abs(numbers[0] - expected_axis) < Decimal("0.00000001"), f"Fila {index + 2}: eje discontinuo o duplicado")
        require(numbers[2] > 0 and numbers[3] > 0, f"Fila {index + 2}: M o S no positivo")
        sd = numbers[4:]
        require(all(left < right for left, right in zip(sd, sd[1:])), f"Fila {index + 2}: columnas SD desordenadas")
        require(abs(numbers[2] - numbers[8]) <= Decimal("0.000501"), f"Fila {index + 2}: SD0 no coincide con M al redondear a tres decimales")
    return count, column, unit


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--download-missing", action="store_true", help="Recuperar únicamente originales ausentes y verificar el SHA-256 fijado")
    args = parser.parse_args()
    total = 0
    try:
        files = read_manifest()
        for entry in files:
            count, axis, unit = check_table(original_bytes(entry, args.download_missing), entry)
            total += count
            print(f"OK {entry['path']}: {count} filas, eje {axis}, medida en {unit}")
    except (OSError, ValueError, KeyError, TypeError, IndexError, InvalidOperation, BadZipFile, ET.ParseError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print(f"Verificados {len(files)} originales y {total} filas LMS. No se modificaron los XLSX existentes.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
