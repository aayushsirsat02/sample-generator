import fitz

golden = r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_SI10002_AI Data Center Market_Report.pdf"
g = fitz.open(golden)

# Find figure-only / sparse image pages
print("Looking for figure-dominant pages...")
for i in range(g.page_count):
    page = g[i]
    text = page.get_text()
    words = text.split()
    imgs = [im for im in page.get_image_info(xrefs=True)
            if im.get("bbox") and (im["bbox"][2]-im["bbox"][0]) > 80]
    if not imgs:
        continue
    # skip logo-only
    big = [im for im in imgs if (im["bbox"][2]-im["bbox"][0]) > 150]
    if not big:
        continue
    body_words = [w for w in words if w not in (
        "GLOBAL","AI","DATA","CENTER","MARKET,","2019","-","2035","Back","to","Top",
        "Copyright","Spherical","Insights","|","sales@sphericalinsights.com","www.sphericalinsights.com")]
    # pages with figure caption and mostly image
    low = text.lower()
    if "figure" in low and len(body_words) < 45:
        b = big[0]["bbox"]
        print(f"p{i+1} words={len(body_words)} img=({b[0]:.0f},{b[1]:.0f},{b[2]:.0f},{b[3]:.0f}) "
              f"w={b[2]-b[0]:.0f} h={b[3]-b[1]:.0f} :: {' '.join(body_words[:20])}")

# Segment intro title spacing on p5
page = g[4]
print("\nGolden p5 bullet metrics")
for img in page.get_image_info(xrefs=True):
    b = img["bbox"]
    if b[2]-b[0] < 50:
        print(" bullet img", tuple(round(x,1) for x in b), "w", round(b[2]-b[0],1), "h", round(b[3]-b[1],1))
