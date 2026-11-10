param(
    [string]$ProjectRoot = (Join-Path $PSScriptRoot '../..'),
    [Parameter(Mandatory = $true)][string]$Evidence,
    [string]$Compiler = 'C:/Users/johnb/.p2/pool/plugins/org.eclipse.jdt.core.compiler.batch_3.46.0.v20260528-0407.jar',
    [string]$Java17Home = 'C:/Users/johnb/.jdks/temurin-17.0.1+12/jdk-17.0.1+12'
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$Evidence = [IO.Path]::GetFullPath($Evidence)
if (!$Evidence.StartsWith($ProjectRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'ECJ evidence must remain inside the Electric checkout'
}
if (Test-Path -LiteralPath $Evidence) { throw 'Refusing to overwrite ECJ evidence' }
$null = New-Item -ItemType Directory -Path $Evidence

function Get-Pin([string]$Path) {
    $file = Get-Item -LiteralPath $Path
    [ordered]@{ path = $file.FullName; sha256 = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash }
}
function Save-Report {
    $json = $report | ConvertTo-Json -Depth 15
    [IO.File]::WriteAllText((Join-Path $Evidence 'ecj-results.json'), $json + "`n", [Text.UTF8Encoding]::new($false))
}
function Test-ClassFiles([string]$Directory) {
    $files = @(Get-ChildItem -LiteralPath $Directory -Recurse -File -Filter '*.class')
    if (!$files.Count) { throw "ECJ emitted no classes in $Directory" }
    foreach ($file in $files) {
        $bytes = [IO.File]::ReadAllBytes($file.FullName)
        if ($bytes.Length -lt 8 -or $bytes[0] -ne 0xCA -or $bytes[1] -ne 0xFE -or $bytes[2] -ne 0xBA -or $bytes[3] -ne 0xBE) {
            throw "Invalid class file: $($file.FullName)"
        }
        if (($bytes[6] * 256 + $bytes[7]) -ne 61) { throw "Non-Java-17 ECJ output: $($file.FullName)" }
    }
    return $files.Count
}
function Invoke-Ecj([string]$Name, [string[]]$Sources, [string[]]$Libraries) {
    $output = Join-Path $Evidence $Name
    $null = New-Item -ItemType Directory -Path $output
    $xmlLog = Join-Path $Evidence "$Name.xml"
    # Use the real Java 17 module image and keep forbidden/discouraged accesses fatal.
    $arguments = @('-17', '-proc:none', '-encoding', 'UTF-8', '-err:+forbidden,discouraged', '--system',
        ('"' + $Java17Home.Replace('\', '/') + '"'), '-classpath',
        ('"' + (($Libraries -replace '\\', '/') -join ';') + '"'), '-d',
        ('"' + $output.Replace('\', '/') + '"'), '-log', ('"' + $xmlLog.Replace('\', '/') + '"'))
    $arguments += @($Sources | ForEach-Object { '"' + $_.Replace('\', '/') + '"' })
    $argFile = Join-Path $Evidence "$Name.args"
    [IO.File]::WriteAllLines($argFile, $arguments, [Text.UTF8Encoding]::new($false))
    & $java17 -Xmx2G -jar $Compiler "@$argFile" *> (Join-Path $Evidence "$Name-compile.log")
    $exitCode = $LASTEXITCODE
    $entry = [ordered]@{ exitCode = $exitCode; sourceCount = $Sources.Count; output = $output; log = "$Name-compile.log" }
    $report.compilations[$Name] = $entry
    if ($exitCode -ne 0) { throw "$Name ECJ compilation failed; retain its log and XML diagnostics" }
    [xml]$diagnostics = Get-Content -LiteralPath $xmlLog -Raw
    $problems = @($diagnostics.SelectNodes('//*[@severity]'))
    $entry.errors = @($problems | Where-Object { $_.GetAttribute('severity') -ieq 'ERROR' }).Count
    $entry.warnings = @($problems | Where-Object { $_.GetAttribute('severity') -ieq 'WARNING' }).Count
    if ($entry.errors) { throw "$Name ECJ XML contains errors" }
    $entry.classCount = Test-ClassFiles $output
    return $output
}

$report = [ordered]@{ status = 'RUNNING'; project = $ProjectRoot; evidence = $Evidence
    scope = 'Pinned local baseElectric 1.20.1 candidate; fresh ECJ main/test/helper outputs and worldless Java 17 JUnit; no Gradle or Minecraft launch'
    compiler = $null; toolchains = $null; libraries = @(); sources = @(); gradleInventory = @()
    compilations = [ordered]@{}; junit = $null; negativeControl = $null; apiPinState = $null; capabilityBootstrap = $null }
try {
    $properties = ConvertFrom-StringData -StringData (Get-Content -LiteralPath (Join-Path $ProjectRoot 'gradle.properties') -Raw)
    $report.apiPinState = $properties.poweradvantage_pin_state
    if ($properties.mod_id -ne 'baseelectric' -or $properties.mod_version -ne '2.2.13.120011' -or
        $properties.minecraft_version -ne '1.20.1' -or $properties.forge_version -ne '47.4.10' -or
        $properties.poweradvantage_version -ne '2.4.12.120011' -or $properties.basemetals_version -ne '3.1.2.120011' -or
        $properties.orespawn_version -ne '4.1.0.120011' -or
        $properties.poweradvantage_pin_state -ne 'MERGED_SOURCE' -or
        $properties.poweradvantage_source_ref -cnotmatch '^[a-f0-9]{40}$' -or
        $properties.poweradvantage_runtime_sha256 -cnotmatch '^[A-F0-9]{64}$' -or
        $properties.basemetals_source_commit -cnotmatch '^[a-f0-9]{40}$' -or
        $properties.poweradvantage_deobf_sha256 -cnotmatch '^[A-F0-9]{64}$' -or
        $properties.poweradvantage_unit_bootstrap_sha256 -cnotmatch '^[A-F0-9]{64}$' -or
        $properties.poweradvantage_unit_bootstrap_jar_sha256 -cnotmatch '^[A-F0-9]{64}$' -or
        $properties.basemetals_deobf_sha256 -cnotmatch '^[A-F0-9]{64}$' -or
        $properties.orespawn_deobf_sha256 -cnotmatch '^[A-F0-9]{64}$') {
        throw 'This verifier requires the source-pinned baseElectric 1.20.1 inputs'
    }
    $report.compiler = Get-Pin $Compiler
    if ($report.compiler.sha256 -ne 'D0D43F8E2D7003E5EFED612E2CBB5F01870043397D8F1BBE536FD9128F4FCBF7') { throw 'ECJ compiler pin mismatch' }
    $java17 = Join-Path $Java17Home 'bin/java.exe'
    $capabilityBootstrap = Join-Path $ProjectRoot 'build/unit-bootstrap/forge-native-unit-bootstrap.jar'
    $report.capabilityBootstrap = Get-Pin $capabilityBootstrap
    if ($report.capabilityBootstrap.sha256 -cne $properties.poweradvantage_unit_bootstrap_jar_sha256) {
        throw 'The Eclipse-compiler run requires the same pinned basePower native unit agent as Gradle'
    }
    $report.toolchains = [ordered]@{ java = (Get-Pin $java17)
        java17Release = (Get-Pin (Join-Path $Java17Home 'release'))
        systemModules = (Get-Pin (Join-Path $Java17Home 'lib/modules')) }
    if ((Get-Content -LiteralPath (Join-Path $Java17Home 'release') -Raw) -notmatch 'JAVA_VERSION="17\.0\.1"') {
        throw 'ECJ production system image must be Java 17.0.1'
    }
    $negativeSource = Join-Path $PSScriptRoot 'negative/RestrictedJdkReference.java'
    $negativeLog = Join-Path $Evidence 'restricted-jdk-negative.log'
    & $java17 -jar $Compiler -17 -proc:none '-err:+forbidden,discouraged' --system $Java17Home `
        -d (Join-Path $Evidence 'negative') $negativeSource *> $negativeLog
    $negativeExit = $LASTEXITCODE
    $report.negativeControl = [ordered]@{ source = (Get-Pin $negativeSource); exitCode = $negativeExit
        log = 'restricted-jdk-negative.log'; expected = 'sun.security.x509.X500Name is not accessible' }
    if ($negativeExit -eq 0 -or (Get-Content -LiteralPath $negativeLog -Raw) -notmatch 'X500Name is not accessible') {
        throw 'Restricted-JDK negative control did not fail for the expected native module-access reason'
    }
    [xml]$classpath = Get-Content -LiteralPath (Join-Path $ProjectRoot '.classpath') -Raw
    $report.classpath = Get-Pin (Join-Path $ProjectRoot '.classpath')
    $mainLibraries = @()
    $testLibraries = @()
    foreach ($entry in @($classpath.classpath.classpathentry | Where-Object { $_.kind -eq 'lib' })) {
        $file = if ([IO.Path]::IsPathRooted([string]$entry.path)) { [IO.Path]::GetFullPath([string]$entry.path) }
            else { [IO.Path]::GetFullPath((Join-Path $ProjectRoot ([string]$entry.path))) }
        if ([IO.Path]::GetFileName($file) -like 'baseElectric*.jar' -or
            [IO.Path]::GetFileName($file) -like 'ElectricAdvantage*.jar') {
            throw 'Electric output jar cannot satisfy its own ECJ compilation'
        }
        $scopeNode = $entry.SelectSingleNode('attributes/attribute[@name="gradle_used_by_scope"]/@value')
        $scope = if ($scopeNode) { $scopeNode.Value } else { '' }
        $pin = Get-Pin $file
        $pin.scope = $scope
        $report.libraries += $pin
        if (!$scope -or $scope.Split(',') -contains 'main') { $mainLibraries += $file }
        if (!$scope -or $scope.Split(',') -contains 'test') { $testLibraries += $file }
    }
    foreach ($dependency in @(@{ prefix = 'basePower-'; hash = $properties.poweradvantage_deobf_sha256 },
        @{ prefix = 'BaseMetals-'; hash = $properties.basemetals_deobf_sha256 }, @{ prefix = 'OreSpawn-'; hash = $properties.orespawn_deobf_sha256 })) {
        $matching = @($report.libraries | Where-Object { [IO.Path]::GetFileName($_.path).StartsWith($dependency.prefix) })
        if ($matching.Count -ne 1 -or $matching[0].sha256 -ne $dependency.hash) { throw "Typed dependency pin mismatch: $($dependency.prefix)" }
    }
    $sources = @{}
    foreach ($set in @('main', 'test')) {
        $sources[$set] = @(Get-ChildItem -LiteralPath (Join-Path $ProjectRoot "src/$set/java") -Recurse -File -Filter '*.java' | Sort-Object FullName | ForEach-Object { $_.FullName })
        $report.sources += @($sources[$set] | ForEach-Object { Get-Pin $_ })
    }
    $classes = @()
    $expected = 0
    foreach ($xml in @(Get-ChildItem -LiteralPath (Join-Path $ProjectRoot 'build/test-results/test') -File -Filter 'TEST-*.xml' | Sort-Object Name)) {
        [xml]$inventory = Get-Content -LiteralPath $xml.FullName -Raw
        if ([int]$inventory.testsuite.failures -or [int]$inventory.testsuite.errors -or [int]$inventory.testsuite.skipped) { throw 'Gradle suite inventory contains failed/skipped tests' }
        $classes += [string]$inventory.testsuite.name
        $expected += [int]$inventory.testsuite.tests
        $report.gradleInventory += Get-Pin $xml.FullName
    }
    if ($classes.Count -ne 27 -or $expected -ne 99 -or $sources.test.Count -ne 28 -or $sources.main.Count -ne 86) { throw 'Unexpected Electric 1.20.1 source/test inventory' }
    $main = Invoke-Ecj 'main' $sources.main $mainLibraries
    $test = Invoke-Ecj 'test' $sources.test (@($main) + $testLibraries)
    $helperSource = Join-Path $PSScriptRoot 'AuditedJUnit.java'
    $report.helper = Get-Pin $helperSource
    $helper = Invoke-Ecj 'helper' @($helperSource) $testLibraries
    $junitXml = Join-Path $Evidence 'junit.xml'
    $runtime = @($main, $test, $helper, (Join-Path $ProjectRoot 'build/resources/main'), (Join-Path $ProjectRoot 'build/resources/test')) + $testLibraries
    $report.runtime = $runtime
    Push-Location $ProjectRoot
    try {
        & $java17 "-javaagent:$capabilityBootstrap" -Xmx2G -cp ($runtime -join ';') zone.moddev.mc.baseelectric.verification.AuditedJUnit $junitXml $expected @classes *> (Join-Path $Evidence 'junit.log')
        $junitExit = $LASTEXITCODE
    } finally { Pop-Location }
    $report.junit = [ordered]@{ exitCode = $junitExit; expected = $expected; suites = $classes; log = 'junit.log'; xml = 'junit.xml' }
    if ($junitExit -ne 0) { throw 'Fresh ECJ JUnit execution failed' }
    [xml]$junit = Get-Content -LiteralPath $junitXml -Raw
    if ($junit.testsuite.passed -ne 'true' -or [int]$junit.testsuite.tests -ne $expected -or
        [int]$junit.testsuite.failures -or [int]$junit.testsuite.ignored -or [int]$junit.testsuite.assumptions) { throw 'Strict ECJ test counts failed' }
    foreach ($pin in @($report.sources) + @($report.libraries) + @($report.helper) + @($report.negativeControl.source) + @($report.capabilityBootstrap)) {
        if ((Get-FileHash -LiteralPath $pin.path -Algorithm SHA256).Hash -ne $pin.sha256) { throw "Input changed during ECJ qualification: $($pin.path)" }
    }
    $report.status = 'PASS'
} catch {
    $report.status = 'FAIL'
    $report.error = $_.ToString()
    throw
} finally { Save-Report }
Get-Content -LiteralPath (Join-Path $Evidence 'junit.log') -Tail 8
Write-Output "ECJ PASS: $($sources.main.Count) main, $($sources.test.Count) test sources; $expected tests; Java 17 class files"
