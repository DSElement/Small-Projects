using ClosedXML.Excel;
using System.Text.RegularExpressions;

namespace ExcelGMProject
{
    class MeritProcessor
    {
        private readonly Dictionary<string, SheetBucket> _buckets =
            new Dictionary<string, SheetBucket>();

        private readonly Dictionary<string, List<string>> _sectionSheets =
            new Dictionary<string, List<string>>()
            {
                { "I",  new List<string>() },
                { "II", new List<string>() },
                { "III",new List<string>() }
            };

        private bool _sectionSheetsInitialized = false;

        public void LoadWorkbook(string filePath)
        {
            using var wb = new XLWorkbook(filePath);

            if (!_sectionSheetsInitialized)
            {
                InitSectionSheets(wb);
                _sectionSheetsInitialized = true;

                Console.WriteLine("Selected sheet names (first 5 of each section):");
                foreach (var kvp in _sectionSheets)
                {
                    Console.WriteLine($"Section {kvp.Key}: {string.Join(", ", kvp.Value)}");
                }
                Console.WriteLine();
            }

            foreach (var sheetName in GetAllTargetSheetNames())
            {
                var ws = wb.Worksheets.FirstOrDefault(x => x.Name == sheetName);
                if (ws == null)
                {
                    Console.WriteLine($"  Sheet '{sheetName}' not found in {Path.GetFileName(filePath)}, skipping.");
                    continue;
                }

                Console.WriteLine($"  Reading sheet '{sheetName}' from {Path.GetFileName(filePath)}");

                var headerRows = FindHeaderRows(ws);
                var headers = GetHeaders(headerRows);

                var bucket = GetOrCreateBucket(sheetName);
                bucket.RegisterHeaders(headers.Select(h => h.Name));

                var rowDicts = ReadDataRows(ws, headerRows, headers);
                bucket.Rows.AddRange(rowDicts);
            }
        }

        private void InitSectionSheets(XLWorkbook wb)
        {
            foreach (var ws in wb.Worksheets)
            {
                var section = GetSectionFromSheetName(ws.Name);    // "I", "II", "III"
                if (section == null)
                    continue;

                var list = _sectionSheets[section];
                if (list.Count < 5)
                {
                    list.Add(ws.Name);
                }

                if (_sectionSheets["I"].Count >= 5 &&
                    _sectionSheets["II"].Count >= 5 &&
                    _sectionSheets["III"].Count >= 5)
                {
                    break;
                }
            }
        }

        private IEnumerable<string> GetAllTargetSheetNames()
        {
            return _sectionSheets["I"]
                .Concat(_sectionSheets["II"])
                .Concat(_sectionSheets["III"]);
        }

        private SheetBucket GetOrCreateBucket(string sheetName)
        {
            if (!_buckets.TryGetValue(sheetName, out var bucket))
            {
                bucket = new SheetBucket(sheetName);
                _buckets[sheetName] = bucket;
            }
            return bucket;
        }

        private static string? GetSectionFromSheetName(string name)
        {
            if (name.StartsWith("III"))
                return "III";

            if (name.StartsWith("II"))
                return "II";

            if (name.StartsWith("I."))
                return "I";

            return null;
        }

        private static List<IXLRow> FindHeaderRows(IXLWorksheet ws)
        {
            IXLRow? firstHeader = null;

            foreach (var row in ws.RowsUsed())
            {
                var cells = row.CellsUsed().ToList();
                if (cells.Count >= 3) //&& cells.Count(c => c.DataType == XLDataType.Text) >= 2)
                {
                    firstHeader = row;
                    break;
                }
            }

            if (firstHeader == null)
                throw new Exception($"Header row not found in sheet '{ws.Name}'.");

            var headerRows = new List<IXLRow>();
            headerRows.Add(firstHeader);

            int r = firstHeader.RowNumber() + 1;
            
            while (true)
            {
                var row = ws.Row(r);
                var cells = row.CellsUsed().ToList();

                if (cells.Any(c => c.HasFormula && c.FormulaA1.IndexOf("SUMIF", StringComparison.OrdinalIgnoreCase) >= 0))
                {
                    break;
                }
                else
                {
                    headerRows.Add(row);
                    r++;
                }
            }

            return headerRows;
        }

        private static List<(int Index, string Name)> GetHeaders(List<IXLRow> headerRows)
        {
            var columnIndices =
                (from row in headerRows
                from cell in row.CellsUsed()
                select cell.Address.ColumnNumber)
                .Distinct()
                .OrderBy(i => i);

            var headers = new List<(int, string)>();

            foreach (var col in columnIndices)
            {
                var parts = new List<string>();

                foreach (var row in headerRows)
                {
                    var cell = row.Cell(col);
                    var text = cell.GetString().Replace("\n", " ").Replace("\r", " ").Trim();
                    text = Regex.Replace(text, @"\s+", " ").Trim();

                    if (string.IsNullOrEmpty(text) && cell.IsMerged())
                    {
                        var merged = cell.MergedRange();
                        text = merged.FirstCell().GetString().Replace("\n", " ").Replace("\r", " ").Trim();
                    }

                    if (!string.IsNullOrEmpty(text))
                    {
                        if (parts.Count == 0 ||
                            !string.Equals(parts.Last(), text, StringComparison.OrdinalIgnoreCase))
                        {
                            parts.Add(text);
                        }
                    }
                }

                if (parts.Count == 0)
                    continue;

                var name = string.Join(" ", parts);
                headers.Add((col, name));
            }

            return headers;
        }

        private static IEnumerable<Dictionary<string, string>> ReadDataRows(
                                                                            IXLWorksheet ws,
                                                                            List<IXLRow> headerRows,
                                                                            List<(int Index, string Name)> headers)
        {
            int startRow = headerRows.Last().RowNumber() + 1;

            foreach (var row in ws.RowsUsed().Where(r => r.RowNumber() >= startRow))
            {
                var record = new Dictionary<string, string>();
                bool hasAnyValue = false;

                foreach (var (index, name) in headers)
                {
                    var value = row.Cell(index).GetString().Trim();
                    if (!string.IsNullOrEmpty(value))
                        hasAnyValue = true;

                    record[name] = value;
                }

                if (!hasAnyValue)
                    continue;

                yield return record;
            }
        }

        public void WriteOutput(string outputPath)
        {
            using var wb = new XLWorkbook();

            foreach (var sheetName in GetAllTargetSheetNames())
            {
                //var bucket = _buckets[sheetName]; 
                if (!_buckets.TryGetValue(sheetName, out var bucket))
                {
                    wb.AddWorksheet(sheetName);
                    continue;
                }

                var ws = wb.AddWorksheet(sheetName);

                for (int i = 0; i < bucket.HeaderOrder.Count; i++)
                {
                    ws.Cell(1, i + 1).Value = bucket.HeaderOrder[i];
                }

                int currentRow = 2;
                foreach (var record in bucket.Rows)
                {
                    for (int i = 0; i < bucket.HeaderOrder.Count; i++)
                    {
                        var header = bucket.HeaderOrder[i];
                        record.TryGetValue(header, out var value);
                        ws.Cell(currentRow, i + 1).Value = value ?? string.Empty;
                    }
                    currentRow++;
                }

                ws.Columns().AdjustToContents();
            }

            wb.SaveAs(outputPath);
        }
    }
}