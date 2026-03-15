namespace ExcelGMProject
{
    class SheetBucket
    {
        public string SheetName { get; }
        private readonly HashSet<string> _headersSet =
            new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        public List<string> HeaderOrder { get; } = new List<string>();
        public List<Dictionary<string, string>> Rows { get; } = new List<Dictionary<string, string>>();

        public SheetBucket(string sheetName)
        {
            SheetName = sheetName;
        }

        public void RegisterHeaders(IEnumerable<string> headersInOrder)
        {
            foreach (var h in headersInOrder)
            {
                if (!_headersSet.Contains(h))
                {
                    _headersSet.Add(h);
                    HeaderOrder.Add(h);
                }
            }
        }

        public void PrintDicts()
        {
            int i = 0;
            foreach (var dict in Rows)
            {
                Console.WriteLine($"Row {i++}:");

                foreach (var kvp in dict)
                {
                    Console.WriteLine($"  [{kvp.Key}] = \"{kvp.Value}\"");
                }

                Console.WriteLine();
            }
        }
    }
}