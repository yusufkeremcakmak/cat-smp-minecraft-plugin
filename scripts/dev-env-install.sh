#!/bin/bash

# Cat SMP Minecraft Plugin - Development Environment Setup
# Cross-platform script for VS Code (works on macOS, Linux, and Windows with Git Bash/WSL)

set -e  # Exit on any error

echo "🐱 Cat SMP Minecraft Plugin - Development Environment Setup"
echo "=========================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Detect operating system
detect_os() {
    case "$(uname -s)" in
        Darwin*) OS="mac" ;;
        Linux*) OS="linux" ;;
        CYGWIN*|MINGW*|MSYS*) OS="windows" ;;
        *) OS="unknown" ;;
    esac
    print_status "Detected OS: $OS"
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Install package manager based on OS
install_package_manager() {
    case $OS in
        "mac")
            if ! command_exists brew; then
                print_status "Installing Homebrew..."
                /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
                
                # Add Homebrew to PATH for Apple Silicon Macs
                if [[ $(uname -m) == "arm64" ]]; then
                    echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
                    eval "$(/opt/homebrew/bin/brew shellenv)"
                else
                    echo 'eval "$(/usr/local/bin/brew shellenv)"' >> ~/.zprofile
                    eval "$(/usr/local/bin/brew shellenv)"
                fi
                print_success "Homebrew installed!"
            else
                print_success "Homebrew already installed!"
                brew update
            fi
            ;;
        "linux")
            # Check for different Linux package managers
            if command_exists apt-get; then
                PACKAGE_MANAGER="apt"
                print_status "Using APT package manager"
                sudo apt-get update
            elif command_exists yum; then
                PACKAGE_MANAGER="yum"
                print_status "Using YUM package manager"
            elif command_exists dnf; then
                PACKAGE_MANAGER="dnf"
                print_status "Using DNF package manager"
            elif command_exists pacman; then
                PACKAGE_MANAGER="pacman"
                print_status "Using Pacman package manager"
                sudo pacman -Sy
            else
                print_error "No supported package manager found!"
                exit 1
            fi
            ;;
        "windows")
            # Check for Chocolatey or SCOOP on Windows
            if ! command_exists choco && ! command_exists scoop; then
                print_status "Installing Chocolatey (requires PowerShell as Admin)..."
                print_warning "Please run the following command in PowerShell as Administrator:"
                echo "Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))"
                print_warning "After installing Chocolatey, restart this script."
                exit 1
            elif command_exists choco; then
                print_success "Chocolatey found!"
                PACKAGE_MANAGER="choco"
            elif command_exists scoop; then
                print_success "Scoop found!"
                PACKAGE_MANAGER="scoop"
            fi
            ;;
    esac
}

# Install Java based on OS and package manager
install_java() {
    print_status "Installing JDK 21..."
    
    case $OS in
        "mac")
            # Check if OpenJDK 21 is already installed (formula)
            if brew list openjdk@21 >/dev/null 2>&1; then
                print_success "JDK 21 already installed!"
            elif brew list openjdk >/dev/null 2>&1; then
                print_success "OpenJDK already installed!"
            else
                # Try OpenJDK 21 formula first
                if brew install openjdk@21; then
                    print_success "JDK 21 installed!"
                elif brew install openjdk; then
                    print_success "OpenJDK installed!"
                else
                    # Fallback to Microsoft OpenJDK cask
                    print_warning "Homebrew OpenJDK failed, trying Microsoft OpenJDK..."
                    brew install --cask microsoft-openjdk@21 || brew install --cask microsoft-openjdk
                    print_success "Microsoft OpenJDK installed!"
                fi
            fi
            ;;
        "linux")
            case $PACKAGE_MANAGER in
                "apt")
                    if ! java -version 2>&1 | grep -q "21\|17"; then
                        sudo apt-get install -y openjdk-21-jdk || sudo apt-get install -y openjdk-17-jdk || sudo apt-get install -y default-jdk
                        print_success "OpenJDK installed!"
                    else
                        print_success "Java already installed!"
                    fi
                    ;;
                "yum"|"dnf")
                    if ! java -version 2>&1 | grep -q "21\|17"; then
                        sudo $PACKAGE_MANAGER install -y java-21-openjdk-devel || sudo $PACKAGE_MANAGER install -y java-17-openjdk-devel || sudo $PACKAGE_MANAGER install -y java-latest-openjdk-devel
                        print_success "OpenJDK installed!"
                    else
                        print_success "Java already installed!"
                    fi
                    ;;
                "pacman")
                    if ! java -version 2>&1 | grep -q "21\|17"; then
                        sudo pacman -S --noconfirm jdk21-openjdk || sudo pacman -S --noconfirm jdk-openjdk
                        print_success "OpenJDK installed!"
                    else
                        print_success "Java already installed!"
                    fi
                    ;;
            esac
            ;;
        "windows")
            case $PACKAGE_MANAGER in
                "choco")
                    if ! command_exists java; then
                        choco install openjdk21 -y || choco install openjdk -y
                        print_success "OpenJDK installed via Chocolatey!"
                    else
                        print_success "Java already installed!"
                    fi
                    ;;
                "scoop")
                    if ! command_exists java; then
                        scoop bucket add java
                        scoop install openjdk21 || scoop install openjdk
                        print_success "OpenJDK installed via Scoop!"
                    else
                        print_success "Java already installed!"
                    fi
                    ;;
            esac
            ;;
    esac
}

