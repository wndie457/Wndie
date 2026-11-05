package com.maks.otacard;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.InputStream;

public class Hook implements IXposedHookLoadPackage {
    private static final String TAG = "OtaCard";
    private static final int OVERLAY_TAG = 0x0A7A0001;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!"com.android.settings".equals(lp.packageName)) return;
        XposedBridge.log(TAG + ": loaded in Settings");
        XposedHelpers.findAndHookMethod(View.class, "onAttachedToWindow", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                try {
                    View v = (View) param.thisObject;
                    if (!(v instanceof FrameLayout) || v.getId() == View.NO_ID) return;
                    String name = v.getResources().getResourceEntryName(v.getId());
                    if (!"about_device_top_video_card".equals(name)) return;
                    apply((FrameLayout) v);
                } catch (Throwable ignored) { }
            }
        });
    }

    private static void apply(FrameLayout card) {
        Context ctx = card.getContext();
        Bitmap bm = load(ctx);
        View old = card.findViewWithTag(OVERLAY_TAG);
        if (old != null) card.removeView(old);
        if (bm == null) return;
        ImageView iv = new ImageView(ctx);
        iv.setTag(OVERLAY_TAG);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setImageBitmap(bm);
        final float r = 21 * ctx.getResources().getDisplayMetrics().density;
        iv.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline o) { o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r); }
        });
        iv.setClipToOutline(true);
        int idx = Math.min(1, card.getChildCount());
        card.addView(iv, idx, new FrameLayout.LayoutParams(-1, -1));
        for (String n : new String[]{"lin_center", "update_text"}) {
            int id = ctx.getResources().getIdentifier(n, "id", ctx.getPackageName());
            View t = id != 0 ? card.findViewById(id) : null;
            if (t != null) t.bringToFront();
        }
        XposedBridge.log(TAG + ": overlay applied");
    }

    private static Bitmap load(Context ctx) {
        try (InputStream in = ctx.getContentResolver().openInputStream(Uri.parse("content://com.maks.otacard.provider/card"))) {
            return in == null ? null : BitmapFactory.decodeStream(in);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": load failed " + t);
            return null;
        }
    }
}
