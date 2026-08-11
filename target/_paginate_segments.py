import fitz, re
from pathlib import Path

golden = Path(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_SI10002_AI Data Center Market_Report.pdf")
current = Path(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_Report (17).pdf")

def lines(doc, i):
    return [ln.strip() for ln in doc[i].get_text('text').splitlines() if ln.strip()]

def summarize_range(path, start, end, label):
    doc = fitz.open(path)
    print(f'\n##### {label} pages {start}-{end} #####')
    for i in range(start-1, min(end, len(doc))):
        ls = lines(doc, i)
        # keep meaningful content lines, drop header/footer-ish
        keep = []
        for ln in ls:
            u = ln.upper()
            if 'SPHERICAL INSIGHTS' in u and len(ln)<40: continue
            if u.startswith('COPYRIGHT'): continue
            if u == 'BACK TO TOP': continue
            if re.fullmatch(r'\d+', ln): continue
            if 'GLOBAL AI DATA CENTER' in u or 'GLOBAL JAPAN WINE' in u or 'JAPAN WINE MARKET, 2019' in u: 
                if len(ln) < 80: continue
            keep.append(ln)
        print(f'--- p{i+1} ({len(keep)} lines) ---')
        for ln in keep[:18]:
            print(' ', ln[:130])
        if len(keep) > 18:
            print(f'  ... (+{len(keep)-18} more)')
        # fill heuristic: few content lines => underfilled
        if len(keep) <= 4:
            print('  ** UNDERFILLED/SPARSE **')
    doc.close()

# Market segment chapters golden Ch2-4, current Ch2-4
summarize_range(golden, 31, 42, 'GOLDEN Ch2-4')
summarize_range(current, 27, 43, 'CURRENT Ch2-4')
