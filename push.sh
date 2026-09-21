#!/bin/bash

# BlissGems Push Script
# Stages all changes, commits, and pushes to GitHub
# Usage: ./push.sh ["commit message"]

set -e

cd "$(dirname "$0")"

# Add all changes
git add -A

# Check if there are changes to commit
if git diff --staged --quiet; then
    echo "No changes to commit"
    exit 0
fi

# Commit message from argument, or ask for one
commit_message="$1"
if [ -z "$commit_message" ]; then
    echo "Enter commit message:"
    read -r commit_message
fi

if [ -z "$commit_message" ]; then
    echo "Commit message cannot be empty"
    exit 1
fi

git commit -m "$commit_message"

git push origin main

echo "Successfully pushed to GitHub!"
