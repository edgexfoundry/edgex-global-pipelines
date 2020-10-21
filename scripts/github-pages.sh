#! /bin/bash

ls -al .
cp -rlf docs/* .
rm -rf docs
ls -al .

originalCommitMsg=$(git log --format=%B -n 1 | grep -v Signed-off-by | head -n 1)
changesDetected=$(git diff-index --quiet HEAD --)

if [ $? -ne 0 ]; then
    echo "We have detected there are changes to commit: $changesDetected"
    git config --global user.email "jenkins@edgexfoundry.org"
    git config --global user.name "EdgeX Jenkins"
    git add .
    git commit -s -m "ci: ${originalCommitMsg}"
fi