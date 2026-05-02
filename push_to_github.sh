#!/bin/bash
set -e

REPO_URL="https://${GITHUB_PERSONAL_ACCESS_TOKEN}@github.com/TITANICBHAI/SmartAssistant.git"

echo "Configuring remote with authentication..."
git remote set-url origin "$REPO_URL"

echo "Configuring git identity..."
git config user.email "replit-bot@replit.com"
git config user.name "Replit Bot"

echo "Staging all changes..."
git add -A

echo "Checking for changes to commit..."
if git diff --cached --quiet; then
  echo "Nothing to commit — working tree is clean."
else
  TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
  git commit -m "chore: auto-push from Replit [$TIMESTAMP]"
  echo "Committed changes."
fi

echo "Pushing to GitHub (TITANICBHAI/SmartAssistant)..."
git push origin HEAD

echo "Done! Successfully pushed to GitHub."
