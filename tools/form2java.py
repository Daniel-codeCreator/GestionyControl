"""Convierte los formularios .form de IntelliJ en codigo Java para el JAR.

El GUI Designer de IntelliJ inyecta los componentes al compilar (solo funciona
dentro del IDE). Para generar un JAR ejecutable fuera de IntelliJ, este script
genera una clase <Vista>Form por formulario que construye los componentes
(incluidos los paneles anidados) y los asigna a los campos privados de la
vista mediante reflexion.

Uso:  python tools/form2java.py <raiz_del_proyecto> <carpeta_destino>
"""

from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path


NS = "{http://www.intellij.com/uidesigner/form/}"

ANCHOR = {"0": "GridBagConstraints.CENTER", "1": "GridBagConstraints.NORTH",
          "2": "GridBagConstraints.NORTHEAST", "3": "GridBagConstraints.EAST",
          "4": "GridBagConstraints.SOUTHEAST", "5": "GridBagConstraints.SOUTH",
          "6": "GridBagConstraints.SOUTHWEST", "7": "GridBagConstraints.WEST",
          "8": "GridBagConstraints.NORTHWEST"}

FILL = {"0": "GridBagConstraints.NONE", "1": "GridBagConstraints.HORIZONTAL",
        "2": "GridBagConstraints.VERTICAL", "3": "GridBagConstraints.BOTH"}


def escapar(valor: str) -> str:
    return valor.replace("\\", "\\\\").replace('"', '\\"')


def valor_de_propiedades(propiedades: ET.Element | None, etiqueta: str, defecto: str = "") -> str:
    if propiedades is None:
        return defecto
    nodo = propiedades.find(f"{NS}{etiqueta}")
    if nodo is None:
        return defecto
    return nodo.get("value") or defecto


def fuente_de(propiedades: ET.Element | None) -> str:
    if propiedades is None:
        return ""
    nodo = propiedades.find(f"{NS}font")
    if nodo is None:
        return ""

    nombre = nodo.get("name") or "Arial"
    tamano = nodo.get("size") or ""
    estilo = nodo.get("style") or "0"

    constante = {"1": "Font.BOLD", "2": "Font.ITALIC",
                 "3": "Font.BOLD | Font.ITALIC"}.get(estilo, "Font.PLAIN")

    if not tamano:
        return f'new Font("{escapar(nombre)}", {constante}, 12)'

    return f'new Font("{escapar(nombre)}", {constante}, {tamano})'


def tipo_de(clase: str) -> str:
    for sufijo, tipo in (
        ("JLabel", "JLabel"),
        ("JButton", "JButton"),
        ("JTextField", "JTextField"),
        ("JPasswordField", "JPasswordField"),
        ("JComboBox", "JComboBox"),
        ("JSpinner", "JSpinner"),
        ("JTable", "JTable"),
    ):
        if clase.endswith(sufijo):
            return tipo
    return "JPanel"


class Generador:
    def __init__(self) -> None:
        self.lineas: list[str] = []
        self.asignaciones: list[str] = []
        self.contador = 0

    def nuevo(self) -> str:
        variable = f"c{self.contador}"
        self.contador += 1
        return variable

    def restricciones(self, elemento: ET.Element) -> dict[str, object]:
        grid = elemento.find(f"{NS}constraints/{NS}grid")
        if grid is None:
            return {"fila": 0, "columna": 0, "alto": 1, "ancho": 1,
                    "anclaje": "GridBagConstraints.CENTER",
                    "relleno": "GridBagConstraints.NONE", "peso_x": 0, "peso_y": 0}
        return {
            "fila": int(grid.get("row", "0")),
            "columna": int(grid.get("column", "0")),
            "alto": int(grid.get("row-span", "1")),
            "ancho": int(grid.get("col-span", "1")),
            "anclaje": ANCHOR.get(grid.get("anchor", "0"), "GridBagConstraints.CENTER"),
            "relleno": FILL.get(grid.get("fill", "0"), "GridBagConstraints.NONE"),
            "peso_x": 0 if grid.get("hsize-policy", "0") == "0" else 1,
            "peso_y": 0 if grid.get("vsize-policy", "0") == "0" else 1,
        }

    def procesar_grid(self, grid: ET.Element, panel: str) -> None:
        """Genera los componentes de un grid y los agrega al panel indicado."""
        contenedor = grid.find(f"{NS}children")
        hijos = list(contenedor) if contenedor is not None else list(grid)
        elementos: list[tuple[int, int, str, dict[str, object]]] = []

        for hijo in hijos:
            if hijo.tag == f"{NS}component":
                elementos.append(self.procesar_componente(hijo, panel))
            elif hijo.tag == f"{NS}grid":
                elementos.append(self.procesar_grid_anidado(hijo, panel))

        elementos.sort(key=lambda item: (int(item[0]), int(item[1])))

        self.lineas.append(f"        GridBagConstraints gbc_{panel} = new GridBagConstraints();")
        for _fila, _columna, variable, datos in elementos:
            self.lineas.append(f"        gbc_{panel}.gridx = {datos['columna']};")
            self.lineas.append(f"        gbc_{panel}.gridy = {datos['fila']};")
            self.lineas.append(f"        gbc_{panel}.gridwidth = {datos['ancho']};")
            self.lineas.append(f"        gbc_{panel}.gridheight = {datos['alto']};")
            self.lineas.append(f"        gbc_{panel}.anchor = {datos['anclaje']};")
            self.lineas.append(f"        gbc_{panel}.fill = {datos['relleno']};")
            self.lineas.append(f"        gbc_{panel}.weightx = {datos['peso_x']};")
            self.lineas.append(f"        gbc_{panel}.weighty = {datos['peso_y']};")
            self.lineas.append(f"        {panel}.add({variable}, gbc_{panel});")

    def procesar_grid_anidado(self, grid: ET.Element, panel_padre: str) -> tuple[int, int, str, dict[str, object]]:
        datos = self.restricciones(grid)
        panel = self.nuevo()

        margen = grid.find(f"{NS}margin")
        arriba = int(margen.get("top", "0")) if margen is not None else 0
        izquierda = int(margen.get("left", "0")) if margen is not None else 0
        abajo = int(margen.get("bottom", "0")) if margen is not None else 0
        derecha = int(margen.get("right", "0")) if margen is not None else 0

        self.lineas.append(f"        JPanel {panel} = new JPanel(new GridBagLayout());")
        self.lineas.append(
            f"        {panel}.setBorder(BorderFactory.createEmptyBorder({arriba}, {izquierda}, {abajo}, {derecha}));"
        )

        binding = grid.get("binding")
        if binding:
            self.asignaciones.append(f'        asignar(vista, "{binding}", {panel});')

        self.procesar_grid(grid, panel)
        return int(datos["fila"]), int(datos["columna"]), panel, datos

    def procesar_componente(self, componente: ET.Element, panel: str) -> tuple[int, int, str, dict[str, object]]:
        clase = componente.get("class", "")
        binding = componente.get("binding")
        tipo = tipo_de(clase)
        variable = self.nuevo()
        propiedades = componente.find(f"{NS}properties")
        datos = self.restricciones(componente)

        if tipo == "JComboBox":
            self.lineas.append(f"        JComboBox<Object> {variable} = new JComboBox<>();")
        else:
            self.lineas.append(f"        {tipo} {variable} = new {tipo}();")

        texto = valor_de_propiedades(propiedades, "text", "")
        if texto and tipo in {"JLabel", "JButton", "JTextField"}:
            self.lineas.append(f'        {variable}.setText("{escapar(texto)}");')

        fuente = fuente_de(propiedades)
        if fuente:
            self.lineas.append(f"        {variable}.setFont({fuente});")

        if valor_de_propiedades(propiedades, "enabled", "") == "false":
            self.lineas.append(f"        {variable}.setEnabled(false);")

        if binding:
            self.asignaciones.append(f'        asignar(vista, "{binding}", {variable});')

        return int(datos["fila"]), int(datos["columna"]), variable, datos


