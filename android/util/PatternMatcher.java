package android.util;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A simple pattern matcher, which is safe to use on untrusted data.
 * 
 * This is a simple mock implementation for the purposes of simulating
 * the Android SDK PatternMatcher class.
 */
public class PatternMatcher implements Parcelable {
    private final String mPattern;
    private final int mType;
    
    /**
     * Pattern type: the given pattern must exactly match the string.
     */
    public static final int PATTERN_LITERAL = 0;
    
    /**
     * Pattern type: the given pattern must match the beginning of the string.
     */
    public static final int PATTERN_PREFIX = 1;
    
    /**
     * Pattern type: the given pattern must be a full regular expression
     * consisting of literal characters and the wildcards
     * '*' to match zero or more occurrences of the character before it, and
     * '.' to match any character.
     */
    public static final int PATTERN_SIMPLE_GLOB = 2;
    
    /**
     * Pattern type: the given pattern must be a full regular expression
     * syntactically similar to perl regular expressions.
     */
    public static final int PATTERN_ADVANCED_GLOB = 3;
    
    /**
     * Construct a new PatternMatcher.
     * 
     * @param pattern The pattern to match against.
     * @param type The type of pattern matching to do.
     */
    public PatternMatcher(String pattern, int type) {
        mPattern = pattern;
        mType = type;
    }
    
    /**
     * Construct a PatternMatcher from the Parcel source.
     * 
     * @param src The Parcel to read the PatternMatcher from.
     */
    public PatternMatcher(Parcel src) {
        mPattern = src.readString();
        mType = src.readInt();
    }
    
    /**
     * Return the pattern that this PatternMatcher uses.
     */
    public String getPath() {
        return mPattern;
    }
    
    /**
     * Return the type of this PatternMatcher.
     */
    public int getType() {
        return mType;
    }
    
    /**
     * Return whether the given string matches this pattern matcher.
     * 
     * @param str The string to match against.
     * @return True if str matches the pattern.
     */
    public boolean match(String str) {
        if (str == null || mPattern == null) {
            return false;
        }
        
        switch (mType) {
            case PATTERN_LITERAL:
                return mPattern.equals(str);
            case PATTERN_PREFIX:
                return str.startsWith(mPattern);
            case PATTERN_SIMPLE_GLOB:
                // Simple implementation for mock purposes
                return str.matches(simpleGlobToRegex(mPattern));
            case PATTERN_ADVANCED_GLOB:
                // Simple implementation for mock purposes
                return str.matches(mPattern);
            default:
                return false;
        }
    }
    
    private String simpleGlobToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '.':
                    regex.append("\\.");
                    break;
                case '\\':
                    regex.append("\\\\");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '[':
                    regex.append("\\[");
                    break;
                case ']':
                    regex.append("\\]");
                    break;
                case '{':
                    regex.append("\\{");
                    break;
                case '}':
                    regex.append("\\}");
                    break;
                case '(':
                    regex.append("\\(");
                    break;
                case ')':
                    regex.append("\\)");
                    break;
                case '+':
                    regex.append("\\+");
                    break;
                case '^':
                    regex.append("\\^");
                    break;
                case '$':
                    regex.append("\\$");
                    break;
                default:
                    regex.append(c);
            }
        }
        return regex.toString();
    }
    
    @Override
    public String toString() {
        String type = "unknown";
        switch (mType) {
            case PATTERN_LITERAL:
                type = "LITERAL";
                break;
            case PATTERN_PREFIX:
                type = "PREFIX";
                break;
            case PATTERN_SIMPLE_GLOB:
                type = "GLOB";
                break;
            case PATTERN_ADVANCED_GLOB:
                type = "ADVANCED_GLOB";
                break;
        }
        return "PatternMatcher{" + type + " " + mPattern + "}";
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPattern);
        dest.writeInt(mType);
    }
    
    public static final Parcelable.Creator<PatternMatcher> CREATOR =
            new Parcelable.Creator<PatternMatcher>() {
        public PatternMatcher createFromParcel(Parcel src) {
            return new PatternMatcher(src);
        }
        
        public PatternMatcher[] newArray(int size) {
            return new PatternMatcher[size];
        }
    };
}