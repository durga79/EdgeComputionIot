@echo off
setlocal enabledelayedexpansion

:: CloudSim-based Edge Computing IoT Simulation Runner
:: Windows Batch File Version

echo ========================================
echo     CloudSim-based Edge Computing IoT   
echo ========================================

:: Default configuration
set TIMEOUT=60
set MAX_MEMORY=2048m
set CONFIG_TYPE=src\main\resources\configs\realistic_config.json
set OUTPUT_DIR=results\run_%date:~10,4%%date:~4,2%%date:~7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set OUTPUT_DIR=%OUTPUT_DIR: =0%

:: Parse command line arguments
:parse_args
if "%~1"=="" goto :end_parse_args
if "%~1"=="--help" (
    echo Usage: run_cloudsim.bat [options]
    echo.
    echo Options:
    echo   --help                 Show this help message
    echo   --timeout SECONDS      Set maximum runtime in seconds (default: 60)
    echo   --memory SIZE          Set maximum memory in MB (default: 2048)
    echo   --config TYPE          Set configuration type or path to config file:
    echo                          baseline, energy-efficient, latency-optimized, high-density
    echo                          or path to a custom JSON configuration file
    echo   --output DIR           Set output directory (default: results/timestamp)
    echo.
    exit /b 0
) else if "%~1"=="--timeout" (
    set TIMEOUT=%~2
    shift
) else if "%~1"=="--memory" (
    set MAX_MEMORY=%~2m
    shift
) else if "%~1"=="--config" (
    set CONFIG_TYPE=%~2
    shift
) else if "%~1"=="--output" (
    set OUTPUT_DIR=%~2
    shift
) else (
    echo Unknown option: %~1
    echo Run 'run_cloudsim.bat --help' for usage information.
    exit /b 1
)
shift
goto :parse_args
:end_parse_args

:: Create output directory
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

echo Configuration:
echo   - Timeout: %TIMEOUT% seconds
echo   - Max Memory: %MAX_MEMORY%
echo   - Config Type: %CONFIG_TYPE%
echo   - Output Directory: %OUTPUT_DIR%

