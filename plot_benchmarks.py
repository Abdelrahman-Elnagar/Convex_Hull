import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# Read the CSV file
df = pd.read_csv('benchmark_results.csv')

# Calculate average time for each algorithm and point count
avg_times = df.groupby(['Algorithm', 'PointCount'])['Time(ms)'].mean().reset_index()

# Create the plot
plt.figure(figsize=(12, 8))

# Plot each algorithm
algorithms = avg_times['Algorithm'].unique()
for algorithm in algorithms:
    algorithm_data = avg_times[avg_times['Algorithm'] == algorithm]
    plt.plot(algorithm_data['PointCount'], algorithm_data['Time(ms)'], 
             marker='o', label=algorithm, linewidth=2)

# Customize the plot
plt.xlabel('Number of Points', fontsize=12)
plt.ylabel('Average Time (ms)', fontsize=12)
plt.title('Convex Hull Algorithms Performance Comparison', fontsize=14)
plt.grid(True, linestyle='--', alpha=0.7)
plt.legend(fontsize=10)

# Use log scale for better visualization of wide-ranging values
plt.xscale('log')
plt.yscale('log')

# Add complexity annotations
complexities = {
    'GrahamScan': 'O(n log n)',
    'ChansAlgorithm': 'O(n log h)',
    'MergeHull': 'O(n log n)'
}

# Save the plot
plt.savefig('benchmark_results.png', dpi=300, bbox_inches='tight')
plt.close()

# Create a second plot showing memory usage
avg_memory = df.groupby(['Algorithm', 'PointCount'])['MemoryUsage(MB)'].mean().reset_index()

plt.figure(figsize=(12, 8))

for algorithm in algorithms:
    algorithm_data = avg_memory[avg_memory['Algorithm'] == algorithm]
    plt.plot(algorithm_data['PointCount'], algorithm_data['MemoryUsage(MB)'], 
             marker='o', label=algorithm, linewidth=2)

plt.xlabel('Number of Points', fontsize=12)
plt.ylabel('Average Memory Usage (MB)', fontsize=12)
plt.title('Convex Hull Algorithms Memory Usage Comparison', fontsize=14)
plt.grid(True, linestyle='--', alpha=0.7)
plt.legend(fontsize=10)

plt.xscale('log')
plt.yscale('log')

plt.savefig('memory_usage.png', dpi=300, bbox_inches='tight')
plt.close()

print("Plots have been generated: benchmark_results.png and memory_usage.png") 