package org.qualet.irlflashback.keyframe;

import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.keyframe.handler.KeyframeHandler;
import com.moulberry.flashback.keyframe.handler.MinecraftKeyframeHandler;
import imgui.moulberry90.ImGui;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.Nullable;
import org.qualet.irlflashback.IrlLightsBridge;

/**
 * Keyframe type that animates one IR Lights source. Soft-dependent on the {@code irl-redactor}
 * mod: when IR Lights is absent the type is still registered (so saved scenes load), but
 * {@link #canBeCreatedNormally()} hides it from the add menu and {@link #supportsHandler} stops it
 * ever applying. Multiple tracks (one per light) are allowed via
 * {@link #allowApplyingDuplicateKeyframeChanges()}.
 *
 * <p>Unlike a Flashback fork, this addon does NOT touch {@code MinecraftKeyframeHandler}; instead of
 * relying on its {@code supportedChanges} set, {@link #supportsHandler} matches the client handler
 * type directly (the same trick {@code AudioKeyframeType} uses), and the change applies through the
 * IRL bridge rather than a handler method.</p>
 */
public class IrlLightKeyframeType implements KeyframeType<IrlLightKeyframe> {

    public static IrlLightKeyframeType INSTANCE = new IrlLightKeyframeType();

    private IrlLightKeyframeType() {
    }

    @Override
    public Class<? extends KeyframeChange> keyframeChangeType() {
        return KeyframeChangeIrlLight.class;
    }

    @Override
    public boolean supportsHandler(KeyframeHandler handler) {
        // Client handler only (preview + export both use MinecraftKeyframeHandler), and only when
        // IR Lights is present. No mixin into the handler's supportedChanges is needed.
        return IrlLightsBridge.isAvailable() && MinecraftKeyframeHandler.class.isAssignableFrom(handler.getClass());
    }

    @Override
    public @Nullable String icon() {
        return null;
    }

    @Override
    public String name() {
        return I18n.get("flashback.keyframe.irl_light");
    }

    @Override
    public String id() {
        return "IRL_LIGHT";
    }

    @Override
    public boolean allowApplyingDuplicateKeyframeChanges() {
        // One track per light: each track's change must apply, not be deduped by class.
        return true;
    }

    @Override
    public boolean canBeCreatedNormally() {
        // Hidden from the timeline add menu unless IR Lights is present.
        return IrlLightsBridge.isAvailable();
    }

    @Override
    public @Nullable IrlLightKeyframe createDirect() {
        return null;
    }

    @Override
    public KeyframeCreatePopup<IrlLightKeyframe> createPopup() {
        return () -> {
            String[] lights = IrlLightsBridge.listLights();
            if (lights.length == 0) {
                ImGui.text(I18n.get("flashback.keyframe.irl_light.none"));
                if (ImGui.button(I18n.get("gui.cancel"))) {
                    ImGui.closeCurrentPopup();
                }
                return null;
            }

            ImGui.text(I18n.get("flashback.keyframe.irl_light.pick"));
            for (int i = 0; i + 1 < lights.length; i += 2) {
                String uid = lights[i];
                String name = lights[i + 1];
                if (ImGui.selectable(name == null || name.isEmpty() ? uid : name)) {
                    double[] snapshot = IrlLightsBridge.snapshot(uid);
                    if (snapshot == null) {
                        snapshot = new double[IrlLightsBridge.VALUE_COUNT];
                    }
                    ImGui.closeCurrentPopup();
                    return new IrlLightKeyframe(uid, name, snapshot);
                }
            }
            if (ImGui.button(I18n.get("gui.cancel"))) {
                ImGui.closeCurrentPopup();
            }
            return null;
        };
    }
}
