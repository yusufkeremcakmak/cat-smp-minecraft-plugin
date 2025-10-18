#!/bin/bash

# Cat SMP Minecraft Plugin - Development Environment Verification
# Cross-platform verification script for VS Code

echo "🔍 Cat SMP Minecraft Plugin - Environment Verification"
echo "====================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Function to print colored output
print_check() {
    if [[ "$2" == "PASS" ]]; then
        echo -e "${GREEN}✓${NC} $1"
        ((PASSED++))
    elif [[ "$2" == "FAIL" ]]; then
        echo -e "${RED}✗${NC} $1"
        ((FAILED++))
    elif [[ "$2" == "WARN" ]]; then
        echo -e "${YELLOW}⚠${NC} $1"
        ((WARNINGS++))
    else
        echo -e "${BLUE}ℹ${NC} $1"
    fi
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check operating system
echo ""
echo "📋 System Information"
echo "===================="

OS_INFO=$(uname -s)
print_check "Operating System: $OS_INFO" "INFO"

if [[ -n "$VSCODE_PID" ]] || [[ "$TERM_PROGRAM" == "vscode" ]]; then
    print_check "Running in VS Code environment" "PASS"
else
    print_check "Not running in VS Code (this is fine)" "WARN"
fi

# Check shell
SHELL_INFO=$(echo $SHELL | rev | cut -d'/' -f1 | rev)
print_check "Shell: $SHELL_INFO" "INFO"

echo ""
echo "☕ Java Environment"
echo "=================="

# Check Java
if command_exists java; then
    JAVA_VERSION_OUTPUT=$(java -version 2>&1 | head -n 1)
    # Extract version number more robustly
    JAVA_VERSION=$(echo "$JAVA_VERSION_OUTPUT" | sed -n 's/.*"\([0-9][0-9.]*\)".*/\1/p')
    if [[ -z "$JAVA_VERSION" ]]; then
        # Fallback extraction method
        JAVA_VERSION=$(echo "$JAVA_VERSION_OUTPUT" | sed -n 's/.*version "\([^"]*\)".*/\1/p')
    fi
    
    if [[ -n "$JAVA_VERSION" ]]; then
        JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f1)
        # Handle cases where version starts with "1.8" format
        if [[ "$JAVA_MAJOR" == "1" ]]; then
            JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f2)
        fi
        
        if [[ "$JAVA_MAJOR" -ge 21 ]]; then
            print_check "Java Runtime: $JAVA_VERSION" "PASS"
        elif [[ "$JAVA_MAJOR" -ge 17 ]]; then
            print_check "Java Runtime: $JAVA_VERSION (JDK 21+ recommended)" "WARN"
        else
            print_check "Java Runtime: $JAVA_VERSION (JDK 21+ required for best compatibility)" "WARN"
        fi
    else
        print_check "Java Runtime: Version detection failed" "WARN"
    fi
else
    print_check "Java Runtime: Not found" "FAIL"
fi

# Check Java Compiler
if command_exists javac; then
    JAVAC_VERSION=$(javac -version 2>&1)
    print_check "Java Compiler: $JAVAC_VERSION" "PASS"
else
    print_check "Java Compiler (javac): Not found" "FAIL"
fi

# Check JAVA_HOME
if [[ -n "$JAVA_HOME" ]]; then
    if [[ -d "$JAVA_HOME" ]]; then
        print_check "JAVA_HOME: $JAVA_HOME" "PASS"
    else
        print_check "JAVA_HOME: $JAVA_HOME (directory does not exist)" "FAIL"
    fi
else
    print_check "JAVA_HOME: Not set" "WARN"
    
    # Try to suggest JAVA_HOME
    if command_exists java; then
        case "$(uname -s)" in
            Darwin*)
                SUGGESTED_JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || echo "")
                if [[ -n "$SUGGESTED_JAVA_HOME" ]]; then
                    print_check "Suggested JAVA_HOME: $SUGGESTED_JAVA_HOME" "INFO"
                fi
                ;;
            Linux*)
                JAVA_PATH=$(readlink -f "$(which java)" 2>/dev/null)
                if [[ -n "$JAVA_PATH" ]]; then
                    SUGGESTED_JAVA_HOME=$(dirname "$(dirname "$JAVA_PATH")")
                    print_check "Suggested JAVA_HOME: $SUGGESTED_JAVA_HOME" "INFO"
                fi
                ;;
        esac
    fi
fi

