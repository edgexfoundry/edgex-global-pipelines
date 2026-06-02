#!/bin/bash

DRY_RUN=${DRY_RUN:-false}
GH_PAGES_BRANCH=${GH_PAGES_BRANCH}
COMMIT_MSG=${COMMIT_MSG}

ls -al .
cp -rlf docs/* .
rm -rf docs
ls -al .

git diff-index --quiet HEAD --
trackedChanged=$?

# git diff-index HEAD only sees tracked files; catch a brand-new untracked
# version directory (e.g. 4.0.2/) that a version-bump publish adds on its own
untrackedVersionDirs=$(git ls-files --others --exclude-standard --directory | grep -E '^[0-9]+\.[0-9]+(\.[0-9]+)?/' || true)

if [ "$trackedChanged" -ne 0 ] || [ -n "$untrackedVersionDirs" ]; then
  echo "[edgeXGHPagesPublish] Detected changes to commit (trackedChanged=$trackedChanged, untrackedVersionDirs=$untrackedVersionDirs)"
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