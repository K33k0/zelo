import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
const root = path.resolve('app/src/main/assets/www');
http
  .createServer((req, res) => {
    const file = path.resolve(
      root,
      '.' +
        (new URL(req.url, 'http://localhost').pathname === '/'
          ? '/index.html'
          : new URL(req.url, 'http://localhost').pathname),
    );
    if (!file.startsWith(root + path.sep)) {
      res.writeHead(403).end();
      return;
    }
    fs.readFile(file, (err, data) => {
      if (err) {
        res.writeHead(404).end();
        return;
      }
      res.setHeader(
        'Content-Type',
        { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript' }[
          path.extname(file)
        ] || 'application/octet-stream',
      );
      res.end(data);
    });
  })
  .listen(4173, '127.0.0.1', () => console.log('Stride preview: http://127.0.0.1:4173'));
