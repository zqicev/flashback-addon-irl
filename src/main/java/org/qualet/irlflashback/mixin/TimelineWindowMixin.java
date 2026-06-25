package org.qualet.irlflashback.mixin;

import com.moulberry.flashback.editor.ui.windows.TimelineWindow;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.state.EditorScene;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.KeyframeTrack;
import org.qualet.irlflashback.IrlLightsBridge;
import org.qualet.irlflashback.keyframe.IrlLightKeyframe;
import org.qualet.irlflashback.keyframe.IrlLightKeyframeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * UX nicety: once an IRL Light track has its first keyframe (which chose the target light), every
 * later keyframe on that track reuses the same target and snapshots the light's current live state,
 * instead of re-opening the target picker. Mirrors how a camera keyframe records "the camera as it
 * is now". Without this the addon still works, it just shows the picker on every keyframe.
 */
// remap = false: targets are Flashback's own (non-Minecraft) members, whose names are
// identical in dev and production, so the mixin AP must not try to remap them.
@Mixin(value = TimelineWindow.class, remap = false)
public class TimelineWindowMixin {

    @Shadow
    private static EditorScene editorScene;

    @Shadow
    private static EditorState editorState;

    @Shadow
    private static void upgradeToSceneWrite() {
        throw new AssertionError();
    }

    @Inject(method = "createNewKeyframe", at = @At("HEAD"), cancellable = true)
    private static void irl$inheritTarget(int trackIndex, int tick, KeyframeType<?> keyframeType,
                                          KeyframeTrack keyframeTrack, CallbackInfo ci) {
        if (keyframeType != IrlLightKeyframeType.INSTANCE || keyframeTrack.keyframesByTick.isEmpty()) {
            return;
        }

        IrlLightKeyframe existing = (IrlLightKeyframe) keyframeTrack.keyframesByTick.firstEntry().getValue();
        double[] snapshot = IrlLightsBridge.snapshot(existing.targetUid);
        if (snapshot == null) {
            snapshot = existing.values.clone();
        }

        upgradeToSceneWrite();
        editorScene.setKeyframe(trackIndex, tick, new IrlLightKeyframe(existing.targetUid, existing.targetName, snapshot));
        editorState.markDirty();
        ci.cancel();
    }
}
