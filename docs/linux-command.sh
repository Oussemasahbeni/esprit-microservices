#!/bin/bash
set -euo pipefail

jq -c '.[]' consul-kv-utf8.json | while IFS= read -r item; do
    key=$(jq -r '.key' <<< "$item")
    b64val=$(jq -r '.value' <<< "$item")

    tmpfile=$(mktemp)
    echo "$b64val" | base64 -d > "$tmpfile"

    docker exec -i consul consul kv put "$key" - < "$tmpfile"

    rm -f "$tmpfile"
    echo -e "\033[32mImported: $key\033[0m"
done