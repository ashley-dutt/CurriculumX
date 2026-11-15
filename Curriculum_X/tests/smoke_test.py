"""Run:  python tests/smoke_test.py   (uses a temporary database; no Gemini key or internet needed)"""
import os, sys, tempfile, json
os.environ["CDR_DB"] = os.path.join(tempfile.mkdtemp(), "t.db"); os.environ["ALLOWED_DOMAIN"] = "uni.edu"; os.environ["GEMINI_API_KEY"] = ""
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import app as A
from werkzeug.security import generate_password_hash as gh
import sqlite3, time
c = sqlite3.connect(A.DB_PATH)
for e, r, d in [("fac@uni.edu", "Faculty", "CSE"), ("fac2@uni.edu", "Faculty", "ECE"), ("hod@uni.edu", "HOD", "CSE"), ("hod2@uni.edu", "HOD", "ECE")]:
    c.execute("INSERT INTO users VALUES(?,?,?,?,?,?)", (e, e, r, d, gh("correct-horse-1"), time.time()))
c.commit()
ok = fail = 0
def check(name, cond):
    global ok, fail
    print(("PASS " if cond else "FAIL ") + name); ok += bool(cond); fail += (not cond)

def login(email, role, pw="correct-horse-1"):
    cl = A.app.test_client(); r = cl.post("/api/login", json={"email": email, "password": pw, "role": role}); return cl, r
def post(cl, path, body, tok): return cl.post(path, json=body, headers={"X-CSRF-Token": tok})

cl, r = login("fac@uni.edu", "Faculty"); tok = r.get_json().get("csrf", "")
check("login ok", r.status_code == 200 and tok)
check("wrong password rejected", login("fac@uni.edu", "Faculty", "nope")[1].status_code == 401)
check("wrong role rejected", login("fac@uni.edu", "HOD")[1].status_code == 401)
check("unauthenticated API blocked", A.app.test_client().get("/api/submissions").status_code == 401)
check("CSRF missing blocked", cl.post("/api/benchmark", json={"units": [{"t": "x", "x": ["y"]}]}).status_code == 403)
check("cross-site Origin blocked", cl.post("/api/benchmark", json={}, headers={"X-CSRF-Token": tok, "Origin": "https://evil.example"}).status_code == 403)
h = cl.get("/").headers
check("security headers set", "frame-ancestors 'none'" in h["Content-Security-Policy"] and h["X-Content-Type-Options"] == "nosniff")
for _ in range(6): login("fac@uni.edu", "Faculty", "bad")
check("login lockout after repeated failures", login("fac@uni.edu", "Faculty")[1].status_code == 429)
A._hits.clear(); cl, r = login("fac@uni.edu", "Faculty"); tok = r.get_json()["csrf"]

units = [{"t": "Dynamic Programming", "x": ["Introduction to dynamic programming", "Knapsack problem", "Matrix-chain multiplication"]},
         {"t": "Graph Algorithms", "x": ["Breadth-first search", "Depth-first search", "Topological sort"]}]
b = post(cl, "/api/benchmark", {"course": "CSE408", "mode": "syl", "units": units}, tok).get_json()
texts = {t["text"] for t in A.CORPUS}
flat = [g for u in b["units"] for g in u]
check("ranking mode works with no Gemini key", b["meta"]["mode"] == "ranking" and flat)
check("every suggested topic is verbatim from a cited source", all(g[0] in texts and g[5].startswith("https://") for g in flat))
check("topics already in the course are not suggested", not any("Breadth-first" in g[0] or "Depth-first" in g[0] for g in flat))
print("   sample:", [(g[0], g[2]) for g in flat][:3], b["a"])
nb = post(cl, "/api/benchmark", {"course": "PES390", "mode": "syl", "units": [{"t": "Interview Practice", "x": ["STAR+ methodology", "Cultural intelligence"]}]}, tok).get_json()
check("no matching benchmark -> no suggestions, clear note", nb["meta"]["nobench"] and not any(nb["units"]) and "No verified" in nb["meta"]["note"])

