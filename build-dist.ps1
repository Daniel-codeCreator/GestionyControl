# =====================================================================
#  SIGELAB - Generador de la distribucion ejecutable
#  Crea:  dist\SIGELAB.jar  +  dist\lib\*.jar  +  dist\facial-service\
#         dist\ejecutar.bat  +  dist\LEEME.txt  +  dist\SIGELAB.zip
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
$dist      = Join-Path $raiz "dist"
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

Write-Host "[1/6] Limpiando build y dist..." -ForegroundColor Cyan
Remove-Item -Recurse -Force $build -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force $dist -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $build | Out-Null
New-Item -ItemType Directory -Force $dist | Out-Null
New-Item -ItemType Directory -Force (Join-Path $dist "lib") | Out-Null

Write-Host "[2/6] Compilando el proyecto (Java 21)..." -ForegroundColor Cyan
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
$fuentes = Get-ChildItem $src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
& $javac -encoding UTF-8 --release 21 -cp $cp -d (Join-Path $build "classes") $fuentes
if ($LASTEXITCODE -ne 0) { throw "La compilacion fallo." }

Write-Host "[3/6] Copiando recursos (conexion.xml)..." -ForegroundColor Cyan
if (Test-Path $resources) {
    Copy-Item (Join-Path $resources "*") (Join-Path $build "classes") -Recurse -Force
}

Write-Host "[4/6] Empaquetando SIGELAB.jar..." -ForegroundColor Cyan
& $jar --create --file (Join-Path $dist "SIGELAB.jar") --main-class Proyecto.universidad.main -C (Join-Path $build "classes") .
if ($LASTEXITCODE -ne 0) { throw "No se pudo crear el JAR." }
Copy-Item (Join-Path $resources "conexion.xml") $dist -Force

Write-Host "[5/6] Copiando librerias en lib\ y el servicio facial..." -ForegroundColor Cyan
foreach ($dep in $dependencias) {
    Copy-Item $dep (Join-Path $dist "lib") -Force
}

$destinoFacial = Join-Path $dist "facial-service"
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

Write-Host "[6/6] Copiando los scripts de ejecucion y comprimiendo..." -ForegroundColor Cyan
Copy-Item (Join-Path $raiz "packaging\iniciar-facial.bat") $destinoFacial -Force
Copy-Item (Join-Path $raiz "packaging\ejecutar.bat") $dist -Force
Copy-Item (Join-Path $raiz "packaging\LEEME.txt") $dist -Force

$zip = Join-Path $dist "SIGELAB.zip"
Remove-Item $zip -Force -ErrorAction SilentlyContinue
Compress-Archive -Path (Join-Path $dist "*") -DestinationPath $zip -Force

Write-Host ""
Write-Host "Distribucion lista en: $dist" -ForegroundColor Green
Get-ChildItem $dist | Select-Object Name, Length | Format-Table -AutoSize
Write-Host "Lanzador: dist\ejecutar.bat" -ForegroundColor Green
