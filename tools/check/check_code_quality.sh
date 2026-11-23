#!/usr/bin/env bash

# Copyright (c) 2025 Element Creations Ltd.
# Copyright 2023-2024 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
# Please see LICENSE files in the repository root for full details.

set -eu

checker=$(mktemp)
trap 'rm -f "$checker"' EXIT
curl --fail --silent --show-error --location --retry 2 \
    https://raw.githubusercontent.com/matrix-org/matrix-dev-tools/3a80c2057a5f2672cacc6545e6e26fe43366b20b/bin/search_forbidden_strings.pl \
    --output "$checker"

check_directories() {
    local name=$1 rules=$2
    local -a dirs
    mapfile -d '' -t dirs < <(find . -type d \( -name build -o -name .gradle -o -name .git \) -prune -o -type d -name "$name" -print0)
    if (( ${#dirs[@]} == 0 )); then
        echo "No $name directories found" >&2
        return 1
    fi
    perl "$checker" "$rules" "${dirs[@]}"
}

status=0
check_directories kotlin tools/check/forbidden_strings_in_code.txt || status=1
check_directories res tools/check/forbidden_strings_in_xml.txt || status=1
exit "$status"
