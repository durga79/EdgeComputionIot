# Edge Computing IoT Simulation Framework
## 6-Minute Presentation

---

## Introduction (30 seconds)

- CloudSim-based simulation framework for IoT environments
- Models task offloading strategies across device-edge-cloud architecture
- Addresses optimization of computational resource allocation
- Balances energy efficiency, latency, and cost considerations

---

## Project Overview (1 minute)

### Multi-tier Computing Environment:
- **IoT Devices**: Limited resources (battery, CPU, memory)
- **Edge Nodes**: Intermediate computing capabilities
- **Cloud Datacenters**: High-performance, centralized processing

### Key Objectives:
- Evaluate different task offloading policies
- Determine optimal distribution of computational workloads
- Simulate realistic IoT scenarios with configurable parameters

---

## Technical Architecture (1.5 minutes)

### Built on CloudSim Plus 6.1.0
- Extended with IoT and edge computing capabilities

### Key Components:
1. **Configuration System**
   - JSON-based configuration files
   - Defines simulation parameters, network characteristics, device specs

2. **Offloading Policies**
   - Energy-aware decision algorithms
   - Considers: battery levels, network quality, task requirements

3. **Service Slicing**
   - Edge nodes support different service types
   - Optimized for specific workload characteristics

4. **Comprehensive Metrics**
   - Execution times, energy consumption
   - Resource utilization, deadline compliance

---

## Demonstration Results (1.5 minutes)

### Current Simulation Parameters:
- 12 IoT devices with varying capabilities
- 4 edge nodes with different service offerings
- 1 cloud datacenter with high-performance resources
- 60-second simulation duration
- ~1,300 tasks generated

### Key Results:
- **Task Distribution**:
  - ~39% executed locally on IoT devices
  - ~61% offloaded to the cloud
  - 0% processed at edge nodes (service slice configuration issue)

- **Performance Metrics**:
  - Average response time: ~59 seconds
  - Cloud resources: ~8% CPU utilization, ~11% RAM utilization
  - Average device battery consumption: minimal (~1.8%)

---

## Cross-Platform Support (1 minute)

### Linux Environment:
- `run_cloudsim.sh`: Main execution script
- `test_simulation.sh`: Detailed output and metrics

### Windows Environment:
- `run_cloudsim.bat`: Windows equivalent
- `test_simulation.bat`: Detailed output and metrics

### Output Generated:
- CSV data files for detailed analysis
- JSON summary of key metrics
- Visualization charts:
  - Task distribution pie chart
  - Resource utilization line charts
  - Battery level tracking
  - Response time analysis

---

## Future Work & Conclusion (30 seconds)

### Planned Improvements:
1. Fix edge node service slice configurations
2. Implement ML-based offloading policies
3. Add support for mobile IoT devices with changing network conditions

### Conclusion:
- Valuable testbed for edge computing strategies
- Optimizes resource usage before real-world deployment
- Improves application performance in IoT environments

---

## Thank You!

- Questions?
- Demo available upon request
