package com.ensias.ihearu.output.graphical;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.DimenRes;
import androidx.annotation.LayoutRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.ensias.ihearu.util.ExceptionUtils;

import com.ensias.ihearu.R;

import com.ensias.ihearu.util.ThemeUtils;

//this class helps building the different views
public final class GraphicalOutputUtils {

    private GraphicalOutputUtils() {
    }


    // params :- context the Android context to use for the layout inflater
    //         - layout the layout resource id of the layout to inflate
    // return the inflated view

    public static View inflate(final Context context, @LayoutRes final int layout) {
        return LayoutInflater.from(context).inflate(layout, null);
    }

    //return the layout parameters to apply to a child of a vertical linear layout in order to make it horizontally centered.

    public static LinearLayoutCompat.LayoutParams getCenteredLinearLayoutParams() {
        final LinearLayoutCompat.LayoutParams layoutParams = new LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.weight = 1.0f;
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        return layoutParams;
    }

    // Builds a text view
    // parmas: context the Android context to use to initialize the view
    //         text the content of the text view
    //         size the dimension resource id representing the text size
    //         attrColor the attribute resource id pointing to the themed color to give to the text
    //return the built view

    public static TextView buildText(final Context context,
                                     final CharSequence text,
                                     @DimenRes final int size,
                                     @AttrRes final int attrColor) {
        final TextView header = new TextView(context);
        header.setLayoutParams(getCenteredLinearLayoutParams());
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        header.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(size));
        header.setTextColor(ThemeUtils.resolveColorFromAttr(context, attrColor));
        header.setText(text);
        return header;
    }

    //param context the Android context to use to initialize the view
    //      text the content of the text view
    //return the built view

    public static TextView buildHeader(final Context context, final CharSequence text) {
        return buildText(context, text, R.dimen.outputHeaderTextSize,
                android.R.attr.textColorPrimary);
    }

    //
    //param context the Android context to use to initialize the view
    //      text the content of the text view
    //return the built view

    public static TextView buildSubHeader(final Context context, final CharSequence text) {
        return buildText(context, text, R.dimen.outputSubHeaderTextSize,
                android.R.attr.textColorPrimary);
    }

    //param context the Android context to use to initialize the view
    //      text the content of the text view
    //return the built view

    public static TextView buildDescription(final Context context, final CharSequence text) {
        return buildText(context, text, R.dimen.outputDescriptionTextSize,
                android.R.attr.textColorSecondary);
    }

    //param context the Android context to use to initialize the view
    //      divider the drawable to display in between items
    //return the built view

    public static LinearLayout buildVerticalLinearLayout(final Context context,
                                                         final Drawable divider) {
        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(getCenteredLinearLayoutParams());
        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setDividerDrawable(divider);
        linearLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        return linearLayout;
    }

    //param   context the Android context to use to initialize the view
    //        divider the drawable to display in between items
    //        views the children to add, in order, to the layout
    //return the built view

    public static LinearLayout buildVerticalLinearLayout(final Context context,
                                                         final Drawable divider,
                                                         final View... views) {
        final LinearLayout linearLayout = buildVerticalLinearLayout(context, divider);
        for (final View view : views) {
            linearLayout.addView(view);
        }
        return linearLayout;
    }

    //param context the Android context to use to initialize the view
    //return the built view

    public static View buildNetworkErrorMessage(final Context context) {
        return buildVerticalLinearLayout(context,
                AppCompatResources.getDrawable(context, R.drawable.divider_items),
                buildHeader(context, context.getString(R.string.eval_network_error)),
                buildDescription(context,
                        context.getString(R.string.eval_network_error_description)));
    }

    //param context the Android context to use to initialize the view
     //     throwable the exception to show information about
     //return the built view

    public static View buildErrorMessage(final Context context, final Throwable throwable) {
        return buildVerticalLinearLayout(context,
                AppCompatResources.getDrawable(context, R.drawable.divider_items),
                buildHeader(context, throwable.getMessage()),
                buildDescription(context, ExceptionUtils.getStackTraceString(throwable)));
    }
}
