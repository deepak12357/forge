# Git Hooks Setup

This directory contains scripts to set up and manage git hooks for automatic code formatting.

## Automatic Pre-Commit Formatting

The project is configured to automatically run **Spotless** before each commit to ensure all code follows the project's formatting standards.

### How It Works

1. **Pre-Commit Hook** (`.git/hooks/pre-commit`)
   - Automatically runs before each commit
   - Executes `mvn spotless:apply` to format all staged files
   - Auto-stages the formatting changes
   - Allows the commit to proceed even if formatting fails

### Manual Hook Setup

If the hook was not automatically installed, you can set it up manually:

#### On Linux/Mac/Git Bash:
```bash
cd /path/to/forge
chmod +x .git/hooks/pre-commit
```

#### Or run the setup script:
```bash
bash backend/scripts/setup-git-hooks.sh
```

### What Gets Formatted

The Spotless configuration (in `backend/pom.xml`) formats:
- **Java files** using Google Java Format
- **Unused imports** are removed automatically

### Configuration Location

- **Spotless Config**: `backend/pom.xml` (search for `spotless-maven-plugin`)
- **Pre-Commit Hook**: `.git/hooks/pre-commit`
- **Setup Script**: `backend/scripts/setup-git-hooks.sh`

### Troubleshooting

**Hook not running?**
1. Verify hook file exists: `.git/hooks/pre-commit`
2. Verify permissions (on Unix): `ls -la .git/hooks/pre-commit`
3. Test manually: `cd backend && ./mvnw spotless:apply`

**Files not being formatted?**
- Check if Git Bash is being used for commits (recommended on Windows)
- Try running manually: `cd backend && ./mvnw spotless:apply`
- The hook logs output prefixed with `[Git Hook]`

**Want to skip formatting for a commit?**
```bash
git commit --no-verify
```

This bypasses all git hooks (use with caution!).

### CI/CD Integration

The project also has a `spotless:check` goal that runs during the Maven `verify` phase, which is typically run by CI/CD pipelines:

```bash
./mvnw verify  # checks formatting without applying changes
./mvnw spotless:apply  # applies formatting
```

---

**Summary**: After every change, before you commit, the hook will automatically format your code. No manual formatting required! ✨
