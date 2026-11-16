package com.example.gap;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Edge-to-edge setup (enforced when targeting Android 15+). Keeps content clear of the status bar,
 * navigation bar, display cutout and keyboard.
 */
final class SystemBars {

    private SystemBars() {}

    /** Same as {@link #apply(ComponentActivity, View, View, boolean)} using the content view's root. */
    static void apply(ComponentActivity activity, @Nullable View topBar, boolean darkStatusBar) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        apply(activity, content.getChildAt(0), topBar, darkStatusBar);
    }

    /**
     * @param root          the activity's root view; gets side/bottom padding (and top padding if there is no top bar)
     * @param topBar        optional coloured toolbar that should extend behind the status bar
     * @param darkStatusBar true when the area behind the status bar is dark/coloured (use light icons)
     */
    static void apply(ComponentActivity activity, View root, @Nullable View topBar, boolean darkStatusBar) {
        EdgeToEdge.enable(activity,
                darkStatusBar ? SystemBarStyle.dark(Color.TRANSPARENT)
                        : SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT));

        final int rootL = root.getPaddingLeft(), rootT = root.getPaddingTop();
        final int rootR = root.getPaddingRight(), rootB = root.getPaddingBottom();
        final int barT = topBar != null ? topBar.getPaddingTop() : 0;
        final int barHeight = topBar != null && topBar.getLayoutParams() != null
                ? topBar.getLayoutParams().height : ViewGroup.LayoutParams.WRAP_CONTENT;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom = Math.max(bars.bottom, ime.bottom);

            int top = rootT;
            if (topBar != null) {
                topBar.setPadding(topBar.getPaddingLeft(), barT + bars.top,
                        topBar.getPaddingRight(), topBar.getPaddingBottom());
                if (barHeight > 0) {
                    ViewGroup.LayoutParams lp = topBar.getLayoutParams();
                    lp.height = barHeight + bars.top;
                    topBar.setLayoutParams(lp);
                }
            } else {
                top += bars.top;
            }
            v.setPadding(rootL + bars.left, top, rootR + bars.right, rootB + bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
