const https = require('https');

const options = {
    hostname: 'marcus-mattress-tions-versus.trycloudflare.com',
    port: 443,
    path: '/api/chittas',
    method: 'OPTIONS',
    headers: {
        'Origin': 'https://nivash005.github.io',
        'Access-Control-Request-Method': 'POST',
        'Access-Control-Request-Headers': 'Bypass-Tunnel-Reminder, Content-Type'
    }
};

const req = https.request(options, (res) => {
    console.log('Tunnel OPTIONS status:', res.statusCode);
    console.log('Tunnel OPTIONS headers:', res.headers);
});
req.on('error', e => console.error(e));
req.end();
