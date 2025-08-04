#!/bin/bash

# =========================================
# CloudSim-based Edge Computing IoT Simulation Runner
# =========================================
# This script runs the EdgeComputingSimulation with CloudSim
# with reasonable parameters to ensure controlled execution
# and produce meaningful results with cloud utilization
# =========================================

# Colors for better readability
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Configuration
TIMEOUT=60            # Maximum runtime in seconds
MAX_MEMORY="2048m"    # Maximum JVM heap size
OUTPUT_DIR="results/run_$(date +%Y%m%d_%H%M%S)"
CONFIG_TYPE="src/main/resources/configs/realistic_config.json" # Default to realistic config for better offloading

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --timeout)
      TIMEOUT="$2"
      shift 2
      ;;
    --memory)
      MAX_MEMORY="$2"
      shift 2
      ;;
    --config)
      CONFIG_TYPE="$2"
      shift 2
      ;;
    --help)
      echo -e "${BLUE}CloudSim-based Edge Computing IoT Simulation Runner${NC}"
      echo -e "Usage: ./run_cloudsim.sh [options]"
      echo -e "Options:"
      echo -e "  --timeout SEC    Maximum runtime in seconds (default: 30)"
      echo -e "  --memory MEM     Maximum JVM heap size (default: 2048m)"
      echo -e "  --config TYPE    Configuration type or path to config file"
      echo -e "                   Built-in types: baseline, energy-efficient, latency-optimized, high-density"
      echo -e "                   Or specify path to a custom JSON config file"
      echo -e "  --help           Display this help message"
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      echo -e "Run './run_cloudsim.sh --help' for usage information."
      exit 1
      ;;
  esac
done

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}    CloudSim-based Edge Computing IoT   ${NC}"
echo -e "${BLUE}========================================${NC}"

echo -e "${GREEN}Configuration:${NC}"
echo -e "  - Timeout: ${TIMEOUT} seconds"
echo -e "  - Max Memory: ${MAX_MEMORY}"
echo -e "  - Config Type: ${CONFIG_TYPE}"
echo -e "  - Output Directory: ${OUTPUT_DIR}"

# Ensure we have a proper configuration file for the selected type
CONFIG_FILE="src/main/resources/simulation_config.json"

# Check if CONFIG_TYPE is a path to a file
if [[ -f "$CONFIG_TYPE" ]]; then
    CONFIG_FILE="$CONFIG_TYPE"
    echo -e "${YELLOW}Using custom configuration file: ${CONFIG_FILE}${NC}"
