# Development Environment Scripts

This folder contains scripts to help set up and verify the development environment for the Cat SMP Minecraft Plugin.

## Scripts

### `dev-env-install.sh`
Cross-platform installation script that automatically:
- Detects your operating system (macOS, Linux, Windows)
- Installs appropriate package managers
- Installs JDK 21 (Long Term Support version)
- Installs Apache Maven
- Configures environment variables (JAVA_HOME, PATH)

**Usage:**
```bash
chmod +x scripts/dev-env-install.sh
./scripts/dev-env-install.sh
```

### `dev-env-verify.sh`
Comprehensive verification script that checks:
- Java installation and version
- Maven installation and configuration
- Environment variables (JAVA_HOME, PATH)
- Project structure
- Development tools availability

**Usage:**
```bash
chmod +x scripts/dev-env-verify.sh
./scripts/dev-env-verify.sh
```

## Requirements

- **VS Code** with integrated terminal
- **Git Bash** (Windows) or native terminal (macOS/Linux)
- **Administrator/sudo privileges** for package installation

## Supported Platforms

- ✅ **macOS** (Intel & Apple Silicon)
- ✅ **Linux** (Ubuntu, Debian, RHEL, Fedora, Arch)
- ✅ **Windows** (with Git Bash or WSL)

All scripts are designed to work seamlessly in VS Code's integrated terminal across all supported platforms.