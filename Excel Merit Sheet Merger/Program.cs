namespace ExcelGMProject
{
    class Program
    {
        static void Main(string[] args)
        {
            if (args.Length < 2)
            {
                Console.WriteLine("Usage:");
                Console.WriteLine("  dotnet run -- <output.xlsx> <input1.xlsx> [input2.xlsx] ...");
                return;
            }

            var outputPath = args[0];
            var inputFiles = args.Skip(1).ToList();

            var processor = new MeritProcessor();

            foreach (var file in inputFiles)
            {
                if (!File.Exists(file))
                {
                    Console.WriteLine($"Warning: input file not found: {file}");
                    continue;
                }

                Console.WriteLine($"Loading: {file}");
                processor.LoadWorkbook(file);
            }

            //processor._buckets["III.2"].PrintDicts();
            Console.WriteLine($"Writing merged output to: {outputPath}");
            processor.WriteOutput(outputPath);
        }
    }
}
