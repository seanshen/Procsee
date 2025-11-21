# Distributed Session Demo Walkthrough

This walkthrough guides you through running and testing the distributed session demo.

## Prerequisites
- Docker & Docker Compose
- Java 17+ & Maven
- Node.js & npm
- Postman

## Architecture Overview
This demo demonstrates distributed session management using:
- **3 Spring Boot servers** (ports 8081, 8082, 8083) sharing sessions via Redis
- **Node.js reverse proxy** (port 8080) with round-robin routing and keep-alive connections
- **Redis** for centralized session storage

## 1. Start Infrastructure (Redis)
Start the Redis container using Docker Compose:

```bash
cd /Users/xiangshen/workspace/Procsee
docker-compose up -d
```

## 2. Start Java Web Servers
Run the script to start 3 instances of the Spring Boot application (ports 8081, 8082, 8083):

```bash
cd /Users/xiangshen/workspace/Procsee/java-session-demo
chmod +x run_servers.sh
./run_servers.sh
```

The script will:
- Build the application with Maven
- Kill any existing processes on ports 8081, 8082, 8083
- Start 3 server instances in the background

Check `server1.log`, `server2.log`, and `server3.log` to ensure they started correctly.

> **Note for Windows users**: The script includes commented PowerShell commands for Windows 11. Uncomment and use those instead of the bash commands.

## 3. Start Node.js Reverse Proxy
Install dependencies and start the proxy server (port 8080):

```bash
cd /Users/xiangshen/workspace/Procsee/node-proxy
npm install
npm start
```

The proxy features:
- Round-robin load balancing across the 3 servers
- Keep-alive connections for better performance
- Custom headers: `SERVER_PORT` and `session-id`

## 4. Run Verification Tests

### Using Postman
1. Open Postman
2. Import the collection file: `/Users/xiangshen/workspace/Procsee/postman/distributed-session.postman_collection.json`
3. Run the collection using the Collection Runner

### Expected Results
Each request includes a 2-second delay for easier observation.

- **Request 1 - Set testAttr = 100**
  - Sets `testAttr` to 100
  - Returns session ID in response body and `session-id` header
  - Test results show: Status 200, Session ID, SERVER_PORT value, and session-id
  - Visualize tab displays all response data in a table

- **Request 2 - Get testAttr (Expect 100)**
  - Retrieves `testAttr` value (should be 100)
  - May be served by a different server port (proves session sharing via Redis)
  - Same session ID confirms session persistence

- **Request 3 - Set testAttr = 200**
  - Updates `testAttr` to 200
  - Same session continues

- **Request 4 - Get testAttr (Expect 200)**
  - Retrieves updated `testAttr` value (should be 200)
  - Confirms session data is shared across all servers

### Observing the Results
- **Test Results Panel**: Shows test names including SERVER_PORT values and session IDs
- **Console** (View → Show Postman Console): Displays full session-id and SERVER_PORT values
- **Visualize Tab**: Shows a formatted table with:
  - Session ID
  - Test Attribute value
  - Server Port (from response body)
  - Server Port (from HTTP header)

### Key Features Demonstrated
1. **Session Persistence**: Same session ID across all requests
2. **Load Balancing**: Different server ports may handle different requests
3. **Distributed Session**: Session data accessible from any server instance
4. **Custom Headers**: `SERVER_PORT` and `session-id` headers for debugging

## Cleanup
To stop the servers:

```bash
# Stop Java servers
lsof -ti:8081,8082,8083 | xargs kill -9

# Stop Proxy (if running in background)
lsof -ti:8080 | xargs kill -9

# Stop Redis
docker-compose down
```

## Troubleshooting

### Servers won't start
- Check if ports 8081, 8082, 8083 are already in use
- Review the server log files for error messages

### Proxy connection issues
- Ensure all 3 Java servers are running
- Check that Redis is running: `docker ps`
- Verify proxy is listening on port 8080: `lsof -i :8080`

### Session not persisting
- Confirm Redis container is running
- Check Redis connection in server logs
- Verify `application.properties` has correct Redis configuration
