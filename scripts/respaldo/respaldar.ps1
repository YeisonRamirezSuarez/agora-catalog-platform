# Respaldo diario de Agorá Catalog corriendo nativo en Windows (PostgreSQL instalado + API con su .env):
# la base (pg_dump) Y la carpeta de fotos. Una sin la otra no sirve: la base guarda las URL de las fotos.
# Para producción en Docker está respaldar.sh.
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\respaldo\respaldar.ps1
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\respaldo\respaldar.ps1 -Programar   # tarea diaria 3:30 a. m.
#
# Lee DB_URL, DB_USERNAME, DB_PASSWORD y STORAGE_PATH de agora-catalog-back\.env. Variables opcionales
# (en el mismo .env o en el entorno):
#   RESPALDO_DIR      carpeta local (por defecto %USERPROFILE%\respaldos\agora-catalog)
#   RESPALDO_DIAS     días que se guardan (por defecto 14)
#   RESPALDO_REMOTO   carpeta fuera de este PC (disco externo, unidad de red o carpeta sincronizada con la
#                     nube). Sin ella, si se daña este PC se pierden también los respaldos.
#
# Restaurar: ver README, "Respaldos".
param([switch]$Programar)
$ErrorActionPreference = 'Stop'

$repo = (Resolve-Path "$PSScriptRoot\..\..").Path
$back = Join-Path $repo 'agora-catalog-back'

if ($Programar) {
    $accion = New-ScheduledTaskAction -Execute 'powershell.exe' `
        -Argument "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$PSCommandPath`""
    $cuando = New-ScheduledTaskTrigger -Daily -At '3:30'
    # Si el PC estaba apagado a esa hora, corre apenas se prenda.
    $ajustes = New-ScheduledTaskSettingsSet -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Hours 1)
    Register-ScheduledTask -TaskName 'Agora Catalog - respaldo diario' -Action $accion -Trigger $cuando `
        -Settings $ajustes -Description 'Base y fotos de Agora Catalog (scripts\respaldo\respaldar.ps1)' -Force | Out-Null
    Write-Output 'Tarea programada: todos los días a las 3:30 a. m. (o al prender el PC si estaba apagado).'
    exit 0
}

# Valores del .env (KEY='valor' o KEY=valor); el entorno del sistema tiene prioridad, como en la API.
$env_ = @{}
foreach ($linea in Get-Content (Join-Path $back '.env') -Encoding UTF8) {
    if ($linea -match "^\s*([A-Z_][A-Z0-9_]*)\s*=\s*'?(.*?)'?\s*$") { $env_[$Matches[1]] = $Matches[2] }
}
function Valor($clave, $porDefecto) {
    $v = [Environment]::GetEnvironmentVariable($clave)
    if ($v) { return $v }
    if ($env_[$clave]) { return $env_[$clave] }
    return $porDefecto
}

$dbUrl = Valor 'DB_URL' ''
if ($dbUrl -notmatch '^jdbc:postgresql://([^:/]+)(?::(\d+))?/([^?]+)') { throw "DB_URL no reconocida: $dbUrl" }
$dbHost, $dbPort, $dbName = $Matches[1], ($(if ($Matches[2]) { $Matches[2] } else { '5432' })), $Matches[3]
$fotos = Valor 'STORAGE_PATH' './uploads'
if (-not [IO.Path]::IsPathRooted($fotos)) { $fotos = Join-Path $back $fotos }
$dir = Valor 'RESPALDO_DIR' (Join-Path $env:USERPROFILE 'respaldos\agora-catalog')
$dias = [int](Valor 'RESPALDO_DIAS' '14')
$remoto = Valor 'RESPALDO_REMOTO' ''
$pgDump = Get-ChildItem 'C:\Program Files\PostgreSQL\*\bin\pg_dump.exe' | Sort-Object FullName -Descending | Select-Object -First 1
if (-not $pgDump) { throw 'No se encontró pg_dump.exe en C:\Program Files\PostgreSQL' }

New-Item -ItemType Directory -Force $dir | Out-Null
$fecha = Get-Date -Format 'yyyy-MM-dd_HHmmss'
$bd = Join-Path $dir "bd-$fecha.dump"
$zip = Join-Path $dir "fotos-$fecha.zip"
$log = Join-Path $dir 'respaldo.log'
function Anotar($texto) { "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $texto" | Tee-Object -FilePath $log -Append }

try {
    # Formato custom (-Fc): comprimido y se restaura con pg_restore, incluso tabla por tabla.
    $env:PGPASSWORD = Valor 'DB_PASSWORD' ''
    & $pgDump.FullName -h $dbHost -p $dbPort -U (Valor 'DB_USERNAME' 'postgres') -Fc -f "$bd.tmp" $dbName
    if ($LASTEXITCODE -ne 0) { throw "pg_dump terminó con código $LASTEXITCODE" }
    Move-Item "$bd.tmp" $bd -Force

    # shortcut: Compress-Archive de Windows PowerShell no maneja archivos de más de 2 GB; con muchas fotos,
    # pasar a robocopy de la carpeta o a 7-Zip.
    if (Test-Path $fotos) { Compress-Archive -Path (Join-Path $fotos '*') -DestinationPath $zip -Force }
    else { Anotar "AVISO: no existe la carpeta de fotos $fotos" }

    Get-ChildItem $dir -File | Where-Object { $_.Name -match '^(bd-.*\.dump|fotos-.*\.zip)$' -and $_.LastWriteTime -lt (Get-Date).AddDays(-$dias) } |
        Remove-Item -Force

    $tam = '{0:N1} MB' -f (((Get-Item $bd).Length + $(if (Test-Path $zip) { (Get-Item $zip).Length } else { 0 })) / 1MB)
    if ($remoto) {
        New-Item -ItemType Directory -Force $remoto | Out-Null
        Copy-Item $bd, $zip -Destination $remoto -Force -ErrorAction SilentlyContinue
        Get-ChildItem $remoto -File | Where-Object { $_.Name -match '^(bd-.*\.dump|fotos-.*\.zip)$' -and $_.LastWriteTime -lt (Get-Date).AddDays(-$dias) } |
            Remove-Item -Force
        Anotar "OK $fecha ($tam) en $dir y copia en $remoto"
    } else {
        Anotar "OK $fecha ($tam) en $dir. AVISO: sin RESPALDO_REMOTO, el respaldo queda solo en este PC."
    }
} catch {
    Anotar "ERROR $fecha : $($_.Exception.Message)"
    exit 1
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
