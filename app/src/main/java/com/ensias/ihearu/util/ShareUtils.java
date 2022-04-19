package com.ensias.ihearu.util;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.ensias.ihearu.R;


public final class ShareUtils {
    private ShareUtils() {
    }

    public static boolean openUrlInBrowser(@NonNull final Context context,
                                           final String url,
                                           final boolean httpDefaultBrowserTest) {
        final String defaultPackageName;
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (httpDefaultBrowserTest) {
            defaultPackageName = getDefaultAppPackageName(context, new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            defaultPackageName = getDefaultAppPackageName(context, intent);
        }

        if (defaultPackageName.equals("android")) {
            // No browser set as default (doesn't work on some devices)
            openAppChooser(context, intent, true);
        } else {
            if (defaultPackageName.isEmpty()) {
                // No app installed to open a web url
                Toast.makeText(context, R.string.no_app_to_open_intent, Toast.LENGTH_LONG).show();
                return false;
            } else {
                try {
                    intent.setPackage(defaultPackageName);
                    context.startActivity(intent);
                } catch (final ActivityNotFoundException e) {
                    // Not a browser but an app chooser because of OEMs changes
                    intent.setPackage(null);
                    openAppChooser(context, intent, true);
                }
            }
        }

        return true;
    }


    public static boolean openUrlInBrowser(@NonNull final Context context, final String url) {
        return openUrlInBrowser(context, url, true);
    }

    public static boolean openIntentInApp(@NonNull final Context context,
                                          @NonNull final Intent intent,
                                          final boolean showToast) {
        final String defaultPackageName = getDefaultAppPackageName(context, intent);

        if (defaultPackageName.isEmpty()) {
            // No app installed to open the intent
            if (showToast) {
                Toast.makeText(context, R.string.no_app_to_open_intent, Toast.LENGTH_LONG)
                        .show();
            }
            return false;
        } else {
            context.startActivity(intent);
        }

        return true;
    }


    private static void openAppChooser(@NonNull final Context context,
                                       @NonNull final Intent intent,
                                       final boolean setTitleChooser) {
        final Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, intent);
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (setTitleChooser) {
            chooserIntent.putExtra(Intent.EXTRA_TITLE, context.getString(R.string.open_with));
        }

        final int permFlags;
        permFlags = intent.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        if (permFlags != 0) {
            ClipData targetClipData = intent.getClipData();
            if (targetClipData == null && intent.getData() != null) {
                final ClipData.Item item = new ClipData.Item(intent.getData());
                final String[] mimeTypes;
                if (intent.getType() != null) {
                    mimeTypes = new String[] {intent.getType()};
                } else {
                    mimeTypes = new String[] {};
                }
                targetClipData = new ClipData(null, mimeTypes, item);
            }
            if (targetClipData != null) {
                chooserIntent.setClipData(targetClipData);
                chooserIntent.addFlags(permFlags);
            }
        }
        context.startActivity(chooserIntent);
    }


    private static String getDefaultAppPackageName(@NonNull final Context context,
                                                   @NonNull final Intent intent) {
        final ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        if (resolveInfo == null) {
            return "";
        } else {
            return resolveInfo.activityInfo.packageName;
        }
    }


    public static void shareText(@NonNull final Context context,
                                 @NonNull final String title,
                                 final String content,
                                 final String imagePreviewUrl) {
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        if (!TextUtils.isEmpty(title)) {
            shareIntent.putExtra(Intent.EXTRA_TITLE, title);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        }

        /*
        if (!imagePreviewUrl.isEmpty()) {
            //shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }*/

        openAppChooser(context, shareIntent, false);
    }


    public static void shareText(@NonNull final Context context,
                                 @NonNull final String title,
                                 final String content) {
        shareText(context, title, content, "");
    }


    public static void copyToClipboard(@NonNull final Context context, final String text) {
        final ClipboardManager clipboardManager =
                ContextCompat.getSystemService(context, ClipboardManager.class);

        if (clipboardManager == null) {
            Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_LONG).show();
            return;
        }

        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, text));
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }
}
