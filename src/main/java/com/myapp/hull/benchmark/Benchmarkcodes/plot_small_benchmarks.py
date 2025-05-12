import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# Read the CSV file
df = pd.read_csv('small_benchmark_results.csv')

# Create the time plot
plt.figure(figsize=(12, 8))

# Plot each algorithm
algorithms = df['Algorithm'].unique()
for algorithm in algorithms:
    algorithm_data = df[df['Algorithm'] == algorithm]
    plt.plot(algorithm_data['PointCount'], algorithm_data['Time(ms)'], 
             marker='.', markersize=2, label=algorithm, linewidth=1)

# Customize the plot
plt.xlabel('Number of Points', fontsize=12)
plt.ylabel('Time (ms)', fontsize=12)
plt.title('Convex Hull Algorithms Performance (Small Inputs, up to 10,000)', fontsize=14)
plt.grid(True, linestyle='--', alpha=0.7)
plt.legend(fontsize=10)

# Use linear scale for better visualization of small values
plt.xscale('linear')
plt.yscale('linear')

# Save the plot
plt.savefig('small_benchmark_results.png', dpi=300, bbox_inches='tight')
plt.close()

# Create memory usage plot
plt.figure(figsize=(12, 8))

for algorithm in algorithms:
    algorithm_data = df[df['Algorithm'] == algorithm]
    plt.plot(algorithm_data['PointCount'], algorithm_data['MemoryUsage(MB)'], 
             marker='.', markersize=2, label=algorithm, linewidth=1)

plt.xlabel('Number of Points', fontsize=12)
plt.ylabel('Memory Usage (MB)', fontsize=12)
plt.title('Convex Hull Algorithms Memory Usage (Small Inputs, up to 10,000)', fontsize=14)
plt.grid(True, linestyle='--', alpha=0.7)
plt.legend(fontsize=10)

plt.xscale('linear')
plt.yscale('linear')

plt.savefig('small_memory_usage.png', dpi=300, bbox_inches='tight')
plt.close()

# Create a smoothed version of the time plot
plt.figure(figsize=(12, 8))

for algorithm in algorithms:
    algorithm_data = df[df['Algorithm'] == algorithm]
    # Sort by PointCount to ensure correct smoothing
    algorithm_data = algorithm_data.sort_values('PointCount')
    # Compute moving average with window size 100
    smoothed_time = algorithm_data['Time(ms)'].rolling(window=100, min_periods=1).mean()
    plt.plot(algorithm_data['PointCount'], smoothed_time, marker='.', markersize=2, label=algorithm, linewidth=1)

plt.xlabel('Number of Points', fontsize=12)
plt.ylabel('Time (ms)', fontsize=12)
plt.title('Convex Hull Algorithms Performance (Small Inputs, up to 10,000, Smoothed)', fontsize=14)
plt.grid(True, linestyle='--', alpha=0.7)
plt.legend(fontsize=10)

plt.xscale('linear')
plt.yscale('linear')

plt.savefig('small_benchmark_results_smooth.png', dpi=300, bbox_inches='tight')
plt.close()

print("Small benchmark plots have been generated: small_benchmark_results.png and small_memory_usage.png") 