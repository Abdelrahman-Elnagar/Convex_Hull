# Convex Hull Visualizer User Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Basic Usage](#basic-usage)
4. [Advanced Features](#advanced-features)
5. [Troubleshooting](#troubleshooting)

## Introduction

The Convex Hull Visualizer is a Java application that demonstrates various algorithms for computing convex hulls. A convex hull is the smallest convex polygon that contains all given points. This application provides an interactive way to explore different algorithms and their performance characteristics.

## Installation

### System Requirements
- Java 11 or later
- 4GB RAM minimum
- 100MB disk space

### Installation Steps
1. Download the application JAR file
2. Ensure Java is installed and in your PATH
3. Run the application:
   ```bash
   java -jar convex-hull-1.0-SNAPSHOT.jar
   ```

## Basic Usage

### Adding Points
1. Click anywhere on the canvas to add points
2. Points are displayed as blue circles
3. The view automatically scales to show all points

### Computing the Hull
1. Select an algorithm from the dropdown menu
2. Click the "Compute" button
3. The hull will be drawn in red
4. Hull vertices are highlighted

### File Operations
- **Load**: Import points from a text file
- **Save**: Export the current hull to a text file
- **Clear**: Remove all points and reset the canvas

## Advanced Features

### Animation
- Toggle animation to see the hull being constructed step by step
- Adjust animation speed using the slider
- Animation can be paused/resumed

### Algorithm Selection
- **Graham Scan**: O(n log n) algorithm, good for general cases
- **Chan's Algorithm**: O(n log h) algorithm, efficient for small hulls
- **Merge Hull**: O(n log n) algorithm, naturally extends to 3D

### Visualization Options
- Adjust point size using the slider
- Points and hull are drawn on separate layers
- Hull vertices are highlighted for clarity

## Troubleshooting

### Common Issues
1. **Application won't start**
   - Ensure Java 11+ is installed
   - Check system requirements
   - Verify JAR file integrity

2. **Points not displaying**
   - Check if the canvas is visible
   - Try clearing and adding points again
   - Restart the application

3. **Hull computation fails**
   - Ensure at least 3 points are present
   - Try a different algorithm
   - Check for collinear points

### Performance Tips
- Use Chan's Algorithm for point sets with small hulls
- Graham Scan is generally the most robust choice
- For very large point sets (>100,000), consider using Merge Hull

## Support

For additional support:
- Check the [GitHub repository](https://github.com/yourusername/convex-hull)
- Submit issues on GitHub
- Contact support@example.com 