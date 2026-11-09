param(
    [Parameter(Mandatory)][string]$Evidence,
    [Parameter(Mandatory)][string]$ServerSeed,
    [Parameter(Mandatory)][string]$EulaFile,
    [Parameter(Mandatory)][string]$Java,
    [Parameter(Mandatory)][string]$ElectricJar,
    [Parameter(Mandatory)][ValidatePattern('^[A-F0-9]{64}$')][string]$ElectricSha256,
    [Parameter(Mandatory)][string]$PowerJar,
    [Parameter(Mandatory)][ValidatePattern('^[A-F0-9]{64}$')][string]$PowerSha256,
    [Parameter(Mandatory)][string]$BaseMetalsJar,
    [Parameter(Mandatory)][string]$OreSpawnJar
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
$output = [IO.Path]::GetFullPath($Evidence, $root)
$evidenceRoot = Join-Path $root 'build/port-evidence'
if (!$output.StartsWith($evidenceRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or
        (Test-Path -LiteralPath $output)) {
    throw 'Use a new disposable directory inside build/port-evidence.'
}
$null = New-Item -ItemType Directory -Path $output
$pins = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $root 'gradle.properties') -Raw)
$seed = (Resolve-Path -LiteralPath $ServerSeed).Path
$javaPath = (Resolve-Path -LiteralPath $Java).Path
$winArgs = 'libraries/net/minecraftforge/forge/1.18.2-40.3.0/win_args.txt'
$receipt = [ordered]@{
    status = 'FAIL'; scope = 'Packaged, loopback-only Forge 1.18.2 dedicated server smoke; no client or migration proof'
    seed = $seed; java = $javaPath; inputs = @(); checks = [ordered]@{}; exitCode = $null; error = $null
}
$process = $null
$stdout = $null
$stderr = $null

function Wait-ForLine([string]$Pattern, [int]$Seconds) {
    $timer = [Diagnostics.Stopwatch]::StartNew()
    while ($timer.Elapsed.TotalSeconds -lt $Seconds) {
        $read = $process.StandardOutput.ReadLineAsync()
        while (!$read.Wait(250)) {
            if ($timer.Elapsed.TotalSeconds -ge $Seconds) { throw "Timed out waiting for $Pattern" }
        }
        $line = $read.Result
        if ($null -eq $line) { throw "Server output ended before $Pattern" }
        $stdout.WriteLine($line)
        if ($line -match $Pattern) { return $line }
        if ($line -match 'Failed to start the minecraft server|Failed to complete lifecycle event|Failed to load datapacks') {
            throw "Server reported a startup failure: $line"
        }
    }
    throw "Timed out waiting for $Pattern"
}

function Send-Command([string]$Command) {
    $stdout.WriteLine(">> $Command")
    $process.StandardInput.WriteLine($Command)
    $process.StandardInput.Flush()
}

