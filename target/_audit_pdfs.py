import fitz
from pathlib import Path

REF = Path(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_DAR2211_Turmeric Market_Report.pdf")
CUR = Path(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_Report (19).pdf")
OUT = Path(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\target")


def chapter_pages(doc):
    hits = []
    for i in range(doc.page_count):
        text = doc[i].get_text("text")
        for ln in text.splitlines():
            s = ln.strip()
            if s.upper().startswith("CHAPTER ") and len(s) < 160:
                hits.append((i + 1, s))
                break
    return hits


def extract_outline(pdf_path, out_path):
    doc = fitz.open(pdf_path)
    lines = [f"FILE: {pdf_path.name}", f"PAGES: {doc.page_count}", ""]
    toc = doc.get_toc()
    if toc:
        lines.append("=== TOC ===")
        for lvl, title, page in toc:
            lines.append(f"{'  ' * (lvl - 1)}{title} | p{page}")
        lines.append("")

    lines.append("=== CHAPTERS ===")
    for p, s in chapter_pages(doc):
        lines.append(f"p{p}: {s}")
    lines.append("")

    lines.append("=== PAGE SNIPPETS ===")
    for i in range(doc.page_count):
        text = doc[i].get_text("text")
        nonempty = [ln.strip() for ln in text.splitlines() if ln.strip()]
        interesting = []
        for ln in nonempty[:40]:
            if (
                ln.startswith("CHAPTER")
                or ln[:2].isdigit()
                and "." in ln[:6]
                or ln.startswith(("TABLE", "FIGURE", "Fig", "Table", "Source"))
                or any(
                    k in ln
                    for k in (
                        "Executive",
                        "Competitive",
                        "Company Profile",
                        "Industry Analysis",
                        "Marketing",
                        "Conclusion",
                        "Methodology",
                        "Regional",
                        "COVID",
                        "Overview",
                        "Market Share",
                    )
                )
            ):
                interesting.append(ln)
            elif len(interesting) < 2 and len(ln) < 120:
                interesting.append(ln)
        lines.append(f"-- p{i + 1} --")
        for ln in interesting[:15]:
            lines.append(f"  {ln}")
    out_path.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {out_path.name} pages={doc.page_count} toc={len(toc)}")


def dump_range(pdf_path, start, end, out_path):
    doc = fitz.open(pdf_path)
    parts = []
    for i in range(start - 1, min(end, doc.page_count)):
        parts.append(f"===== PAGE {i + 1} =====\n{doc[i].get_text('text')}")
    out_path.write_text("\n".join(parts), encoding="utf-8")
    print(f"Wrote {out_path.name}")


extract_outline(REF, OUT / "_ref_outline.txt")
extract_outline(CUR, OUT / "_cur_outline.txt")

ref = fitz.open(REF)
cur = fitz.open(CUR)
print("REF chapters:")
for p, s in chapter_pages(ref):
    print(f"  p{p}: {s}")
print("CUR chapters:")
for p, s in chapter_pages(cur):
    print(f"  p{p}: {s}")
