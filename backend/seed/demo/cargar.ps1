param(
    [string]$Database,
    [switch]$Preview
)

$ErrorActionPreference = 'Stop'
$backend = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if ($Preview) {
    Push-Location $backend
    try {
        & ./gradlew.bat --no-daemon seedDemoData --args=--preview
        if ($LASTEXITCODE -ne 0) { throw 'Falló la vista previa de demo.' }
    } finally { Pop-Location }
    return
}
if ([string]::IsNullOrWhiteSpace($Database)) { throw 'Indicá -Database con el nombre de la base ficticia de destino.' }
$previousDatabase = $env:DEMO_DATABASE
$previousPassword = $env:DEMO_PASSWORD
$securePassword = Read-Host 'Contraseña nueva para las tres cuentas ficticias (12 caracteres, letra y número)' -AsSecureString
try {
    $env:DEMO_DATABASE = $Database
    $env:DEMO_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
    Push-Location $backend
    try {
        & ./gradlew.bat --no-daemon seedDemoData
        if ($LASTEXITCODE -ne 0) { throw 'Falló la carga de demo; revisá el mensaje del cargador.' }
    } finally { Pop-Location }
} finally {
    $env:DEMO_DATABASE = $previousDatabase
    $env:DEMO_PASSWORD = $previousPassword
    $securePassword.Dispose()
}
