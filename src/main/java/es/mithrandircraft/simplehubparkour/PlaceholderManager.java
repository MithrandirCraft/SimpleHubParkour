package es.mithrandircraft.simplehubparkour;

public class PlaceholderManager {
    static String SubstitutePlaceholders(String toReplace, String time, int falls)
    {
        toReplace = toReplace.replaceAll("\\{time}", time);
        toReplace = toReplace.replaceAll("\\{falls}", Integer.toString(falls));
        return toReplace;
    }
}
