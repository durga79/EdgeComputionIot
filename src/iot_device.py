import random
import time
import json
import logging
from task import Task

class IoTDevice:
    """
    Represents an IoT device that can generate tasks and make offloading decisions.
    
    Attributes:
        device_id (int): Unique identifier for the device
        device_type (str): Type of IoT device (sensor, smartphone, etc.)
        mips (int): Processing capability in MIPS
        ram (int): Memory in MB
        battery_capacity (float): Maximum battery capacity in mAh
        current_battery (float): Current battery level in mAh
        task_generation_rate (float): Rate at which tasks are generated per second
        wireless_tech (str): Wireless technology used (WiFi, LTE, etc.)
        mobility (bool): Whether the device is mobile or stationary
        location (tuple): Current (x, y) location coordinates
        connected_edge_node (int): ID of the currently connected edge node
    """
    
    def __init__(self, device_id, config, device_type=None):
        """
        Initialize an IoT device with specified parameters.
        
        Args:
            device_id (int): Unique identifier for this device
            config (dict): Configuration parameters for this device
            device_type (str, optional): Type of device to create. If None, randomly selected.
        """
        self.device_id = device_id
        
        # If device type not specified, randomly choose from available types
        if device_type is None:
            device_types = config['iot_devices']['types']
            device_config = random.choice(device_types)
        else:
            device_config = next(
                (t for t in config['iot_devices']['types'] if t['name'] == device_type), 
                config['iot_devices']['types'][0]
            )
        
        # Set device properties from configuration
        self.device_type = device_config['name']
        self.mips = device_config['mips']
        self.ram = device_config['ram']
        self.battery_capacity = device_config['battery_capacity']
        self.current_battery = self.battery_capacity  # Start with full battery
        self.battery_consumption_rate = device_config['battery_consumption_rate']
        self.task_generation_rate = device_config['task_generation_rate']
        self.wireless_tech = device_config['wireless_technology']
        self.mobility = device_config['mobility']
        
        # Set initial location randomly within a 1000x1000 grid
        self.location = (random.uniform(0, 1000), random.uniform(0, 1000))
        self.connected_edge_node = None
        
        # Store network parameters for the device's wireless technology
        self.network_params = config['network']['technologies'][self.wireless_tech]
        
        # Offloading thresholds
        self.offloading_policy = config['offloading_policy']
        
        self.tasks_generated = 0
        self.tasks_offloaded = 0
        self.tasks_processed_locally = 0
        self.energy_consumed = 0
        
        logging.info(f"Created IoT device {self.device_id} of type {self.device_type} "
                     f"with {self.mips} MIPS and {self.wireless_tech} connectivity")

    def update_battery(self, time_elapsed):
        """Update battery level based on time elapsed and device activity"""
        consumption = self.battery_consumption_rate * time_elapsed
        self.current_battery -= consumption
        self.energy_consumed += consumption
        
        if self.current_battery <= 0:
            self.current_battery = 0
            logging.warning(f"Device {self.device_id} battery depleted!")
            return False
        return True

    def generate_task(self, simulation_time):
        """
        Generate a new task based on the device's task generation rate.
        
        Args:
            simulation_time (float): Current simulation time
            
        Returns:
            Task or None: A new Task if one should be generated, None otherwise
        """
        # Determine if a task should be generated based on probability
        if random.random() <= self.task_generation_rate:
            task_id = f"{self.device_id}_{self.tasks_generated}"
            self.tasks_generated += 1
            
            # Choose task type based on device capabilities
            if self.mips < 1000:
                task_type = "lightweight"
            elif self.mips < 3000:
                task_type = random.choice(["lightweight", "medium"])
            else:
                task_type = random.choice(["lightweight", "medium", "intensive"])
            
            # Create and return a new task
            new_task = Task(task_id, task_type, self.device_id, simulation_time)
            logging.info(f"Device {self.device_id} generated task {task_id} of type {task_type}")
            return new_task
        
        return None

    def should_offload_task(self, task, edge_nodes):
        """
        Decide whether to offload a task based on multiple factors.
        
        Args:
            task (Task): The task to potentially offload
            edge_nodes (list): Available edge nodes
            
        Returns:
            bool: True if task should be offloaded, False otherwise
        """
        # If battery is too low, offload to save energy
        if self.current_battery / self.battery_capacity < self.offloading_policy['threshold_battery_level']:
            return True
            
        # If task is computationally intensive, offload
        if task.mips_required > self.mips:
            return True
        
        # If task is large, consider offloading
        if task.input_size > self.offloading_policy['threshold_task_size']:
            # But only if network quality is good enough
            network_quality = self.network_params['reliability'] * (
                self.network_params['bandwidth_mbps'] / 100)  # Normalize to 0-1
            if network_quality > self.offloading_policy['threshold_network_quality']:
                return True
        
        # Calculate estimated completion time locally
        local_completion_time = task.mips_required / self.mips
        
        # Find best edge node based on distance
        best_edge_node = self._select_best_edge_node(edge_nodes)
        if best_edge_node:
            # Calculate estimated completion time on edge
            # Consider network latency, transfer time, and processing time
            latency_sec = self.network_params['latency_ms'] / 1000
            transfer_time = task.input_size / (self.network_params['bandwidth_mbps'] * 125000)  # Convert Mbps to bytes/sec
            edge_processing_time = task.mips_required / best_edge_node.mips
            edge_completion_time = latency_sec + transfer_time + edge_processing_time
            
            # Calculate energy consumption for local vs edge processing
            local_energy = (task.mips_required / self.mips) * self.battery_consumption_rate * 2  # Higher local energy use
            edge_energy = transfer_time * self.network_params['energy_consumption']
            
            # Calculate utility based on weighted factors
            local_utility = (
                self.offloading_policy['weight_energy'] * local_energy +
                self.offloading_policy['weight_latency'] * local_completion_time
            )
            
            edge_utility = (
                self.offloading_policy['weight_energy'] * edge_energy +
                self.offloading_policy['weight_latency'] * edge_completion_time +
                self.offloading_policy['weight_cost'] * (task.mips_required * best_edge_node.cost_per_mips)
            )
            
            # Lower utility is better (costs less)
            return edge_utility < local_utility
            
        return False
    
    def _select_best_edge_node(self, edge_nodes):
        """
        Select the best edge node based on distance and load.
        
        Args:
            edge_nodes (list): Available edge nodes
            
        Returns:
            EdgeNode: The best edge node to connect to, or None if none available
        """
        if not edge_nodes:
            return None
            
        best_node = None
        best_score = float('inf')
        
        for node in edge_nodes:
            # Calculate distance to edge node
            distance = ((self.location[0] - node.location[0])**2 + 
                        (self.location[1] - node.location[1])**2)**0.5
            
            # Calculate load factor (0-1, higher is more loaded)
            load_factor = node.get_utilization()
            
            # Calculate score (lower is better)
            # We weight distance more heavily for higher latency technologies
            distance_weight = {
                'WiFi': 1.0,
                'BLE': 1.5,
                'LTE': 0.7,
                'FiveG': 0.5
            }.get(self.wireless_tech, 1.0)
            
            score = (distance * distance_weight) + (load_factor * 100)
            
            if score < best_score:
                best_score = score
                best_node = node
                
        return best_node
    
    def process_task_locally(self, task, current_time):
        """
        Process a task on the local device.
        
        Args:
            task (Task): The task to process
            current_time (float): Current simulation time
            
        Returns:
            float: Time when task will be completed
        """
        # Calculate execution time based on MIPS
        execution_time = task.mips_required / self.mips
        
        # Update task status
        task.start_time = current_time
        task.completion_time = current_time + execution_time
        task.executed_on = f"device_{self.device_id}"
        task.status = "processing"
        
        # Update device statistics
        self.tasks_processed_locally += 1
        
        # Calculate and update energy consumption
        energy_used = execution_time * self.battery_consumption_rate * 2  # Processing uses more energy
        self.current_battery -= energy_used
        self.energy_consumed += energy_used
        
        logging.info(f"Device {self.device_id} processing task {task.task_id} locally, "
                     f"estimated completion at {task.completion_time:.2f}")
                     
        return task.completion_time
        
    def update_location(self, time_elapsed):
        """
        Update device location if it's mobile.
        
        Args:
            time_elapsed (float): Time elapsed since last update
        """
        if not self.mobility:
            return
            
        # Simple random movement model
        speed = 5.0  # units per second
        distance = speed * time_elapsed
        
        # Random direction
        angle = random.uniform(0, 2 * 3.14159)
        dx = distance * math.cos(angle)
        dy = distance * math.sin(angle)
        
        # Update location
        new_x = max(0, min(1000, self.location[0] + dx))
        new_y = max(0, min(1000, self.location[1] + dy))
        self.location = (new_x, new_y)
        
    def get_status(self):
        """
        Get the current status of the device.
        
        Returns:
            dict: Status information for this device
        """
        return {
            "device_id": self.device_id,
            "type": self.device_type,
            "battery_level": self.current_battery / self.battery_capacity,
            "location": self.location,
            "tasks_generated": self.tasks_generated,
            "tasks_offloaded": self.tasks_offloaded,
            "tasks_local": self.tasks_processed_locally,
            "energy_consumed": self.energy_consumed
        }
