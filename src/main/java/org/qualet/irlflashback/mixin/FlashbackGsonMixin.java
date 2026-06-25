package org.qualet.irlflashback.mixin;

import com.google.gson.GsonBuilder;
import com.moulberry.flashback.FlashbackGson;
import org.qualet.irlflashback.keyframe.IrlLightKeyframe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Registers the concrete {@link IrlLightKeyframe} Gson adapter into Flashback's serialiser. The
 * fork did this with a plain {@code registerTypeAdapter} line in {@code FlashbackGson.build()};
 * the addon adds it by injecting at the builder's RETURN ({@code registerTypeAdapter} mutates the
 * builder in place, so no return-value rewrite is needed). Mirrors how Flashback registers an
 * adapter for every built-in keyframe (CameraKeyframe, etc.).
 *
 * <p>Without this, {@code context.serialize(irlLightKeyframe)} (used by the editor history) falls
 * back to default field reflection and produces JSON the {@code Keyframe} hierarchy deserialiser
 * can't read — breaking editor-state load.</p>
 */
@Mixin(value = FlashbackGson.class, remap = false)
public class FlashbackGsonMixin {

    @Inject(method = "build", at = @At("RETURN"), remap = false)
    private static void irl$registerIrlLightAdapter(CallbackInfoReturnable<GsonBuilder> cir) {
        cir.getReturnValue().registerTypeAdapter(IrlLightKeyframe.class, new IrlLightKeyframe.TypeAdapter());
    }
}