# Gemini path with a hallucinating model: invented ids / out-of-range indexes must be discarded
A.GEMINI_KEY = "test"; A._cache.clear()
real_id = flat[0][0]; rid = next(t["id"] for t in A.CORPUS if t["text"] == real_id)
def fake(prompt, schema, user): return {"units": [{"unit": 0, "picks": [
    {"id": "MIT-FAKE-999", "replace_index": 0, "reason": "invented topic <script>alert(1)</script>"},
    {"id": rid, "replace_index": 99, "reason": "bad index"},
    {"id": rid, "replace_index": 1, "reason": "Real <b>id</b> with a valid index"}]}, {"unit": 1, "picks": []}]}
A.gemini_json = fake
g = post(cl, "/api/benchmark", {"course": "CSE408b", "mode": "syl", "units": units}, tok).get_json()
got = [x for u in g["units"] for x in u]
check("Gemini mode used", g["meta"]["mode"] == "gemini")
check("hallucinated id and bad index discarded", all(x[0] in texts for x in got) and len(got) == 1)
check("model text is sanitised", "<" not in got[0][3] and ">" not in got[0][3])
def boom(*a, **k): raise RuntimeError("quota")
A.gemini_json = boom; A._cache.clear()
g2 = post(cl, "/api/benchmark", {"course": "CSE408c", "mode": "syl", "units": units}, tok).get_json()
check("Gemini failure falls back to ranking", g2["meta"]["mode"] == "ranking" and "unavailable" in g2["meta"]["note"])

d = post(cl, "/api/draft", {"code": "CSE999", "name": "Quantum Basket Weaving", "ltp": "3-0-0"}, tok)
check("draft refuses unsupported subject (no invention)", d.status_code == 422)
d = post(cl, "/api/draft", {"code": "CSE410", "name": "Dynamic programming and graph algorithms sorting", "ltp": "3-0-0"}, tok)
dj = d.get_json(); check("draft built only from sources", d.status_code == 200 and all(x in texts for u in dj["units"] for x in u["x"]))

course = {"n": "T", "ltp": "3-0-2", "u": [{"t": "U1<img onerror=x>", "x": ["a"], "g": [["Dijkstra", "a", "MIT 6.006 · Lecture 16", "why", "", "https://ocw.mit.edu/x"]]}]}
bad = post(cl, "/api/submit", {"key": "K", "term": "T", "mode": "syl", "course": course, "dec": {"0-0": "Added"}, "frozen": []}, tok)
check("cannot submit with unfrozen units", bad.status_code == 400)
s = post(cl, "/api/submit", {"key": "K", "term": "T", "mode": "syl", "course": course, "dec": {"0-0": "Added", "x": "<b>"}, "frozen": [0]}, tok); sid = s.get_json()["id"]
check("submit ok", s.status_code == 200)
cl2, r2 = login("hod@uni.edu", "HOD"); t2 = r2.get_json()["csrf"]
lst = cl2.get("/api/submissions").get_json()["items"]; check("HOD (same dept) sees submission", any(i["id"] == sid for i in lst))
sub = cl2.get(f"/api/submission/{sid}").get_json()
check("stored data sanitised", "<" not in json.dumps(sub["course"]) and list(sub["dec"]) == ["0-0"])
cl3, r3 = login("hod2@uni.edu", "HOD"); check("HOD of another dept cannot see it", cl3.get(f"/api/submission/{sid}").status_code == 404)
check("faculty cannot make decisions", post(cl, "/api/decision", {"id": sid, "decision": "Correction requested"}, tok).status_code == 403)
dd = post(cl2, "/api/decision", {"id": sid, "decision": "Correction requested", "remarks": "Please add shortest paths"}, t2)
check("HOD decision saved", dd.status_code == 200)
mine = cl.get("/api/submissions").get_json()["items"][0]; check("faculty sees status and remarks", mine["status"] == "Correction requested" and "shortest" in mine["remarks"])
print(f"\n{ok} passed, {fail} failed"); sys.exit(1 if fail else 0)
