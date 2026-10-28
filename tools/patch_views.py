"""Parchea las copias de las vistas para aplicar el formulario generado.

En IntelliJ, el GUI Designer inyecta los componentes del .form al compilar.
Para el JAR se genera una clase <Vista>Form (tools/form2java.py) y aqui se
inserta la llamada <Vista>Form.aplicar(this) al inicio de cada constructor.

Se trabaja sobre COPIAS en build/, el repositorio no se modifica.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path


def parchear(clase: str, ruta: Path) -> bool:
    texto = ruta.read_text(encoding="utf-8")
    if f"{clase}Form.aplicar(this);" in texto:
        return False

    patron = re.compile(r"(public\s+" + re.escape(clase) + r"\s*\([^)]*\)\s*(?:throws\s+[\w\s,.]+)?\{)")

    def reemplazar(m: re.Match) -> str:
        # Los constructores que delegan (this(...)) no admiten otra sentencia:
        # basta con parchear el constructor real.
        if re.match(r"\s*this\s*\(", texto[m.end():]):
            return m.group(1)
        return m.group(1) + f"\n        {clase}Form.aplicar(this);"

    resultado, cambios = patron.subn(reemplazar, texto)

    if cambios == 0 or f"{clase}Form.aplicar(this);" not in resultado:
        return False

    ruta.write_text(resultado, encoding="utf-8")
    return True


def main() -> None:
    if len(sys.argv) < 3:
        print("Uso: python tools/patch_views.py <carpeta_de_codigo> <carpeta_de_formularios>")
        raise SystemExit(2)

    codigo = Path(sys.argv[1]).resolve()
    formularios = Path(sys.argv[2]).resolve()

    for formulario in sorted(formularios.rglob("*.form")):
        import xml.etree.ElementTree as ET

        NS = "{http://www.intellij.com/uidesigner/form/}"
        raiz = ET.parse(formulario).getroot()
        clase_vista = raiz.get("bind-to-class")
        if not clase_vista:
            continue

        paquete, nombre_simple = clase_vista.rsplit(".", 1)
        archivo = codigo / paquete.replace(".", "/") / f"{nombre_simple}.java"
        if not archivo.is_file():
            print("  aviso: no existe", archivo)
            continue

        if parchear(nombre_simple, archivo):
            print(f"  parcheada: {nombre_simple}.java")

    print("  (las copias parcheadas viven en build/, el repositorio queda intacto)")


if __name__ == "__main__":
    main()
