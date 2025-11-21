#!/bin/bash

# Build the project
mvn clean package -DskipTests

# Kill existing processes on ports 8081, 8082, 8083
lsof -ti:8081,8082,8083 | xargs kill -9 2>/dev/null
# Windows PowerShell: Get-NetTCPConnection -LocalPort 8081,8082,8083 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }

# Start 3 instances
nohup java -jar target/demo-0.0.1-SNAPSHOT.jar --server.port=8081 > server1.log 2>&1 &
echo "Started server on 8081"
# Windows PowerShell: Start-Process -FilePath "java" -ArgumentList "-jar", "target/demo-0.0.1-SNAPSHOT.jar", "--server.port=8081" -RedirectStandardOutput "server1.log" -RedirectStandardError "server1.log" -NoNewWindow
# Write-Host "Started server on 8081"

nohup java -jar target/demo-0.0.1-SNAPSHOT.jar --server.port=8082 > server2.log 2>&1 &
echo "Started server on 8082"
# Windows PowerShell: Start-Process -FilePath "java" -ArgumentList "-jar", "target/demo-0.0.1-SNAPSHOT.jar", "--server.port=8082" -RedirectStandardOutput "server2.log" -RedirectStandardError "server2.log" -NoNewWindow
# Write-Host "Started server on 8082"

nohup java -jar target/demo-0.0.1-SNAPSHOT.jar --server.port=8083 > server3.log 2>&1 &
echo "Started server on 8083"
# Windows PowerShell: Start-Process -FilePath "java" -ArgumentList "-jar", "target/demo-0.0.1-SNAPSHOT.jar", "--server.port=8083" -RedirectStandardOutput "server3.log" -RedirectStandardError "server3.log" -NoNewWindow
# Write-Host "Started server on 8083"
