#!/bin/bash

DRY_RUN=${DRY_RUN:-false}
GH_PAGES_BRANCH=${GH_PAGES_BRANCH}
COMMIT_MSG=${COMMIT_MSG}

ls -al .
cp -rlf docs/* .
rm -rf docs
ls -al .

changesDetected=$(git diff-index --quiet HEAD --)

if [ $? -ne 0 ]; then
  echo "[edgeXGHPagesPublish] We have detected there are changes to commit: $changesDetected"
  git config --global user.email "jenkins@edgexfoundry.org"
  git config --global user.name "EdgeX Jenkins"
  git add .
  git commit -s -m "ci: $COMMIT_MSG"
  echo "[edgeXGHPagesPublish] DRY_RUN set to : $DRY_RUN"
  if [ "$DRY_RUN" == "false" ]; then
    echo "[edgeXGHPagesPublish] DRY_RUN disabled. Pushing changes to $GH_PAGES_BRANCH"
    git push origin "$GH_PAGES_BRANCH"
  fi
fi