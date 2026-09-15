"""Run against the local fixture only; start the app first."""
import json
import subprocess
from urllib.parse import urlencode

attack = "' OR 1=1 -- "
cases = {
    "direct": ["Budget", "Payroll", "O'Brien"],
    "alias": ["Budget", "Payroll", "O'Brien"],
    "wrapped": ["Budget", "Payroll", "O'Brien"],
    "bound": [], "repaired": [], "constant": ["Budget"],
}
for mode, expected in cases.items():
    url = "http://127.0.0.1:8093/search/" + mode + "?" + urlencode({"title": attack})
    reply = subprocess.run(["curl", "-fsS", url],
                           check=True, capture_output=True, text=True).stdout
    assert json.loads(reply) == expected, (mode, reply)
    print(f"PASS {mode}: {reply}")
