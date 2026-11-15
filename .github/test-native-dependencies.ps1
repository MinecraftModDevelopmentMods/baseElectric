param([string]$EvidenceDirectory)
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'NativeDependencyPins.psm1') -Force
$project = Split-Path $PSScriptRoot -Parent
$properties = Read-NativeProperties (Join-Path $project 'gradle.properties')
$lockPath = Join-Path $PSScriptRoot 'native-dependencies.json'
$original = Get-Content -LiteralPath $lockPath -Raw | ConvertFrom-Json -AsHashtable
$root = if ($EvidenceDirectory) { [IO.Path]::GetFullPath($EvidenceDirectory) }
    else { Join-Path ([IO.Path]::GetTempPath()) ('electric-native-pin-tests-' + [guid]::NewGuid().ToString('N')) }
if (Test-Path -LiteralPath $root) { throw 'Use a new dependency-test evidence directory' }
New-Item -ItemType Directory -Path $root | Out-Null
$results = [Collections.Generic.List[object]]::new()
function Check([string]$Name, [scriptblock]$Action) {
    & $Action
    $results.Add(@{name=$Name; status='PASS'})
}
function Reject([scriptblock]$Action, [string]$Message = '.') {
    $failed = $false
    try { & $Action | Out-Null } catch {
        if ($_.Exception.Message -notmatch $Message) { throw }
        $failed = $true
    }
    if (!$failed) { throw 'Expected the unsafe input to be rejected' }
}
function Clone($Value) { return ($Value | ConvertTo-Json -Depth 20 | ConvertFrom-Json -AsHashtable) }
function Test-Lock($Value, $Props = $properties) {
    $path = Join-Path $root ('lock-' + [guid]::NewGuid().ToString('N') + '.json')
    $Value | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $path -Encoding utf8
    Read-NativeDependencyPins $path $Props
}
function New-TestJar($Pin, [string]$Version, [switch]$Forge, [switch]$OmitApi) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $path = Join-Path $root ([guid]::NewGuid().ToString('N') + '.jar')
    $archive = [IO.Compression.ZipFile]::Open($path, [IO.Compression.ZipArchiveMode]::Create)
    try {
        $name = if ($Forge) { 'META-INF/mods.toml' } else { 'META-INF/neoforge.mods.toml' }
        $writer = [IO.StreamWriter]::new($archive.CreateEntry($name).Open())
        try {
            $writer.Write("modLoader=`"javafml`" # native loader`nloaderVersion=`"[4,)`"`n[[mods]] # mandatory header`nmodId=`"$($Pin.id)`" # identity`nversion=`"$Version`" # exact version`n[[dependencies.$($Pin.id)]]`n    modId=`"neoforge`" # native dependency`n    type=`"required`"`n")
        } finally { $writer.Dispose() }
        if ($Pin.ContainsKey('requiredEntry') -and !$OmitApi) {
            $stream = $archive.CreateEntry($Pin.requiredEntry).Open()
            $stream.Dispose()
        }
    } finally { $archive.Dispose() }
    return $path
}
try {
    Check 'current lock enforces its declared hosted readiness' {
        if ($original.status -ne 'HOSTED_INPUTS_READY') {
            Reject { Read-NativeDependencyPins $lockPath $properties } 'BOOTSTRAP_BLOCKED'
        } else { Read-NativeDependencyPins $lockPath $properties | Out-Null }
    }
    $valid = Clone $original
    $valid.status = 'HOSTED_INPUTS_READY'
    $validProperties = Clone $properties
    $power = $valid.dependencies | Where-Object id -eq 'basepower'
    # Synthetic merged pins exist only in this offline test fixture, never in the project lock.
    $power.state = 'PUBLISHED'
    $power.source.commit = 'a' * 40
    $validProperties.poweradvantage_source_ref = $power.source.commit
    $validProperties.poweradvantage_pin_state = 'MERGED_SOURCE'
    Check 'synthetic complete public pin structure is accepted' { Test-Lock $valid $validProperties | Out-Null }
    Check 'a ready status alone cannot approve local Power' {
        $mutant = Clone $valid
        ($mutant.dependencies | Where-Object id -eq 'basepower').state = 'LOCAL_SOURCE_UNCOMMITTED'
        Reject { Test-Lock $mutant $validProperties } 'not a published input'
    }
    Check 'bootstrap state remains blocked even with otherwise complete pins' {
        $mutant = Clone $valid
        $mutant.status = 'BOOTSTRAP_LOCAL_ONLY'
        Reject { Test-Lock $mutant $validProperties } 'BOOTSTRAP_BLOCKED'
    }
    Check 'Power source tag cannot be omitted' {
        $mutant = Clone $valid
        ($mutant.dependencies | Where-Object id -eq 'basepower').source.Remove('tag')
        Reject { Test-Lock $mutant $validProperties } 'approved Power source tag'
    }
    Check 'Power source tag must match its public version' {
        $mutant = Clone $valid
        ($mutant.dependencies | Where-Object id -eq 'basepower').source.tag = '2.4.18.121012'
        Reject { Test-Lock $mutant $validProperties } 'approved Power source tag'
    }
    Check 'Power project and lock source tags must agree' {
        $mutant = Clone $validProperties
        $mutant.poweradvantage_source_tag = '2.4.18.121012'
        Reject { Test-Lock $valid $mutant } 'Project source tag differs'
    }
    foreach ($id in 'basepower','basemetals','mmdlib','orespawn') {
        Check "$id hash drift is rejected" {
            $mutant = Clone $valid
            ($mutant.dependencies | Where-Object id -eq $id).sha256 = '0' * 64
            Reject { Test-Lock $mutant $validProperties } 'Project pin differs'
        }
        Check "$id unpublished state is rejected" {
            $mutant = Clone $valid
            ($mutant.dependencies | Where-Object id -eq $id).state = 'LOCAL_SOURCE_UNCOMMITTED'
            Reject { Test-Lock $mutant $validProperties } 'not a published input'
        }
        Check "$id cannot be omitted" {
            $mutant = Clone $valid
            $mutant.dependencies = @($mutant.dependencies | Where-Object id -ne $id)
            Reject { Test-Lock $mutant $validProperties } 'Exactly four'
        }
    }
    Check 'Power API and runtime hashes must match' {
        $mutant = Clone $validProperties
        $mutant.poweradvantage_sha256 = '0' * 64
        Reject { Test-Lock $valid $mutant } 'API/runtime'
    }
    Check 'Power must be marked merged in project properties' {
        $mutant = Clone $validProperties
        $mutant.poweradvantage_pin_state = 'LOCAL_SOURCE'
        Reject { Test-Lock $valid $mutant } 'approved merged'
    }
    Check 'old Base Metals cannot substitute for the hook release' {
        $mutant = Clone $valid
        ($mutant.dependencies | Where-Object id -eq 'basemetals').version = '4.0.0.121012+Neo'
        Reject { Test-Lock $mutant $validProperties } 'Project pin differs'
    }
    Check 'duplicate dependency cannot replace a missing dependency' {
        $mutant = Clone $valid
        $mutant.dependencies[1] = Clone $mutant.dependencies[0]
        Reject { Test-Lock $mutant $validProperties } 'Exactly four'
    }
    Check 'wrong native platform target is rejected' {
        $mutant = Clone $valid
        $mutant.target.neoforge = '52.1.0'
        Reject { Test-Lock $mutant $validProperties } 'Wrong native build target'
    }
    foreach ($id in 'basepower','basemetals','orespawn') {
        Check "$id requires an exact source commit" {
            $mutant = Clone $valid
            ($mutant.dependencies | Where-Object id -eq $id).source.commit = $null
            Reject { Test-Lock $mutant $validProperties } 'source ancestry pin'
        }
        Check "$id source pin cannot be removed" {
            $mutant = Clone $valid
            ($mutant.dependencies | Where-Object id -eq $id).Remove('source')
            Reject { Test-Lock $mutant $validProperties } 'Missing merged source'
        }
        $pin = $valid.dependencies | Where-Object id -eq $id
        foreach ($status in 'identical','ahead','behind','diverged','unknown') {
            Check "$id ancestry status $status is handled strictly" {
                $expected = "repos/$($pin.source.repository)/compare/$($pin.source.commit)...$($pin.source.branch)"
                $expectedStatus = $status
                $compare = { param($uri)
                    if ($uri -cne $expected) { throw "Wrong source ancestry query: $uri" }
                    return $expectedStatus
                }.GetNewClosure()
                if ($status -in @('identical','ahead')) { Assert-NativeSourceAncestry $pin $compare }
                else { Reject { Assert-NativeSourceAncestry $pin $compare } 'not merged' }
            }
        }
    }
    foreach ($id in 'basepower','basemetals','mmdlib','orespawn') {
        $pin = Clone ($valid.dependencies | Where-Object id -eq $id)
        $jar = New-TestJar $pin $pin.modVersion
        $pin.sha256 = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash
        Check "$id native metadata and exact bytes are accepted" { Assert-NativeDependencyArchive $jar $pin }
        Check "$id modified jar hash is rejected" {
            $bad = Clone $pin
            $bad.sha256 = '0' * 64
            Reject { Assert-NativeDependencyArchive $jar $bad } 'SHA-256 mismatch'
        }
        Check "$id Forge jar is rejected even with its matching checksum" {
            $badJar = New-TestJar $pin $pin.modVersion -Forge
            $bad = Clone $pin
            $bad.sha256 = (Get-FileHash -LiteralPath $badJar -Algorithm SHA256).Hash
            Reject { Assert-NativeDependencyArchive $badJar $bad } 'Not a native NeoForge jar'
        }
        Check "$id wrong mod version is rejected even with its matching checksum" {
            $badJar = New-TestJar $pin '0.0.0'
            $bad = Clone $pin
            $bad.sha256 = (Get-FileHash -LiteralPath $badJar -Algorithm SHA256).Hash
            Reject { Assert-NativeDependencyArchive $badJar $bad } 'Jar metadata differs'
        }
        if ($pin.ContainsKey('requiredEntry')) {
            Check "$id jar without its required native API is rejected" {
                $badJar = New-TestJar $pin $pin.modVersion -OmitApi
                $bad = Clone $pin
                $bad.sha256 = (Get-FileHash -LiteralPath $badJar -Algorithm SHA256).Hash
                Reject { Assert-NativeDependencyArchive $badJar $bad } 'required native API'
            }
        }
    }
    $refUri = "repos/$($power.source.repository)/git/ref/tags/$($power.source.tag)"
    $objectSha = 'b' * 40
    $objectUri = "repos/$($power.source.repository)/git/tags/$objectSha"
    foreach ($mode in 'lightweight','annotated','wrong-commit','wrong-type','invalid-object','tag-cycle','unavailable') {
        Check "Power release tag $mode is handled strictly" {
            $responses = @{}
            $responses[$refUri] = @{object=@{type='commit';sha=$power.source.commit}}
            $message = $null
            switch ($mode) {
                'annotated' {
                    $responses[$refUri] = @{object=@{type='tag';sha=$objectSha}}
                    $responses[$objectUri] = @{object=@{type='commit';sha=$power.source.commit}}
                }
                'wrong-commit' {
                    $responses[$refUri].object.sha = '0' * 40
                    $message = 'does not resolve to approved commit'
                }
                'wrong-type' {
                    $responses[$refUri].object.type = 'blob'
                    $message = 'does not resolve to approved commit'
                }
                'invalid-object' {
                    $responses[$refUri] = @{object=@{type='tag';sha='invalid'}}
                    $message = 'Invalid annotated source tag object'
                }
                'tag-cycle' {
                    $responses[$refUri] = @{object=@{type='tag';sha=$objectSha}}
                    $responses[$objectUri] = @{object=@{type='tag';sha=$objectSha}}
                    $message = 'resolution depth exceeded'
                }
                'unavailable' {
                    $responses.Clear()
                    $message = 'synthetic unavailable tag'
                }
            }
            $request = { param($uri)
                if (!$responses.ContainsKey($uri)) { throw "synthetic unavailable tag: $uri" }
                return $responses[$uri]
            }.GetNewClosure()
            if ($message) { Reject { Assert-NativeSourceTag $power $request } $message }
            else { Assert-NativeSourceTag $power $request }
        }
    }
} finally {
    @{scope='OFFLINE_GUARD_TESTS_NOT_JAVA_OR_RELEASE_QUALIFICATION'; results=@($results.ToArray())} |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $root 'results.json') -Encoding utf8
}
Write-Output "PASS: $($results.Count) offline native dependency guard checks. No network, Java or Gradle run. Evidence: $root"
