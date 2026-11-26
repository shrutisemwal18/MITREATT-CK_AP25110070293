import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** One ATT&CK technique or sub-technique from Dataset/techniques.csv. */
public class Technique {
    public final String id, name, tactic, description;
    public final List<String> keywords;

    public Technique(String id, String name, String tactic, String description, String keywords) {
        this.id = id.trim();
        this.name = name.trim();
        this.tactic = tactic.trim();
        this.description = description.trim();
        this.keywords = new ArrayList<>();
        for (String k : keywords.split(";")) if (!k.trim().isEmpty()) this.keywords.add(k.trim());
    }

    /** T1059.001 -> T1059 */
    public String parentId() {
        int i = id.indexOf('.');
        return i < 0 ? id : id.substring(0, i);
    }

    public static String parentOf(String id) {
        int i = id.indexOf('.');
        return i < 0 ? id : id.substring(0, i);
    }

    public static List<Technique> load(Path csv) throws IOException {
        List<Technique> out = new ArrayList<>();
        List<String[]> rows = Csv.read(csv);
        for (int i = 1; i < rows.size(); i++) {      // row 0 = header
            String[] r = Arrays.copyOf(rows.get(i), 5);
            for (int j = 0; j < 5; j++) if (r[j] == null) r[j] = "";
            out.add(new Technique(r[0], r[1], r[2], r[3], r[4]));
        }
        return out;
    }
}