else
    # Use built-in configuration types
    case "$CONFIG_TYPE" in
      energy-efficient)
        CONFIG_FILE="src/main/resources/configs/energy_efficient_config.json"
    # Create directory if it doesn't exist
    mkdir -p "src/main/resources/configs"
    
    # Create energy-efficient config if it doesn't exist
    if [ ! -f "$CONFIG_FILE" ]; then
      echo -e "${YELLOW}Creating energy-efficient configuration...${NC}"
      cat > "$CONFIG_FILE" << EOF
{
  "simulation": {
    "duration": 60.0,
    "time_step": 1.0,
    "time_unit": "SECONDS",
    "debug": false
  },
  "iot_devices": {
    "count": 10,
    "types": [
      {
        "name": "sensor",
        "mips": 500,
        "ram": 512,
        "battery_capacity": 5000,
        "battery_consumption_rate": 0.2,
        "task_generation_rate": 0.3,
        "wireless_technology": "BLE",
        "mobility": false,
        "mobility_speed": 0.0
      },
      {
        "name": "smartphone",
        "mips": 2000,
        "ram": 2048,
        "battery_capacity": 3000,
        "battery_consumption_rate": 1.0,
        "task_generation_rate": 0.5,
        "wireless_technology": "WiFi",
        "mobility": true,
        "mobility_speed": 1.5
      }
    ]
  },
  "edge_nodes": {
    "count": 3,
    "types": [
      {
        "name": "small_edge",
        "mips": 5000,
        "ram": 8192,
        "storage": 102400,
        "bw": 1000,
        "cost_per_mips": 0.01
      }
    ]
  },
  "cloud": {
    "mips": 50000,
    "ram": 32768,
    "storage": 1048576,
    "bw": 10000,
    "cost_per_mips": 0.05,
    "latency_to_edge_ms": 100
  },
  "network": {
    "technologies": {
      "WiFi": {
        "latency_ms": 10,
        "bandwidth": 100,
        "energy_per_bit": 0.0001
      },
      "LTE": {
        "latency_ms": 50,
        "bandwidth": 50,
        "energy_per_bit": 0.0005
      },
      "BLE": {
        "latency_ms": 5,
        "bandwidth": 1,
        "energy_per_bit": 0.00001
      }
    }
  },
  "service_slicing": {
    "slices": [
      {
        "name": "default",
        "resource_percentage": 1.0,
        "priority": 1,
        "task_types": ["lightweight", "medium", "intensive"]
      }
    ]
  },
  "offloading_policy": {
    "type": "energy_aware",
    "parameters": {
      "weight_latency": 0.2,
      "weight_energy": 0.6,
      "weight_cost": 0.2
    }
  }
}
EOF
    fi
    ;;
    
  latency-optimized)
    CONFIG_FILE="src/main/resources/configs/latency_optimized_config.json"
    # Create directory if it doesn't exist
    mkdir -p "src/main/resources/configs"
    
    # Create latency-optimized config if it doesn't exist
    if [ ! -f "$CONFIG_FILE" ]; then
      echo -e "${YELLOW}Creating latency-optimized configuration...${NC}"
      cat > "$CONFIG_FILE" << EOF
{
  "simulation": {
    "duration": 60.0,
    "time_step": 1.0,
    "time_unit": "SECONDS",
    "debug": false
  },
  "iot_devices": {
    "count": 10,
    "types": [
      {
        "name": "sensor",
        "mips": 500,
        "ram": 512,
        "battery_capacity": 5000,
        "battery_consumption_rate": 0.5,
        "task_generation_rate": 0.3,
        "wireless_technology": "WiFi",
        "mobility": false,
        "mobility_speed": 0.0
      },
      {
        "name": "smartphone",
        "mips": 2000,
        "ram": 2048,
        "battery_capacity": 3000,
        "battery_consumption_rate": 1.0,
        "task_generation_rate": 0.5,
        "wireless_technology": "LTE",
        "mobility": true,
        "mobility_speed": 1.5
      }
    ]
  },
  "edge_nodes": {
    "count": 5,
    "types": [
      {
        "name": "small_edge",
        "mips": 8000,
        "ram": 8192,
        "storage": 102400,
        "bw": 2000,
        "cost_per_mips": 0.02
      }
    ]
  },
  "cloud": {
    "mips": 100000,
    "ram": 65536,
    "storage": 1048576,
    "bw": 10000,
    "cost_per_mips": 0.05,
    "latency_to_edge_ms": 80
  },
  "network": {
    "technologies": {
      "WiFi": {
        "latency_ms": 8,
        "bandwidth": 150,
        "energy_per_bit": 0.0001
      },
      "LTE": {
        "latency_ms": 40,
        "bandwidth": 80,
        "energy_per_bit": 0.0005
      },
      "BLE": {
        "latency_ms": 5,
        "bandwidth": 1,
        "energy_per_bit": 0.00001
      }
    }
  },
  "service_slicing": {
    "slices": [
      {
        "name": "default",
        "resource_percentage": 1.0,
        "priority": 1,
        "task_types": ["lightweight", "medium", "intensive"]
      }
    ]
  },
  "offloading_policy": {
    "type": "latency_aware",
    "parameters": {
      "weight_latency": 0.7,
      "weight_energy": 0.1,
      "weight_cost": 0.2
    }
  }
}
EOF
    fi
    ;;

  high-density)
    CONFIG_FILE="src/main/resources/configs/high_density_config.json"
    # Create directory if it doesn't exist
    mkdir -p "src/main/resources/configs"
    
    # Create high-density config if it doesn't exist
    if [ ! -f "$CONFIG_FILE" ]; then
      echo -e "${YELLOW}Creating high-density configuration...${NC}"
      cat > "$CONFIG_FILE" << EOF
{
  "simulation": {
    "duration": 60.0,
    "time_step": 1.0,
    "time_unit": "SECONDS",
    "debug": false
  },
  "iot_devices": {
    "count": 20,
    "types": [
      {
        "name": "sensor",
        "mips": 500,
        "ram": 512,
        "battery_capacity": 5000,
        "battery_consumption_rate": 0.5,
        "task_generation_rate": 0.4,
        "wireless_technology": "WiFi",
        "mobility": false,
        "mobility_speed": 0.0
      },
      {
        "name": "smartphone",
        "mips": 2000,
        "ram": 2048,
        "battery_capacity": 3000,
        "battery_consumption_rate": 1.0,
        "task_generation_rate": 0.6,
        "wireless_technology": "LTE",
        "mobility": true,
        "mobility_speed": 1.5
      }
    ]
  },
  "edge_nodes": {
    "count": 3,
    "types": [
      {
        "name": "small_edge",
        "mips": 5000,
        "ram": 8192,
        "storage": 102400,
        "bw": 1000,
        "cost_per_mips": 0.01
      }
    ]
  },
  "cloud": {
    "mips": 80000,
    "ram": 65536,
    "storage": 2097152,
    "bw": 20000,
    "cost_per_mips": 0.04,
    "latency_to_edge_ms": 90
  },
  "network": {
    "technologies": {
      "WiFi": {
        "latency_ms": 10,
        "bandwidth": 100,
        "energy_per_bit": 0.0001
      },
      "LTE": {
        "latency_ms": 50,
        "bandwidth": 50,
        "energy_per_bit": 0.0005
      },
      "BLE": {
        "latency_ms": 5,
        "bandwidth": 1,
        "energy_per_bit": 0.00001
      }
    }
  },
  "service_slicing": {
    "slices": [
      {
        "name": "default",
        "resource_percentage": 0.7,
        "priority": 1,
        "task_types": ["lightweight", "medium"]
      },
      {
        "name": "premium",
        "resource_percentage": 0.3,
        "priority": 2,
        "task_types": ["intensive"]
      }
    ]
  },
  "offloading_policy": {
    "type": "balanced",
    "parameters": {
      "weight_latency": 0.4,
      "weight_energy": 0.3,
      "weight_cost": 0.3
    }
  }
}
EOF
    fi
    ;;
    
  baseline|*)
    # Check if baseline config exists, create if not
    if [ ! -f "$CONFIG_FILE" ]; then
      echo -e "${YELLOW}Creating baseline configuration...${NC}"
      cat > "$CONFIG_FILE" << EOF
{
  "simulation": {
    "duration": 60.0,
    "time_step": 1.0,
    "time_unit": "SECONDS",
    "debug": false
  },
  "iot_devices": {
    "count": 5,
    "types": [
      {
        "name": "sensor",
        "mips": 500,
        "ram": 512,
        "battery_capacity": 5000,
        "battery_consumption_rate": 0.5,
        "task_generation_rate": 0.3,
        "wireless_technology": "WiFi",
        "mobility": false,
        "mobility_speed": 0.0
      },
      {
        "name": "smartphone",
        "mips": 2000,
        "ram": 2048,
        "battery_capacity": 3000,
        "battery_consumption_rate": 1.0,
        "task_generation_rate": 0.5,
        "wireless_technology": "LTE",
        "mobility": true,
        "mobility_speed": 1.5
      }
    ]
  },
  "edge_nodes": {
    "count": 2,
    "types": [
      {
        "name": "small_edge",
        "mips": 5000,
        "ram": 8192,
        "storage": 102400,
        "bw": 1000,
        "cost_per_mips": 0.01
      }
    ]
  },
  "cloud": {
    "mips": 50000,
    "ram": 32768,
    "storage": 1048576,
    "bw": 10000,
    "cost_per_mips": 0.05,
    "latency_to_edge_ms": 100
  },
  "network": {
    "technologies": {
      "WiFi": {
        "latency_ms": 10,
        "bandwidth": 100,
        "energy_per_bit": 0.0001
      },
      "LTE": {
        "latency_ms": 50,
        "bandwidth": 50,
        "energy_per_bit": 0.0005
      },
      "BLE": {
        "latency_ms": 5,
        "bandwidth": 1,
        "energy_per_bit": 0.00001
      }
    }
  },
  "service_slicing": {
    "slices": [
      {
        "name": "default",
        "resource_percentage": 1.0,
        "priority": 1,
        "task_types": ["lightweight", "medium", "intensive"]
      }
    ]
  },
  "offloading_policy": {
    "type": "utility",
    "parameters": {
      "weight_latency": 0.4,
      "weight_energy": 0.3,
      "weight_cost": 0.3
    }
  }
}
EOF
    fi
    ;;
