#!/bin/bash

set -e

taggedVersion=$1
tagName=${2:-experimental}

usage() {
  echo "This script will create a named tag for a specific tagged version"
  echo "$0 tag-version [tag-name]"
  echo "  Example: $0 v1.0.0 stable"
}

if [ "${taggedVersion}" == '' ]; then
  echo "Missing required tagged version."
  echo
  usage
  exit 1
fi

read -r -p "Are you sure you want to update the [$tagName] tag to [${taggedVersion}] ? [y/N] " response
response=$(echo "$response" | tr '[:upper:]' '[:lower:]')    # tolower

if [[ "$response" =~ ^(yes|y)$ ]]; then
  echo "Getting latest changes from origin"
  git fetch origin

  echo "Checking to see if tagged version [${taggedVersion}] exists on remote..."
  if git tag | grep -w "${taggedVersion}" ; then
    echo "Tagged version exists. Continuing..."
  else
    echo "Tagged version does not exist. Exiting..."
    exit 1
  fi

  echo "Deleting local and remote tags..."
  if git tag | grep "${tagName}" ; then
    git tag -d "${tagName}"
    git push --delete origin "${tagName}"

    echo "Successfully deleted previous tag. Now tagging new version."
  fi

  commit=$(git rev-list -n 1 "${taggedVersion}")
  git tag -m "update ${tagName} to ${taggedVersion}" -s "${tagName}" "$commit"
  git push origin "${tagName}"
else
  echo "cancelled update"
fi
