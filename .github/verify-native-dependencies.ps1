param([string]$DownloadDirectory, [switch]$ValidateOnly)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
Import-Module (Join-Path $PSScriptRoot 'NativeDependencyPins.psm1') -Force
$properties = Read-NativeProperties (Join-Path $project 'gradle.properties')
$lock = Read-NativeDependencyPins (Join-Path $PSScriptRoot 'native-dependencies.json') $properties
if ($ValidateOnly) {
    Write-Output 'Hosted pin structure is valid; remote source and jar checks NOT RUN.'
    return
}
if (!$DownloadDirectory) { throw 'Supply a dedicated -DownloadDirectory for the four pinned public jars' }
$directory = [IO.Path]::GetFullPath($DownloadDirectory)
if (Test-Path -LiteralPath $directory) { throw 'Use a new native dependency download directory' }
New-Item -ItemType Directory -Path $directory | Out-Null
$records = [Collections.Generic.List[object]]::new()
try {
    foreach ($pin in $lock.dependencies) {
        $segments = @($pin.group.Split('.')) + @($pin.artifact, $pin.mavenVersion, "$($pin.artifact)-$($pin.mavenVersion).jar")
        $url = 'https://maven.moddev.zone/releases/' + (($segments | ForEach-Object { [Uri]::EscapeDataString($_) }) -join '/')
        $path = Join-Path $directory "$($pin.artifact)-$($pin.mavenVersion).jar"
        $record = [ordered]@{ id=$pin.id; version=$pin.modVersion; path=$path; sha256=$pin.sha256; url=$url; status='VERIFYING' }
        $records.Add($record)
        Assert-NativeSourceAncestry $pin
        Assert-NativeSourceTag $pin
        if ($pin.ContainsKey('source')) { $record.source = $pin.source }
        Invoke-WebRequest -Uri $url -OutFile $path -MaximumRetryCount 3
        Assert-NativeDependencyArchive $path $pin
        $record.status = 'PASS'
    }
} catch {
    if ($records.Count) { $records[-1].status = 'FAIL'; $records[-1].error = $_.Exception.Message }
    throw
} finally {
    @($records.ToArray()) | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $directory 'verified-inputs.json') -Encoding utf8
}
if ($env:GITHUB_ENV) {
    $names = @{ basepower='POWER_PLAYER'; basemetals='BASE_METALS_JAR'; mmdlib='MMDLIB_JAR'; orespawn='ORESPAWN_JAR' }
    foreach ($record in $records) { "$($names[$record.id])=$($record.path)" | Add-Content -LiteralPath $env:GITHUB_ENV -Encoding utf8 }
    "CHILD_VERSION=$($properties.mod_version)" | Add-Content -LiteralPath $env:GITHUB_ENV -Encoding utf8
}
Write-Output 'Verified four public native jars, required merged source ancestry and the exact Power release tag. This is not gameplay or release approval.'
