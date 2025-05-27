package android.graphics;

/**
 * Mock implementation of Android PorterDuff class for development outside of Android.
 * This contains blending modes for compositing digital images.
 */
public class PorterDuff {
    
    /**
     * Porter-Duff blending modes
     */
    public enum Mode {
        /**
         * [0, 0]
         */
        CLEAR,
        
        /**
         * [Sa, Sc]
         */
        SRC,
        
        /**
         * [Da, Dc]
         */
        DST,
        
        /**
         * [Sa + Da - Sa*Da, Sc + Dc - Sc*Dc]
         */
        SRC_OVER,
        
        /**
         * [Da + Sa - Sa*Da, Dc + Sc - Sc*Dc]
         */
        DST_OVER,
        
        /**
         * [Sa * Da, Sc * Dc]
         */
        SRC_IN,
        
        /**
         * [Sa * Da, Sa * Dc]
         */
        DST_IN,
        
        /**
         * [Sa * (1 - Da), Sc * (1 - Dc)]
         */
        SRC_OUT,
        
        /**
         * [Da * (1 - Sa), Dc * (1 - Sc)]
         */
        DST_OUT,
        
        /**
         * [Da, Sc * Da + Dc * (1 - Sa)]
         */
        SRC_ATOP,
        
        /**
         * [Sa, Sc * (1 - Da) + Dc * Sa]
         */
        DST_ATOP,
        
        /**
         * [Sa + Da - 2 * Sa * Da, Sc + Dc - 2 * Sc * Dc]
         */
        XOR,
        
        /**
         * [Sa + Da - Sa*Da, Sc*Da + Dc*Sa - Sc*Dc]
         */
        DARKEN,
        
        /**
         * [Sa + Da - Sa*Da, Sc*(1 - Da) + Dc*(1 - Sa) + max(Sc, Dc)]
         */
        LIGHTEN,
        
        /**
         * [Sa * Da, Sc * Dc]
         */
        MULTIPLY,
        
        /**
         * [Sa + Da - Sa * Da, Sc + Dc - Sc * Dc]
         */
        SCREEN,
        
        /**
         * Overlay mode
         */
        OVERLAY,
        
        /**
         * Adds the source pixels to the destination pixels and saturates the result.
         */
        ADD,
        
        /**
         * Multiplies or screens the colors, depending on the destination color.
         */
        OVERLAY2
    }
}