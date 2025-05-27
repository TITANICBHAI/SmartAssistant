package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of Android Uri class for development outside of Android.
 * Immutable URI reference. A URI reference includes a URI and a fragment.
 */
public abstract class Uri implements Parcelable, Comparable<Uri> {
    /**
     * A basic implementation of Uri that can be used to create custom URIs.
     */
    private static class HierarchicalUri extends Uri {
        private final String mScheme;
        private final String mAuthority;
        private final String mPath;
        private final String mQuery;
        private final String mFragment;
        
        public HierarchicalUri(String scheme, String authority, String path, String query, String fragment) {
            mScheme = scheme;
            mAuthority = authority;
            mPath = path;
            mQuery = query;
            mFragment = fragment;
        }
        
        @Override
        public String getScheme() {
            return mScheme;
        }
        
        @Override
        public String getAuthority() {
            return mAuthority;
        }
        
        @Override
        public String getPath() {
            return mPath;
        }
        
        @Override
        public String getQuery() {
            return mQuery;
        }
        
        @Override
        public String getFragment() {
            return mFragment;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (mScheme != null) {
                sb.append(mScheme).append("://");
            }
            if (mAuthority != null) {
                sb.append(mAuthority);
            }
            if (mPath != null) {
                sb.append(mPath);
            }
            if (mQuery != null) {
                sb.append("?").append(mQuery);
            }
            if (mFragment != null) {
                sb.append("#").append(mFragment);
            }
            return sb.toString();
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HierarchicalUri that = (HierarchicalUri) o;
            return equals(mScheme, that.mScheme) &&
                   equals(mAuthority, that.mAuthority) &&
                   equals(mPath, that.mPath) &&
                   equals(mQuery, that.mQuery) &&
                   equals(mFragment, that.mFragment);
        }
        
        private static boolean equals(Object a, Object b) {
            return (a == b) || (a != null && a.equals(b));
        }
        
        @Override
        public int hashCode() {
            int result = mScheme != null ? mScheme.hashCode() : 0;
            result = 31 * result + (mAuthority != null ? mAuthority.hashCode() : 0);
            result = 31 * result + (mPath != null ? mPath.hashCode() : 0);
            result = 31 * result + (mQuery != null ? mQuery.hashCode() : 0);
            result = 31 * result + (mFragment != null ? mFragment.hashCode() : 0);
            return result;
        }
        
