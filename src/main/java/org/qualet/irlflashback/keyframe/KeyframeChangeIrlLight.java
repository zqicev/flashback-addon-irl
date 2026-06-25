package org.qualet.irlflashback.keyframe;

import com.moulberry.flashback.Interpolation;
import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.keyframe.handler.KeyframeHandler;
import org.qualet.irlflashback.IrlLightsBridge;

/**
 * Interpolated light state to push into IR Lights for a single target light. The {@code values}
 * array follows the fixed 18-slot layout shared with the IRL bridge.
 *
 * <p>Unlike the in-fork version, the addon does not add an {@code applyIrlLight} method to vanilla
 * Flashback's {@code KeyframeHandler}; the change applies directly through {@link IrlLightsBridge}
 * (the keyframe only ever runs on the client handler — gated by {@code IrlLightKeyframeType}).</p>
 */
public record KeyframeChangeIrlLight(String uid, double[] values) implements KeyframeChange {

    @Override
    public void apply(KeyframeHandler keyframeHandler) {
        IrlLightsBridge.apply(this.uid, this.values);
    }

    @Override
    public KeyframeChange interpolate(KeyframeChange to, double amount) {
        KeyframeChangeIrlLight other = (KeyframeChangeIrlLight) to;
        int n = Math.min(this.values.length, other.values.length);
        double[] result = new double[this.values.length];
        for (int i = 0; i < this.values.length; i++) {
            result[i] = i < n ? Interpolation.linear(this.values[i], other.values[i], amount) : this.values[i];
        }
        return new KeyframeChangeIrlLight(this.uid, result);
    }
}