try {
    if ($pins.poweradvantage_pin_state -ne 'LOCAL_CHECKPOINT' -or $PowerSha256 -ne $pins.poweradvantage_runtime_sha256) {
        throw 'Packaged smoke requires the exact source-pinned basePower player JAR.'
    }
    $receipt.powerSourceRef = $pins.poweradvantage_source_ref
    $javaRelease = Join-Path (Split-Path (Split-Path $javaPath)) 'release'
    if ((Get-Content -LiteralPath $javaRelease -Raw) -notmatch 'JAVA_VERSION="17\.') {
        throw 'The dedicated-server smoke requires Java 17.'
    }
    $receipt.javaRelease = (Get-FileHash -LiteralPath $javaRelease).Hash
    $receipt.javaSha256 = (Get-FileHash -LiteralPath $javaPath).Hash
    $argsText = Get-Content -LiteralPath (Join-Path $seed $winArgs) -Raw
    foreach ($marker in @('--launchTarget forgeserver', '--fml.forgeVersion 40.3.0',
            '--fml.mcVersion 1.18.2', '--fml.mcpVersion 20220404.173914')) {
        if (!$argsText.Contains($marker)) { throw "Unexpected Forge seed arguments: missing $marker" }
    }
    $receipt.seedArgumentsSha256 = (Get-FileHash -LiteralPath (Join-Path $seed $winArgs)).Hash
    $forgeUniversal = Join-Path $seed 'libraries/net/minecraftforge/forge/1.18.2-40.3.0/forge-1.18.2-40.3.0-universal.jar'
    $receipt.forgeUniversalSha256 = (Get-FileHash -LiteralPath $forgeUniversal).Hash
    if ((Get-Content -LiteralPath $EulaFile -Raw) -notmatch '(?m)^eula=true\s*$') {
        throw 'Pass the existing accepted EULA from the local smoke fixture.'
    }
    Copy-Item -LiteralPath (Join-Path $seed 'libraries') -Destination $output -Recurse
    Copy-Item -LiteralPath $EulaFile -Destination (Join-Path $output 'eula.txt')
    $modDir = (New-Item -ItemType Directory -Path (Join-Path $output 'mods')).FullName
    foreach ($input in @(
            @{ path = $ElectricJar; hash = $ElectricSha256; name = 'baseElectric-2.2.11.118021.jar' },
            @{ path = $PowerJar; hash = $PowerSha256; name = 'basePower-2.4.10.118021.jar' },
            @{ path = $BaseMetalsJar; hash = $pins.basemetals_runtime_sha256; name = 'BaseMetals-3.1.2.118021.jar' },
            @{ path = $OreSpawnJar; hash = $pins.orespawn_runtime_sha256; name = 'OreSpawn-4.1.0.118021.jar' })) {
        $actual = Get-FileHash -LiteralPath $input.path
        if ($actual.Hash -ne $input.hash -or [IO.Path]::GetFileName($actual.Path) -cne $input.name) {
            throw "Wrong packaged input: $($actual.Path) SHA-256 $($actual.Hash)"
        }
        $copy = Join-Path $modDir $input.name
        Copy-Item -LiteralPath $actual.Path -Destination $copy
        if ((Get-FileHash -LiteralPath $copy).Hash -ne $input.hash) { throw "Copy changed: $copy" }
        $receipt.inputs += [ordered]@{ path = $actual.Path; sha256 = $actual.Hash; copy = $copy }
    }
    [IO.File]::WriteAllLines((Join-Path $output 'server.properties'), @(
        'server-ip=127.0.0.1', 'server-port=0', 'enable-rcon=false', 'enable-query=false',
        'level-name=electric-smoke', 'level-seed=118021', 'generate-structures=false',
        'view-distance=3', 'simulation-distance=3', 'spawn-protection=0'
    ))
    $start = [Diagnostics.ProcessStartInfo]::new($javaPath)
    $start.WorkingDirectory = $output
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardInput = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    foreach ($arg in @('-Xmx3G', '-Dfml.earlyprogresswindow=false', "@$winArgs", 'nogui')) {
        $start.ArgumentList.Add($arg)
    }
    $stdout = [IO.StreamWriter]::new((Join-Path $output 'console.log'))
    $stdout.AutoFlush = $true
    $process = [Diagnostics.Process]::Start($start)
    $stderr = $process.StandardError.ReadToEndAsync()
    $receipt.checks.commonSetup = Wait-ForLine 'baseElectric 2\.2\.11\.118021 common setup complete' 120
    $receipt.checks.ready = Wait-ForLine 'Done \([^)]+\)! For help' 180

    Send-Command 'forceload add 0 0'
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        Send-Command 'execute if block 0 300 0 minecraft:air'
        $probe = Wait-ForLine 'Test passed|Test failed|That position is not loaded' 10
        if ($probe -match 'Test passed') { $receipt.checks.testChunkReady = $probe; break }
        if ($probe -match 'Test failed') { throw 'The smoke test coordinates are not clear.' }
        Start-Sleep -Milliseconds 500
    }
    if (!$receipt.checks.Contains('testChunkReady')) { throw 'The smoke chunk never loaded.' }
    Send-Command 'setblock 0 300 0 baseelectric:electric_drill'
    $receipt.checks.placeDrill = Wait-ForLine 'Changed the block at 0, 300, 0' 20
    Send-Command 'data get block 0 300 0'
    $receipt.checks.drillNbt = Wait-ForLine 'block data:.*baseelectric:electric_drill' 20
    Send-Command 'setblock 1 300 0 baseelectric:sulfur_ore'
    $receipt.checks.placeSulfur = Wait-ForLine 'Changed the block at 1, 300, 0' 20
    Send-Command 'loot spawn 1 300 0 mine 1 300 0 minecraft:wooden_pickaxe'
    $receipt.checks.sulfurLoot = Wait-ForLine 'Dropped [1-4] \[Powdered Sulfur\] from loot table baseelectric:blocks/sulfur_ore' 20
    Send-Command 'data get entity @e[type=minecraft:item,sort=nearest,limit=1,x=1,y=300,z=0,distance=..5] Item'
    $receipt.checks.sulfurItem = Wait-ForLine 'entity data:.*baseelectric:sulfur_powder' 20
    $expectedCount = [regex]::Match($receipt.checks.sulfurLoot, 'Dropped ([1-4]) ').Groups[1].Value
    if ($receipt.checks.sulfurItem -notmatch "Count: ${expectedCount}b") {
        throw 'The live item stack count does not match the sulfur loot response.'
    }
    Send-Command 'save-all flush'
    $receipt.checks.save = Wait-ForLine 'Saved the game' 30
    Send-Command 'stop'
    $receipt.checks.shutdown = Wait-ForLine 'All dimensions are saved' 45
    if (!$process.WaitForExit(30000) -or $process.ExitCode -ne 0) { throw 'Server did not stop cleanly.' }
    $receipt.status = 'PASS'
} catch {
    $receipt.error = $_.ToString()
    throw
} finally {
    if ($process) {
        if (!$process.HasExited) {
            try { $process.StandardInput.WriteLine('stop'); $process.StandardInput.Flush() } catch {}
            if (!$process.WaitForExit(10000)) {
                $process.Kill($true)
                $process.WaitForExit()
            }
        }
        $receipt.exitCode = $process.ExitCode
        if ($stderr) { [IO.File]::WriteAllText((Join-Path $output 'stderr.log'), $stderr.GetAwaiter().GetResult()) }
        $process.Dispose()
    }
    if ($stdout) { $stdout.Dispose() }
    [IO.File]::WriteAllText((Join-Path $output 'receipt.json'), ($receipt | ConvertTo-Json -Depth 8) + "`n")
}

$receipt | ConvertTo-Json -Depth 8
