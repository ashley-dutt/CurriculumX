# Curriculum X (CDR: Curriculum Development and Revision Automation System)

## Files
- index.html: the whole app (single file). Open it in a browser.
- data/courses.json: the six courses used (CSE408, INT373, INT395, INT396, PEA306, PES390), unit by unit, taken from the 2026-27 syllabus PDFs.
- data/rag_corpus.json: the retrieval index (units, lecture-plan topics, outcomes, practicals).

## Flow
Login (Faculty or HOD) -> choose Syllabus or Instruction plan -> course, scheme, term -> Generate -> Syllabus -> Course table -> Coverage -> Review (Add / Replace / Ignore, Freeze each unit) -> Magic display -> Submit -> HOD dashboard (benchmarking report, PDF download, recommend or request correction).

## Standalone & AI Execution
- **Offline / Local browser execution:** `index.html` runs out of the box in any browser. It includes a built-in benchmark dataset across all 6 courses for both Syllabus and Instruction Plan modes, verified via BM25 retrieval over the LPU corpus.
- **Claude Artifact support:** When run inside a Claude artifact environment (`window.claude`), it automatically utilizes the live LLM API and artifact download provider.
- **PDF Download:** Generated using jsPDF directly in the browser (`doc.save()`), with fallback to artifact downloads when available.

## Known limits
- University scores are AI estimates, not measured against real MIT, Stanford, CMU or NUS syllabi.
- Data is kept in memory only; reloading the page resets it.
