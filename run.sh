#!/bin/bash

# Find JavaFX SDK in Downloads
FX_DIR=$(find ~/Downloads -maxdepth 1 -name "javafx-sdk-*" -type d | head -n 1)

if [ -z "$FX_DIR" ]; then
    echo "Could not find JavaFX SDK in ~/Downloads. Please download it from https://gluonhq.com/products/javafx/"
    exit 1
fi

PATH_TO_FX="$FX_DIR/lib"
echo "Using JavaFX SDK from: $PATH_TO_FX"

# Create bin directory
mkdir -p bin

# Copy resources (images, music, css) from src to bin
echo "Copying resources..."
rsync -a --exclude="*.java" src/ bin/

# Compile Java files
echo "Compiling..."
javac --module-path "$PATH_TO_FX" -d bin $(find src -name "*.java")

# Run the application
echo "Running..."
java --module-path "$PATH_TO_FX:bin" --module project/application.Main
