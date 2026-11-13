param(
    [string]$ProjectRoot = (Join-Path $PSScriptRoot '../..'),
    [Parameter(Mandatory = $true)][string]$Evidence,
    [string]$Compiler = 'C:/Users/johnb/.p2/pool/plugins/org.eclipse.jdt.core.compiler.batch_3.46.0.v20260528-0407.jar',
    [string]$Java25Home = 'C:/Users/johnb/.jdks/jdk-25.0.3+9'
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
$inheritedJvmOptions = @{}
foreach ($name in @('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS')) {
    $inheritedJvmOptions[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
    [Environment]::SetEnvironmentVariable($name, $null, 'Process')
}

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
        if (($bytes[6] * 256 + $bytes[7]) -ne 69) { throw "Non-Java-25 ECJ output: $($file.FullName)" }
    }
    return $files.Count
}
function Invoke-Ecj([string]$Name, [string[]]$Sources, [string[]]$Libraries) {
    $output = Join-Path $Evidence $Name
    $null = New-Item -ItemType Directory -Path $output
    $xmlLog = Join-Path $Evidence "$Name.xml"
    # Use the real Java 25 module image and keep forbidden/discouraged accesses fatal.
    $arguments = @('-25', '-proc:none', '-encoding', 'UTF-8', '-err:+forbidden,discouraged', '--system',
        ('"' + $Java25Home.Replace('\', '/') + '"'), '-classpath',
        ('"' + (($Libraries -replace '\\', '/') -join ';') + '"'), '-d',
        ('"' + $output.Replace('\', '/') + '"'), '-log', ('"' + $xmlLog.Replace('\', '/') + '"'))
    $arguments += @($Sources | ForEach-Object { '"' + $_.Replace('\', '/') + '"' })
    $argFile = Join-Path $Evidence "$Name.args"
    [IO.File]::WriteAllLines($argFile, $arguments, [Text.UTF8Encoding]::new($false))
    & $java25 -XX:ActiveProcessorCount=4 -Xmx2G -jar $Compiler "@$argFile" *> (Join-Path $Evidence "$Name-compile.log")
    $exitCode = $LASTEXITCODE
    $entry = [ordered]@{ exitCode = $exitCode; sourceCount = $Sources.Count; output = $output; log = "$Name-compile.log" }
    $entry.jvmArguments = @('-XX:ActiveProcessorCount=4', '-Xmx2G', '-jar', $Compiler, "@$argFile")
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
    scope = 'Pinned local baseElectric 26.3 candidate; fresh ECJ main/test/helper outputs and worldless Java 25 JUnit; no Gradle or Minecraft launch'
    compiler = $null; toolchains = $null; libraries = @(); sources = @(); gradleInventory = @()
    compilations = [ordered]@{}; junit = $null; negativeControl = $null; apiPinState = $null }
try {
    $properties = ConvertFrom-StringData -StringData (Get-Content -LiteralPath (Join-Path $ProjectRoot 'gradle.properties') -Raw)
    $report.apiPinState = $properties.poweradvantage_pin_state
    if ($properties.mod_id -ne 'baseelectric' -or $properties.mod_version -ne '2.2.19.2603001' -or
        $properties.minecraft_version -ne '26.3' -or $properties.forge_version -ne '66.0.0' -or
        $properties.poweradvantage_version -ne '2.4.18.2603001' -or $properties.basemetals_version -ne '3.1.2.2603001' -or
        $properties.orespawn_version -ne '4.1.1.2603001' -or
        $properties.poweradvantage_pin_state -notin @('LOCAL_SOURCE', 'PUBLISHED') -or
        $properties.poweradvantage_source_ref -cnotmatch '^[a-f0-9]{40}$' -or
        $properties.poweradvantage_runtime_sha256 -cnotmatch '^[A-F0-9]{64}$' -or
        $properties.basemetals_source_commit -cnotmatch '^[a-f0-9]{40}$' -or
        $properties.poweradvantage_deobf_sha256 -cnotmatch '^[A-F0-9]{64}$' -or
        $properties.basemetals_deobf_sha256 -cnotmatch '^[A-F0-9]{64}$' -or
        $properties.orespawn_deobf_sha256 -cnotmatch '^[A-F0-9]{64}$') {
        throw 'This verifier requires the source-pinned baseElectric 26.3 inputs'
    }
    $report.compiler = Get-Pin $Compiler
    if ($report.compiler.sha256 -ne 'D0D43F8E2D7003E5EFED612E2CBB5F01870043397D8F1BBE536FD9128F4FCBF7') { throw 'ECJ compiler pin mismatch' }
    $java25 = Join-Path $Java25Home 'bin/java.exe'
    $report.toolchains = [ordered]@{ java = (Get-Pin $java25)
        java25Release = (Get-Pin (Join-Path $Java25Home 'release'))
        systemModules = (Get-Pin (Join-Path $Java25Home 'lib/modules')) }
    if ($report.toolchains.java.sha256 -cne 'D5F1B9E03E298EBFB04D2326ECC1E2AF3E6817F9B4D7F9F151FE856D56975650') {
        throw 'ECJ must use the pinned Temurin 25.0.3+9 executable'
    }
    if ((Get-Content -LiteralPath (Join-Path $Java25Home 'release') -Raw) -notmatch 'JAVA_VERSION="25\.0\.3"') {
        throw 'ECJ production system image must be Java 25.0.3'
    }
    $negativeSource = Join-Path $PSScriptRoot 'negative/RestrictedJdkReference.java'
    $negativeLog = Join-Path $Evidence 'restricted-jdk-negative.log'
    & $java25 -XX:ActiveProcessorCount=4 -jar $Compiler -25 -proc:none '-err:+forbidden,discouraged' --system $Java25Home `
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
    $methodInventory = @()
    $expected = 0
    foreach ($xml in @(Get-ChildItem -LiteralPath (Join-Path $ProjectRoot 'build/test-results/test') -File -Filter 'TEST-*.xml' | Sort-Object Name)) {
        [xml]$inventory = Get-Content -LiteralPath $xml.FullName -Raw
        if ([int]$inventory.testsuite.failures -or [int]$inventory.testsuite.errors -or [int]$inventory.testsuite.skipped) { throw 'Gradle suite inventory contains failed/skipped tests' }
        $classes += [string]$inventory.testsuite.name
        $expected += [int]$inventory.testsuite.tests
        foreach ($case in @($inventory.testsuite.testcase)) {
            if ([string]$case.classname -cne [string]$inventory.testsuite.name) { throw 'Unexpected JUnit testcase owner' }
            $methodInventory += ([string]$case.classname + '#' + [string]$case.name)
        }
        $report.gradleInventory += Get-Pin $xml.FullName
    }
    if ($classes.Count -ne 35 -or $expected -ne 119 -or $sources.test.Count -ne 38 -or $sources.main.Count -ne 87) { throw 'Unexpected Electric 26.3 source/test inventory' }
    $support = @(Get-ChildItem -LiteralPath (Join-Path $ProjectRoot 'build/generated-unit-support') -Recurse -File -Filter '*.java' | Sort-Object Name)
    if ($support.Count -ne 3) { throw 'Expected exactly the three pinned native unit support sources' }
    $supportPins = @{ 'ForgeUnitRegistries.java' = $properties.unit_registries_java_sha256; 'Forge64UnitLoader.java' = $properties.unit_loader_java_sha256;
        'NativeEmptyEntityTracker.java' = $properties.unit_entity_tracker_java_sha256 }
    foreach ($source in $support) {
        $pin = Get-Pin $source.FullName
        if (!$supportPins.ContainsKey($source.Name) -or $pin.sha256 -cne $supportPins[$source.Name]) { throw 'Pinned unit support source mismatch' }
        $report.sources += $pin
    }
    $bootstrap = Join-Path $ProjectRoot 'build/unit-bootstrap/forge-native-unit-bootstrap.jar'
    $report.unitBootstrap = Get-Pin $bootstrap
    if ($report.unitBootstrap.sha256 -cne $properties.unit_bootstrap_sha256) { throw 'Frozen unit bootstrap jar mismatch' }
    $main = Invoke-Ecj 'main' $sources.main $mainLibraries
    $test = Invoke-Ecj 'test' (@($sources.test) + @($support.FullName)) (@($main) + $testLibraries)
    $gameTests = @(Get-ChildItem -LiteralPath (Join-Path $ProjectRoot 'src/gametest/java') -Recurse -File -Filter '*.java' | ForEach-Object { $_.FullName })
    if ($gameTests.Count -ne 1) { throw 'Unexpected GameTest source inventory' }
    $report.sources += @($gameTests | ForEach-Object { Get-Pin $_ })
    $null = Invoke-Ecj 'gametest' $gameTests (@($main) + $testLibraries)
    $clientTests = @($sources.test | Where-Object { $_ -match '\\graphics\\|\\gui\\|\\ShaderResourceContractTest\.java$' })
    if ($clientTests.Count -ne 3) { throw 'Unexpected client-only contract inventory' }
    $null = Invoke-Ecj 'client' $clientTests (@($main, $test) + $testLibraries)
    $helperSource = Join-Path $PSScriptRoot 'AuditedJUnit.java'
    $report.helper = Get-Pin $helperSource
    $inventorySource = Join-Path $PSScriptRoot 'EclipseTestInventory.java'
    $report.inventoryHelper = Get-Pin $inventorySource
    $helper = Invoke-Ecj 'helper' @($helperSource, $inventorySource) $testLibraries
    $inventoryPath = Join-Path $Evidence 'gradle-method-inventory.txt'
    [IO.File]::WriteAllLines($inventoryPath, $methodInventory, [Text.UTF8Encoding]::new($false))
    $inventoryRuntime = @($helper) + $testLibraries
    & $java25 -XX:ActiveProcessorCount=4 -cp ($inventoryRuntime -join ';') zone.moddev.mc.baseelectric.verification.EclipseTestInventory $test $inventoryPath $expected *> (Join-Path $Evidence 'inventory.log')
    if ($LASTEXITCODE -ne 0) { throw 'Fresh compiled JUnit method inventory does not match Gradle XML' }
    $report.inventory = [ordered]@{ status = 'PASS'; methods = $methodInventory.Count; suites = $classes.Count; negatives = @() }
    foreach ($prefix in @('zone.moddev.mc.baseelectric.entities.', 'zone.moddev.mc.baseelectric.items.')) {
        $filtered = @($methodInventory | Where-Object { !$_.StartsWith($prefix, [StringComparison]::Ordinal) })
        if (!$filtered.Count -or $filtered.Count -eq $methodInventory.Count) { throw 'Inventory negative control excluded no tests' }
        $negativePath = Join-Path $Evidence ('inventory-exclusion-' + $report.inventory.negatives.Count + '.txt')
        [IO.File]::WriteAllLines($negativePath, $filtered, [Text.UTF8Encoding]::new($false))
        $log = $negativePath + '.log'
        & $java25 -XX:ActiveProcessorCount=4 -cp ($inventoryRuntime -join ';') zone.moddev.mc.baseelectric.verification.EclipseTestInventory $test $negativePath $filtered.Count *> $log
        if ($LASTEXITCODE -eq 0 -or (Get-Content -LiteralPath $log -Raw) -notmatch 'Incomplete or stale Gradle method inventory') { throw 'JUnit prefix-exclusion negative control was not rejected' }
        $report.inventory.negatives += [ordered]@{ prefix = $prefix; tests = $filtered.Count; expectedRejection = $true; log = $log }
    }
    $junitXml = Join-Path $Evidence 'junit.xml'
    $runtime = @($main, $test, $helper, (Join-Path $ProjectRoot 'build/resources/main'), (Join-Path $ProjectRoot 'build/resources/test')) + $testLibraries
    $report.runtime = $runtime
    Push-Location $ProjectRoot
    try {
        & $java25 -XX:ActiveProcessorCount=4 -Xmx2G "-javaagent:$bootstrap" -cp ($runtime -join ';') zone.moddev.mc.baseelectric.verification.AuditedJUnit $junitXml $expected @classes *> (Join-Path $Evidence 'junit.log')
        $junitExit = $LASTEXITCODE
    } finally { Pop-Location }
    $report.junit = [ordered]@{ exitCode = $junitExit; expected = $expected; suites = $classes; log = 'junit.log'; xml = 'junit.xml' }
    $report.junit.jvmArguments = @('-XX:ActiveProcessorCount=4', '-Xmx2G', "-javaagent:$bootstrap", '-cp', ($runtime -join ';'))
    $report.junit.inheritedJvmEnvironment = 'CLEARED'
    if ($junitExit -ne 0) { throw 'Fresh ECJ JUnit execution failed' }
    [xml]$junit = Get-Content -LiteralPath $junitXml -Raw
    if ($junit.testsuite.passed -ne 'true' -or [int]$junit.testsuite.tests -ne $expected -or
        [int]$junit.testsuite.failures -or [int]$junit.testsuite.ignored -or [int]$junit.testsuite.assumptions) { throw 'Strict ECJ test counts failed' }
    foreach ($pin in @($report.sources) + @($report.libraries) + @($report.helper, $report.inventoryHelper, $report.unitBootstrap) + @($report.negativeControl.source)) {
        if ((Get-FileHash -LiteralPath $pin.path -Algorithm SHA256).Hash -ne $pin.sha256) { throw "Input changed during ECJ qualification: $($pin.path)" }
    }
    $report.status = 'PASS'
} catch {
    $report.status = 'FAIL'
    $report.error = $_.ToString()
    throw
} finally {
    Save-Report
    foreach ($name in $inheritedJvmOptions.Keys) {
        [Environment]::SetEnvironmentVariable($name, $inheritedJvmOptions[$name], 'Process')
    }
}
Get-Content -LiteralPath (Join-Path $Evidence 'junit.log') -Tail 8
Write-Output "ECJ PASS: $($sources.main.Count) main, $($sources.test.Count) test sources; $expected tests; Java 25 class files"
