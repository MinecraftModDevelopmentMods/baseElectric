Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Read-NativeProperties([string]$Path) {
    $properties = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*([^#!\s=]+)\s*=(.*)$') {
            if ($properties.ContainsKey($Matches[1])) { throw "Duplicate project property: $($Matches[1])" }
            $properties[$Matches[1]] = $Matches[2].Trim()
        }
    }
    return $properties
}

function Read-NativeDependencyPins([string]$LockPath, [hashtable]$Properties) {
    $lock = Get-Content -LiteralPath $LockPath -Raw | ConvertFrom-Json -AsHashtable
    if ($lock.schemaVersion -ne 1) { throw 'Unsupported native dependency lock schema' }
    if ($lock.status -ne 'HOSTED_INPUTS_READY') { throw "BOOTSTRAP_BLOCKED: $($lock.reason)" }
    $target = $lock.target
    if ($target.minecraft -ne '1.21.1' -or $target.neoforge -ne '21.1.265' -or
            $target.java -ne '21' -or $target.moddev -ne '2.0.148' -or $target.gradle -ne '9.8.0' -or
            $target.releaseBranch -ne 'master-1.21.1-neo') { throw 'Wrong native build target' }
    if ($Properties.minecraft_version -ne $target.minecraft -or $Properties.neo_version -ne $target.neoforge -or
            $Properties.java_version -ne $target.java -or $Properties.loader_name -ne 'neoforge' -or
            $Properties.loader_code -ne '2') { throw 'Dependency lock differs from the project target' }
    $ids = @($lock.dependencies | ForEach-Object { $_.id } | Sort-Object)
    if (($ids -join ',') -ne 'basemetals,basepower,mmdlib,orespawn') {
        throw 'Exactly four unique native dependencies are required'
    }
    $coordinates = @{
        basepower = @('zone.moddev.mc.basepower', 'basePower', 'poweradvantage_version', 'poweradvantage_runtime_sha256')
        basemetals = @('zone.moddev.mc.basemetals', 'BaseMetals', 'basemetals_version', 'basemetals_sha256')
        mmdlib = @('zone.moddev.mc.lib', 'mmdlib', 'mmdlib_version', 'mmdlib_sha256')
        orespawn = @('zone.moddev.mc.orespawn', 'OreSpawn', 'orespawn_version', 'orespawn_sha256')
    }
    $requiredEntries = @{
        basepower = 'zone/moddev/mc/basepower/compat/CrushingRecipes.class'
        basemetals = 'zone/moddev/mc/basemetals/migration/LegacyWorldDataHook.class'
    }
    foreach ($pin in $lock.dependencies) {
        $expected = $coordinates[$pin.id]
        if ($pin.state -ne 'PUBLISHED') { throw "Dependency $($pin.id) is not a published input" }
        if ($pin.group -cne $expected[0] -or $pin.artifact -cne $expected[1] -or
                $pin.versionProperty -cne $expected[2] -or $pin.hashProperty -cne $expected[3]) {
            throw "Unexpected coordinate or project property for $($pin.id)"
        }
        if ($pin.version -cne $Properties[$pin.versionProperty] -or
                $pin.sha256 -cne $Properties[$pin.hashProperty]) { throw "Project pin differs for $($pin.id)" }
        if ($pin.sha256 -cnotmatch '^[A-F0-9]{64}$' -or
                $pin.mavenVersion -cnotmatch '^\d+\.\d+\.\d+\.121012(?:\+Neo)?$' -or
                $pin.mavenVersion -cne $pin.version -or $pin.modVersion -cne $pin.mavenVersion) {
            throw "Missing exact public native artifact identity for $($pin.id)"
        }
        if ($pin.ContainsKey('source')) {
            $source = $pin.source
            if ($source.repository -cne "MinecraftModDevelopmentMods/$($pin.artifact)" -or
                    $source.commit -cnotmatch '^[a-f0-9]{40}$' -or $source.branch -cne $target.releaseBranch) {
                throw "Missing approved source ancestry pin for $($pin.id)"
            }
            $property = if ($pin.id -eq 'basepower') { 'poweradvantage_source_ref' } else { "$($pin.id)_source_commit" }
            if (!$Properties.ContainsKey($property) -or $Properties[$property] -cne $source.commit) {
                throw "Project source pin differs for $($pin.id)"
            }
            if ($pin.id -eq 'basepower') {
                if (!$source.ContainsKey('tag') -or $source.tag -cne $pin.mavenVersion) {
                    throw 'Missing approved Power source tag'
                }
                if (!$Properties.ContainsKey('poweradvantage_source_tag') -or
                        $Properties.poweradvantage_source_tag -cne $source.tag) {
                    throw 'Project source tag differs for basepower'
                }
            }
        } elseif ($pin.id -ne 'mmdlib') { throw "Missing merged source pin for $($pin.id)" }
        if ($requiredEntries.ContainsKey($pin.id) -and
                (!$pin.ContainsKey('requiredEntry') -or $pin.requiredEntry -cne $requiredEntries[$pin.id])) {
            throw "Required native API gate was removed for $($pin.id)"
        }
        if ($pin.id -eq 'basepower' -and
                ($Properties.poweradvantage_pin_state -cne 'MERGED_SOURCE' -or
                 $Properties.poweradvantage_sha256 -cne $pin.sha256)) {
            throw 'Power source and API/runtime pins are not approved merged inputs'
        }
    }
    return $lock
}

