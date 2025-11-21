const http = require('http');
const httpProxy = require('http-proxy');

const keepAliveAgent = new http.Agent({ keepAlive: true });
const proxy = httpProxy.createProxyServer({ agent: keepAliveAgent });
const servers = [
    { target: 'http://localhost:8081' },
    { target: 'http://localhost:8082' },
    { target: 'http://localhost:8083' }
];

// Simple round-robin counter
let counter = 0;

// Map to store session ID to server index
const sessionMap = new Map();

const server = http.createServer((req, res) => {
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

    console.log(`Proxying request to ${targetUrl} (Session: ${sessionId || 'New'})`);

    proxy.web(req, res, { target: targetUrl }, (err) => {
        console.error('Proxy error:', err);
        res.statusCode = 500;
        res.end('Proxy error');
    });
});

console.log('Proxy listening on port 8080');
server.listen(8080);