# Install Maven based on OS and package manager
install_maven() {
    print_status "Installing Maven..."
    
    if command_exists mvn; then
        print_success "Maven already installed!"
        return
    fi
    
    case $OS in
        "mac")
            brew install maven
            print_success "Maven installed via Homebrew!"
            ;;
        "linux")
            case $PACKAGE_MANAGER in
                "apt")
                    sudo apt-get install -y maven
                    ;;
                "yum"|"dnf")
                    sudo $PACKAGE_MANAGER install -y maven
                    ;;
                "pacman")
                    sudo pacman -S --noconfirm maven
                    ;;
            esac
            print_success "Maven installed!"
            ;;
        "windows")
            case $PACKAGE_MANAGER in
                "choco")
                    choco install maven -y
                    print_success "Maven installed via Chocolatey!"
                    ;;
                "scoop")
                    scoop install maven
                    print_success "Maven installed via Scoop!"
                    ;;
            esac
            ;;
    esac
}

# Set up Java environment variables
setup_java_env() {
    print_status "Setting up Java environment..."
    
    # Try to find JAVA_HOME
    JAVA_HOME_PATH=""
    
    if command_exists java; then
        case $OS in
            "mac")
                JAVA_HOME_PATH=$(/usr/libexec/java_home 2>/dev/null || echo "")
                if [[ -z "$JAVA_HOME_PATH" ]]; then
                    # Fallback for Homebrew formula installations
                    if [[ -d "/opt/homebrew/opt/openjdk@21" ]]; then
                        JAVA_HOME_PATH="/opt/homebrew/opt/openjdk@21"
                    elif [[ -d "/usr/local/opt/openjdk@21" ]]; then
                        JAVA_HOME_PATH="/usr/local/opt/openjdk@21"
                    elif [[ -d "/opt/homebrew/opt/openjdk" ]]; then
                        JAVA_HOME_PATH="/opt/homebrew/opt/openjdk"
                    elif [[ -d "/usr/local/opt/openjdk" ]]; then
                        JAVA_HOME_PATH="/usr/local/opt/openjdk"
                    # Check for Microsoft OpenJDK cask installations
                    elif [[ -d "/opt/homebrew/Cellar/microsoft-openjdk@21" ]]; then
                        JAVA_HOME_PATH="/opt/homebrew/Cellar/microsoft-openjdk@21/$(ls /opt/homebrew/Cellar/microsoft-openjdk@21)/libexec/openjdk.jdk/Contents/Home"
                    elif [[ -d "/usr/local/Cellar/microsoft-openjdk@21" ]]; then
                        JAVA_HOME_PATH="/usr/local/Cellar/microsoft-openjdk@21/$(ls /usr/local/Cellar/microsoft-openjdk@21)/libexec/openjdk.jdk/Contents/Home"
                    fi
                fi
                ;;
            "linux")
                JAVA_HOME_PATH=$(readlink -f /usr/bin/java | sed "s:bin/java::" 2>/dev/null || echo "")
                if [[ -z "$JAVA_HOME_PATH" ]]; then
                    JAVA_HOME_PATH="/usr/lib/jvm/default-java"
                fi
                ;;
            "windows")
                # On Windows with Git Bash, try to find Java installation
                JAVA_PATH=$(which java 2>/dev/null || echo "")
                if [[ -n "$JAVA_PATH" ]]; then
                    JAVA_HOME_PATH=$(dirname "$(dirname "$JAVA_PATH")")
                fi
                ;;
        esac
        
        if [[ -n "$JAVA_HOME_PATH" && -d "$JAVA_HOME_PATH" ]]; then
            # Add to shell profile
            SHELL_PROFILE=""
            if [[ -n "$ZSH_VERSION" ]]; then
                SHELL_PROFILE="$HOME/.zshrc"
            elif [[ -n "$BASH_VERSION" ]]; then
                if [[ "$OS" == "mac" ]]; then
                    SHELL_PROFILE="$HOME/.zprofile"
                else
                    SHELL_PROFILE="$HOME/.bashrc"
                fi
            fi
            
            if [[ -n "$SHELL_PROFILE" ]]; then
                if ! grep -q "JAVA_HOME.*$JAVA_HOME_PATH" "$SHELL_PROFILE" 2>/dev/null; then
                    echo "export JAVA_HOME=\"$JAVA_HOME_PATH\"" >> "$SHELL_PROFILE"
                    echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\"" >> "$SHELL_PROFILE"
                fi
            fi
            
            export JAVA_HOME="$JAVA_HOME_PATH"
            export PATH="$JAVA_HOME/bin:$PATH"
            print_success "JAVA_HOME set to: $JAVA_HOME_PATH"
        else
            print_warning "Could not automatically detect JAVA_HOME"
        fi
    fi
}