echo ""
echo "📦 Maven Environment"
echo "==================="

# Check Maven
if command_exists mvn; then
    MAVEN_VERSION=$(mvn -version 2>/dev/null | head -n 1 | sed 's/Apache Maven //' | cut -d' ' -f1)
    if [[ -n "$MAVEN_VERSION" ]]; then
        MAVEN_MAJOR=$(echo $MAVEN_VERSION | cut -d'.' -f1)
        if [[ "$MAVEN_MAJOR" -ge 3 ]]; then
            print_check "Maven: $MAVEN_VERSION" "PASS"
        else
            print_check "Maven: $MAVEN_VERSION (version 3+ recommended)" "WARN"
        fi
    else
        print_check "Maven: Installed but version check failed" "WARN"
    fi
    
    # Check Maven's Java detection
    MAVEN_JAVA=$(mvn -version 2>/dev/null | grep "Java version" | cut -d':' -f2 | xargs)
    if [[ -n "$MAVEN_JAVA" ]]; then
        print_check "Maven using Java: $MAVEN_JAVA" "PASS"
    else
        print_check "Maven Java detection: Failed" "WARN"
    fi
else
    print_check "Maven: Not found" "FAIL"
fi

echo ""
echo "🛠️ Development Tools"
echo "==================="

# Check Git
if command_exists git; then
    GIT_VERSION=$(git --version | cut -d' ' -f3)
    print_check "Git: $GIT_VERSION" "PASS"
else
    print_check "Git: Not found (recommended for version control)" "WARN"
fi

# Check common editors/IDEs
if command_exists code; then
    print_check "VS Code CLI: Available" "PASS"
else
    print_check "VS Code CLI: Not in PATH (this is fine)" "INFO"
fi

echo ""
echo "🧪 Project Validation"
echo "==================="

# Check if we're in a valid project directory
if [[ -f "pom.xml" ]]; then
    print_check "Maven project file (pom.xml): Found" "PASS"
    
    # Try to validate the project
    if command_exists mvn; then
        echo -n "   Testing Maven project validation... "
        if mvn validate -q >/dev/null 2>&1; then
            echo -e "${GREEN}✓${NC}"
            ((PASSED++))
        else
            echo -e "${RED}✗${NC}"
            print_check "Maven project validation: Failed" "FAIL"
        fi
    fi
else
    print_check "Maven project file (pom.xml): Not found in current directory" "WARN"
fi

# Check for common Minecraft plugin files
if [[ -f "src/main/resources/plugin.yml" ]]; then
    print_check "Minecraft plugin descriptor (plugin.yml): Found" "PASS"
else
    print_check "Minecraft plugin descriptor (plugin.yml): Not found (will be needed)" "WARN"
fi

# Check project structure
if [[ -d "src/main/java" ]]; then
    print_check "Java source directory: Found" "PASS"
else
    print_check "Java source directory: Not found (create src/main/java)" "WARN"
fi

echo ""
echo "📊 Environment Summary"
echo "====================="

TOTAL=$((PASSED + FAILED + WARNINGS))

echo -e "${GREEN}✓ Passed: $PASSED${NC}"
echo -e "${YELLOW}⚠ Warnings: $WARNINGS${NC}"
echo -e "${RED}✗ Failed: $FAILED${NC}"
echo "Total checks: $TOTAL"

echo ""

if [[ $FAILED -eq 0 ]]; then
    echo -e "${GREEN}🎉 Environment is ready for development!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Create your Maven project structure (if not already done)"
    echo "2. Start coding your Minecraft plugin"
    echo "3. Use 'mvn compile' to build your project"
    echo "4. Use 'mvn package' to create the plugin JAR"
elif [[ $FAILED -le 2 ]]; then
    echo -e "${YELLOW}⚠️  Environment has minor issues but should work${NC}"
    echo ""
    echo "Recommended fixes:"
    if ! command_exists java; then
        echo "- Install Java JDK 17 or later"
    fi
    if ! command_exists mvn; then
        echo "- Install Apache Maven"
    fi
    if [[ -z "$JAVA_HOME" ]]; then
        echo "- Set JAVA_HOME environment variable"
    fi
else
    echo -e "${RED}❌ Environment needs setup before development${NC}"
    echo ""
    echo "Required fixes:"
    echo "- Run the installation script: ./install-dev-env.sh"
    echo "- Or manually install Java JDK and Maven"
fi

echo ""
echo "💡 For help with setup, see README.md or run ./scripts/dev-env-install.sh"