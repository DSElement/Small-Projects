# Excel Merit Sheet Merger

## Overview
A C# command-line tool that merges multiple identically-structured academic merit evaluation workbooks (Romanian *Grila de Merite* format) into a single consolidated Excel file. Each input workbook is filled in by a different candidate; the tool extracts data from the matching sheets across all inputs and writes one combined sheet per section into the output.

## Features
- Accepts any number of input `.xlsx` files and a single output path
- Automatically detects and targets up to 5 sheets per Roman-numeral section (`I.*`, `II*`, `III.*`) based on the first loaded workbook's sheet layout
- Handles multi-row, merged-cell headers by reconstructing full column names from all header rows above the first data row
- Skips sheets that are missing from a particular input file without crashing
- Deduplicates column headers across files so the output schema is consistent regardless of minor per-file variations
- Auto-fits column widths in the output

## Technologies Used
- **C# / .NET** — application logic
- **ClosedXML** — reading and writing `.xlsx` files

## Project Structure
| File | Description |
|------|-------------|
| `Program.cs` | Entry point; parses CLI arguments and drives the load/write pipeline |
| `MeritProcessor.cs` | Core logic: sheet discovery, header parsing, data extraction, and output generation |
| `SheetBucket.cs` | Per-sheet data container that accumulates headers and rows across multiple input files |

> **Note:** The sample input workbooks (`GM_2021_*.xlsx`) and the merged output (`out.xlsx`) are not included in this repository as they contain personal academic data.

## How to Build / Run
```bash
dotnet build
dotnet run -- out.xlsx GM_2021_CandidateA.xlsx GM_2021_CandidateB.xlsx ...
```

The first argument is the output file path; all subsequent arguments are input workbooks.

## Notes
- Sheet selection is initialized from the first input file: sections `I`, `II`, and `III` each contribute up to 5 sheets, identified by name prefix (`I.`, `II`, `III`).
- Header detection stops at the first row containing a `SUMIF` formula, which the source workbooks use for per-section totals.
- The tool is tailored to a specific Romanian university merit evaluation template but could be adapted to similar multi-sheet aggregation tasks by adjusting the section prefix rules in `GetSectionFromSheetName`.