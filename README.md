# Implementation - ATT&CK Technique Extractor (Java Swing desktop app)

Pure Java, no external libraries. Needs JDK 11 or newer (JDK 17/21 recommended).

## Run in VS Code (MacBook M4 or Windows)
1. Install a JDK (Mac: `brew install --cask temurin`, native Apple Silicon build) and the
   "Extension Pack for Java" in VS Code.
2. File > Open Folder... and choose THIS `Implementation` folder (keep `Dataset/` beside it).
3. Open `src/Main.java` and click Run above `main`, or press F5.

## Run from terminal
- Mac/Linux: `./run.sh`
- Windows: `run.bat`
- Console-only evaluation: `javac -d out src/*.java && java -cp out Main --eval`

## Code map
| File | Role |
|------|------|
| Main.java | Swing GUI (Extractor tab + Evaluation tab), CSV export |
| Pipeline.java | Sentence splitting, top-N mapping, status labels (evidence-supported / ambiguous / inferred) |
| KeywordMatcher.java | Baseline 1: keyword / phrase matching |
| TfidfRetriever.java | Baseline 2: TF-IDF + cosine similarity |
| HybridRetriever.java | Combination of the two |
| Evaluator.java | Top-1, Top-5, Parent@1, MRR + error analysis |
| Csv, Technique, Text, Scored, Retriever | Helpers |
