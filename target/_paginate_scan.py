import fitz, re
from pathlib import Path

golden = Path(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_SI10002_AI Data Center Market_Report.pdf")
current = Path(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_Report (17).pdf")

CHAPTER_RE = re.compile(r'^CHAPTER\s+\d+', re.I)
SECTION_RE = re.compile(r'^(\d+(?:\.\d+)*)\s+')

def page_lines(doc, i):
    text = doc[i].get_text('text')
    return [ln.strip() for ln in text.splitlines() if ln.strip()]

def find_starts(path):
    doc = fitz.open(path)
    starts = []
    for i in range(len(doc)):
        lines = page_lines(doc, i)
        for ln in lines[:20]:
            up = ln.upper()
            if CHAPTER_RE.match(ln) or up.startswith('TABLE OF CONTENTS') or up.startswith('LIST OF FIGURES') or up.startswith('LIST OF TABLES') or up == 'ABOUT US' or up.startswith('DISCLAIMER'):
                starts.append((i+1, ln[:140]))
                break
    return len(doc), starts, doc

for label, path in [('GOLDEN', golden), ('CURRENT', current)]:
    n, starts, doc = find_starts(path)
    print(f'=== {label} pages={n} ===')
    for p,t in starts:
        print(f'  p{p}: {t}')
    doc.close()
    print()
