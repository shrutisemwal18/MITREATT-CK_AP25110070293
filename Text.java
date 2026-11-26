import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Text helpers: normalisation, tokenisation, light stemming. */
public class Text {
    private static final Set<String> STOP = Set.of(
        "a","an","the","and","or","of","to","in","on","at","by","for","with","from","as","is","are","was","were",
        "be","been","it","its","this","that","these","those","then","than","so","into","over","also","their",
        "they","them","he","she","his","her","we","our","you","your","can","could","may","might","will","would",
        "has","have","had","not","no","but","if","which","who","whom","after","before","each","other","such","used","use");

    /** Lower-case, non-alphanumerics become spaces, padded with one space on each side. */
    public static String norm(String s) {
        return " " + s.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim() + " ";
    }

    public static int wordCount(String normalised) {
        String t = normalised.trim();
        return t.isEmpty() ? 0 : t.split(" ").length;
    }

    public static List<String> tokens(String s) {
        List<String> out = new ArrayList<>();
        for (String t : s.toLowerCase().split("[^a-z0-9]+")) {
            if (t.length() < 2 || STOP.contains(t)) continue;
            out.add(stem(t));
        }
        return out;
    }

    /** Very small suffix stripper (enough to match execute/executed/executes/execution). */
    static String stem(String t) {
        if (t.length() > 5 && t.endsWith("ing")) t = t.substring(0, t.length() - 3);
        else if (t.length() > 5 && t.endsWith("ion")) t = t.substring(0, t.length() - 3);
        else if (t.length() > 4 && t.endsWith("ed")) t = t.substring(0, t.length() - 2);
        else if (t.length() > 4 && t.endsWith("es")) t = t.substring(0, t.length() - 2);
        else if (t.length() > 3 && t.endsWith("s") && !t.endsWith("ss")) t = t.substring(0, t.length() - 1);
        if (t.length() > 4 && t.endsWith("e")) t = t.substring(0, t.length() - 1);
        return t;
    }
}
