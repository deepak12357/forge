#!/bin/bash
# Setup git hooks for automatic code formatting

set -e

REPO_ROOT="$(git rev-parse --show-toplevel)"
BACKEND_DIR="$REPO_ROOT/backend"
HOOKS_DIR="$REPO_ROOT/.git/hooks"

echo "Setting up git hooks in $HOOKS_DIR..."

# Create pre-commit hook
cat > "$HOOKS_DIR/pre-commit" << 'EOF'
#!/bin/bash
# Pre-commit hook: Auto-format code with Spotless

set -e

# Navigate to backend directory
cd "$(git rev-parse --show-toplevel)/backend" || exit 1

echo "[Git Hook] Running Spotless to format code..."

# Run spotless:apply
if [ -f "./mvnw" ]; then
    ./mvnw -q spotless:apply 2>/dev/null || {
        echo "[Git Hook] Warning: spotless:apply failed, but proceeding with commit"
    }
elif command -v mvn &> /dev/null; then
    mvn -q spotless:apply 2>/dev/null || {
        echo "[Git Hook] Warning: spotless:apply failed, but proceeding with commit"
    }
else
    echo "[Git Hook] Maven not found, skipping format check"
fi

# Stage all modified files (formatting changes)
git add -u

echo "[Git Hook] Code formatting complete"
EOF

chmod +x "$HOOKS_DIR/pre-commit"
echo "✓ Pre-commit hook installed at $HOOKS_DIR/pre-commit"

echo ""
echo "Git hooks setup complete!"
echo "- Pre-commit hook will automatically run spotless:apply on staged changes"
echo "- Formatted files will be automatically staged (git add -u)"
echo ""

