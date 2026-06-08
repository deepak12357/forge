#!/bin/sh
echo "[Git Hook] Executing spotless:apply to format code before committing..."

# 1. Stash any unstaged changes so we only format what you intentionally staged
git stash -q --keep-index

# 2. Run spotless:apply using Maven or the Maven Wrapper
if command -v ./mvnw > /dev/null 2>&1; then
    ./mvnw spotless:apply
else
    mvn spotless:apply
fi

# Capture the outcome of the formatting
RESULT=$?

# 3. Re-stage any files that Spotless automatically corrected
git add -u

# 4. Pop the unstaged changes back to your working directory
git stash pop -q

# If Spotless failed structurally, block the commit; otherwise, succeed
exit $RESULT