# Verify installations
verify_installation() {
    echo ""
    echo "🔍 Verifying installations..."
    echo "=============================="
    
    # Check Java
    if command_exists java; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1)
        print_success "Java: $JAVA_VERSION"
    else
        print_error "Java not found!"
    fi
    
    # Check Java Compiler
    if command_exists javac; then
        JAVAC_VERSION=$(javac -version 2>&1)
        print_success "Java Compiler: $JAVAC_VERSION"
    else
        print_error "Java Compiler (javac) not found!"
    fi
    
    # Check Maven
    if command_exists mvn; then
        MAVEN_VERSION=$(mvn -version 2>/dev/null | head -n 1 || echo "Maven installed but version check failed")
        print_success "Maven: $MAVEN_VERSION"
    else
        print_error "Maven not found!"
    fi
    
    # Check JAVA_HOME
    if [[ -n "$JAVA_HOME" ]]; then
        print_success "JAVA_HOME: $JAVA_HOME"
    else
        print_warning "JAVA_HOME not set"
    fi
}

# Main installation process
main() {
    detect_os
    
    if [[ "$OS" == "unknown" ]]; then
        print_error "Unsupported operating system!"
        exit 1
    fi
    
    print_status "Starting installation process..."
    
    install_package_manager
    install_java
    install_maven
    setup_java_env
    verify_installation
    
    echo ""
    echo "🎉 Development environment setup complete!"
    echo "=========================================="
    echo ""
    echo "📝 Next steps:"
    echo "1. Restart your terminal or run: source ~/.zshrc (or ~/.bashrc)"
    echo "2. Verify installation by running: ./scripts/dev-env-verify.sh"
    echo "3. Start developing your Minecraft plugin!"
    echo ""
    echo "💡 Useful commands:"
    echo "   java -version    # Check Java version"
    echo "   mvn -version     # Check Maven version"
    echo "   mvn compile      # Compile your project"
    echo "   mvn package      # Build your plugin JAR"
    echo ""
    echo "🚀 Ready to code! Open VS Code and start building your plugin."
}

# Run main function
main "$@"