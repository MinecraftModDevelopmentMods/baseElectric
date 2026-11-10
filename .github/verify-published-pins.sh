#!/usr/bin/env bash
set -euo pipefail

value() { sed -n "s/^$1=//p" gradle.properties | tr -d '\r'; }

require_merged() {
    local repo="$1" ref="$2" status
    [[ "$ref" =~ ^[a-f0-9]{40}$ ]]
    status="$(gh api "repos/MinecraftModDevelopmentMods/$repo/compare/$ref...master-1.20.1" --jq '.status')"
    case "$status" in
        identical|ahead) ;;
        *) echo "::error::$repo $ref is not on upstream master-1.20.1 ($status)."; exit 1 ;;
    esac
}

file_id="$(value poweradvantage_public_file_id)"
expected="$(value poweradvantage_runtime_sha256)"
if [[ ! "$file_id" =~ ^[1-9][0-9]*$ || ! "$expected" =~ ^[A-F0-9]{64}$ ]]; then
    echo '::error::A verified public basePower 1.20.1 file ID and player checksum are required.'
    exit 1
fi
require_merged basePower "$(value poweradvantage_source_ref)"
require_merged BaseMetals "$(value basemetals_source_commit)"

player="${RUNNER_TEMP:?}/basePower-public-player.jar"
curl --fail --silent --show-error --location --proto '=https' --proto-redir '=https' \
    --output "$player" "https://www.curseforge.com/api/v1/mods/1780140/files/$file_id/download"
read -r actual _ < <(sha256sum < "$player")
if [[ "${actual^^}" != "$expected" ]]; then
    echo "::error::Public basePower file $file_id SHA-256 is ${actual^^}, expected $expected."
    exit 1
fi
printf 'power_player=%s\n' "$player" >> "${GITHUB_OUTPUT:?}"