def generar(ruta_form: Path, destino: Path) -> str | None:
    arbol = ET.parse(ruta_form).getroot()
    clase_vista = arbol.get("bind-to-class")
    if not clase_vista:
        return None

    paquete, nombre_simple = clase_vista.rsplit(".", 1)
    raiz = arbol.find(f"{NS}grid")
    if raiz is None:
        return None

    generador = Generador()

    margen = raiz.find(f"{NS}margin")
    arriba = int(margen.get("top", "0")) if margen is not None else 0
    izquierda = int(margen.get("left", "0")) if margen is not None else 0
    abajo = int(margen.get("bottom", "0")) if margen is not None else 0
    derecha = int(margen.get("right", "0")) if margen is not None else 0

    generador.lineas.append("        JPanel panelPrincipal = new JPanel(new GridBagLayout());")
    generador.lineas.append(
        f"        panelPrincipal.setBorder(BorderFactory.createEmptyBorder({arriba}, {izquierda}, {abajo}, {derecha}));"
    )

    binding_raiz = raiz.get("binding")
    if binding_raiz:
        generador.asignaciones.insert(0, f'        asignar(vista, "{binding_raiz}", panelPrincipal);')

    generador.procesar_grid(raiz, "panelPrincipal")

    salida: list[str] = [
        f"package {paquete};",
        "",
        "import javax.swing.*;",
        "import java.awt.*;",
        "import java.lang.reflect.Field;",
        "",
        "/**",
        f" * Generado automaticamente desde {ruta_form.name} (herramienta tools/form2java.py).",
        " * Construye la interfaz del formulario y la asigna a los campos de la vista.",
        " */",
        f"public final class {nombre_simple}Form {{",
        "",
        f"    private {nombre_simple}Form() {{",
        "    }",
        "",
        f"    public static void aplicar({nombre_simple} vista) {{",
    ]
    salida.extend(generador.lineas)
    salida.append("")
    salida.extend(generador.asignaciones)
    salida.append("    }")
    salida.append("")
    salida.append("    private static void asignar(Object destino, String nombre, Object valor) {")
    salida.append("        try {")
    salida.append("            Field campo = destino.getClass().getDeclaredField(nombre);")
    salida.append("            campo.setAccessible(true);")
    salida.append("            campo.set(destino, valor);")
    salida.append("        } catch (ReflectiveOperationException e) {")
    salida.append("            throw new IllegalStateException(\"No se pudo asignar el campo \" + nombre, e);")
    salida.append("        }")
    salida.append("    }")
    salida.append("}")

    destino.mkdir(parents=True, exist_ok=True)
    archivo = destino / paquete.replace(".", "/") / f"{nombre_simple}Form.java"
    archivo.parent.mkdir(parents=True, exist_ok=True)
    archivo.write_text("\n".join(salida) + "\n", encoding="utf-8")
    return str(archivo)


def main() -> None:
    if len(sys.argv) < 3:
        print("Uso: python tools/form2java.py <raiz> <destino>")
        raise SystemExit(2)

    raiz = Path(sys.argv[1]).resolve()
    destino = Path(sys.argv[2]).resolve()

    for formulario in sorted((raiz / "src" / "main" / "java").rglob("*.form")):
        resultado = generar(formulario, destino)
        if resultado:
            print("  generado:", resultado)


if __name__ == "__main__":
    main()

