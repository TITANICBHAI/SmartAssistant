#!/bin/bash
set -e

REPO_URL="https://${GITHUB_PERSONAL_ACCESS_TOKEN}@github.com/TITANICBHAI/SmartAssistant.git"

echo "Setting up git environment (no .git/config writes)..."
export GIT_AUTHOR_NAME="Replit Bot"
export GIT_AUTHOR_EMAIL="replit-bot@replit.com"
export GIT_COMMITTER_NAME="Replit Bot"
export GIT_COMMITTER_EMAIL="replit-bot@replit.com"
export GIT_TERMINAL_PROMPT=0
export GIT_ASKPASS=""
export SSH_ASKPASS=""
unset SSH_AUTH_SOCK

echo "Staging all changes..."
git add -A

echo "Checking for changes to commit..."
if git diff --cached --quiet; then
  echo "Nothing to commit — working tree is clean."
else
  TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
  git \
    -c credential.helper='' \
    -c http.extraHeader='' \
    commit -m "chore: auto-push from Replit [$TIMESTAMP]"
  echo "Committed changes."
fi

echo "Pushing to GitHub (TITANICBHAI/SmartAssistant)..."
git \
  -c credential.helper='' \
  -c http.extraHeader='' \
  push "$REPO_URL" HEAD:main --no-progress

echo "Done! Successfully pushed to GitHub."
