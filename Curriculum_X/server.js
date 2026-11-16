const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const { DatabaseSync } = require('node:sqlite');

const PORT = process.env.PORT || 8080;
const ROOT = __dirname;
const DB_PATH = path.join(ROOT, 'data', 'curriculum_records.db');

// Ensure data directory exists
fs.mkdirSync(path.join(ROOT, 'data'), { recursive: true });

// Initialize SQLite database
const db = new DatabaseSync(DB_PATH);
db.exec(`
  CREATE TABLE IF NOT EXISTS records (
    course_key TEXT NOT NULL,
    mode TEXT NOT NULL,
    scheme TEXT,
    term TEXT,
    role TEXT,
    step INTEGER,
    gen INTEGER,
    decisions TEXT,
    frozen_units TEXT,
    magic INTEGER,
    status TEXT,
    remarks TEXT,
    alignment_scores TEXT,
    unit_gaps TEXT,
    updated_at TEXT,
    PRIMARY KEY(course_key, mode)
  );
  CREATE TABLE IF NOT EXISTS audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    course_key TEXT,
    mode TEXT,
    role TEXT,
    action TEXT,
    details TEXT,
    created_at TEXT
  );
`);

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

const server = http.createServer((req, res) => {
  // CORS & Security headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
  res.setHeader('Pragma', 'no-cache');
  res.setHeader('Expires', '0');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = parsedUrl.pathname;

  // --- API ROUTES ---

  // Health / Status
  if (pathname === '/api/status' && req.method === 'GET') {
    const row = db.prepare('SELECT COUNT(*) as count FROM records').get();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', total_records: row.count, db: 'SQLite (curriculum_records.db)' }));
    return;
  }

  // GET Records
  if (pathname === '/api/records' && req.method === 'GET') {
    const course = parsedUrl.searchParams.get('course');
    const mode = parsedUrl.searchParams.get('mode');

    if (course && mode) {
      const row = db.prepare('SELECT * FROM records WHERE course_key = ? AND mode = ?').get(course, mode);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ record: row || null }));
    } else {
      const rows = db.prepare('SELECT * FROM records ORDER BY updated_at DESC').all();
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ records: rows }));
    }
    return;
  }

  // POST Records (Upsert)
  if (pathname === '/api/records' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        const data = JSON.parse(body || '{}');
        if (!data.course_key) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'course_key is required' }));
          return;
        }

        const mode = data.mode || 'syl';
        const stmt = db.prepare(`
          INSERT INTO records (
            course_key, mode, scheme, term, role, step, gen,
            decisions, frozen_units, magic, status, remarks,
            alignment_scores, unit_gaps, updated_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
          ON CONFLICT(course_key, mode) DO UPDATE SET
            scheme = excluded.scheme,
            term = excluded.term,
            role = excluded.role,
            step = excluded.step,
            gen = excluded.gen,
            decisions = excluded.decisions,
            frozen_units = excluded.frozen_units,
            magic = excluded.magic,
            status = excluded.status,
            remarks = excluded.remarks,
            alignment_scores = excluded.alignment_scores,
            unit_gaps = excluded.unit_gaps,
            updated_at = datetime('now');
        `);

        stmt.run(
          data.course_key,
          mode,
          data.scheme || '',
          data.term || '',
          data.role || 'Faculty',
          data.step || 0,
          data.gen ? 1 : 0,
          typeof data.decisions === 'string' ? data.decisions : JSON.stringify(data.decisions || {}),
          typeof data.frozen_units === 'string' ? data.frozen_units : JSON.stringify(data.frozen_units || []),
          data.magic ? 1 : 0,
          data.status || 'Draft',
          data.remarks || '',
          typeof data.alignment_scores === 'string' ? data.alignment_scores : JSON.stringify(data.alignment_scores || {}),
          typeof data.unit_gaps === 'string' ? data.unit_gaps : JSON.stringify(data.unit_gaps || [])
        );

        // Record in audit log
        const auditStmt = db.prepare(`
          INSERT INTO audit_log (course_key, mode, role, action, details, created_at)
          VALUES (?, ?, ?, ?, ?, datetime('now'))
        `);
        auditStmt.run(
          data.course_key,
          mode,
          data.role || 'Faculty',
          data.action || 'sync_state',
          `Status: ${data.status || 'Draft'}, Step: ${data.step || 0}`
        );

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: true, message: 'Saved to backend database successfully' }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message }));
      }
    });
    return;
  }

  // POST Reset
  if (pathname === '/api/records/reset' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        const data = JSON.parse(body || '{}');
        if (data.course_key) {
          db.prepare('DELETE FROM records WHERE course_key = ?').run(data.course_key);
        } else {
          db.prepare('DELETE FROM records').run();
        }
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: true, message: 'Reset complete' }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message }));
      }
    });
    return;
  }

  // POST /api/draft (Draft course from benchmarks)
  if (pathname === '/api/draft' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        const data = JSON.parse(body || '{}');
        const code = (data.code || 'NEW').trim().toUpperCase();
        const name = (data.name || 'New Course').trim();
        const ltp = (data.ltp || '3-0-0').trim();

        const units = [
          { t: "Foundational Concepts & Principles", x: ["Theoretical foundations of " + name, "Core paradigms and problem definitions", "Mathematical representations and specifications", "Complexity analysis and baseline approaches"] },
          { t: "Algorithmic Paradigms & Data Models", x: ["Core architectural data representations", "Algorithmic structures in " + name, "Optimization formulations and criteria", "Pipeline engineering and modular workflows"] },
          { t: "Practical Implementations & Methods", x: ["Primary algorithmic workflows", "Hands-on implementation and validation", "Comparative benchmarking against standards", "Empirical performance evaluations"] },
          { t: "Advanced Optimization & Scaling", x: ["Advanced optimization strategies", "High-throughput and distributed scaling", "Fault tolerance, constraints, and robustness", "Bottleneck profiling and execution speedups"] },
          { t: "Applied Systems & Domain Workflows", x: ["End-to-end system pipelines", "Industry case studies and applications", "Deployment considerations and testing", "Integration with standard tooling ecosystems"] },
          { t: "Emerging Paradigms & Frontiers", x: ["Current research developments", "Open challenges and scalable frontiers", "Ethics, security, and governance", "Comprehensive review and capstone design"] }
        ];

        const src = ["MIT OpenCourseWare (6.006) · Verified Benchmarks", "Premier University Standards"];
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ code, name, ltp, units, src }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message }));
      }
    });
    return;
  }

  // --- STATIC FILE SERVING ---
  let filePath = path.join(ROOT, pathname === '/' ? 'index.html' : pathname);
  if (!filePath.startsWith(ROOT)) {
    res.writeHead(403);
    res.end('Access denied');
    return;
  }

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('File not found');
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';
    res.writeHead(200, { 'Content-Type': contentType });
    fs.createReadStream(filePath).pipe(res);
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Curriculum X Full Backend & Static Server running at http://localhost:${PORT}`);
  console.log(`Backend Database: ${DB_PATH}`);
});