:: Determine if CONFIG_TYPE is a file path or a built-in type
set CONFIG_FILE=src\main\resources\simulation_config.json
if exist "%CONFIG_TYPE%" (
    echo Using custom configuration file: %CONFIG_TYPE%
) else (
    :: Handle built-in configuration types
    if "%CONFIG_TYPE%"=="energy-efficient" (
        set CONFIG_FILE=src\main\resources\configs\energy_efficient_config.json
        if not exist "!CONFIG_FILE!" (
            echo Creating energy-efficient configuration...
            > "!CONFIG_FILE!" (
                echo {
                echo   "simulation": {
                echo     "duration": 60.0,
                echo     "time_step": 1.0,
                echo     "time_unit": "SECONDS",
                echo     "debug": false
                echo   },
                echo   "iot_devices": {
                echo     "count": 10,
                echo     "types": [
                echo       {
                echo         "name": "sensor",
                echo         "mips": 500,
                echo         "ram": 512,
                echo         "battery_capacity": 5000,
                echo         "battery_consumption_rate": 0.3,
                echo         "task_generation_rate": 0.2,
                echo         "wireless_technology": "BLE",
                echo         "mobility": false,
                echo         "mobility_speed": 0.0
                echo       }
                echo     ]
                echo   },
                echo   "edge_nodes": {
                echo     "count": 3,
                echo     "types": [
                echo       {
                echo         "name": "energy_efficient_edge",
                echo         "mips": 4000,
                echo         "ram": 4096,
                echo         "storage": 102400,
                echo         "bw": 1000,
                echo         "cost_per_mips": 0.005
                echo       }
                echo     ]
                echo   },
                echo   "cloud": {
                echo     "mips": 40000,
                echo     "ram": 16384,
                echo     "storage": 1048576,
                echo     "bw": 10000,
                echo     "cost_per_mips": 0.03,
                echo     "latency_to_edge_ms": 100
                echo   },
                echo   "network": {
                echo     "technologies": {
                echo       "WiFi": {
                echo         "latency_ms": 10,
                echo         "bandwidth": 100,
                echo         "energy_per_bit": 0.0001
                echo       },
                echo       "LTE": {
                echo         "latency_ms": 50,
                echo         "bandwidth": 50,
                echo         "energy_per_bit": 0.0005
                echo       },
                echo       "BLE": {
                echo         "latency_ms": 5,
                echo         "bandwidth": 1,
                echo         "energy_per_bit": 0.00001
                echo       }
                echo     }
                echo   },
                echo   "service_slicing": {
                echo     "slices": [
                echo       {
                echo         "name": "default",
                echo         "resource_percentage": 1.0,
                echo         "priority": 1,
                echo         "task_types": ["lightweight", "medium", "intensive"]
                echo       }
                echo     ]
                echo   },
                echo   "offloading_policy": {
                echo     "type": "energy_aware",
                echo     "parameters": {
                echo       "weight_latency": 0.2,
                echo       "weight_energy": 0.7,
                echo       "weight_cost": 0.1
                echo     }
                echo   }
                echo }
            )
        )
    ) else if "%CONFIG_TYPE%"=="latency-optimized" (
        set CONFIG_FILE=src\main\resources\configs\latency_optimized_config.json
        if not exist "!CONFIG_FILE!" (
            echo Creating latency-optimized configuration...
            > "!CONFIG_FILE!" (
                echo {
                echo   "simulation": {
                echo     "duration": 60.0,
                echo     "time_step": 1.0,
                echo     "time_unit": "SECONDS",
                echo     "debug": false
                echo   },
                echo   "iot_devices": {
                echo     "count": 8,
                echo     "types": [
                echo       {
                echo         "name": "smartphone",
                echo         "mips": 2000,
                echo         "ram": 2048,
                echo         "battery_capacity": 3000,
                echo         "battery_consumption_rate": 1.0,
                echo         "task_generation_rate": 0.5,
                echo         "wireless_technology": "WiFi",
                echo         "mobility": true,
                echo         "mobility_speed": 1.5
                echo       }
                echo     ]
                echo   },
                echo   "edge_nodes": {
                echo     "count": 5,
                echo     "types": [
                echo       {
                echo         "name": "low_latency_edge",
                echo         "mips": 8000,
                echo         "ram": 8192,
                echo         "storage": 204800,
                echo         "bw": 2000,
                echo         "cost_per_mips": 0.02
                echo       }
                echo     ]
                echo   },
                echo   "cloud": {
                echo     "mips": 50000,
                echo     "ram": 32768,
                echo     "storage": 1048576,
                echo     "bw": 10000,
                echo     "cost_per_mips": 0.05,
                echo     "latency_to_edge_ms": 100
                echo   },
                echo   "network": {
                echo     "technologies": {
                echo       "WiFi": {
                echo         "latency_ms": 5,
                echo         "bandwidth": 200,
                echo         "energy_per_bit": 0.0001
                echo       },
                echo       "LTE": {
                echo         "latency_ms": 20,
                echo         "bandwidth": 100,
                echo         "energy_per_bit": 0.0005
                echo       },
                echo       "BLE": {
                echo         "latency_ms": 5,
                echo         "bandwidth": 1,
                echo         "energy_per_bit": 0.00001
                echo       }
                echo     }
                echo   },
                echo   "service_slicing": {
                echo     "slices": [
                echo       {
                echo         "name": "default",
                echo         "resource_percentage": 1.0,
                echo         "priority": 1,
                echo         "task_types": ["lightweight", "medium", "intensive"]
                echo       }
                echo     ]
                echo   },
                echo   "offloading_policy": {
                echo     "type": "utility",
                echo     "parameters": {
                echo       "weight_latency": 0.7,
                echo       "weight_energy": 0.2,
                echo       "weight_cost": 0.1
                echo     }
                echo   }
                echo }
            )
        )
    ) else if "%CONFIG_TYPE%"=="high-density" (
        set CONFIG_FILE=src\main\resources\configs\high_density_config.json
        if not exist "!CONFIG_FILE!" (
            echo Creating high-density configuration...
            > "!CONFIG_FILE!" (
                echo {
                echo   "simulation": {
                echo     "duration": 60.0,
                echo     "time_step": 1.0,
                echo     "time_unit": "SECONDS",
                echo     "debug": false
                echo   },
                echo   "iot_devices": {
                echo     "count": 20,
                echo     "types": [
                echo       {
                echo         "name": "sensor",
                echo         "mips": 500,
                echo         "ram": 512,
                echo         "battery_capacity": 5000,
                echo         "battery_consumption_rate": 0.5,
                echo         "task_generation_rate": 0.3,
                echo         "wireless_technology": "WiFi",
                echo         "mobility": false,
                echo         "mobility_speed": 0.0
                echo       },
                echo       {
                echo         "name": "smartphone",
                echo         "mips": 2000,
                echo         "ram": 2048,
                echo         "battery_capacity": 3000,
                echo         "battery_consumption_rate": 1.0,
                echo         "task_generation_rate": 0.5,
                echo         "wireless_technology": "LTE",
                echo         "mobility": true,
                echo         "mobility_speed": 1.5
                echo       }
                echo     ]
                echo   },
                echo   "edge_nodes": {
                echo     "count": 8,
                echo     "types": [
                echo       {
                echo         "name": "high_capacity_edge",
                echo         "mips": 10000,
                echo         "ram": 16384,
                echo         "storage": 409600,
                echo         "bw": 5000,
                echo         "cost_per_mips": 0.03
                echo       }
                echo     ]
                echo   },
                echo   "cloud": {
                echo     "mips": 100000,
                echo     "ram": 65536,
                echo     "storage": 2097152,
                echo     "bw": 20000,
                echo     "cost_per_mips": 0.05,
                echo     "latency_to_edge_ms": 100
                echo   },
                echo   "network": {
                echo     "technologies": {
                echo       "WiFi": {
                echo         "latency_ms": 10,
                echo         "bandwidth": 100,
                echo         "energy_per_bit": 0.0001
                echo       },
                echo       "LTE": {
                echo         "latency_ms": 50,
                echo         "bandwidth": 50,
                echo         "energy_per_bit": 0.0005
                echo       },
                echo       "BLE": {
                echo         "latency_ms": 5,
                echo         "bandwidth": 1,
                echo         "energy_per_bit": 0.00001
                echo       }
                echo     }
                echo   },
                echo   "service_slicing": {
                echo     "slices": [
                echo       {
                echo         "name": "default",
                echo         "resource_percentage": 1.0,
                echo         "priority": 1,
                echo         "task_types": ["lightweight", "medium", "intensive"]
                echo       }
                echo     ]
                echo   },
                echo   "offloading_policy": {
                echo     "type": "utility",
                echo     "parameters": {
                echo       "weight_latency": 0.4,
                echo       "weight_energy": 0.3,
                echo       "weight_cost": 0.3
                echo     }
                echo   }
                echo }
            )
        )
    ) else (
        :: Default to baseline configuration
        if not exist "!CONFIG_FILE!" (
            echo Creating baseline configuration...
            > "!CONFIG_FILE!" (
                echo {
                echo   "simulation": {
                echo     "duration": 60.0,
                echo     "time_step": 1.0,
                echo     "time_unit": "SECONDS",
                echo     "debug": false
                echo   },
                echo   "iot_devices": {
                echo     "count": 5,
                echo     "types": [
                echo       {
                echo         "name": "sensor",
                echo         "mips": 500,
                echo         "ram": 512,
                echo         "battery_capacity": 5000,
                echo         "battery_consumption_rate": 0.5,
                echo         "task_generation_rate": 0.3,
                echo         "wireless_technology": "WiFi",
                echo         "mobility": false,
                echo         "mobility_speed": 0.0
                echo       },
                echo       {
                echo         "name": "smartphone",
                echo         "mips": 2000,
                echo         "ram": 2048,
                echo         "battery_capacity": 3000,
                echo         "battery_consumption_rate": 1.0,
                echo         "task_generation_rate": 0.5,
                echo         "wireless_technology": "LTE",
                echo         "mobility": true,
                echo         "mobility_speed": 1.5
                echo       }
                echo     ]
                echo   },
                echo   "edge_nodes": {
                echo     "count": 2,
                echo     "types": [
                echo       {
                echo         "name": "small_edge",
                echo         "mips": 5000,
                echo         "ram": 8192,
                echo         "storage": 102400,
                echo         "bw": 1000,
                echo         "cost_per_mips": 0.01
                echo       }
                echo     ]
                echo   },
                echo   "cloud": {
                echo     "mips": 50000,
                echo     "ram": 32768,
                echo     "storage": 1048576,
                echo     "bw": 10000,
                echo     "cost_per_mips": 0.05,
                echo     "latency_to_edge_ms": 100
                echo   },
                echo   "network": {
                echo     "technologies": {
                echo       "WiFi": {
                echo         "latency_ms": 10,
                echo         "bandwidth": 100,
                echo         "energy_per_bit": 0.0001
                echo       },
                echo       "LTE": {
                echo         "latency_ms": 50,
                echo         "bandwidth": 50,
                echo         "energy_per_bit": 0.0005
                echo       },
                echo       "BLE": {
                echo         "latency_ms": 5,
                echo         "bandwidth": 1,
                echo         "energy_per_bit": 0.00001
                echo       }
                echo     }
                echo   },
                echo   "service_slicing": {
                echo     "slices": [
                echo       {
                echo         "name": "default",
                echo         "resource_percentage": 1.0,
                echo         "priority": 1,
                echo         "task_types": ["lightweight", "medium", "intensive"]
                echo       }
                echo     ]
                echo   },
                echo   "offloading_policy": {
                echo     "type": "utility",
                echo     "parameters": {
                echo       "weight_latency": 0.4,
                echo       "weight_energy": 0.3,
                echo       "weight_cost": 0.3
                echo     }
                echo   }
                echo }
            )
        )
    )
)

