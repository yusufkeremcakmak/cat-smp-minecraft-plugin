# Cat SMP Minecraft Plugin 🐱

A Minecraft plugin for the Cat SMP server, built with Java and Maven.

## 🚀 Quick Start - Development Environment Setup

To start developing this plugin, you'll need Java Development Kit (JDK) 21 and Maven installed on your machine. Since you're using VS Code, we've created a unified bash script that works across all platforms (macOS, Linux, and Windows with Git Bash/WSL).

### Prerequisites

- **VS Code** with integrated terminal
- **Git Bash** (Windows users) or native terminal (macOS/Linux)
- **Administrator/sudo privileges** for package installation

### One-Command Installation

Simply run our cross-platform installation script in VS Code's terminal:

```bash
# Make the script executable and run it
chmod +x scripts/dev-env-install.sh
./scripts/dev-env-install.sh
```

The script automatically:
- Detects your operating system (macOS, Linux, or Windows)
- Installs the appropriate package manager (Homebrew, apt/yum/pacman, or Chocolatey)
- Installs JDK 21 (Long Term Support version, perfect for Minecraft plugins)
- Installs Apache Maven
- Configures environment variables (JAVA_HOME, PATH)
- Works seamlessly in VS Code's integrated terminal

### Platform-Specific Details

#### 🍎 macOS
- Uses **Homebrew** for package management
- Automatically handles Apple Silicon vs Intel Macs
- Sets up shell profile (`.zprofile`) for environment variables

#### 🐧 Linux
- Supports **APT** (Ubuntu/Debian), **YUM/DNF** (RedHat/Fedora), and **Pacman** (Arch)
- Uses distribution's OpenJDK packages
- Updates shell profile (`.bashrc`) for environment variables

#### 🪟 Windows
- Uses **Chocolatey** or **Scoop** package managers
- Works with **Git Bash**, **WSL**, or **PowerShell** in VS Code
- Automatically handles Windows-specific Java installation paths

### Manual Installation (If Needed)

If the automated script doesn't work for your system:

#### Java Development Kit (JDK) 21
- **Oracle JDK**: [Download from Oracle](https://www.oracle.com/java/technologies/downloads/#java21)
- **OpenJDK**: [Download from OpenJDK.net](https://jdk.java.net/21/)
- **Adoptium Eclipse Temurin**: [Download from Adoptium.net](https://adoptium.net/temurin/releases/?version=21)

#### Apache Maven
- **Download**: [Apache Maven Downloads](https://maven.apache.org/download.cgi)
- **Installation Guide**: [Maven Installation Instructions](https://maven.apache.org/install.html)

### Verification

After installation, verify your setup:

```bash
# Run our verification script
chmod +x scripts/dev-env-verify.sh
./scripts/dev-env-verify.sh
```

Or manually check:

```bash
java -version     # Should show Java 21
javac -version    # Should show Java 21
mvn -version      # Should show Maven 3.x
echo $JAVA_HOME   # Should show Java installation path
```

## 🛠️ Development

### Building the Plugin

```bash
# Compile the project
mvn compile

# Run tests
mvn test

# Build the plugin JAR
mvn package
```

The built plugin JAR will be located in the `target/` directory.

### Installing to Server

1. Build the plugin using `mvn package`
2. Copy the JAR file from `target/` to your Minecraft server's `plugins/` directory
3. Restart the server or use a plugin manager to reload

## 📁 Project Structure

```
cat-smp-minecraft-plugin/
├── scripts/               # Development setup scripts
│   ├── dev-env-install.sh # Cross-platform environment installer
│   └── dev-env-verify.sh  # Environment verification tool
├── src/
│   ├── main/
│   │   ├── java/          # Java source code
│   │   └── resources/     # Plugin configuration files
│   └── test/
│       └── java/          # Test files
├── target/                # Built artifacts (generated)
├── pom.xml               # Maven configuration
└── README.md             # This file
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests to ensure everything works
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🐛 Troubleshooting

### Common Issues

1. **Script permission denied**: Run `chmod +x scripts/dev-env-install.sh` to make the script executable
2. **Java version mismatch**: The script installs JDK 21 (Long Term Support version recommended for Minecraft plugins)
3. **Package manager not found**: 
   - **Windows**: Install Git Bash or enable WSL in VS Code
   - **macOS**: The script will automatically install Homebrew
   - **Linux**: Most distributions have supported package managers pre-installed
4. **VS Code terminal issues**: Make sure you're using the integrated terminal (Ctrl/Cmd + `)

### Platform-Specific Issues

#### Windows
- **Git Bash not found**: Install [Git for Windows](https://git-scm.com/download/win)
- **Chocolatey installation fails**: Run VS Code as Administrator
- **Path issues**: Restart VS Code after running the installation script

#### macOS
- **Homebrew installation fails**: Check your internet connection and try again
- **Permission issues**: The script handles most permission requirements automatically
- **Apple Silicon compatibility**: The script automatically detects and handles M1/M2 Macs

#### Linux
- **Package manager permission denied**: The script uses `sudo` for package installation
- **Missing dependencies**: The script will attempt to install required dependencies

### Getting Help

If you encounter issues:
1. Run `./scripts/dev-env-verify.sh` to diagnose environment problems
2. Check the [GitHub Issues](../../issues) page for known problems
3. Create a new issue with:
   - Your operating system and version
   - VS Code version
   - Terminal type (bash, zsh, PowerShell, etc.)
   - Complete error message from the installation script

### Manual Setup for Edge Cases

If the automated script fails completely, you can set up manually:

1. **Install Java JDK 21** from your preferred source
2. **Install Maven** from Apache Maven website  
3. **Set environment variables**:
   ```bash
   export JAVA_HOME=/path/to/java
   export PATH=$JAVA_HOME/bin:$PATH
   ```
4. **Add to shell profile** (`.bashrc`, `.zshrc`, etc.)
5. **Restart VS Code** and verify with `./scripts/dev-env-verify.sh`
