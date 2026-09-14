package lab;
final class PatternCount {
    static int ids(String body) { return (int) java.util.regex.Pattern.compile("\"id\":").matcher(body).results().count(); }
}
