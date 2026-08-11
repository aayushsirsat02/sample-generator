import fitz
from pathlib import Path

REF = Path(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_DAR2211_Turmeric Market_Report.pdf")
CUR = Path(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\Sample_Report (19).pdf")
OUT = Path(r"C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample\target")


def dump_range(pdf_path, start, end, out_path):
    doc = fitz.open(pdf_path)
    parts = []
    for i in range(start - 1, min(end, doc.page_count)):
        parts.append(f"===== PAGE {i + 1} =====\n{doc[i].get_text('text')}")
    out_path.write_text("\n".join(parts), encoding="utf-8")
    print(f"Wrote {out_path.name} ({start}-{min(end, doc.page_count)})")


# Body sections of interest
dump_range(REF, 32, 55, OUT / "_ref_exec_seg.txt")
dump_range(CUR, 21, 50, OUT / "_cur_exec_seg.txt")
dump_range(REF, 115, 145, OUT / "_ref_tail.txt")
dump_range(CUR, 98, 174, OUT / "_cur_tail.txt")
