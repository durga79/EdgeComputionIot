#!/bin/bash

# Colors for better readability
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Testing Edge Computing IoT Simulation ${NC}"
echo -e "${GREEN}========================================${NC}"

# Configuration
CONFIG_FILE="src/main/resources/configs/realistic_config.json"
OUTPUT_DIR="results/test_$(date +%Y%m%d_%H%M%S)"
MAX_MEMORY="2048m"
TIMEOUT=60

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Compile and package
echo -e "${GREEN}Building project...${NC}"
mvn -q clean package -DskipTests

if [ $? -ne 0 ]; then
  echo -e "${RED}Build failed!${NC}"
  exit 1
fi

# Run simulation
echo -e "${GREEN}Running simulation with realistic configuration...${NC}"
echo -e "${GREEN}Output directory: $OUTPUT_DIR${NC}"
echo -e "${GREEN}Timeout: $TIMEOUT seconds${NC}"

# Set JVM options
export _JAVA_OPTIONS="-Xmx$MAX_MEMORY"

# Run with timeout
timeout $TIMEOUT java -cp target/edge-computing-iot-1.0-SNAPSHOT-jar-with-dependencies.jar \
  org.edgecomputing.EdgeComputingDemo "$CONFIG_FILE" "$OUTPUT_DIR"

# Check exit status
EXIT_STATUS=$?
if [ $EXIT_STATUS -eq 124 ]; then
  echo -e "${RED}Simulation timed out after $TIMEOUT seconds${NC}"
  exit 1
elif [ $EXIT_STATUS -ne 0 ]; then
  echo -e "${RED}Simulation failed with exit code $EXIT_STATUS${NC}"
  exit 1
fi

# Check for results
if [ -f "$OUTPUT_DIR/summary.json" ]; then
  echo -e "${GREEN}========================================${NC}"
  echo -e "${GREEN}  Simulation Results Summary${NC}"
  echo -e "${GREEN}========================================${NC}"
  cat "$OUTPUT_DIR/summary.json"
  
  # Extract key metrics
  echo -e "\n${GREEN}========================================${NC}"
  echo -e "${GREEN}  Key Metrics${NC}"
  echo -e "${GREEN}========================================${NC}"
  
  # Task distribution
  TOTAL_TASKS=$(grep -A 1 "Total tasks:" "$OUTPUT_DIR/summary.json" | tail -1 | tr -d ' ')
  LOCAL_TASKS=$(grep -A 1 "Tasks executed locally:" "$OUTPUT_DIR/summary.json" | tail -1 | tr -d ' ' | cut -d'(' -f1)
  LOCAL_PERCENT=$(grep -A 1 "Tasks executed locally:" "$OUTPUT_DIR/summary.json" | tail -1 | tr -d ' ' | grep -o '([0-9.]*%)' | tr -d '(%)')
  EDGE_TASKS=$(grep -A 1 "Tasks executed on edge:" "$OUTPUT_DIR/summary.json" | tail -1 | tr -d ' ' | cut -d'(' -f1)
  EDGE_PERCENT=$(grep -A 1 "Tasks executed on edge:" "$OUTPUT_DIR/summary.json" | tail -1 | tr -d ' ' | grep -o '([0-9.]*%)' | tr -d '(%)')
  CLOUD_TASKS=$(grep -A 1 "Tasks executed on cloud:" "$OUTPUT_DIR/summary.json" | tail -1 | tr -d ' ' | cut -d'(' -f1)
  CLOUD_PERCENT=$(grep -A 1 "Tasks executed on cloud:" "$OUTPUT_DIR/summary.json" | tail -1 | tr -d ' ' | grep -o '([0-9.]*%)' | tr -d '(%)')
  
  echo "Total Tasks: $TOTAL_TASKS"
  echo "Tasks executed locally: $LOCAL_TASKS ($LOCAL_PERCENT%)"
  echo "Tasks executed on edge: $EDGE_TASKS ($EDGE_PERCENT%)"
  echo "Tasks executed on cloud: $CLOUD_TASKS ($CLOUD_PERCENT%)"
  
  # Resource utilization
  EDGE_CPU=$(grep -A 1 "Average CPU utilization:" "$OUTPUT_DIR/summary.json" | tail -1 | tr -d ' ')
  EDGE_RAM=$(grep -A 1 "Average RAM utilization:" "$OUTPUT_DIR/summary.json" | tail -1 | tr -d ' ')
  CLOUD_CPU=$(grep -A 1 "CPU utilization:" "$OUTPUT_DIR/summary.json" | tail -1 | tr -d ' ')
  CLOUD_RAM=$(grep -A 1 "RAM utilization:" "$OUTPUT_DIR/summary.json" | tail -1 | tr -d ' ')
  
  echo -e "\nResource Utilization:"
  echo "Edge CPU: $EDGE_CPU"
  echo "Edge RAM: $EDGE_RAM"
  echo "Cloud CPU: $CLOUD_CPU"
  echo "Cloud RAM: $CLOUD_RAM"
  
  echo -e "\n${GREEN}Simulation completed successfully!${NC}"
  echo -e "${GREEN}Results available in: $OUTPUT_DIR${NC}"
else
  echo -e "${RED}No summary.json found in $OUTPUT_DIR${NC}"
  echo -e "${RED}Simulation may have failed or not generated results${NC}"
  ls -la "$OUTPUT_DIR"
  exit 1
fi
