# =====================================================================
#  SIGELAB - Generador de la distribucion ejecutable en publicshell\
#
#  Crea:  publicshell\SIGELAB.jar
#         publicshell\lib\*.jar
#         publicshell\conexion.xml
#         publicshell\facial-service\   (incluye el entorno .venv ya listo)
#         publicshell\ejecutar.bat
#         publicshell\LEEME.txt
#         publicshell\SIGELAB.zip
#
#  Uso:  powershell -ExecutionPolicy Bypass -File build-dist.ps1
#  (no requiere Maven: usa el JDK 21 con javac + jar)
# =====================================================================

$ErrorActionPreference = "Stop"

$raiz      = Split-Path -Parent $MyInvocation.MyCommand.Path
$src       = Join-Path $raiz "src\main\java"
$resources = Join-Path $raiz "src\main\resources"
$facial    = Join-Path $raiz "facial-service"
$build     = Join-Path $raiz "build"
$destino   = Join-Path $raiz "publicshell"
$m2        = Join-Path $env:USERPROFILE ".m2\repository"

# Localiza el JDK 21 (javac / jar) aunque no esten en el PATH
function Resolver-Herramienta([string]$nombre) {
    $enPath = Get-Command $nombre -ErrorAction SilentlyContinue
    if ($enPath) { return $enPath.Source }

    $raizJdk = $null
    if ($env:JAVA_HOME) { $raizJdk = $env:JAVA_HOME }
    if (-not $raizJdk -and (Test-Path "C:\Program Files\Java\jdk-21.0.12.1")) {
        $raizJdk = "C:\Program Files\Java\jdk-21.0.12.1"
    }
    if (-not $raizJdk) {
        $candidatos = Get-ChildItem "C:\Program Files\Java" -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -like "jdk*" } | Sort-Object Name -Descending
        if ($candidatos) { $raizJdk = $candidatos[0].FullName }
    }
    if ($raizJdk -and (Test-Path (Join-Path $raizJdk "bin\$nombre.exe"))) {
        return Join-Path $raizJdk "bin\$nombre.exe"
    }
    throw "No se encontro $nombre.exe. Instala el JDK 21 o define JAVA_HOME."
}

$javac = Resolver-Herramienta "javac"
$jar   = Resolver-Herramienta "jar"

Write-Host "[1/7] Limpiando build y publicshell..." -ForegroundColor Cyan
Remove-Item -Recurse -Force $build -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force $destino -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $build | Out-Null
New-Item -ItemType Directory -Force $destino | Out-Null
New-Item -ItemType Directory -Force (Join-Path $destino "lib") | Out-Null

Write-Host "[2/7] Compilando el proyecto (Java 21)..." -ForegroundColor Cyan
$dependencias = @(
    (Join-Path $m2 "com\microsoft\sqlserver\mssql-jdbc\13.4.0.jre11\mssql-jdbc-13.4.0.jre11.jar"),
    (Join-Path $m2 "org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar"),
    (Join-Path $m2 "com\google\zxing\core\3.5.3\core-3.5.3.jar"),
    (Join-Path $m2 "com\google\zxing\javase\3.5.3\javase-3.5.3.jar")
)

$jcalendar = Get-ChildItem (Join-Path $m2 "com\toedter\jcalendar") -Recurse -Filter "jcalendar-*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1
if ($jcalendar) { $dependencias += $jcalendar.FullName }

foreach ($dep in $dependencias) {
    if (-not (Test-Path $dep)) {
        throw "Falta la dependencia $dep. Descarga las librerias con Maven o ajustala en el script."
    }
}

$cp = ($dependencias -join ";")

# Copia el codigo y genera el codigo de los formularios .form (el JAR debe
# funcionar fuera de IntelliJ, donde no hay inyeccion del GUI Designer).
$codigoCopia = Join-Path $build "src"
$generados = Join-Path $build "generated-sources"
Copy-Item $src $codigoCopia -Recurse -Force

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) { $python = Get-Command py -ErrorAction SilentlyContinue }
if (-not $python) { throw "Se necesita Python para compilar los formularios .form." }

& $python.Source (Join-Path $raiz "tools\form2java.py") $raiz $generados
if ($LASTEXITCODE -ne 0) { throw "No se pudieron generar los formularios." }

& $python.Source (Join-Path $raiz "tools\patch_views.py") $codigoCopia (Join-Path $src "Proyecto\universidad\view")
if ($LASTEXITCODE -ne 0) { throw "No se pudieron parchear las vistas." }

