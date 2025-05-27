package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for fixing method signature issues.
 * This class provides methods for detecting and fixing method signature
 * mismatches, particularly with regard to override annotations.
 */
public class MethodSignatureHelper {
    
    /**
     * Class representing a method signature
     */
    public static class MethodSignature {
        private final String name;
        private final String returnType;
        private final String[] parameterTypes;
        private final String[] parameterNames;
        private final String access;
        private final boolean isStatic;
        
        /**
         * Create a new MethodSignature
         * 
         * @param name The method name
         * @param returnType The return type
         * @param parameterTypes The parameter types
         * @param parameterNames The parameter names
         * @param access The access modifier
         * @param isStatic Whether the method is static
         */
        public MethodSignature(String name, String returnType, String[] parameterTypes, 
                        String[] parameterNames, String access, boolean isStatic) {
            this.name = name;
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
            this.parameterNames = parameterNames;
            this.access = access;
            this.isStatic = isStatic;
        }
        
        /**
         * Get the method name
         * 
         * @return The method name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Get the return type
         * 
         * @return The return type
         */
        public String getReturnType() {
            return returnType;
        }
        
        /**
         * Get the parameter types
         * 
         * @return The parameter types
         */
        public String[] getParameterTypes() {
            return parameterTypes;
        }
        
        /**
         * Get the parameter names
         * 
         * @return The parameter names
         */
        public String[] getParameterNames() {
            return parameterNames;
        }
        
        /**
         * Get the access modifier
         * 
         * @return The access modifier
         */
        public String getAccess() {
            return access;
        }
        
        /**
         * Check if the method is static
         * 
         * @return True if the method is static
         */
        public boolean isStatic() {
            return isStatic;
        }
        
        /**
         * Get the full method signature
         * 
         * @return The full method signature
         */
        public String getFullSignature() {
            StringBuilder sb = new StringBuilder();
            
            // Add access modifier
            if (access != null && !access.isEmpty()) {
                sb.append(access).append(" ");
            }
            
            // Add static modifier
            if (isStatic) {
                sb.append("static ");
            }
            
            // Add return type
            sb.append(returnType).append(" ");
            
            // Add method name
            sb.append(name).append("(");
            
            // Add parameters
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(parameterTypes[i]).append(" ").append(parameterNames[i]);
            }
            
            sb.append(")");
            
            return sb.toString();
        }
        
        /**
         * Get the method signature with parameter types only
         * 
         * @return The method signature with parameter types only
         */
        public String getSignatureWithParameterTypes() {
            StringBuilder sb = new StringBuilder();
            
            // Add method name
            sb.append(name).append("(");
            
            // Add parameter types
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(parameterTypes[i]);
            }
            
            sb.append(")");
            