esac

# Configure minimal logging to reduce output verbosity
echo -e "${YELLOW}Setting up logging configuration...${NC}"
cat > src/main/resources/logback.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss} %-5level %logger{0} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="org.cloudbus.cloudsim" level="ERROR"/>
    <logger name="org.edgecomputing.models" level="WARN"/>
    <logger name="org.edgecomputing.simulation" level="INFO"/>
    <logger name="org.edgecomputing.policies" level="WARN"/>
    
    <root level="WARN">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
EOF

# Clean and compile the project
echo -e "${GREEN}Building project...${NC}"
mvn -q clean compile

if [ $? -ne 0 ]; then
  echo -e "${RED}Build failed!${NC}"
  echo -e "${YELLOW}Checking for compilation errors...${NC}"
  mvn compile
  exit 1
fi

# Package the project
echo -e "${GREEN}Packaging project...${NC}"
mvn -q package -DskipTests

if [ $? -ne 0 ]; then
  echo -e "${RED}Packaging failed!${NC}"
  exit 1
fi

# Run the simulation with timeout
echo "Running CloudSim simulation (max ${TIMEOUT}s)..."

# Set JVM options for controlled memory usage
export _JAVA_OPTIONS="-Xmx${MAX_MEMORY}"

echo "Starting simulation..."
echo "If the simulation hangs, it will be terminated automatically after ${TIMEOUT} seconds."

