param(
    [ValidateSet('StoreFlow', 'Cart')]
    [string]$ServerTarget = 'StoreFlow',
    [switch]$SkipTests,
    [switch]$SkipMavenTests,
    [switch]$StartBrowserServerOnly
)

$ErrorActionPreference = 'Stop'

# Limpa variaveis de provisionamento admin eventualmente definidas na sessao do terminal para nao interferir nos testes
Remove-Item Env:ADMIN_EMAIL -ErrorAction SilentlyContinue
Remove-Item Env:ADMIN_NAME -ErrorAction SilentlyContinue
Remove-Item Env:ADMIN_PASSWORD -ErrorAction SilentlyContinue

# Configura JAVA_HOME apenas no processo atual se ainda nao estiver apontando para o JDK 21
if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME) -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $defaultJdk = 'C:\Program Files\Android\Android Studio\jbr'
    if (Test-Path (Join-Path $defaultJdk 'bin\java.exe')) {
        $env:JAVA_HOME = $defaultJdk
    } else {
        throw 'Defina JAVA_HOME apontando para o JDK 21 antes de executar este roteiro.'
    }
}
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Solicita credenciais de cupcakes_test por entrada protegida somente se ausentes no processo atual
if ([string]::IsNullOrWhiteSpace($env:DB_TEST_USERNAME) -or [string]::IsNullOrWhiteSpace($env:DB_TEST_PASSWORD)) {
    $credential = Get-Credential -Message 'Informe o usuario (ex: postgres) e a senha locais do banco PostgreSQL cupcakes_test'
    $env:DB_TEST_USERNAME = $credential.UserName
    $env:DB_TEST_PASSWORD = $credential.GetNetworkCredential().Password
}
$env:DB_TEST_URL = 'jdbc:postgresql://localhost:5432/cupcakes_test'

if (-not $SkipTests -and -not $SkipMavenTests -and -not $StartBrowserServerOnly) {
    Write-Host '=== 1/2 Executando rodada consolidada de integracao PostgreSQL em cupcakes_test ===' -ForegroundColor Cyan
    .\mvnw.cmd '-Dtest=OrderAndAdminPostgresIntegrationTest,CartPostgresIntegrationTest,AddressShippingPostgresTest,AddressRestartPostgresTest,AuthenticationPostgresIntegrationTest,AuthenticationRestartPostgresTest,ProductPostgresIntegrationTest' test
    if ($LASTEXITCODE -ne 0) {
        throw "A rodada de testes PostgreSQL falhou com codigo $LASTEXITCODE."
    }
}

$mainClass = if ($ServerTarget -eq 'Cart') {
    'br.com.pit.cupcakes.carrinho.CartBrowserServer'
} else {
    'br.com.pit.cupcakes.pedido.StoreFlowBrowserServer'
}
$port = if ($ServerTarget -eq 'Cart') { '8093' } else { '8094' }

Write-Host "=== 2/2 Iniciando servidor isolado $ServerTarget na porta $port (cupcakes_test) ===" -ForegroundColor Cyan
Write-Host 'Mantenha este terminal aberto enquanto a automacao de navegador e executada.' -ForegroundColor Yellow
.\mvnw.cmd test-compile "-Dspring-boot.run.main-class=$mainClass" spring-boot:test-run