$fuentes = Get-ChildItem $codigoCopia -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
$fuentes += Get-ChildItem $generados -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
& $javac -encoding UTF-8 --release 21 -cp $cp -d (Join-Path $build "classes") $fuentes
if ($LASTEXITCODE -ne 0) { throw "La compilacion fallo." }

Write-Host "[3/7] Empaquetando SIGELAB.jar..." -ForegroundColor Cyan
if (Test-Path $resources) {
    Copy-Item (Join-Path $resources "*") (Join-Path $build "classes") -Recurse -Force
}
& $jar --create --file (Join-Path $destino "SIGELAB.jar") --main-class Proyecto.universidad.main -C (Join-Path $build "classes") .
if ($LASTEXITCODE -ne 0) { throw "No se pudo crear el JAR." }

# La configuracion viaja tambien suelta: se puede editar sin recompilar el JAR.
Copy-Item (Join-Path $resources "conexion.xml") $destino -Force

Write-Host "[4/7] Copiando librerias en lib\..." -ForegroundColor Cyan
foreach ($dep in $dependencias) {
    Copy-Item $dep (Join-Path $destino "lib") -Force
}

Write-Host "[5/7] Copiando el programa Python (facial-service)..." -ForegroundColor Cyan
$destinoFacial = Join-Path $destino "facial-service"
New-Item -ItemType Directory -Force $destinoFacial | Out-Null
foreach ($carpeta in @("app", "scripts", "sql", "tests")) {
    Copy-Item (Join-Path $facial $carpeta) $destinoFacial -Recurse -Force
}
foreach ($archivo in @("requirements.txt", "pytest.ini", "README.md", ".env", ".env.example",
                        "setup-facial.ps1", "setup-facial-linux.sh", "setup-facial-macos.sh",
                        "start-facial.ps1", "start-facial.bat", "start-facial.sh",
                        "enroll-user.ps1", "enroll-user.sh", "demo-check.ps1", "demo-check.sh")) {
    $origen = Join-Path $facial $archivo
    if (Test-Path $origen) { Copy-Item $origen $destinoFacial -Force }
}
Get-ChildItem $destinoFacial -Recurse -Directory -Filter "__pycache__" | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "[6/7] Copiando el entorno virtual de Python (para que el reconocimiento facial funcione de inmediato)..." -ForegroundColor Cyan
$venvOrigen = Join-Path $facial ".venv"
if (Test-Path (Join-Path $venvOrigen "Scripts\python.exe")) {
    Write-Host "      copiando .venv (puede tardar un minuto)..." -ForegroundColor DarkGray
    robocopy $venvOrigen (Join-Path $destinoFacial ".venv") /E /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "No se pudo copiar el entorno virtual de Python." }
    Write-Host "      entorno virtual copiado" -ForegroundColor Green
} else {
    Write-Host "      no hay .venv en facial-service: se creara con setup-facial.ps1 al ejecutar" -ForegroundColor Yellow
}

Write-Host "[7/7] Escribiendo los lanzadores y comprimiendo..." -ForegroundColor Cyan
Copy-Item (Join-Path $raiz "packaging\iniciar-facial.bat") $destinoFacial -Force
Copy-Item (Join-Path $raiz "packaging\ejecutar.bat") $destino -Force
Copy-Item (Join-Path $raiz "packaging\LEEME.txt") $destino -Force

# El ZIP NO incluye el entorno virtual de Python (pesa ~700 MB): el lanzador
# lo recrea con setup-facial.ps1 la primera vez.
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = Join-Path $destino "SIGELAB.zip"
Remove-Item $zip -Force -ErrorAction SilentlyContinue
$excluir = @("*\.venv\*", "*\__pycache__\*", "*\tmp\*", "*\.pytest_cache\*")

$archivoZip = [System.IO.Compression.ZipFile]::Open($zip, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    $archivos = Get-ChildItem $destino -Recurse -File | Where-Object {
        $relativo = $_.FullName.Substring($destino.Length + 1)
        $coincide = $false
        foreach ($patron in $excluir) {
            if ($relativo -like $patron) { $coincide = $true; break }
        }
        -not $coincide
    }
    foreach ($archivo in $archivos) {
        if ($archivo.FullName -eq $zip) { continue }
        $relativo = $archivo.FullName.Substring($destino.Length + 1)
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $archivoZip, $archivo.FullName, $relativo, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally {
    $archivoZip.Dispose()
}

Write-Host ""
Write-Host "Distribucion lista en: $destino" -ForegroundColor Green
Get-ChildItem $destino | Select-Object Name, Length | Format-Table -AutoSize
Write-Host "Para ejecutar:  publicshell\ejecutar.bat" -ForegroundColor Green
