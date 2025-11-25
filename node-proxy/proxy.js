const util = require('util');

// Polyfill/Silence deprecated util._extend for http-proxy
util._extend = function(origin, add) {
    return Object.assign(origin || {}, add);
};

const http = require('http');
const httpProxy = require('http-proxy');
const fs = require('fs');

const keepAliveAgent = new http.Agent({ keepAlive: true });
const proxy = httpProxy.createProxyServer({ agent: keepAliveAgent });

// Log request after response to capture session ID
proxy.on('proxyRes', function (proxyRes, req, res) {
    let sessionId = null;
    // Try to get from request cookies (JSESSIONID or SESSION)
    if (req.headers.cookie) {
        let match = req.headers.cookie.match(/JSESSIONID=([^;]+)/);
        if (match) {
            sessionId = match[1];
        } else {
            match = req.headers.cookie.match(/SESSION=([^;]+)/);
            if (match) {
                // Decode base64 SESSION cookie to get actual session ID
                try {
                    sessionId = Buffer.from(match[1], 'base64').toString('utf-8');
                } catch (e) {
                    sessionId = match[1];
                }
            }
        }
    }
    // If not, try to get from response cookies
    if (!sessionId && proxyRes.headers['set-cookie']) {
        const setCookies = proxyRes.headers['set-cookie'];
        for (const cookie of setCookies) {
            let match = cookie.match(/JSESSIONID=([^;]+)/);
            if (match) {
                sessionId = match[1];
                break;
            }
            match = cookie.match(/SESSION=([^;]+)/);
            if (match) {
                // Decode base64 SESSION cookie to get actual session ID
                try {
                    sessionId = Buffer.from(match[1], 'base64').toString('utf-8');
                } catch (e) {
                    sessionId = match[1];
                }
                break;
            }
        }
    }
    
    const method = req.method.substring(0, 3).toUpperCase();
    console.log(`${sessionId || 'New'} ${req.targetPort} ${method} ${req.url}`);
});
const servers = [
    { target: 'http://localhost:8081' },
    { target: 'http://localhost:8082' },
    { target: 'http://localhost:8083' }
];

// Simple round-robin counter
let counter = 0;

// Map to store session ID to server index
const sessionMap = new Map();

// Recording state
let recording = false;
let recordedRequests = [];

// Helper function to capture request body
function captureRequestBody(req, callback) {
    let body = [];
    req.on('data', (chunk) => {
        body.push(chunk);
    });
    req.on('end', () => {
        callback(Buffer.concat(body).toString());
    });
}

// Helper function to generate Postman collection
function generatePostmanCollection() {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
    const collection = {
        info: {
            name: `Recorded Collection ${timestamp}`,
            schema: "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
        },
        item: recordedRequests.map((record, index) => {
            const item = {
                name: `${index + 1}. ${record.method} ${record.path}`,
                event: [
                    {
                        listen: "test",
                        script: {
                            exec: [
                                `pm.test("Status code is ${record.response.statusCode}", function () {`,
                                `    pm.response.to.have.status(${record.response.statusCode});`,
                                "});",
                                "setTimeout(function(){}, 2000);"
                            ],
                            type: "text/javascript"
                        }
                    }
                ],
                request: {
                    method: record.method,
                    header: Object.entries(record.headers)
                        .filter(([key]) => key.toLowerCase() !== 'host' && key.toLowerCase() !== 'content-length')
                        .map(([key, value]) => ({ key, value })),
                    url: {
                        raw: `http://localhost:8080${record.path}`,
                        protocol: "http",
                        host: ["localhost"],
                        port: "8080",
                        path: record.path.split('/').filter(p => p)
                    }
                },
                response: []
            };

            // Add body if present
            if (record.body) {
                item.request.body = {
                    mode: "raw",
                    raw: record.body,
                    options: {
                        raw: {
                            language: "json"
                        }
                    }
                };
            }

            return item;
        })
    };

    const filename = `recorded-collection-${timestamp}.json`;
    fs.writeFileSync(filename, JSON.stringify(collection, null, 2));
    return filename;
}

const server = http.createServer((req, res) => {
    // Handle /newtest endpoint
    if (req.url === '/newtest') {
        recording = true;
        recordedRequests = [];
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Recording started', status: 'success' }));
        console.log('Recording started');
        return;
    }

    // Handle /endtest endpoint
    if (req.url === '/endtest') {
        recording = false;
        const filename = generatePostmanCollection();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ 
            message: 'Recording stopped and saved', 
            filename: filename,
            requestCount: recordedRequests.length,
            status: 'success' 
        }));
        console.log(`Recording stopped. Saved ${recordedRequests.length} requests to ${filename}`);
        return;
    }

    let targetServer;
    
    // Check for JSESSIONID in cookies
    const cookies = req.headers.cookie;
    let sessionId = null;
    
    if (cookies) {
        const match = cookies.match(/JSESSIONID=([^;]+)/);
        if (match) {
            sessionId = match[1];
        }
    }
        
    targetServer = servers[counter % servers.length];
    counter++;

    const targetUrl = targetServer.target;
    const targetPort = targetUrl.split(':')[2];
    res.setHeader('SERVER_PORT', targetPort);

    req.targetPort = targetPort;

    // If recording, capture the request
    if (recording) {
        const requestRecord = {
            method: req.method,
            path: req.url,
            headers: { ...req.headers },
            timestamp: new Date().toISOString()
        };

        // Capture request body if present
        if (req.method === 'POST' || req.method === 'PUT' || req.method === 'PATCH') {
            let body = [];
            req.on('data', (chunk) => {
                body.push(chunk);
            });
            req.on('end', () => {
                requestRecord.body = Buffer.concat(body).toString();
            });
        }

        // Intercept the response
        const originalProxyRes = proxy.web.bind(proxy);
        
        // Store original response methods
        const originalWrite = res.write.bind(res);
        const originalEnd = res.end.bind(res);
        const originalWriteHead = res.writeHead.bind(res);
        
        let responseBody = [];
        let statusCode = 200;

        // Override writeHead to capture status code
        res.writeHead = function(code, headers) {
            statusCode = code;
            requestRecord.response = {
                statusCode: code,
                headers: headers || {}
            };
            return originalWriteHead(code, headers);
        };

        // Override write to capture response body
        res.write = function(chunk) {
            if (chunk) {
                responseBody.push(Buffer.from(chunk));
            }
            return originalWrite(chunk);
        };

        // Override end to capture final response and save record
        res.end = function(chunk) {
            if (chunk) {
                responseBody.push(Buffer.from(chunk));
            }
            
            if (!requestRecord.response) {
                requestRecord.response = {
                    statusCode: res.statusCode || 200,
                    headers: res.getHeaders()
                };
            }
            
            requestRecord.response.body = Buffer.concat(responseBody).toString();
            recordedRequests.push(requestRecord);
            console.log(`Recorded request ${recordedRequests.length}: ${req.method} ${req.url}`);
            
            return originalEnd(chunk);
        };
    }

    proxy.web(req, res, { target: targetUrl }, (err) => {
        console.error('Proxy error:', err);
        res.statusCode = 500;
        res.end('Proxy error');
    });
});

console.log('Proxy listening on port 8080');
console.log('Special endpoints:');
console.log('  GET /newtest  - Start recording requests');
console.log('  GET /endtest  - Stop recording and save Postman collection');
server.listen(8080);
