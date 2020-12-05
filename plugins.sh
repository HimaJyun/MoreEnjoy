#!/bin/bash -eu

BASE_DIR="$(dirname "$0")"

rm -rfv "${BASE_DIR}/target/plugins"
mkdir -p "${BASE_DIR}/target/plugins"

find "${BASE_DIR}" -maxdepth 3 \
  \( -name "*.jar" -a -not -name "original-*" -a -not -name "MoreEnjoy-MoreUtils-*" \) \
  -exec cp -t "${BASE_DIR}/target/plugins" {} +

sha256sum "${BASE_DIR}/target/plugins/"* | sed -E 's@^([a-f0-9]{64}) .*\/(.*.jar)@\2 \1@g'
