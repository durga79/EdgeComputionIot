# Edge Computing Task Offloading Implementation

## Project Overview
This project implements a proof-of-concept based on the research paper "IoT Service Slicing and Task Offloading for Edge Computing" (IEEE Communications Magazine, 2021). The implementation demonstrates service distribution in IoT environments using edge computing for efficient task offloading.

## Research Paper Reference
This implementation is based on the research paper:
**"IoT Service Slicing and Task Offloading for Edge Computing"**  
*IEEE Communications Magazine, 2021*

## Key Components

1. **IoT Device Simulation**: Simulates multiple IoT devices generating data and computational tasks with varying requirements.

2. **Edge Computing Layer**: Implements edge nodes that can process offloaded tasks from IoT devices.

3. **Cloud Computing Layer**: Models cloud datacenter for offloaded tasks that require higher computational resources.

4. **Task Offloading Mechanism**: Decision engine that determines whether to process tasks locally on IoT devices or offload them to edge nodes or cloud.

5. **Service Slicing**: Resource allocation mechanism that partitions edge computing resources based on task types and priorities.

6. **Network Connectivity Simulation**: Models different wireless technologies connecting IoT devices to edge nodes with varying latency, bandwidth, and reliability.

## System Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│                 │     │                  │     │                 │
│   IoT Devices   │◄───►│   Edge Nodes     │◄───►│  Cloud Backend  │
│   (Generators)  │     │  (Processing)    │     │  (High Compute) │
│                 │     │                  │     │                 │
└─────────────────┘     └──────────────────┘     └─────────────────┘
        │                       │                        │
        │                       │                        │
        ▼                       ▼                        ▼
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Task Creation  │     │ Service Slicing  │     │ Data Analytics │
│  & Scheduling   │     │ & Resource Mgmt  │     │   & Storage    │
└─────────────────┘     └──────────────────┘     └─────────────────┘
```

## Performance Metrics

The implementation measures:

- Task completion time and deadline satisfaction
- Energy consumption and battery preservation
- Network bandwidth usage and quality of service
- Resource utilization across devices, edge nodes, and cloud
- Response time for different offloading strategies
- Cost efficiency of resource allocation

## Technology Stack

- Java JDK 11+
- CloudSim Plus 7.2.0 (Cloud/Edge computing simulation framework)
- Maven for dependency management
- Gson for JSON configuration parsing
- SLF4J and Logback for logging
- JFreeChart for visualization

## Setup Instructions

1. Clone this repository
2. Build the project using Maven:
   ```
   mvn clean package
   ```
3. Run the simulation with default configuration:
   ```
   java -jar target/edge-computing-iot-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```
4. Run with a specific scenario:
   ```
   java -cp target/edge-computing-iot-1.0-SNAPSHOT-jar-with-dependencies.jar org.edgecomputing.EdgeComputingDemo energy-efficient results/energy
   ```
5. Run scenario evaluation:
   ```
   java -cp target/edge-computing-iot-1.0-SNAPSHOT-jar-with-dependencies.jar org.edgecomputing.evaluation.ScenarioEvaluator evaluation_results
   ```

## Configuration

The system parameters can be configured using JSON files in the resources directory:

- `simulation_config.json`: Baseline configuration with balanced parameters
- `configs/energy_efficient_config.json`: Configuration prioritizing energy preservation
- `configs/latency_optimized_config.json`: Configuration prioritizing low response times
- `configs/high_density_config.json`: Configuration with high device density

Configuration parameters include:
- Simulation duration and time step
- IoT device types, counts, and capabilities
- Edge node resources and costs
- Cloud datacenter specifications
- Task types and requirements
- Network technologies and parameters
- Service slicing configurations
- Offloading policy weights and thresholds

## Project Structure

```
src/main/java/org/edgecomputing/
├── EdgeComputingDemo.java           # Main demonstration class
├── simulation/
│   └── EdgeComputingSimulation.java # Main simulation engine
├── models/                          # Data models
│   ├── IoTDevice.java               # IoT device implementation
│   ├── EdgeNode.java                # Edge node implementation
│   ├── CloudDatacenter.java         # Cloud datacenter implementation
│   └── Task.java                    # Task model
├── policies/                        # Offloading and scheduling policies
│   ├── OffloadingPolicy.java        # Interface for offloading strategies
│   ├── EnergyAwareOffloadingPolicy.java # Concrete offloading implementation
│   ├── ServiceSlicingPolicy.java    # Service slicing implementation
│   └── TaskScheduler.java           # Task scheduler
├── utils/                           # Utility classes
│   ├── SimulationResults.java       # Results collection and export
│   └── ResultsVisualizer.java       # Visualization tools
└── evaluation/                      # Evaluation tools
    └── ScenarioEvaluator.java       # Multi-scenario evaluation

src/main/resources/
├── simulation_config.json           # Baseline simulation configuration
└── configs/                         # Alternative configurations
    ├── energy_efficient_config.json # Energy-optimized configuration
    ├── latency_optimized_config.json # Latency-optimized configuration
    └── high_density_config.json     # High device density configuration
```

## Running Different Scenarios

The project includes several predefined scenarios:

1. **Baseline**: Balanced weights for energy, latency, and cost
2. **Energy-Efficient**: Prioritizes battery preservation and energy efficiency
3. **Latency-Optimized**: Prioritizes fast response times and deadline satisfaction
4. **High-Density**: Tests scalability with a large number of IoT devices

Each scenario can be run individually or compared using the ScenarioEvaluator.

## Results Analysis

The simulation outputs include:

1. **CSV Data Files**:
   - `devices.csv`: IoT device metrics (battery levels, task generation, etc.)
   - `edge_nodes.csv`: Edge node performance (utilization, tasks processed)
   - `cloud.csv`: Cloud datacenter metrics
   - `tasks.csv`: Detailed task execution data

2. **Summary Report**:
   - Performance statistics for the entire simulation
   - Resource utilization metrics
   - Task distribution analysis

3. **Visualization Charts**:
   - Task distribution pie charts
   - Resource utilization line charts
   - Response time analysis
   - Battery level tracking
   - Comparative performance graphs

4. **Scenario Comparison Report**:
   - HTML-formatted comparison across different scenarios
   - Highlights trade-offs between configurations
   - Quantitative performance differentials

## Future Work

Potential extensions to the project:
- Machine learning-based offloading decision making
- Dynamic service slice reconfiguration
- Real-time mobility patterns and network condition changes
- Integration with real IoT device data streams
- Federation between multiple edge computing domains
# EdgeComputionIot