function Assert-NativeSourceAncestry($Pin, [scriptblock]$Compare = {
    param($Uri)
    $status = & gh api $Uri --jq '.status'
    if ($LASTEXITCODE -ne 0) { throw 'GitHub source comparison failed' }
    return $status
}) {
    if (!$Pin.ContainsKey('source')) { return }
    $source = $Pin.source
    $uri = "repos/$($source.repository)/compare/$($source.commit)...$($source.branch)"
    $status = & $Compare $uri
    if ($status -notin @('identical', 'ahead')) {
        throw "Source $($source.commit) is not merged on $($source.branch): $status"
    }
}

function Assert-NativeDependencyArchive([string]$Path, $Pin) {
    if ((Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash -cne $Pin.sha256) {
        throw "SHA-256 mismatch for $($Pin.id)"
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($Path)
    try {
        $entry = $archive.GetEntry('META-INF/neoforge.mods.toml')
        if (!$entry -or $archive.GetEntry('META-INF/mods.toml')) { throw "Not a native NeoForge jar: $($Pin.id)" }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { $metadata = $reader.ReadToEnd() } finally { $reader.Dispose() }
        $header = [regex]::Match($metadata, '(?ms)^[ \t]*\[\[mods\]\][ \t]*(?:#[^\r\n]*)?\r?\n(.*?)(?=^[ \t]*\[|\z)').Groups[1].Value
        $id = [regex]::Escape($Pin.id)
        $version = [regex]::Escape($Pin.modVersion)
        if ($metadata -match '\$\{[^}]+\}' -or
                $header -notmatch "(?m)^[ \t]*modId[ \t]*=[ \t]*`"$id`"[ \t]*(?:#[^\r\n]*)?\r?$" -or
                $header -notmatch "(?m)^[ \t]*version[ \t]*=[ \t]*`"$version`"[ \t]*(?:#[^\r\n]*)?\r?$" -or
                $metadata -notmatch '(?m)^[ \t]*modId[ \t]*=[ \t]*"neoforge"[ \t]*(?:#[^\r\n]*)?\r?$') {
            throw "Jar metadata differs for $($Pin.id)"
        }
        if ($Pin.ContainsKey('requiredEntry') -and !$archive.GetEntry($Pin.requiredEntry)) {
            throw "Jar lacks the required native API: $($Pin.id)"
        }
    } finally { $archive.Dispose() }
}

function Assert-NativeSourceTag($Pin, [scriptblock]$Request = {
    param($Uri)
    $response = & gh api $Uri
    if ($LASTEXITCODE -ne 0) { throw 'GitHub source tag lookup failed' }
    return ($response -join "`n" | ConvertFrom-Json -AsHashtable)
}) {
    if (!$Pin.ContainsKey('source') -or !$Pin.source.ContainsKey('tag')) { return }
    $source = $Pin.source
    $prefix = "repos/$($source.repository)"
    $ref = & $Request "$prefix/git/ref/tags/$([Uri]::EscapeDataString($source.tag))"
    $object = $ref.object
    $depth = 0
    while ($object.type -ceq 'tag') {
        $depth++
        if ($depth -gt 5) { throw 'Source tag resolution depth exceeded' }
        if ($object.sha -cnotmatch '^[a-f0-9]{40}$') { throw 'Invalid annotated source tag object' }
        $tag = & $Request "$prefix/git/tags/$($object.sha)"
        $object = $tag.object
    }
    if ($object.type -cne 'commit' -or $object.sha -cne $source.commit) {
        throw "Source tag $($source.tag) does not resolve to approved commit $($source.commit)"
    }
}

Export-ModuleMember -Function Read-NativeProperties, Read-NativeDependencyPins, Assert-NativeSourceAncestry, Assert-NativeSourceTag, Assert-NativeDependencyArchive
