#!/bin/bash

wait_for() {
    command=${1}
    exit_code=${2:-0}
    sleep_for=${3:-5}

    while true; do
        echo "${command}"
        eval "${command}"
        if [ $? -eq $exit_code ]; then
            break
        fi
        sleep ${sleep_for}
    done
}

if [ -z "$WAIT_FOR_IMAGES" ]; then
    images=( "$@" )
else
    images=($WAIT_FOR_IMAGES)
fi

trap ctrl_c INT

ctrl_c() {
    echo "Cleanup bg jobs"
    kill $(jobs -p)
    exit 1
}

for image in "${images[@]}"; do
    wait_for "docker pull $image" &
done

wait < <(jobs -p)