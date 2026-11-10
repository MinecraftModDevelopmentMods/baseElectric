#!/usr/bin/env bash
set -euo pipefail

cd "${GITHUB_WORKSPACE:?}"

value() {
    sed -n "s/^$1=//p" gradle.properties | tr -d '\r'
}

fail() {
    echo "$*" >&2
    exit 1
}

check_hash() {
    local file="$1" expected="$2" actual
    [[ -f "$file" ]] || fail "Missing pinned input: $file"
    read -r actual _ < <(sha256sum < "$file")
    [[ "${actual^^}" == "$expected" ]] || fail "SHA-256 mismatch for $file: expected $expected, got ${actual^^}"
}

[[ "$(value minecraft_version)" == '1.19.4' && "$(value forge_version)" == '45.4.0' ]] ||
    fail 'This dependency build is only for Minecraft 1.19.4 / Forge 45.4.0'
[[ "$(value poweradvantage_pin_state)" == 'PUBLISHED_RELEASE' ]] || fail 'The released basePower is not pinned'

ore_ref="$(value orespawn_source_commit)"
bm_ref="$(value basemetals_source_commit)"
power_ref="$(value poweradvantage_source_ref)"
for ref in "$ore_ref" "$bm_ref" "$power_ref"; do
    [[ "$ref" =~ ^[a-f0-9]{40}$ ]] || fail "Invalid dependency source pin: $ref"
done
for key in orespawn_deobf_sha256 orespawn_runtime_sha256 basemetals_deobf_sha256 basemetals_runtime_sha256 \
        poweradvantage_deobf_sha256 poweradvantage_runtime_sha256 poweradvantage_unit_bootstrap_sha256 \
        poweradvantage_unit_bootstrap_jar_sha256 forge_installer_sha256; do
    [[ "$(value "$key")" =~ ^[A-F0-9]{64}$ ]] || fail "Invalid dependency checksum: $key"
done

case "${1:-}" in
    pins)
        printf 'orespawn_ref=%s\nbasemetals_ref=%s\npower_ref=%s\n' "$ore_ref" "$bm_ref" "$power_ref" >> "$GITHUB_OUTPUT"
        exit 0
        ;;
    build) ;;
    *) fail 'Expected pins or build' ;;
esac

orespawn="$GITHUB_WORKSPACE/.ci-dependencies/OreSpawn"
basemetals="$GITHUB_WORKSPACE/.ci-dependencies/BaseMetals"
power="$GITHUB_WORKSPACE/.ci-dependencies/PowerAdvantage"
test "$(git -C "$orespawn" rev-parse HEAD)" = "$ore_ref"
test "$(git -C "$basemetals" rev-parse HEAD)" = "$bm_ref"
test "$(git -C "$power" rev-parse HEAD)" = "$power_ref"

mavenizer="$orespawn/ci-fixtures/tools/minecraft-mavenizer-0.5.21-orespawn-compat.jar"
check_hash "$mavenizer" '341B3FB10ABA6EA7DC612FBE519DC290A1C81867AED5DD320ACD783688AC49DE'

read -r -a toolchains <<< "${MMD_GRADLE_TOOLCHAINS:?}"
ore_version="$(value orespawn_version)"
bm_version="$(value basemetals_version)"
power_version="$(value poweradvantage_version)"
osjar="$orespawn/build/libs-dev/OreSpawn-$ore_version-deobf.jar"
osplayer="$orespawn/build/libs/OreSpawn-$ore_version.jar"
bmjar="$basemetals/build/libs-dev/BaseMetals-$bm_version-deobf.jar"
bmplayer="$basemetals/build/libs/BaseMetals-$bm_version.jar"
powerjar="$power/build/libs-dev/basePower-$power_version-deobf.jar"
powerplayer="$power/build/libs/basePower-$power_version.jar"
bootstrap="$power/test-rig/unit-bootstrap/ForgeCapabilityBootstrap.java"
agent="$power/build/unit-bootstrap/forge-native-unit-bootstrap.jar"
installer="${RUNNER_TEMP:?}/forge-1.19.4-45.4.0-installer.jar"

(cd "$orespawn" && ./gradlew "${toolchains[@]}" assemble --no-daemon --stacktrace)
check_hash "$osjar" "$(value orespawn_deobf_sha256)"
check_hash "$osplayer" "$(value orespawn_runtime_sha256)"
# The pinned Base Metals build already configures its own verified fixture.
(cd "$basemetals" && ./gradlew "${toolchains[@]}" assemble --no-daemon --stacktrace)
git -C "$basemetals" diff --exit-code HEAD --
check_hash "$bmjar" "$(value basemetals_deobf_sha256)"
check_hash "$bmplayer" "$(value basemetals_runtime_sha256)"
check_hash "$bootstrap" "$(value poweradvantage_unit_bootstrap_sha256)"
(cd "$power" && ./gradlew "${toolchains[@]}" assemble unitBootstrapJar verifyReleaseDependencies --no-daemon --stacktrace \
    "-Pforge45MavenizerJar=$mavenizer" "-PbaseMetalsDeobfJar=$bmjar" "-PoreSpawnDeobfJar=$osjar")
git -C "$power" diff --exit-code HEAD --
check_hash "$powerjar" "$(value poweradvantage_deobf_sha256)"
check_hash "$powerplayer" "$(value poweradvantage_runtime_sha256)"
check_hash "$agent" "$(value poweradvantage_unit_bootstrap_jar_sha256)"
curl --fail --silent --show-error --location --proto '=https' \
    'https://maven.minecraftforge.net/net/minecraftforge/forge/1.19.4-45.4.0/forge-1.19.4-45.4.0-installer.jar' \
    --output "$installer"
check_hash "$installer" "$(value forge_installer_sha256)"

# Check public player bytes separately from the source-built development jars.
published="$RUNNER_TEMP/base-electric-published"
mkdir -p "$published"
fetch_public() {
    curl --fail --silent --show-error --location --proto '=https' --proto-redir '=https' --output "$1" "$2"
    check_hash "$1" "$3"
}
maven='https://maven.moddev.zone/releases/zone/moddev/mc'
powerplayer="$published/basePower-$power_version.jar"
bmplayer="$published/BaseMetals-$bm_version.jar"
osplayer="$published/OreSpawn-$ore_version.jar"
fetch_public "$powerplayer" "$maven/basepower/basePower/$power_version/basePower-$power_version.jar?ci=$GITHUB_RUN_ID" \
    "$(value poweradvantage_runtime_sha256)"
fetch_public "$bmplayer" "$maven/basemetals/BaseMetals/$bm_version/BaseMetals-$bm_version.jar?ci=$GITHUB_RUN_ID" \
    "$(value basemetals_runtime_sha256)"
fetch_public "$osplayer" 'https://www.curseforge.com/api/v1/mods/245586/files/9171353/download' \
    "$(value orespawn_runtime_sha256)"

printf 'mavenizer=%s\norespawn=%s\norespawn_player=%s\nbasemetals=%s\nbasemetals_player=%s\npower=%s\npower_player=%s\nbootstrap=%s\nagent=%s\ninstaller=%s\n' \
    "$mavenizer" "$osjar" "$osplayer" "$bmjar" "$bmplayer" "$powerjar" "$powerplayer" "$bootstrap" "$agent" "$installer" >> "$GITHUB_OUTPUT"
