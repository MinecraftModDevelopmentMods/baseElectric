#!/usr/bin/env bash
set -euo pipefail

value() { sed -n "s/^$1=//p" gradle.properties | tr -d '\r'; }

if [[ "$(value poweradvantage_pin_state)" != 'MERGED_SOURCE' ]]; then
    echo '::error::This local port has no merged and public basePower release pin.' >&2
    exit 1
fi
for dependency in poweradvantage basemetals orespawn; do
    id="$(value "${dependency}_public_file_id")"
    if [[ ! "$id" =~ ^[1-9][0-9]*$ ]]; then
        echo "::error::A verified public $dependency file ID is required before hosted builds." >&2
        exit 1
    fi
done

require_merged() {
    local repo="$1" ref="$2" status
    [[ "$ref" =~ ^[a-f0-9]{40}$ ]]
    status="$(gh api "repos/MinecraftModDevelopmentMods/$repo/compare/$ref...master-26.2" --jq '.status')"
    case "$status" in
        identical|ahead) ;;
        *) echo "::error::$repo $ref is not on upstream master-26.2 ($status)."; exit 1 ;;
    esac
}

download() {
    local url="$1" expected="$2" path="$3"
    [[ "$expected" =~ ^[A-F0-9]{64}$ ]]
    curl --fail --silent --show-error --location --proto '=https' --proto-redir '=https' \
        --retry 3 --output "$path" "$url"
    printf '%s  %s\n' "$expected" "$path" | sha256sum -c -
}

public_jar() {
    local name="$1" project="$2" id="$3" sha="$4" jar="$5"
    if [[ ! "$id" =~ ^[1-9][0-9]*$ ]]; then
        echo "::error::A verified public $name file ID is required."
        exit 1
    fi
    download "https://www.curseforge.com/api/v1/mods/$project/files/$id/download" "$sha" "$jar"
}

power_ref="$(value poweradvantage_source_ref)"
unit_support_ref="$(value unit_support_source_ref)"
orespawn_ref="$(value orespawn_source_commit)"
require_merged basePower "$power_ref"
require_merged basePower "$unit_support_ref"
require_merged BaseMetals "$(value basemetals_source_commit)"
require_merged OreSpawn "$orespawn_ref"

inputs="${RUNNER_TEMP:?}/forge65-release-dependencies"
mkdir -p "$inputs"
power="$inputs/basePower-$(value poweradvantage_version).jar"
metals="$inputs/BaseMetals-$(value basemetals_version).jar"
orespawn="$inputs/OreSpawn-$(value orespawn_version).jar"
public_jar basePower 1780140 "$(value poweradvantage_public_file_id)" "$(value poweradvantage_deobf_sha256)" "$power"
public_jar BaseMetals 240967 "$(value basemetals_public_file_id)" "$(value basemetals_deobf_sha256)" "$metals"
public_jar OreSpawn 245586 "$(value orespawn_public_file_id)" "$(value orespawn_deobf_sha256)" "$orespawn"

installer="$inputs/forge-26.2-65.1.0-installer.jar"
download 'https://maven.minecraftforge.net/net/minecraftforge/forge/26.2-65.1.0/forge-26.2-65.1.0-installer.jar' \
    "$(value forge_installer_sha256)" "$installer"
{
    printf 'power_ref=%s\n' "$power_ref"
    printf 'unit_support_ref=%s\n' "$unit_support_ref"
    printf 'orespawn_ref=%s\n' "$orespawn_ref"
} >> "${GITHUB_OUTPUT:?}"
{
    printf 'POWER_PLAYER=%s\n' "$power"
    printf 'METALS_PLAYER=%s\n' "$metals"
    printf 'ORESPAWN_PLAYER=%s\n' "$orespawn"
    printf 'FORGE_INSTALLER=%s\n' "$installer"
} >> "${GITHUB_ENV:?}"
