#!/bin/bash

# BlissGems Push Script
# Stages all changes, commits, and pushes to GitHub

# Add all changes
git add -A

# Check if there are changes to commit
if git diff --staged --quiet; then
    echo "No changes to commit"
    exit 0
fi

# Commit with message
echo "Enter commit message:"
read -r commit_message

if [ -z "$commit_message" ]; then
    echo "Commit message cannot be empty"
    exit 1
fi

git commit -m "$commit_message"

# Push to GitHub using the deploy key
unset SSH_AUTH_SOCK
GIT_SSH_COMMAND="ssh -i /tmp/claude/blissgems_deploy_key -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -F /dev/null -o IdentitiesOnly=yes" git push origin main

echo "Successfully pushed to GitHub!"
