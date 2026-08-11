import fitz
from pathlib import Path
# Confirm blank page before Ch2 and Ch6 figure split in current; golden table page after intro
cur=fitz.open(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_Report (17).pdf")
gol=fitz.open(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_SI10002_AI Data Center Market_Report.pdf")
for label,doc,pages in [('CUR',cur,[25,26,27,28,29,30,31]),('GOL',gol,[30,31,32,33,34])]:
    print('====',label)
    for p in pages:
        t=doc[p-1].get_text('text')
        # non-header body chars
        body='\n'.join(ln for ln in t.splitlines() if ln.strip() and 'Category:' not in ln and 'SPHERICAL' not in ln.upper() and not ln.strip().isdigit() and 'Back to Top' not in ln and 'Copyright' not in ln and 'JAPAN WINE MARKET, 2019' not in ln and 'GLOBAL AI DATA CENTER MARKET, 2019' not in ln)
        print(f'p{p} body_len={len(body.strip())} first={body.strip().splitlines()[:2]}')
