package android.os;

/**
 * Mock implementation of Android's ResultReceiver class.
 * Generic interface for receiving a callback result from someone.
 */
public class ResultReceiver {
    /**
     * Standard handler callback result code indicating success.
     */
    public static final int RESULT_OK = 0;
    
    /**
     * Handler used to receive callback results.
     */
    private final Handler mHandler;
    
    /**
     * Create a new ResultReceiver with a Handler on which to dispatch the result.
     */
    public ResultReceiver(Handler handler) {
        mHandler = handler;
    }
    
    /**
     * Send a result to the ResultReceiver.
     */
    public void send(int resultCode, Bundle resultData) {
        if (mHandler != null) {
            Message msg = Message.obtain();
            msg.what = resultCode;
            msg.obj = resultData;
            mHandler.sendMessage(msg);
        }
    }
    
    /**
     * Overridden to handle the result.
     */
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        // Mock implementation, do nothing
    }
}