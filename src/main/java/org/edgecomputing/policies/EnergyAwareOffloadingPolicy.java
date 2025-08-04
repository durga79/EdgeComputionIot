package org.edgecomputing.policies;

import java.util.List;
import org.edgecomputing.models.EdgeNode;
import org.edgecomputing.models.IoTDevice;
import org.edgecomputing.models.Task;

/**
 * Energy-aware offloading policy implementation based on the paper
 * "IoT Service Slicing and Task Offloading for Edge Computing"
 * 
 * This policy makes offloading decisions based on multiple factors:
 * 1. Device battery level
 * 2. Task computational requirements
 * 3. Network conditions
 * 4. Energy-latency trade-off
 */
public class EnergyAwareOffloadingPolicy implements OffloadingPolicy {
    
    private final double batteryThreshold;
    private final long taskSizeThreshold;
    private final double networkQualityThreshold;
    private final double energyWeight;
    private final double latencyWeight;
    private final double costWeight;
    
    /**
     * Creates a new Energy-Aware Offloading Policy.
     * 
     * @param batteryThreshold Battery level threshold (0-1) below which tasks should be offloaded
     * @param taskSizeThreshold Task size threshold above which offloading should be considered
     * @param networkQualityThreshold Network quality threshold (0-1) required for offloading
     * @param energyWeight Weight of energy factor in decision making
     * @param latencyWeight Weight of latency factor in decision making
     * @param costWeight Weight of cost factor in decision making
     */
    public EnergyAwareOffloadingPolicy(
            double batteryThreshold, 
            long taskSizeThreshold,
            double networkQualityThreshold,
            double energyWeight,
            double latencyWeight,
            double costWeight) {
        this.batteryThreshold = batteryThreshold;
        this.taskSizeThreshold = taskSizeThreshold;
        this.networkQualityThreshold = networkQualityThreshold;
        this.energyWeight = energyWeight;
        this.latencyWeight = latencyWeight;
        this.costWeight = costWeight;
    }

    @Override
    public boolean shouldOffload(Task task, IoTDevice device, List<EdgeNode> edgeNodes) {
        // 1. If battery is too low, offload to save energy
        if (device.getBatteryLevel() < batteryThreshold) {
            System.out.println("[Policy] Device " + device.getDeviceId() + 
                " offloading due to low battery: " + 
                String.format("%.2f%%", device.getBatteryLevel() * 100));
            return true;
        }
            
        // 2. If task is computationally intensive, offload
        if (task.getMipsRequired() > device.getMips()) {
            System.out.println("[Policy] Device " + device.getDeviceId() + 
                " offloading due to insufficient MIPS: " + 
                task.getMipsRequired() + " > " + device.getMips());
            return true;
        }
        
        // 3. If task is large, consider network quality
        if (task.getInputSize() > taskSizeThreshold) {
            // Only offload if network quality is good enough
            double networkQuality = device.getReliability() * (device.getBandwidthMbps() / 100.0);  // Normalize to 0-1
            if (networkQuality > networkQualityThreshold) {
                System.out.println("[Policy] Device " + device.getDeviceId() + 
                    " offloading large task with good network: " + 
                    String.format("%.2f", networkQuality));
                return true;
            }
        }
        
        // 4. Calculate utility-based decision
        EdgeNode bestEdgeNode = selectEdgeNode(task, device, edgeNodes);
        if (bestEdgeNode != null) {
            // Calculate estimated completion time locally
            double localCompletionTime = task.getMipsRequired() / (double) device.getMips();
            
            // Calculate estimated completion time on edge
            // Consider network latency, transfer time, and processing time
            double latencySec = device.getLatencyMs() / 1000.0;
            double transferTime = task.getInputSize() / (device.getBandwidthMbps() * 125000);  // Convert Mbps to bytes/sec
            double edgeProcessingTime = task.getMipsRequired() / (double) bestEdgeNode.getMips();
            double edgeCompletionTime = latencySec + transferTime + edgeProcessingTime;
            
            // Calculate energy consumption for local vs edge processing
            double localEnergy = (task.getMipsRequired() / (double) device.getMips()) * 
                                device.getBatteryConsumptionRate() * 3.0;  // Higher local energy use
            double edgeEnergy = transferTime * device.getNetworkEnergyConsumption() * 0.7; // Reduced energy impact for edge
            
            // Calculate utility based on weighted factors (lower is better)
            double localUtility = (
                energyWeight * localEnergy +
                latencyWeight * localCompletionTime +
                0.1 * (device.getBatteryCapacity() - device.getBatteryLevel()) / device.getBatteryCapacity() // Additional factor for battery preservation
            );
            
            double edgeUtility = (
                energyWeight * edgeEnergy +
                latencyWeight * edgeCompletionTime * 0.8 + // Reduce latency impact for edge
                costWeight * (task.getMipsRequired() * bestEdgeNode.getCostPerMips() * 0.9) // Reduce cost impact
            );
            
            // Make decision based on utility comparison
            boolean shouldOffload = edgeUtility < localUtility;
            
            if (shouldOffload) {
                System.out.println("[Policy] Device " + device.getDeviceId() + 
                    " offloading based on utility: local=" + 
                    String.format("%.2f", localUtility) + ", edge=" + 
                    String.format("%.2f", edgeUtility));
            } else {
                System.out.println("[Policy] Device " + device.getDeviceId() + 
                    " executing locally based on utility: local=" + 
                    String.format("%.2f", localUtility) + ", edge=" + 
                    String.format("%.2f", edgeUtility));
            }
            
            return shouldOffload;
        }
        
        // Default to local execution if no edge nodes available
        return false;
    }