:: Configure minimal logging to reduce output verbosity
echo Setting up logging configuration...
> src\main\resources\logback.xml (
    echo ^<?xml version="1.0" encoding="UTF-8"?^>
    echo ^<configuration^>
    echo     ^<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender"^>
    echo         ^<encoder^>
    echo             ^<pattern^>%%d{HH:mm:ss} %%-5level %%logger{0} - %%msg%%n^</pattern^>
    echo         ^</encoder^>
    echo     ^</appender^>
    echo     
    echo     ^<logger name="org.cloudbus.cloudsim" level="ERROR"/^>
    echo     ^<logger name="org.edgecomputing.models" level="WARN"/^>
    echo     ^<logger name="org.edgecomputing.simulation" level="INFO"/^>
    echo     ^<logger name="org.edgecomputing.policies" level="WARN"/^>
    echo     
    echo     ^<root level="WARN"^>
    echo         ^<appender-ref ref="CONSOLE" /^>
    echo     ^</root^>
    echo ^</configuration^>
)

:: Clean and compile the project
echo Building project...
call mvn -q clean compile
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    echo Checking for compilation errors...
    call mvn compile
    exit /b 1
)

:: Package the project
echo Packaging project...
call mvn -q package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Packaging failed!
    exit /b 1
)

:: Run the simulation with timeout
echo Running CloudSim simulation (max %TIMEOUT%s)...

