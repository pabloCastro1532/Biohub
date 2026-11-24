# Corre la prueba de carga del ASR-1 con k6 dentro de Docker (no hay que instalar k6).
# k6 corre dentro de la red de Compose y le habla al gateway por su nombre (http://gateway:8080), asi no depende
# de los puertos del PC. -Puerto solo se usa para comprobar desde Windows que el gateway esta arriba.
# Uso:  .\pruebas\k6\correr.ps1 [-Puerto 8090]   (5 busquedas/s normales y pico de 60 veces eso)
param([int]$Puerto = 8080, [int]$TasaNormal = 5, [int]$Factor = 60, [int]$FaseSegundos = 60)

$null = docker info --format "{{.ServerVersion}}" 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker no esta corriendo. Abre Docker Desktop y espera a que diga Engine running." -ForegroundColor Yellow
    exit 1
}
try {
    $null = Invoke-RestMethod -Uri "http://localhost:$Puerto/actuator/health" -TimeoutSec 5
} catch {
    Write-Host "El gateway no responde en http://localhost:$Puerto. Levanta el sistema primero:" -ForegroundColor Yellow
    Write-Host "  docker compose -f infra/docker-compose.yml --profile completo up -d --build"
    exit 1
}

$carpeta = $PSScriptRoot
Write-Host "Prueba ASR-1: normal $TasaNormal/s, pico $($TasaNormal * $Factor)/s, $FaseSegundos s por fase (dura unos $(2 * $FaseSegundos + 10) s)..." -ForegroundColor Cyan
docker run --rm -i -v "${carpeta}:/work" -w /work --network biohub_default `
    -e BASE_URL=http://gateway:8080 `
    -e TASA_NORMAL=$TasaNormal -e FACTOR=$Factor -e FASE_SEGUNDOS=$FaseSegundos `
    grafana/k6 run asr1.js
