package org.qualet.irlflashback.keyframe;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;
import com.moulberry.flashback.spline.CatmullRom;
import com.moulberry.flashback.spline.Hermite;
import imgui.moulberry90.ImGui;
import org.joml.Vector3d;
import org.qualet.irlflashback.IrlLightsBridge;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A keyframe that drives one IR Lights source. {@link #targetUid} binds the whole track to a single
 * persistent light (stable across save/reload); {@link #values} holds that light's animated state
 * in the fixed 18-slot layout shared with {@link IrlLightsBridge}. Position (slots 0..2)
 * interpolates as a vector like the camera; the remaining scalar slots interpolate per-component.
 */
public class IrlLightKeyframe extends Keyframe {

    private static final String[] LABELS = {
        "X", "Y", "Z",
        "Red", "Green", "Blue",
        "Intensity", "Radius", "Range",
        "Dir X", "Dir Y", "Dir Z",
        "Outer angle", "Inner angle",
        "Beam", "Density", "Anisotropy", "Bulb size"
    };

    /** Persistent uid of the target light (see IRL's PlacedLight#uid). */
    public String targetUid;
    /** Display name of the target light at capture time (UI only). */
    public String targetName;
    /** Animated values, fixed {@link IrlLightsBridge#VALUE_COUNT}-slot layout. */
    public final double[] values;

    public IrlLightKeyframe(String targetUid, String targetName, double[] values) {
        this(targetUid, targetName, values, InterpolationType.getDefault());
    }

    public IrlLightKeyframe(String targetUid, String targetName, double[] values, InterpolationType interpolationType) {
        this.targetUid = targetUid;
        this.targetName = targetName == null ? "" : targetName;
        this.values = normalize(values);
        this.interpolationType(interpolationType);
    }

    private static double[] normalize(double[] in) {
        double[] out = new double[IrlLightsBridge.VALUE_COUNT];
        if (in != null) {
            System.arraycopy(in, 0, out, 0, Math.min(in.length, out.length));
        }
        return out;
    }

    private Vector3d position() {
        return new Vector3d(this.values[0], this.values[1], this.values[2]);
    }

    @Override
    public KeyframeType<?> keyframeType() {
        return IrlLightKeyframeType.INSTANCE;
    }

    @Override
    public Keyframe copy() {
        return new IrlLightKeyframe(this.targetUid, this.targetName, this.values.clone(), this.interpolationType());
    }

    @Override
    public KeyframeChange createChange() {
        return new KeyframeChangeIrlLight(this.targetUid, this.values.clone());
    }

    @Override
    public KeyframeChange createSmoothInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        float time1 = t1 - t0;
        float time2 = t2 - t0;
        float time3 = t3 - t0;

        IrlLightKeyframe k1 = (IrlLightKeyframe) p1;
        IrlLightKeyframe k2 = (IrlLightKeyframe) p2;
        IrlLightKeyframe k3 = (IrlLightKeyframe) p3;

        double[] out = new double[IrlLightsBridge.VALUE_COUNT];

        Vector3d pos = CatmullRom.position(this.position(), k1.position(), k2.position(), k3.position(),
            time1, time2, time3, amount);
        out[0] = pos.x;
        out[1] = pos.y;
        out[2] = pos.z;

        for (int i = 3; i < out.length; i++) {
            out[i] = CatmullRom.value((float) this.values[i], (float) k1.values[i], (float) k2.values[i],
                (float) k3.values[i], time1, time2, time3, amount);
        }

        return new KeyframeChangeIrlLight(this.targetUid, out);
    }

    @Override
    public KeyframeChange createHermiteInterpolatedChange(Map<Float, Keyframe> keyframes, float amount) {
        double[] out = new double[IrlLightsBridge.VALUE_COUNT];

        Map<Float, Vector3d> posMap = new HashMap<>();
        for (Map.Entry<Float, Keyframe> entry : keyframes.entrySet()) {
            posMap.put(entry.getKey(), ((IrlLightKeyframe) entry.getValue()).position());
        }
        Vector3d pos = Hermite.position(posMap, amount);
        out[0] = pos.x;
        out[1] = pos.y;
        out[2] = pos.z;

        for (int i = 3; i < out.length; i++) {
            final int idx = i;
            Map<Float, Double> map = new HashMap<>();
            for (Map.Entry<Float, Keyframe> entry : keyframes.entrySet()) {
                map.put(entry.getKey(), ((IrlLightKeyframe) entry.getValue()).values[idx]);
            }
            out[i] = Hermite.value(map, amount);
        }

        return new KeyframeChangeIrlLight(this.targetUid, out);
    }

    @Override
    public void renderEditKeyframe(Consumer<Consumer<Keyframe>> update) {
        ImGui.text("Light: " + (this.targetName == null || this.targetName.isEmpty() ? this.targetUid : this.targetName));
        for (int i = 0; i < this.values.length; i++) {
            final int idx = i;
            float[] input = new float[]{(float) this.values[i]};
            if (ImGuiHelper.inputFloat(LABELS[i], input)) {
                if (input[0] != (float) this.values[idx]) {
                    final float newValue = input[0];
                    update.accept(keyframe -> ((IrlLightKeyframe) keyframe).values[idx] = newValue);
                }
            }
        }
    }

    /**
     * Concrete Gson adapter for {@link IrlLightKeyframe}. Registered into Flashback's Gson by
     * {@code FlashbackGsonMixin}. Needed because some Flashback code (e.g. the editor-history
     * {@code SetKeyframe}) serialises a keyframe via {@code context.serialize(keyframe)} — by its
     * RUNTIME type — instead of through the {@code Keyframe} hierarchy adapter. Without this, Gson
     * falls back to default field reflection (camelCase, no {@code "type"}), which the hierarchy
     * deserialiser then can't read, and the whole editor state fails to load. This produces the
     * SAME shape as the {@code Keyframe$TypeAdapter} mixin (snake_case + {@code "type":"irl_light"}).
     */
    public static class TypeAdapter implements JsonSerializer<IrlLightKeyframe>, JsonDeserializer<IrlLightKeyframe> {
        @Override
        public IrlLightKeyframe deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String uid = obj.has("target_uid") ? obj.get("target_uid").getAsString() : "";
            String name = obj.has("target_name") ? obj.get("target_name").getAsString() : "";
            double[] values = context.deserialize(obj.get("values"), double[].class);
            InterpolationType interpolationType = context.deserialize(obj.get("interpolation_type"), InterpolationType.class);
            return new IrlLightKeyframe(uid, name, values, interpolationType);
        }

        @Override
        public JsonElement serialize(IrlLightKeyframe src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("target_uid", src.targetUid);
            obj.addProperty("target_name", src.targetName);
            obj.add("values", context.serialize(src.values));
            obj.addProperty("type", "irl_light");
            obj.add("interpolation_type", context.serialize(src.interpolationType()));
            return obj;
        }
    }
}