        @Override
        public int describeContents() {
            return 0;
        }
        
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mScheme);
            dest.writeString(mAuthority);
            dest.writeString(mPath);
            dest.writeString(mQuery);
            dest.writeString(mFragment);
        }
        
        public static final Parcelable.Creator<HierarchicalUri> CREATOR = new Parcelable.Creator<HierarchicalUri>() {
            @Override
            public HierarchicalUri createFromParcel(Parcel source) {
                String scheme = source.readString();
                String authority = source.readString();
                String path = source.readString();
                String query = source.readString();
                String fragment = source.readString();
                return new HierarchicalUri(scheme, authority, path, query, fragment);
            }
            
            @Override
            public HierarchicalUri[] newArray(int size) {
                return new HierarchicalUri[size];
            }
        };
    }
    
    /**
     * Get the scheme of this URI, or null if no scheme.
     */
    public abstract String getScheme();
    
    /**
     * Get the authority of this URI, or null if no authority.
     */
    public abstract String getAuthority();
    
    /**
     * Get the path of this URI, or null if no path.
     */
    public abstract String getPath();
    
    /**
     * Get the query of this URI, or null if no query.
     */
    public abstract String getQuery();
    
    /**
     * Get the fragment of this URI, or null if no fragment.
     */
    public abstract String getFragment();
    
    /**
     * Get the encoded path segments.
     */
    public String[] getPathSegments() {
        String path = getPath();
        if (path == null) {
            return new String[0];
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path.split("/");
    }
    
    /**
     * Get a decoded query parameter.
     * 
     * @param key the parameter name
     * @return the parameter value, or null if no parameter
     */
    public String getQueryParameter(String key) {
        String query = getQuery();
        if (query == null) {
            return null;
        }
        
        Map<String, String> params = parseQuery(query);
        return params.get(key);
    }
    
    /**
     * Parse a query string and return the parameters.
     */
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx != -1) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                params.put(key, value);
            }
        }
        return params;
    }
    
    /**
     * Get the last segment in the path.
     */
    public String getLastPathSegment() {
        String path = getPath();
        if (path == null) {
            return null;
        }
        
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash == -1) {
            return path;
        }
        
        return path.substring(lastSlash + 1);
    }
    
    /**
     * Create a new Uri by appending a path segment to the end of the path.
     */
    public Uri buildUpon() {
        return new Builder()
                .scheme(getScheme())
                .authority(getAuthority())
                .path(getPath())
                .query(getQuery())
                .fragment(getFragment())
                .build();
    }
    
    /**
     * Builder for Uri objects.
     */
    public static final class Builder {
        private String scheme;
        private String authority;
        private String path;
        private String query;
        private String fragment;
        
        public Builder() {
        }
        
        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }
        
        public Builder authority(String authority) {
            this.authority = authority;
            return this;
        }
        
        public Builder path(String path) {
            this.path = path;
            return this;
        }
        
        public Builder appendPath(String newSegment) {
            if (path == null) {
                path = "";
            }
            
            if (path.endsWith("/")) {
                path += newSegment;
            } else {
                path += "/" + newSegment;
            }
            return this;
        }
        
        public Builder query(String query) {
            this.query = query;
            return this;
        }
        
        public Builder appendQueryParameter(String key, String value) {
            if (key == null) {
                return this;
            }
            
            String paramValue = (value != null) ? value : "";
            String param = key + "=" + paramValue;
            
            if (query == null) {
                query = param;
            } else {
                query += "&" + param;
            }
            return this;
        }
        
        public Builder fragment(String fragment) {
            this.fragment = fragment;
            return this;
        }
        
        public Uri build() {
            return new HierarchicalUri(scheme, authority, path, query, fragment);
        }
    }
    
    /**
     * Create a Uri from the given string.
     * 
     * @param uriString the string to parse
     * @return the parsed Uri
     */
    public static Uri parse(String uriString) {
        // Simplified parsing
        Builder builder = new Builder();
        
        int fragmentIndex = uriString.indexOf('#');
        if (fragmentIndex != -1) {
            builder.fragment(uriString.substring(fragmentIndex + 1));
            uriString = uriString.substring(0, fragmentIndex);
        }
        
        int queryIndex = uriString.indexOf('?');
        if (queryIndex != -1) {
            builder.query(uriString.substring(queryIndex + 1));
            uriString = uriString.substring(0, queryIndex);
        }
        
        int schemeEnd = uriString.indexOf("://");
        if (schemeEnd != -1) {
            builder.scheme(uriString.substring(0, schemeEnd));
            uriString = uriString.substring(schemeEnd + 3);
        }
        
        int pathStart = uriString.indexOf('/');
        if (pathStart != -1) {
            builder.authority(uriString.substring(0, pathStart));
            builder.path(uriString.substring(pathStart));
        } else {
            builder.authority(uriString);
        }
        
        return builder.build();
    }
    
    /**
     * Compare this Uri to another object for equality.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Uri)) return false;
        Uri uri = (Uri) o;
        return toString().equals(uri.toString());
    }
    
    /**
     * Compare this Uri to another.
     */
    @Override
    public int compareTo(Uri other) {
        return toString().compareTo(other.toString());
    }
    
    /**
     * Calculate the hash code.
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }
    
    /**
     * Parcelable implementation.
     */
    public static final Parcelable.Creator<Uri> CREATOR = new Parcelable.Creator<Uri>() {
        @Override
        public Uri createFromParcel(Parcel source) {
            return parse(source.readString());
        }
        
        @Override
        public Uri[] newArray(int size) {
            return new Uri[size];
        }
    };
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(toString());
    }
}