const http = require('http');
const fs = require('fs');
const path = require('path');

const CONSUL_HOST = process.env.CONSUL_HOST || 'localhost';
const CONSUL_PORT = process.env.CONSUL_PORT || '8500';
const FILE_PATH = process.env.CONSUL_KV_FILE || path.join(__dirname, '..', 'consul-kv.json');

function put(key, value) {
  return new Promise((resolve, reject) => {
    const r = http.request(
      { hostname: CONSUL_HOST, port: CONSUL_PORT, path: `/v1/kv/${key}`, method: 'PUT' },
      (res) => {
        let out = '';
        res.on('data', (c) => (out += c));
        res.on('end', () => resolve({ status: res.statusCode, body: out }));
      },
    );
    r.on('error', reject);
    r.write(value);
    r.end();
  });
}

async function main() {
  let raw = fs.readFileSync(FILE_PATH, 'utf16le');
  raw = raw.replace(/^﻿/, '');
  const entries = JSON.parse(raw);

  for (const item of entries) {
    if (!item.key || item.key.endsWith('/')) continue;
    const value = Buffer.from(item.value, 'base64').toString('utf8');
    const res = await put(item.key, value);
    if (res.status !== 200) {
      console.error(`FAILED ${item.key}: HTTP ${res.status}`);
      process.exitCode = 1;
      continue;
    }
    console.log(`imported ${item.key}`);
  }
}

main().catch((e) => {
  console.error('import failed:', e.message);
  process.exit(1);
});
