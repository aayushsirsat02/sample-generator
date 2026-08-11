import fitz
cur=fitz.open(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_Report (17).pdf")
gol=fitz.open(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_SI10002_AI Data Center Market_Report.pdf")
for label,doc,ps in [('CUR',cur,[27,28,29,30,31,32,33]),('GOL',gol,[31,32,33,34])]:
  print('====',label)
  for p in ps:
    print(f'--- p{p} ---')
    for ln in doc[p-1].get_text('text').splitlines():
      s=ln.strip()
      if not s: continue
      if s in ('Back to Top',) or s.isdigit() or s.startswith('Copyright') or 'Category:' in s: continue
      if s.startswith('JAPAN WINE MARKET, 2019') or s.startswith('GLOBAL AI DATA CENTER MARKET, 2019'): continue
      if 'SPHERICAL INSIGHTS'==s: continue
      print(s[:140])
