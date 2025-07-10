#!/bin/bash

echo "🐳 Building ServerSage Docker Image with OpenTelemetry"
echo "====================================================="

# Set image name and tag
IMAGE_NAME="serversage"
IMAGE_TAG="latest"
FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"

echo ""
echo "📋 Build Configuration:"
echo "  Image Name: $FULL_IMAGE_NAME"
echo "  OpenTelemetry Version: 1.32.0"
echo "  Base Image: eclipse-temurin:17-jre-alpine"
echo ""

# Build the Docker image
echo "🔨 Building Docker image..."
docker build -t $FULL_IMAGE_NAME . --no-cache

# Check if build was successful
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Docker image built successfully!"
    echo ""
    echo "📊 Image Details:"
    docker images $FULL_IMAGE_NAME
    echo ""
    echo "🔍 Image Layers:"
    docker history $FULL_IMAGE_NAME --format "table {{.CreatedBy}}\t{{.Size}}"
    echo ""
    echo "🚀 Ready to run with:"
    echo "  docker run -p 8081:8081 $FULL_IMAGE_NAME"
    echo "  OR"
    echo "  docker-compose up -d"
else
    echo ""
    echo "❌ Docker build failed!"
    exit 1
fi
