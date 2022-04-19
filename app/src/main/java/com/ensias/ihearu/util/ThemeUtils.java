package com.ensias.ihearu.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.core.content.ContextCompat;

public final class ThemeUtils {

    private ThemeUtils() {
    }

    // Get a resource id from a resource styled according to the context's theme.

    public static int resolveResourceIdFromAttr(final Context context, @AttrRes final int attr) {
        final TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
        final int attributeResourceId = a.getResourceId(0, 0);
        a.recycle();
        return attributeResourceId;
    }

    // Get a color from an attr styled according to the context's theme.

    public static int resolveColorFromAttr(final Context context, @AttrRes final int attrColor) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attrColor, value, true);

        if (value.resourceId != 0) {
            return ContextCompat.getColor(context, value.resourceId);
        }

        return value.data;
    }
}
