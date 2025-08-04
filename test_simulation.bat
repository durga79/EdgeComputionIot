@echo off
setlocal enabledelayedexpansion

:: Colors for better readability
set GREEN=[92m
set RED=[91m
set NC=[0m

echo %GREEN%========================================%NC%
echo %GREEN%  Testing Edge Computing IoT Simulation %NC%
echo %GREEN%========================================%NC%

:: Configuration
set CONFIG_FILE=src\main\resources\configs\realistic_config.json
set OUTPUT_DIR=results\test_%date:~10,4%%date:~4,2%%date:~7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set OUTPUT_DIR=%OUTPUT_DIR: =0%
set MAX_MEMORY=2048m
set TIMEOUT=60

:: Create output directory
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

:: Compile and package
echo %GREEN%Building project...%NC%
call mvn -q clean package -DskipTests

if %ERRORLEVEL% neq 0 (
    echo %RED%Build failed!%NC%
    exit /b 1
)

:: Run simulation
echo %GREEN%Running simulation with realistic configuration...%NC%
echo %GREEN%Output directory: %OUTPUT_DIR%%NC%
echo %GREEN%Timeout: %TIMEOUT% seconds%NC%

:: Set JVM options
set _JAVA_OPTIONS=-Xmx%MAX_MEMORY%

:: Run with timeout using PowerShell
echo %GREEN%Starting simulation...%NC%
powershell -Command "& {$job = Start-Job -ScriptBlock { cd '%CD%'; java -cp target/edge-computing-iot-1.0-SNAPSHOT-jar-with-dependencies.jar org.edgecomputing.EdgeComputingDemo '%CONFIG_FILE%' '%OUTPUT_DIR%' }; if (Wait-Job $job -Timeout %TIMEOUT%) { Receive-Job $job } else { Write-Host 'Simulation terminated after timeout (%TIMEOUT%s)'; Stop-Job $job }; Remove-Job $job -Force }"

:: Check exit status
if %ERRORLEVEL% equ 0 (
    echo %GREEN%Simulation completed successfully!%NC%
) else (
    echo %RED%Simulation failed with exit code %ERRORLEVEL%%NC%
    exit /b 1
)

:: Check for results
if exist "%OUTPUT_DIR%\summary.json" (
    echo %GREEN%========================================%NC%
    echo %GREEN%  Simulation Results Summary%NC%
    echo %GREEN%========================================%NC%
    type "%OUTPUT_DIR%\summary.json"
    
    echo.
    echo %GREEN%========================================%NC%
    echo %GREEN%  Key Metrics%NC%
    echo %GREEN%========================================%NC%
    
    :: Extract key metrics using PowerShell
    powershell -Command "& {$summary = Get-Content '%OUTPUT_DIR%\summary.json' -Raw; if ($summary -match 'Total tasks: (\d+)') { Write-Host \"Total Tasks: $($matches[1])\" }; if ($summary -match 'Tasks executed locally: (\d+) \(([0-9.]+)%\)') { Write-Host \"Tasks executed locally: $($matches[1]) ($($matches[2])%)\" }; if ($summary -match 'Tasks executed on edge: (\d+) \(([0-9.]+)%\)') { Write-Host \"Tasks executed on edge: $($matches[1]) ($($matches[2])%)\" }; if ($summary -match 'Tasks executed on cloud: (\d+) \(([0-9.]+)%\)') { Write-Host \"Tasks executed on cloud: $($matches[1]) ($($matches[2])%)\" }}"
    
    echo.
    echo Resource Utilization:
    powershell -Command "& {$summary = Get-Content '%OUTPUT_DIR%\summary.json' -Raw; if ($summary -match 'Average CPU utilization: ([0-9.]+)%') { Write-Host \"Edge CPU: $($matches[1])%\" }; if ($summary -match 'Average RAM utilization: ([0-9.]+)%') { Write-Host \"Edge RAM: $($matches[1])%\" }; if ($summary -match 'CPU utilization: ([0-9.]+)%') { Write-Host \"Cloud CPU: $($matches[1])%\" }; if ($summary -match 'RAM utilization: ([0-9.]+)%') { Write-Host \"Cloud RAM: $($matches[1])%\" }}"
    
    echo.
    echo %GREEN%Simulation completed successfully!%NC%
    echo %GREEN%Results available in: %OUTPUT_DIR%%NC%
) else (
    echo %RED%No summary.json found in %OUTPUT_DIR%%NC%
    echo %RED%Simulation may have failed or not generated results%NC%
    dir "%OUTPUT_DIR%"
    exit /b 1
)

endlocal
