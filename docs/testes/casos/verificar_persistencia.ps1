param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Before', 'After')]
    [string]$Stage
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$resultDirectory = Join-Path $projectRoot 'docs/testes/resultados'
if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME) -or [string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    throw 'Execute no terminal que possui DB_USERNAME e DB_PASSWORD configurados. Nenhuma credencial deve ser enviada ao chat.'
}
if ($env:DB_URL -and $env:DB_URL -cne 'jdbc:postgresql://localhost:5432/cupcakes') {
    throw 'DB_URL deve apontar exatamente para localhost:5432/cupcakes.'
}
$psqlCommand = Get-Command psql -ErrorAction SilentlyContinue
if ($psqlCommand) {
    $psqlPath = $psqlCommand.Source
} else {
    $psqlPath = Get-ChildItem -Path (Join-Path $env:ProgramFiles 'PostgreSQL/*/bin/psql.exe') -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $psqlPath) {
    throw 'Inclua a pasta bin do PostgreSQL no PATH deste terminal.'
}
$beforePath = Join-Path $resultDirectory 'persistencia-antes.json'
$afterPath = Join-Path $resultDirectory 'persistencia-depois.json'
$outputPath = if ($Stage -eq 'Before') { $beforePath } else { $afterPath }
if (Test-Path -LiteralPath $outputPath) {
    throw 'A coleta desta fase ja existe. Preserve o arquivo e revise antes de repetir.'
}
if ($Stage -eq 'After' -and -not (Test-Path -LiteralPath $beforePath)) {
    throw 'Execute a coleta Before antes do reinicio.'
}
$query = @'
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SELECT json_build_object(
    'database', current_database(),
    'readOnly', current_setting('transaction_read_only'),
    'serverPort', inet_server_port(),
    'capturedAt', current_timestamp,
    'migrations', COALESCE((SELECT json_agg(m ORDER BY installed_rank) FROM (
        SELECT installed_rank, version, description, type, script, checksum, success
        FROM public.flyway_schema_history
    ) m), '[]'::json),
    'products', COALESCE((SELECT json_agg(p ORDER BY id) FROM (
        SELECT id, nome, descricao, preco, imagem_caminho, disponivel, versao
        FROM public.produto
    ) p), '[]'::json)
);
ROLLBACK;
'@
$previousPassword = $env:PGPASSWORD
$previousOptions = $env:PGOPTIONS
$previousEncoding = $env:PGCLIENTENCODING
$previousOutputEncoding = $OutputEncoding
$previousConsoleEncoding = [Console]::OutputEncoding
try {
    $env:PGPASSWORD = $env:DB_PASSWORD
    $env:PGOPTIONS = '-c default_transaction_read_only=on -c statement_timeout=10000'
    $env:PGCLIENTENCODING = 'UTF8'
    $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    [Console]::OutputEncoding = $OutputEncoding
    $rawResult = & $psqlPath -X -w -q -t -A -h localhost -p 5432 -U $env:DB_USERNAME -d cupcakes -v ON_ERROR_STOP=1 -c $query 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw 'Falha na consulta somente de leitura. Confira as credenciais e a conexao no terminal configurado.'
    }
    $snapshot = ($rawResult -join "`n") | ConvertFrom-Json
} finally {
    $env:PGPASSWORD = $previousPassword
    $env:PGOPTIONS = $previousOptions
    $env:PGCLIENTENCODING = $previousEncoding
    $OutputEncoding = $previousOutputEncoding
    [Console]::OutputEncoding = $previousConsoleEncoding
}
if ($snapshot.database -cne 'cupcakes' -or $snapshot.readOnly -cne 'on' -or $snapshot.serverPort -ne 5432) {
    throw 'Conexao efetiva nao corresponde ao banco de desenvolvimento autorizado em modo somente de leitura.'
}
$schemaMigration = @($snapshot.migrations | Where-Object {
    $_.version -eq '1' -and $_.script -eq 'V1__criar_tabela_produto.sql' -and $_.success -eq $true
})
if ($schemaMigration.Count -ne 1 -or @($snapshot.migrations | Where-Object { $_.success -ne $true }).Count -gt 0) {
    throw 'Historico Flyway requer revisao: a migracao V1 deve constar exatamente uma vez e sem falhas.'
}
$snapshot | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $outputPath -Encoding UTF8
Write-Output ("Coleta {0}: banco cupcakes, V1 aplicada, {1} produtos. Sem alteracao de dados." -f $Stage, @($snapshot.products).Count)
if ($Stage -eq 'After') {
    $before = Get-Content -Raw -LiteralPath $beforePath -Encoding UTF8 | ConvertFrom-Json
    $sameProducts = (ConvertTo-Json -InputObject @($before.products) -Depth 10 -Compress) -ceq
        (ConvertTo-Json -InputObject @($snapshot.products) -Depth 10 -Compress)
    $sameMigrations = (ConvertTo-Json -InputObject @($before.migrations) -Depth 10 -Compress) -ceq
        (ConvertTo-Json -InputObject @($snapshot.migrations) -Depth 10 -Compress)
    if (-not $sameProducts -or -not $sameMigrations) {
        throw 'Os registros ou o historico mudaram entre as coletas. As evidencias foram preservadas para revisao.'
    }
    Write-Output 'Comparacao aprovada: mesmos IDs, campos, quantidades e historico Flyway; nenhuma duplicacao entre as coletas.'
}
