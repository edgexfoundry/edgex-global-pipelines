#!/bin/bash

command=${1}
exit_code=${2:-0}
sleep_for=${3:-5}

usage() {
    echo "${0} command [exit code]"
}

if [ -z "${command}" ]; then
    echo "Missing command."
    usage
    exit 1
fi

while true; do
    echo "${command}"
    eval "${command}"
    if [ $? -eq $exit_code ]; then
        break
    fi
    sleep ${sleep_for}
done

echo "[wait-for] complete..."
exit 0
