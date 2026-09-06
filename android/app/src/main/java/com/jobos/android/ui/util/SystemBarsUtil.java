package com.jobos.android.ui.util;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.DisplayCutoutCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.jobos.android.R;

public final class SystemBarsUtil {

    private SystemBarsUtil() {
    }

    public static void applyTopInsetToToolbarArea(View root) {
        if (root == null) return;

        View toolbar = root.findViewById(R.id.toolbar);
        View target = toolbar != null ? findAppBarAncestor(toolbar) : null;
        if (target == null) {
            target = findFirstAppBar(root);
        }
        if (target == null) {
            target = toolbar;
        }
        if (target != null) {
            applyTopInset(target);
        }
    }

    public static void applyBottomInset(View view) {
        if (view == null) return;

        int startPadding = view.getPaddingStart();
        int topPadding = view.getPaddingTop();
        int endPadding = view.getPaddingEnd();
        int bottomPadding = view.getPaddingBottom();
        int initialHeight = getExplicitHeight(view);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPaddingRelative(startPadding, topPadding, endPadding, bottomPadding + navBars.bottom);
            applyExplicitHeight(v, initialHeight, navBars.bottom);
            return insets;
        });

        requestInsetsWhenAttached(view);
    }

    private static void applyTopInset(View view) {
        int startPadding = view.getPaddingStart();
        int topPadding = view.getPaddingTop();
        int endPadding = view.getPaddingEnd();
        int bottomPadding = view.getPaddingBottom();
        int initialMinHeight = view.getMinimumHeight();
        int initialHeight = getExplicitHeight(view);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int topInset = getTopSafeInset(insets);
            v.setPaddingRelative(startPadding, topPadding + topInset, endPadding, bottomPadding);

            if (initialMinHeight > 0) {
                v.setMinimumHeight(initialMinHeight + topInset);
            }
            applyExplicitHeight(v, initialHeight, topInset);
            return insets;
        });

        requestInsetsWhenAttached(view);
    }

    private static int getTopSafeInset(WindowInsetsCompat insets) {
        Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
        DisplayCutoutCompat cutout = insets.getDisplayCutout();
        int cutoutTop = cutout != null ? cutout.getSafeInsetTop() : 0;
        return Math.max(statusBars.top, cutoutTop);
    }

    private static View findAppBarAncestor(View view) {
        View current = view;
        while (current != null) {
            if (current instanceof AppBarLayout) {
                return current;
            }
            if (!(current.getParent() instanceof View)) {
                return null;
            }
            current = (View) current.getParent();
        }
        return null;
    }

    private static View findFirstAppBar(View view) {
        if (view instanceof AppBarLayout) {
            return view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }

        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            View found = findFirstAppBar(group.getChildAt(i));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int getExplicitHeight(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        return params != null && params.height > 0 ? params.height : 0;
    }

    private static void applyExplicitHeight(View view, int initialHeight, int inset) {
        if (initialHeight <= 0) return;

        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) return;

        int height = initialHeight + inset;
        if (params.height != height) {
            params.height = height;
            view.setLayoutParams(params);
        }
    }

    private static void requestInsetsWhenAttached(View view) {
        if (ViewCompat.isAttachedToWindow(view)) {
            ViewCompat.requestApplyInsets(view);
            return;
        }

        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View attachedView) {
                attachedView.removeOnAttachStateChangeListener(this);
                ViewCompat.requestApplyInsets(attachedView);
            }

            @Override
            public void onViewDetachedFromWindow(View detachedView) {
            }
        });
    }
}
