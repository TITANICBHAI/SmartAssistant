package utils;

import java.util.concurrent.CompletableFuture;

/**
 * Example class demonstrating how to use ActionResultHelper
 * to replace direct calls to ActionResult.success()
 */
public class ActionResultHelperExample {
    
    /**
     * Example of using ActionResultHelper instead of ActionResult.success
     */
    public void exampleUsage() {
        // Original code:
        // CompletableFuture<Object> future = new CompletableFuture<>();
        // future.complete(ActionResult.success("Action executed successfully"));
        
        // Fixed code:
        CompletableFuture<Object> future = new CompletableFuture<>();
        future.complete(ActionResultHelper.success("Action executed successfully"));
    }
}