package org.qualet.irlflashback;

import com.moulberry.flashback.keyframe.KeyframeRegistry;
import net.fabricmc.api.ClientModInitializer;
import org.qualet.irlflashback.keyframe.IrlLightKeyframeType;

/**
 * Addon entrypoint. Registers the {@code IRL Light} keyframe type into Flashback's registry.
 *
 * <p>Registered unconditionally (even without IR Lights) so a scene saved with IRL-light tracks
 * still deserialises when IR Lights is absent; the type hides itself and never applies via
 * {@code IrlLightsBridge.isAvailable()}. Serialisation of the new type is handled by
 * {@code KeyframeTypeAdapterMixin}, so no change to Flashback's source is needed.</p>
 */
public class IrlFlashbackAddonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyframeRegistry.register(IrlLightKeyframeType.INSTANCE);
    }
}