    @Override
    public EdgeNode selectEdgeNode(Task task, IoTDevice device, List<EdgeNode> edgeNodes) {
        if (edgeNodes == null || edgeNodes.isEmpty()) {
            return null;
        }
            
        EdgeNode bestNode = null;
        double bestScore = Double.MAX_VALUE;
        
        for (EdgeNode node : edgeNodes) {
            // Calculate distance to edge node
            double distance = Math.sqrt(
                Math.pow(device.getLocationX() - node.getLocationX(), 2) + 
                Math.pow(device.getLocationY() - node.getLocationY(), 2)
            );
            
            // Calculate load factor (0-1, higher is more loaded)
            double loadFactor = node.getUtilization();
            
            // Calculate score (lower is better)
            // We weight distance more heavily for higher latency technologies
            double distanceWeight;
            switch (device.getWirelessTech()) {
                case "WiFi":
                    distanceWeight = 1.0;
                    break;
                case "BLE":
                    distanceWeight = 1.5;
                    break;
                case "LTE":
                    distanceWeight = 0.7;
                    break;
                case "FiveG":
                    distanceWeight = 0.5;
                    break;
                default:
                    distanceWeight = 1.0;
            }
            
            // Service slicing awareness - check if node has a slice for this task type
            boolean hasSlice = false;
            for (EdgeNode.ServiceSlice slice : node.getServiceSlices().values()) {
                if (slice.supportsTaskType(task.getType())) {
                    hasSlice = true;
                    break;
                }
            }
            
            // Calculate final score with penalty for nodes without appropriate service slice
            double score = (distance * distanceWeight) + (loadFactor * 100);
            if (!hasSlice) {
                score += 10000; // Large penalty for nodes without slice
            }
            
            if (score < bestScore) {
                bestScore = score;
                bestNode = node;
            }
        }
        
        if (bestNode != null) {
            System.out.println("[Policy] Selected edge node " + bestNode.getNodeId() + 
                " for device " + device.getDeviceId() + " with score " + 
                String.format("%.2f", bestScore));
        }
                
        return bestNode;
    }

    @Override
    public String getName() {
        return "Energy-Aware Offloading Policy";
    }
}
