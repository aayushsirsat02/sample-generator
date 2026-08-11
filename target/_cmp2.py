import fitz

golden = r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_SI10002_AI Data Center Market_Report.pdf"
target = r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_Report (7).pdf"
out = r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\.pdf_inspection"

g = fitz.open(golden)
t = fitz.open(target)

for name, doc, pages in [("golden", g, [0, 2, 3, 4, 5, 6, 7]), ("target", t, [0, 2, 3, 4, 5, 6, 7, 8, 9])]:
    for i in pages:
        if i >= doc.page_count:
            continue
        pix = doc[i].get_pixmap(matrix=fitz.Matrix(1.1, 1.1))
        path = f"{out}\\cmp_{name}_p{i+1}.png"
        pix.save(path)
        print("saved", path)

for name, doc, i in [("G", g, 2), ("T", t, 2), ("G", g, 3), ("T", t, 3), ("G", g, 4), ("T", t, 4), ("G", g, 5), ("T", t, 5)]:
    page = doc[i]
    print("====", name, "p", i + 1, "====")
    print("text head:", " ".join(page.get_text().split()[:40]))
    for img in page.get_image_info(xrefs=True):
        print(" img", tuple(round(x, 1) for x in img.get("bbox")))
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
            print(f"  y={y:.1f} x={l['bbox'][0]:.1f} size={s0['size']:.1f} font={s0['font']} :: {txt[:75]}")

# cover.png dimensions
from PIL import Image
import os
cover = r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\src\main\resources\assets\images\cover.png"
mapj = r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\src\main\resources\assets\images\map.jpg"
for p in [cover, mapj]:
    if os.path.exists(p):
        im = Image.open(p)
        print(os.path.basename(p), im.size, im.mode)