:: Set JVM options for controlled memory usage
set JAVA_OPTS=-Xmx%MAX_MEMORY%

echo Starting simulation...
echo If the simulation hangs, it will be terminated automatically after %TIMEOUT% seconds.

:: Create output directory if it doesn't exist
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

:: Run with timeout protection (Windows doesn't have a built-in timeout command like Linux)
:: Using a PowerShell command to implement timeout functionality
powershell -Command "& {$job = Start-Job -ScriptBlock { cd '%CD%'; java -Xmx%MAX_MEMORY% -cp target/edge-computing-iot-1.0-SNAPSHOT-jar-with-dependencies.jar org.edgecomputing.EdgeComputingDemo '%CONFIG_TYPE%' '%OUTPUT_DIR%' }; if (Wait-Job $job -Timeout %TIMEOUT%) { Receive-Job $job } else { Write-Host 'Simulation terminated after timeout (%TIMEOUT%s)'; Stop-Job $job }; Remove-Job $job -Force }"

:: Check exit status
if %ERRORLEVEL% equ 0 (
    echo Simulation completed successfully within time limit!
) else (
    echo Simulation failed with exit code %ERRORLEVEL%
)

:: Check for results
set RESULT_FILES=0
for /f %%a in ('dir /b /a-d "%OUTPUT_DIR%\*" 2^>nul ^| find /c /v ""') do set RESULT_FILES=%%a

if %RESULT_FILES% gtr 0 (
    echo Results generated: %RESULT_FILES% files in %OUTPUT_DIR%
    
    :: Show summary of results if available
    set SUMMARY_FILE=%OUTPUT_DIR%\summary.json
    if exist "%SUMMARY_FILE%" (
        echo --- SIMULATION RESULTS SUMMARY ---
        type "%SUMMARY_FILE%" | findstr /C:"tasks" | findstr /V /C:"^$"
        echo --------------------------------
    )
) else (
    echo No result files generated in %OUTPUT_DIR%
)

echo ========================================
echo       Simulation Run Complete          
echo ========================================

endlocal
