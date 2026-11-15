# Curriculum X (CDR: Curriculum Development and Revision Automation System)

A secure, source-grounded curriculum development and benchmarking automation platform aligned against Top 10 Worldwide Universities (MIT, Stanford, Harvard, Cambridge, Oxford, UC Berkeley, CMU, ETH Zurich, NUS, Imperial College) and Top 10 Indian Institutes (IIT Madras, IIT Delhi, IIT Bombay, IIT Kanpur, IIT Kharagpur, IIT Roorkee, IIT Guwahati, IISc Bangalore, IIT Hyderabad, BITS Pilani).

## Key Features
- **Full Curriculum Benchmarking:** Real-time alignment breakdown across Top 10 Worldwide and Top 10 Indian institutes.
- **Interactive 1-2 Page Official Syllabus Viewer:** Click any university name to inspect its official accredited syllabus, prerequisites, textbooks, modules, and gap recommendations.
- **Dynamic Course Drafting:** Add new courses dynamically (`＋ Add new course`) synthesized and validated against verified academic benchmarks.
- **Department Review & HOD Workflow:** Unit-by-unit freeze, additions, replacements, ignore tracking, HOD approval submission, and PDF report export.
- **Dual Runtime Support:**
  - **Node.js (Default, Zero-dependency):** Run instantly with Node.js built-ins (`node:http`, `node:sqlite`).
  - **Python / Flask (Secure Edition):** Multi-user authentication, salted scrypt password hashing, department isolation, audit logging, and anti-hallucination guardrails.
- **Offline & Bundled:** Works offline out of the box with bundled `static/jspdf.umd.min.js`.

---

## 1. Quick Start (Node.js)

Zero dependencies needed (uses built-in Node.js v18+ & SQLite):

```bash
# Start server
node server.js

# Or inside Curriculum_X
cd Curriculum_X
node server.js
```
Open **[http://localhost:8080](http://localhost:8080)** in your browser.

---

## 2. Quick Start (Python Secure Edition)

```bash
# Install dependencies
pip install -r requirements.txt

# Create initial users (stored as salted scrypt hashes)
python manage_users.py add fac1@lpu.in "Dr. Sharma" Faculty CSE
python manage_users.py add hod.cse@lpu.in "Prof. Verma" HOD CSE

# Start server
python app.py
```
Open **[http://localhost:8000](http://localhost:8000)** in your browser.

### Self-Check & Smoke Tests
Run the 26 automated tests (no API key or internet needed):
```bash
python tests/smoke_test.py
```

---

## 3. Project Structure
- `index.html`: Core single-page curriculum development application with full UI and university modals.
- `server.js`: Node.js backend server with SQLite persistence (`curriculum_records.db`).
- `app.py`: Flask secure backend with department isolation, CSRF protection, and audit logs.
- `data/courses.json`: LPU course catalog across 6 foundational computing & professional courses.
- `data/benchmarks.json`: Grounded and cited benchmark topics from official university syllabi (MIT OpenCourseWare, etc.).
- `data/rag_corpus.json`: Retrieval corpus for topic verification.
- `data/curriculum_records.db`: Persistent SQLite database storing decisions, frozen units, and schemes.
- `static/jspdf.umd.min.js`: Bundled client-side PDF export library.
- `tools/ingest_source.py`: Admin utility to ingest new syllabus source files into verified benchmarks.
- `manage_users.py`: CLI tool for managing user accounts and roles.
- `tests/smoke_test.py`: Comprehensive security and functional test suite.
