import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Evaluates retrievers on Dataset/annotations.csv: Top-1, Top-5, parent-level Top-1 and MRR. */
public class Evaluator {

    public static class Example {
        public final String passage, gold;
        Example(String passage, String gold) { this.passage = passage; this.gold = gold; }
    }

    public static List<Example> load(Path csv) throws IOException {
        List<Example> out = new ArrayList<>();
        List<String[]> rows = Csv.read(csv);
        for (int i = 1; i < rows.size(); i++)
            if (rows.get(i).length >= 3) out.add(new Example(rows.get(i)[1], rows.get(i)[2].trim()));
        return out;
    }

    public static String run(List<Retriever> retrievers, List<Example> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("EVALUATION on ").append(data.size()).append(" annotated passages\n\n");
        sb.append(String.format("%-28s %7s %7s %10s %7s %9s%n", "Method", "Top-1", "Top-5", "Parent@1", "MRR", "No result"));
        sb.append("-".repeat(74)).append("\n");
        Retriever best = null;
        double bestMrr = -1;
        for (Retriever r : retrievers) {
            int top1 = 0, top5 = 0, parent1 = 0, none = 0;
            double rr = 0;
            for (Example e : data) {
                List<Scored> ranked = r.rank(e.passage);
                if (ranked.isEmpty()) none++;
                int pos = -1;
                for (int i = 0; i < ranked.size(); i++) if (ranked.get(i).t.id.equals(e.gold)) { pos = i; break; }
                if (pos == 0) top1++;
                if (pos >= 0 && pos < 5) top5++;
                if (pos >= 0) rr += 1.0 / (pos + 1);
                if (!ranked.isEmpty() && ranked.get(0).t.parentId().equals(Technique.parentOf(e.gold))) parent1++;
            }
            double n = data.size();
            double mrr = rr / n;
            sb.append(String.format("%-28s %6.1f%% %6.1f%% %9.1f%% %7.3f %9d%n",
                    r.name(), 100 * top1 / n, 100 * top5 / n, 100 * parent1 / n, mrr, none));
            if (mrr > bestMrr) { bestMrr = mrr; best = r; }
        }
        sb.append("\nTop-1/Top-5 = exact technique ID in first 1 / first 5 candidates.\n");
        sb.append("Parent@1 = top-1 has the right parent technique (T1059.001 counts for T1059).\n");
        sb.append("MRR = mean reciprocal rank of the gold technique over the full ranked list.\n");

        if (best != null) {
            sb.append("\nERROR ANALYSIS (top-1 mistakes of best method by MRR: ").append(best.name()).append(")\n");
            sb.append("-".repeat(74)).append("\n");
            int shown = 0;
            for (Example e : data) {
                List<Scored> ranked = best.rank(e.passage);
                String pred = ranked.isEmpty() ? "none" : ranked.get(0).t.id + " " + ranked.get(0).t.name;
                if (!ranked.isEmpty() && ranked.get(0).t.id.equals(e.gold)) continue;
                int pos = -1;
                for (int i = 0; i < ranked.size(); i++) if (ranked.get(i).t.id.equals(e.gold)) { pos = i + 1; break; }
                String p = e.passage.length() > 100 ? e.passage.substring(0, 100) + "..." : e.passage;
                sb.append("* ").append(p).append("\n    gold: ").append(e.gold)
                  .append(" (rank ").append(pos < 0 ? "not found" : String.valueOf(pos)).append(")  predicted: ")
                  .append(pred).append("\n");
                shown++;
            }
            if (shown == 0) sb.append("No top-1 errors.\n");
            sb.append("\nTypical causes: paraphrased wording with no keyword, overlapping techniques (parent vs sub-technique),\n");
            sb.append("and generic words shared by several techniques. Review these cases manually.\n");
        }
        return sb.toString();
    }
}
