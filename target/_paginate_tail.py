import fitz, re
from pathlib import Path

golden = Path(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_SI10002_AI Data Center Market_Report.pdf")
current = Path(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_Report (17).pdf")

def markers(path, start, end):
    doc = fitz.open(path)
    print(f'\n=== {path.name} {start}-{end} ===')
    for i in range(start-1, min(end, len(doc))):
        ls = [ln.strip() for ln in doc[i].get_text('text').splitlines() if ln.strip()]
        ms = []
        for ln in ls:
            u = ln.upper()
            if u.startswith(('CHAPTER','FIGURE','TABLE','SOURCE','NOTE','*THE DELIVERABLE')) or re.match(r'^\d+(\.\d+)+(\s|$)', ln) or re.match(r'^\d+\.\d+(\.\d+)*\s', ln):
                ms.append(ln[:120])
        print(f'p{i+1}:')
        for m in ms:
            print(' ', m)
        if not ms:
            print('  (no markers) content=', ls[:5])
    doc.close()

markers(golden, 103, 128)
markers(current, 131, 152)
markers(current, 100, 130)
