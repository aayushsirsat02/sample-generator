import fitz

golden = r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_SI10002_AI Data Center Market_Report.pdf"
g = fitz.open(golden)

# Inspect sparse/special figure pages
for i in [19, 25, 28, 33, 37, 41]:
    page = g[i]
    print("==== G p", i + 1, "====")
    print(" ".join(page.get_text().split()[:50]))
    for img in page.get_image_info(xrefs=True):
        b = img.get("bbox")
        print(" img", tuple(round(x, 1) for x in b), "w", round(b[2]-b[0],1), "h", round(b[3]-b[1],1))
    for b in page.get_text("dict")["blocks"]:
        if b.get("type") != 0:
            continue
        for l in b["lines"]:
            txt = "".join(s["text"] for s in l["spans"]).strip()
            if not txt:
                continue
            s0 = l["spans"][0]
            y = l["bbox"][1]
            if y < 70 or y > 520:
                continue
            print(f"  y={y:.1f} x={l['bbox'][0]:.1f} size={s0['size']:.1f} :: {txt[:80]}")

# Cover text exact positions relative to page
page = g[0]
print("\nCOVER detailed")
for b in page.get_text("dict")["blocks"]:
    if b.get("type") != 0:
        continue
    for l in b["lines"]:
        txt = "".join(s["text"] for s in l["spans"]).strip()
        if not txt:
            continue
        s0 = l["spans"][0]
        print(f"  bbox={tuple(round(x,1) for x in l['bbox'])} size={s0['size']:.1f} font={s0['font']} color={s0.get('color')} :: {txt}")
