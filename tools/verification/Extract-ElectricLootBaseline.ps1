param([Parameter(Mandatory = $true)][string]$AcceptedJar)
$ErrorActionPreference = 'Stop'
$expected = 'A7D12A66CA91A6BACCFEFD2CED9D3EC962F5FEB73BF632E1683F99084384F938'
if ((Get-FileHash -LiteralPath $AcceptedJar -Algorithm SHA256).Hash -cne $expected) {
    throw 'This extractor accepts only the frozen Electric 2.2.18.2602001 jar'
}
$project = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
$output = Join-Path $project 'src/test/resources/loot-baseline-262'
if (Test-Path -LiteralPath $output) { throw 'Refusing to overwrite the independent golden fixture' }
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $AcceptedJar).Path)
try {
    $entries = @($zip.Entries | Where-Object {
        $_.FullName.StartsWith('data/baseelectric/loot_table/') -and $_.FullName.EndsWith('.json')
    } | Sort-Object FullName)
    if ($entries.Count -ne 21) { throw 'Unexpected accepted loot-table inventory' }
    $tables = @()
    foreach ($entry in $entries) {
        $target = [IO.Path]::GetFullPath((Join-Path $output $entry.FullName))
        if (!$target.StartsWith($output + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Archive entry escaped fixture root'
        }
        $null = New-Item -ItemType Directory -Path (Split-Path $target -Parent) -Force
        $input = $entry.Open()
        $file = [IO.File]::Create($target)
        try { $input.CopyTo($file) } finally { $file.Dispose(); $input.Dispose() }
        $tables += [ordered]@{ path = $entry.FullName; bytes = $entry.Length
            sha256 = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash }
    }
    $manifest = [ordered]@{ artifact = 'baseElectric-2.2.18.2602001.jar'; sourceJarSha256 = $expected
        scope = 'Exact accepted 26.2 loot bytes; historical data baseline, not 26.3 qualification'
        tables = $tables }
    $json = ($manifest | ConvertTo-Json -Depth 5).Replace("`r`n", "`n") + "`n"
    [IO.File]::WriteAllText((Join-Path $output 'manifest.json'), $json, [Text.UTF8Encoding]::new($false))
    Get-FileHash -LiteralPath (Join-Path $output 'manifest.json') -Algorithm SHA256
} finally { $zip.Dispose() }
