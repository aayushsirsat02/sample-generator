import fitz

golden = r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_SI10002_AI Data Center Market_Report.pdf"
target = r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_Report (7).pdf"

g = fitz.open(golden)
t = fitz.open(target)
print("golden pages", g.page_count, "target pages", t.page_count)


def summarize(doc, i):
    page = doc[i]
    text = page.get_text()
    words = text.split()
    imgs = page.get_images(full=True)
    first = " ".join(words[:14])
    return len(words), len(imgs), first


def find_special(doc, name):
    print(f"--- {name} ---")
    for i in range(doc.page_count):
        page = doc[i]
        text = page.get_text().strip()
        words = text.split()
        imgs = page.get_images(full=True)
        low = text.lower()
        flags = []
        if i == 0:
            flags.append("COVER")
        if len(words) < 40 and len(imgs) >= 1:
            flags.append("SPARSE+IMG")
        if text.startswith("By ") or "\nBy " in text[:120]:
            flags.append("BY_SEG")
        if "by region" in low[:80] and len(words) < 120:
            flags.append("MAPISH")
        if len(imgs) >= 1 and len(words) < 25 and "figure" in low:
            flags.append("FIG_PAGE")
        # large image occupying most page
        for img in page.get_image_info(xrefs=True):
            bbox = img.get("bbox")
            if bbox:
                w = bbox[2] - bbox[0]
                h = bbox[3] - bbox[1]
                if w > page.rect.width * 0.85 and h > page.rect.height * 0.7:
                    flags.append("FULLPAGE_IMG")
        if flags:
            print(f"  p{i+1} {flags} words={len(words)} imgs={len(imgs)} :: {' '.join(words[:12])}")


find_special(g, "GOLDEN")
find_special(t, "TARGET")

print("\n=== COVER TEXT DETAIL ===")
for name, doc in [("G", g), ("T", t)]:
    page = doc[0]
    print(name, "rect", page.rect)
    for b in page.get_text("dict")["blocks"]:
        if b.get("type") != 0:
            continue
        for l in b["lines"]:
            spans = l["spans"]
            t = "".join(s["text"] for s in spans).strip()
            if not t:
                continue
            s0 = spans[0]
            print(f"  {name} y={l['bbox'][1]:.1f} x={l['bbox'][0]:.1f} size={s0['size']:.1f} font={s0['font']} :: {t[:80]}")
    print(f"  {name} images:")
    for img in page.get_image_info(xrefs=True):
        print("   ", img.get("bbox"), "cs", img.get("colorspace"), "xform", img.get("transform"))
