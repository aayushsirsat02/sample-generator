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
        keep = []
        for ln in ls:
            u = ln.upper()
            if 'SPHERICAL INSIGHTS' in u and len(ln)<40: continue
            if u.startswith('COPYRIGHT'): continue
            if u == 'BACK TO TOP': continue
            if re.fullmatch(r'\d+', ln): continue
            if re.match(r'^(GLOBAL )?(AI DATA CENTER|JAPAN WINE) MARKET', u) and ',' in ln and len(ln)<90: continue
            keep.append(ln)
        # detect headings / figures / tables
        markers = [ln for ln in keep if ln.upper().startswith(('CHAPTER','FIGURE','TABLE','SOURCE')) or re.match(r'^\d+(\.\d+)+(\s|$)', ln)]
        print(f'--- p{i+1} lines={len(keep)} markers={len(markers)} ---')
        for ln in markers[:25]:
            print(' ', ln[:130])
        if len(keep) <= 5:
            print('  ** SPARSE **')
            for ln in keep:
                print('   >', ln[:130])
    doc.close()

summarize_range(golden, 27, 30, 'GOLDEN Exec')
summarize_range(current, 21, 26, 'CURRENT Exec')
summarize_range(golden, 92, 102, 'GOLDEN Ch6-7start')
summarize_range(current, 95, 110, 'CURRENT Ch6-7start')
