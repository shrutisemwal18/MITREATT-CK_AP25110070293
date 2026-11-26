import java.util.ArrayList;
import java.util.List;

/** Splits a report into sentences, maps each to ATT&CK candidates and labels how well-supported each mapping is. */
public class Pipeline {

    public static class Finding {
        public final String sentence;
        public final int rank;
        public final Scored scored;
        public final String status;
        Finding(String sentence, int rank, Scored scored, String status) {
            this.sentence = sentence; this.rank = rank; this.scored = scored; this.status = status;
        }
    }

    public static List<String> sentences(String text) {
        List<String> out = new ArrayList<>();
        for (String para : text.split("\\R+")) {
            for (String s : para.split("(?<=[.!?])\\s+")) {
                s = s.trim();
                if (s.length() >= 30) out.add(s);
            }
        }
        return out;
    }

    public static List<Finding> analyze(String text, Retriever r, int topN, double minScore) {
        List<Finding> out = new ArrayList<>();
        for (String s : sentences(text)) {
            List<Scored> ranked = r.rank(s);
            for (int i = 0; i < Math.min(topN, ranked.size()); i++) {
                Scored sc = ranked.get(i);
                if (sc.score < minScore) break;
                out.add(new Finding(s, i + 1, sc, status(ranked, i)));
            }
        }
        return out;
    }

    /** Distinguishes evidence-supported, ambiguous and inferred mappings. */
    static String status(List<Scored> ranked, int i) {
        boolean ambiguous = ranked.size() > 1 && i <= 1
                && ranked.get(1).score >= 0.9 * ranked.get(0).score;
        if (ambiguous) return "Ambiguous (close scores)";
        if (!ranked.get(i).evidence.isEmpty()) return "Evidence-supported (keyword)";
        return "Inferred (semantic only)";
    }
}