            return sb.toString();
        }
    }
    private static final String TAG = "MethodSignatureHelper";
    
    /**
     * Fix content with method override issues
     * @param content The file content to fix
     * @return The fixed content
     */
    public static String fixFromMethodOverrideIssues(String content) {
        return fixFromMethodOverrideIssues(content, null);
    }
    
    /**
     * Fix content with method override issues
     * @param content The file content to fix
     * @param className The class name (for logging purposes)
     * @return The fixed content
     */
    public static String fixFromMethodOverrideIssues(String content, String className) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        String classIdentifier = className != null ? className : "Unknown";
        
        // Remove incorrect @Override annotations
        Pattern overridePattern = Pattern.compile("(\\s+)@Override(\\s+)");
        Matcher overrideMatcher = overridePattern.matcher(content);
        
        StringBuilder fixedContent = new StringBuilder(content);
        int offset = 0;
        
        while (overrideMatcher.find()) {
            int start = overrideMatcher.start() + offset;
            int end = overrideMatcher.end() + offset;
            String whitespace = overrideMatcher.group(1) + overrideMatcher.group(2);
            
            // Check if the next line has a method definition
            int nextLineStart = content.indexOf('\n', end - offset);
            if (nextLineStart != -1) {
                String nextLine = content.substring(nextLineStart, Math.min(content.length(), nextLineStart + 200));
                if (nextLine.contains("method does not override") || nextLine.contains("does not override or implement")) {
                    // Remove the @Override annotation
                    fixedContent.replace(start, end, whitespace);
                    offset -= "@Override".length();
                }
            }
        }
        
        return fixedContent.toString();
    }
    
    /**
     * Fix method signature mismatches
     * @param content The file content to fix
     * @param className The class name
     * @param methodName The method name
     * @param correctSignature The correct method signature
     * @return The fixed content
     */
    public static String fixMethodSignature(String content, String className, String methodName, String correctSignature) {
        if (content == null || content.isEmpty() || methodName == null || methodName.isEmpty() || correctSignature == null || correctSignature.isEmpty()) {
            return content;
        }
        
        // Find the method in the content
        Pattern methodPattern = Pattern.compile("(\\s+)(public|private|protected)?\\s+(static)?\\s*(\\w+)\\s+" + Pattern.quote(methodName) + "\\s*\\([^)]*\\)");
        Matcher methodMatcher = methodPattern.matcher(content);
        
        if (methodMatcher.find()) {
            int start = methodMatcher.start();
            int end = methodMatcher.end();
            
            // Extract indentation
            String indentation = methodMatcher.group(1);
            
            // Extract access modifier
            String accessModifier = methodMatcher.group(2);
            if (accessModifier == null) {
                accessModifier = "";
            }
            
            // Extract static modifier
            String staticModifier = methodMatcher.group(3);
            if (staticModifier == null) {
                staticModifier = "";
            } else {
                staticModifier += " ";
            }
            
            // Build the replacement
            String replacement = indentation + accessModifier + " " + staticModifier + correctSignature;
            
            // Replace the method signature
            return content.substring(0, start) + replacement + content.substring(end);
        }
        
        return content;
    }
    
    /**
     * Fix parameter type mismatches
     * @param content The file content to fix
     * @param className The class name
     * @param methodName The method name
     * @param parameterIndex The parameter index (0-based)
     * @param correctType The correct parameter type
     * @return The fixed content
     */
    public static String fixParameterType(String content, String className, String methodName, int parameterIndex, String correctType) {
        if (content == null || content.isEmpty() || methodName == null || methodName.isEmpty() || correctType == null || correctType.isEmpty() || parameterIndex < 0) {
            return content;
        }
        
        // Find the method in the content
        Pattern methodPattern = Pattern.compile("(\\s+)(public|private|protected)?\\s+(static)?\\s*(\\w+)\\s+" + Pattern.quote(methodName) + "\\s*\\(([^)]*)\\)");
        Matcher methodMatcher = methodPattern.matcher(content);
        
        if (methodMatcher.find()) {
            String parameters = methodMatcher.group(5);
            
            // Split the parameters
            String[] parameterArray = parameters.split(",");
            
            // Check if the parameter index is valid
            if (parameterIndex >= parameterArray.length) {
                return content;
            }
            
            // Extract the parameter
            String parameter = parameterArray[parameterIndex].trim();
            
            // Extract the variable name
            Pattern variablePattern = Pattern.compile("\\s+(\\w+)$");
            Matcher variableMatcher = variablePattern.matcher(parameter);
            
            if (variableMatcher.find()) {
                String variableName = variableMatcher.group(1);
                
                // Replace the parameter type
                parameterArray[parameterIndex] = correctType + " " + variableName;
                
                // Rebuild the parameters
                StringBuilder parametersBuilder = new StringBuilder();
                for (int i = 0; i < parameterArray.length; i++) {
                    if (i > 0) {
                        parametersBuilder.append(", ");
                    }
                    parametersBuilder.append(parameterArray[i]);
                }
                
                // Replace the parameters in the method signature
                int start = methodMatcher.start(5);
                int end = methodMatcher.end(5);
                
                return content.substring(0, start) + parametersBuilder.toString() + content.substring(end);
            }
        }
        
        return content;
    }
    
    /**
     * Fix return type mismatches
     * @param content The file content to fix
     * @param className The class name
     * @param methodName The method name
     * @param correctReturnType The correct return type
     * @return The fixed content
     */
    public static String fixReturnType(String content, String className, String methodName, String correctReturnType) {
        if (content == null || content.isEmpty() || methodName == null || methodName.isEmpty() || correctReturnType == null || correctReturnType.isEmpty()) {
            return content;
        }
        
        // Find the method in the content
        Pattern methodPattern = Pattern.compile("(\\s+)(public|private|protected)?\\s+(static)?\\s*(\\w+)\\s+" + Pattern.quote(methodName) + "\\s*\\(");
        Matcher methodMatcher = methodPattern.matcher(content);
        
        if (methodMatcher.find()) {
            int start = methodMatcher.start(4);
            int end = methodMatcher.end(4);
            
            // Replace the return type
            return content.substring(0, start) + correctReturnType + content.substring(end);
        }
        
        return content;
    }
}