# Run with timeout protection and capture all output
mkdir -p "${OUTPUT_DIR}"
timeout ${TIMEOUT}s java -cp target/edge-computing-iot-1.0-SNAPSHOT-jar-with-dependencies.jar org.edgecomputing.EdgeComputingDemo "${CONFIG_TYPE}" "${OUTPUT_DIR}"

# Check exit status
EXIT_STATUS=${PIPESTATUS[0]}
if [ $EXIT_STATUS -eq 124 ] || [ $EXIT_STATUS -eq 137 ]; then
    echo -e "${YELLOW}Simulation terminated after timeout (${TIMEOUT}s)${NC}"
    echo -e "${YELLOW}This is EXPECTED behavior to prevent infinite loops${NC}"
elif [ $EXIT_STATUS -ne 0 ]; then
    echo -e "${RED}Simulation failed with exit code $EXIT_STATUS${NC}"
else
    echo -e "${GREEN}Simulation completed successfully within time limit!${NC}"
fi

# Check for results
RESULT_FILES=$(find "$OUTPUT_DIR" -type f | wc -l)
if [ $RESULT_FILES -gt 0 ]; then
    echo -e "${GREEN}Results generated: $RESULT_FILES files in $OUTPUT_DIR${NC}"
    
    # Show summary of results if available
    SUMMARY_FILE="$OUTPUT_DIR/summary.json"
    if [ -f "$SUMMARY_FILE" ]; then
        echo -e "${BLUE}--- SIMULATION RESULTS SUMMARY ---${NC}"
        cat "$SUMMARY_FILE" | grep -A 20 "tasks" | grep -v "^\s*$"
        echo -e "${BLUE}--------------------------------${NC}"
    fi
else
    echo -e "${YELLOW}No result files generated in $OUTPUT_DIR${NC}"
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}      Simulation Run Complete          ${NC}"
echo -e "${BLUE}========================================${NC}"
fi
