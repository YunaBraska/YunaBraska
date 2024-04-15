package berlin.yuna.repos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {
    public static void main(String[] args) {
        String text = "The Lazy ${{ step.step_id.outputs.my_var }} Brown Fox ${{step.step_id.outputs.my_var}} Jumps over the Bed";
        String regex = "\\b(\\w++(?:\\.\\w++)++)\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        int matchCount = 0;
        while (matcher.find()) {
            System.out.println("Match " + ++matchCount + ":");
            for (int i = 0; i <= matcher.groupCount(); i++) {
                int start = matcher.start(i);
                int end = matcher.end(i);
                System.out.println("Group " + i + " starts at " + start + " and ends at " + end + ". Matched text: " + text.substring(start, end));
            }
        }
    }
}
