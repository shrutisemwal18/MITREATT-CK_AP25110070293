import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Minimal CSV reader (handles quotes, commas and newlines inside quotes). */
public class Csv {
    public static List<String[]> read(Path p) throws IOException {
        String text = Files.readString(p, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        List<String[]> rows = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { sb.append('"'); i++; }
                    else quoted = false;
                } else sb.append(c);
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                cur.add(sb.toString()); sb.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                cur.add(sb.toString()); sb.setLength(0);
                if (!(cur.size() == 1 && cur.get(0).isEmpty())) rows.add(cur.toArray(new String[0]));
                cur = new ArrayList<>();
            } else sb.append(c);
        }
        if (sb.length() > 0 || !cur.isEmpty()) {
            cur.add(sb.toString());
            rows.add(cur.toArray(new String[0]));
        }
        return rows;
    }
}
