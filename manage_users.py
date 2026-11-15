"""Add / list / remove users. Passwords are stored as salted scrypt hashes.
   python manage_users.py add faculty1@yourdomain.edu "Dr Name" Faculty "CSE"
   python manage_users.py add hod.cse@yourdomain.edu "Prof Name" HOD "CSE"
   python manage_users.py list | passwd EMAIL | remove EMAIL"""
import sys, time, getpass, sqlite3
from werkzeug.security import generate_password_hash
import app as A

def conn(): return sqlite3.connect(A.DB_PATH)
def newpw():
    p = getpass.getpass("Password (min 10 chars): ")
    if len(p) < 10 or p != getpass.getpass("Repeat: "): sys.exit("Passwords did not match or are too short.")
    return generate_password_hash(p)

a = sys.argv[1:]
if not a: sys.exit(__doc__)
c = conn()
if a[0] == "add" and len(a) == 5:
    email, name, role, dept = a[1].lower(), a[2], a[3], a[4]
    if role not in ("Faculty", "HOD"): sys.exit("Role must be Faculty or HOD")
    if A.ALLOWED_DOMAIN and not email.endswith("@" + A.ALLOWED_DOMAIN): sys.exit("Email must end with @" + A.ALLOWED_DOMAIN)
    c.execute("INSERT OR REPLACE INTO users VALUES(?,?,?,?,?,?)", (email, A.clean(name, 80), role, A.clean(dept, 40), newpw(), time.time())); c.commit(); print("Saved", email)
elif a[0] == "list":
    for r in c.execute("SELECT email,name,role,dept FROM users ORDER BY dept,role,email"): print(*r, sep=" | ")
elif a[0] == "passwd" and len(a) == 2:
    c.execute("UPDATE users SET pw_hash=? WHERE email=?", (newpw(), a[1].lower())); c.commit(); print("Updated")
elif a[0] == "remove" and len(a) == 2:
    c.execute("DELETE FROM users WHERE email=?", (a[1].lower(),)); c.commit(); print("Removed")
else: sys.exit(__doc__)
