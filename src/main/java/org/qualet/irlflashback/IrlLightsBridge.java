package org.qualet.irlflashback;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

/**
 * Soft (reflection-based) bridge to IR Lights (mod id {@code irl-redactor}). IR Lights is NOT a
 * compile/runtime dependency of this addon: everything is gated behind {@link #isAvailable()}
 * ({@code FabricLoader.isModLoaded("irl-redactor")}) and reached by reflection against the mod's
 * frozen public facade {@code org.qualet.irlredactor.api.IrlFlashbackBridge}, so the addon loads
 * and runs identically with or without it.
 *
 * <p>When IR Lights is absent the {@code IRL Light} keyframe type stays hidden in the timeline
 * menu and never applies (see {@code IrlLightKeyframeType}), yet a saved scene containing such a
 * track still loads — the type is registered but inert.</p>
 *
 * <p>Resolve once, cache the {@link Method}s, never throw.</p>
 */
public final class IrlLightsBridge {

    /** Length of the {@link #snapshot}/{@link #apply} value array. Mirror of
     *  {@code IrlFlashbackBridge.VALUE_COUNT}. */
    public static final int VALUE_COUNT = 18;

    private static final boolean PRESENT = FabricLoader.getInstance().isModLoaded("irl-redactor");

    private static final String[] EMPTY = new String[0];

    private static boolean resolved;
    private static boolean failed;
    private static Method listLights;   // String[] listLights()
    private static Method snapshot;     // double[] snapshot(String)
    private static Method apply;        // void apply(String, double[])

    private IrlLightsBridge() {
    }

    /** True only when IR Lights is loaded and its bridge API resolved successfully. */
    public static boolean isAvailable() {
        return PRESENT && resolve();
    }

    /** Flat {@code [uid0, name0, uid1, name1, ...]} of placed lights, empty if unavailable. */
    public static String[] listLights() {
        if (!isAvailable()) {
            return EMPTY;
        }
        try {
            Object result = listLights.invoke(null);
            return result instanceof String[] arr ? arr : EMPTY;
        } catch (Throwable t) {
            return EMPTY;
        }
    }

    /** Current values of the light with this uid (18-slot layout), or null if missing. */
    public static double[] snapshot(String uid) {
        if (!isAvailable()) {
            return null;
        }
        try {
            Object result = snapshot.invoke(null, uid);
            return result instanceof double[] arr ? arr : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Write animated values into the light with this uid. No-op if unavailable. */
    public static void apply(String uid, double[] values) {
        if (!isAvailable()) {
            return;
        }
        try {
            apply.invoke(null, uid, values);
        } catch (Throwable ignored) {
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return !failed;
        }
        resolved = true;
        try {
            Class<?> c = Class.forName("org.qualet.irlredactor.api.IrlFlashbackBridge");
            listLights = c.getMethod("listLights");
            snapshot = c.getMethod("snapshot", String.class);
            apply = c.getMethod("apply", String.class, double[].class);
        } catch (Throwable t) {
            failed = true;
            org.slf4j.LoggerFactory.getLogger("irl-flashback")
                .warn("IrlLightsBridge: reflection to org.qualet.irlredactor.api.IrlFlashbackBridge failed", t);
        }
        return !failed;
    }
}
