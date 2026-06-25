package org.qualet.irlflashback.mixin;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;
import org.qualet.irlflashback.keyframe.IrlLightKeyframe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

/**
 * Teaches Flashback's keyframe (de)serialiser about the addon's {@code irl_light} keyframe without
 * forking Flashback. {@code Keyframe$TypeAdapter} dispatches on a hardcoded {@code switch} (and
 * throws on anything unknown), so both halves are intercepted at HEAD: if the keyframe is ours we
 * build/parse the JSON here and cancel before the switch runs; otherwise the original method
 * proceeds untouched.
 */
// remap = false: targets are Flashback's own (non-Minecraft) members, whose names are
// identical in dev and production, so the mixin AP must not try to remap them.
@Mixin(value = Keyframe.TypeAdapter.class, remap = false)
public class KeyframeTypeAdapterMixin {

    @Inject(method = "deserialize", at = @At("HEAD"), cancellable = true)
    private void irl$deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context,
                                 CallbackInfoReturnable<Keyframe> cir) {
        JsonObject obj = json.getAsJsonObject();
        boolean tagged = obj.has("type") && "irl_light".equals(obj.get("type").getAsString());
        // Tolerate keyframes written by default Gson reflection (camelCase, no "type") — e.g. an
        // editor-history SetKeyframe saved before the concrete adapter was registered. Such a
        // keyframe is unmistakably ours (Flashback's own types always carry "type").
        boolean untaggedLegacy = !obj.has("type") && obj.has("values")
            && (obj.has("targetUid") || obj.has("target_uid"));
        if (tagged || untaggedLegacy) {
            String uid = obj.has("target_uid") ? obj.get("target_uid").getAsString()
                       : obj.has("targetUid") ? obj.get("targetUid").getAsString() : "";
            String name = obj.has("target_name") ? obj.get("target_name").getAsString()
                        : obj.has("targetName") ? obj.get("targetName").getAsString() : "";
            double[] values = context.deserialize(obj.get("values"), double[].class);
            JsonElement interp = obj.has("interpolation_type") ? obj.get("interpolation_type") : obj.get("interpolationType");
            InterpolationType interpolationType = context.deserialize(interp, InterpolationType.class);
            cir.setReturnValue(new IrlLightKeyframe(uid, name, values, interpolationType));
        }
    }

    @Inject(method = "serialize", at = @At("HEAD"), cancellable = true)
    private void irl$serialize(Keyframe src, Type typeOfSrc, JsonSerializationContext context,
                               CallbackInfoReturnable<JsonElement> cir) {
        if (src instanceof IrlLightKeyframe keyframe) {
            JsonObject obj = new JsonObject();
            obj.addProperty("target_uid", keyframe.targetUid);
            obj.addProperty("target_name", keyframe.targetName);
            obj.add("values", context.serialize(keyframe.values));
            obj.addProperty("type", "irl_light");
            obj.add("interpolation_type", context.serialize(keyframe.interpolationType()));
            cir.setReturnValue(obj);
        }
    }
}
