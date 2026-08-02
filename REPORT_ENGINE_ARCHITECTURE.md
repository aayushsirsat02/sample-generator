# Report Generation Module — Architecture

This project is a **single Spring Boot application**. Everything except
report *rendering* (auth, dashboard, database, APIs, business logic,
frontend) is unchanged. Report rendering has been completely rebuilt.

```
┌─────────────────────────────┐        JSON (stdin/file)        ┌───────────────────────────────┐
│         Spring Boot          │ ───────────────────────────────▶ │      Python Report Engine       │
│                               │                                  │        (report-engine/)          │
│  Controllers / Services /    │        rendered file (docx)       │                                   │
│  Repositories / Entities     │ ◀─────────────────────────────── │  builders → renderers → output   │
│  (unchanged)                 │        via local subprocess       │  (no DB access, ever)            │
└─────────────────────────────┘                                  └───────────────────────────────┘
```

There is still only **one deployed service**: Spring Boot invokes the
Python engine as a **local subprocess** on the same droplet. There is no
network hop, no second deployment, and no second database connection —
Python never touches MySQL/H2 directly. It only ever receives a
structured JSON "Report Model" and hands back a finished file.

## What changed

* **Removed completely**: docx4j, Apache POI, and all placeholder-based
  Word generation (`WordGenerationService`, `PlaceholderService`,
  `TemplateLoaderService`, and the empty `HeaderFooterService` /
  `ImageService` / `PageService` / `ParagraphService` / `TableService`
  stubs, plus the old `.docx` template files). Nothing reused from that
  architecture.
* **Added**: `report-engine/`, a standalone Python package, and
  `com.sample_generator.sample.reportengine`, a small Java package that
  builds a JSON Report Model from the database and invokes the engine.

## Java side — `com.sample_generator.sample.reportengine`

| Class | Responsibility |
|---|---|
| `ReportModelBuilder` / `ReportModelBuilderImpl` | Converts a `SampleReport` entity (+ its `MarketSegment` tree and `Company` list) into the structured JSON Report Model — Cover, Executive Summary, Market Overview, Market Dynamics, Segment Analysis, Regional Analysis, Competitive Landscape, Company Profiles, FAQ, Methodology, Appendix. Section/subsection/table-row counts always track the real data. |
| `PythonReportEngineClient` | Writes the model to a temp JSON file, runs `python3 report-engine/cli.py --input ... --output ...` via `ProcessBuilder`, reads the rendered file back into bytes, cleans up. |
| `ReportEngineProperties` | Externalized config (`report-engine.*` in `application.properties`) for the Python executable, script path, working dir, timeout. |
| `ReportEngineException` | Single exception type for engine failures (validation, timeout, non-zero exit, missing output). |

`SampleReportServiceImpl.generateWordReport(...)` now does exactly two
things: build the model, call the engine. It no longer touches document
bytes at all.

## Python side — `report-engine/`

```
report-engine/
├── cli.py                     # stable entrypoint Spring Boot calls
├── requirements.txt
└── engine/
    ├── models.py               # Report Model schema + validation
    ├── exceptions.py
    ├── builders/
    │   └── report_builder.py   # format dispatcher (docx today; pdf/pptx/xlsx plug in here later)
    ├── renderers/
    │   └── docx/
    │       ├── document_renderer.py     # orchestrator: cover → TOC → sections (recursive)
    │       ├── style_setup.py           # applies theme fonts/colors to Word styles
    │       ├── cover_renderer.py
    │       ├── toc_renderer.py          # real Word TOC field, not a typed list
    │       ├── header_footer_renderer.py
    │       ├── paragraph_renderer.py    # heading / paragraph / bullet_list / faq blocks
    │       ├── table_renderer.py        # any number of rows/columns
    │       ├── chart_renderer.py        # bridges to charts/chart_factory
    │       └── image_renderer.py
    ├── charts/
    │   └── chart_factory.py    # pie, bar, stacked_bar, line, area, radar, forecast
    ├── templates/
    │   ├── theme_registry.py
    │   └── themes/*.json       # 16 independent category themes (+ "general" fallback)
    ├── images/
    │   └── asset_generator.py  # procedurally generates themed cover art / brand marks
    └── output/
        └── writer.py
```

### The Report Model contract

Spring Boot and Python agree on one JSON shape:

```json
{
  "format": "docx",
  "toc": true,
  "metadata": { "keyId": "...", "baseYear": 2025, "forecastYear": 2032, "...": "..." },
  "cover": { "title": "...", "subtitle": "...", "category": "Automotive", "keyId": "..." },
  "sections": [
    {
      "id": "segment-analysis",
      "title": "Segment Analysis",
      "content": [
        { "type": "paragraph", "text": "..." },
        { "type": "table", "headers": [...], "rows": [[...], ...], "caption": "..." },
        { "type": "chart", "chartType": "bar", "title": "...", "categories": [...], "series": [{"name": "...", "values": [...]}] }
      ],
      "subsections": [ /* same shape, nested to any depth */ ]
    }
  ]
}
```

Because sections/subsections/blocks are just a tree, a report can have
3 sections or 30, and 1 level of segments or 5 — nothing in the renderer
changes. Adding a new content block type (e.g. `"icon"`) means adding
one function in `renderers/docx/` and one dispatch entry in
`document_renderer.py`.

### Adding a new output format later (PDF / PPTX / XLSX)

Only `engine/builders/report_builder.py` needs a new entry:

```python
_FORMAT_RENDERERS = {
    "docx": _build_docx,
    "pdf":  _build_pdf,   # new: engine/renderers/pdf/
}
```

`cli.py`, the Java client, and the Report Model schema all stay
unchanged.

### Adding a new category template

Drop a new JSON file into `engine/templates/themes/` (see the existing
16 for the shape: `primaryColor`, `secondaryColor`, `accentColor`,
`fontHeading`, `fontBody`, `chartPalette`). No Python or Java code
changes required.

## Running it

```bash
cd report-engine
pip install -r requirements.txt --break-system-packages   # once, per environment
```

Spring Boot is configured via `application.properties`:

```properties
report-engine.python-executable=python3
report-engine.script-path=report-engine/cli.py
report-engine.working-dir=report-engine
report-engine.timeout-seconds=120
```

You can also run the engine standalone for testing:

```bash
python3 report-engine/cli.py --input model.json --output report.docx
```
