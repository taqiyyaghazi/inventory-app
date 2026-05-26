const fs = require('fs');
const b64 = fs.readFileSync('../debug.keystore.base64', 'utf8');
const buf = Buffer.from(b64.trim(), 'base64');
fs.writeFileSync('../debug.keystore', buf);
console.log('Decoded successfully');
