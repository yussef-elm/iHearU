package com.ensias.ihearu.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;

import com.ensias.ihearu.R;

import java.util.Collection;
import java.util.Locale;

public final class LocaleUtils {

    public static class UnsupportedLocaleException extends Exception {
        public UnsupportedLocaleException(final Locale locale) {
            super("Unsupported locale: " + locale);
        }

        public UnsupportedLocaleException() {
            super("No locales provided");
        }
    }

    public static class LocaleResolutionResult {
        @NonNull public Locale availableLocale;
        @NonNull public String supportedLocaleString;

        public LocaleResolutionResult(@NonNull final Locale availableLocale,
                                      @NonNull final String supportedLocaleString) {
            this.availableLocale = availableLocale;
            this.supportedLocaleString = supportedLocaleString;
        }
    }

    private LocaleUtils() {
    }


    @NonNull
    public static LocaleResolutionResult resolveSupportedLocale(
            final LocaleListCompat availableLocales,
            final Collection<String> supportedLocales)
            throws UnsupportedLocaleException {
        UnsupportedLocaleException unsupportedLocaleException = null;
        for (int i = 0; i < availableLocales.size(); i++) {
            try {
                final String supportedLocaleString =
                        resolveLocaleString(availableLocales.get(i), supportedLocales);
                return new LocaleResolutionResult(availableLocales.get(i), supportedLocaleString);
            } catch (final UnsupportedLocaleException e) {
                if (unsupportedLocaleException == null) {
                    unsupportedLocaleException = e;
                }
            }
        }

        if (unsupportedLocaleException == null) {
            throw new UnsupportedLocaleException();
        } else {
            throw unsupportedLocaleException;
        }
    }


    @NonNull
    public static String resolveLocaleString(final Locale locale,
                                             final Collection<String> supportedLocales)
            throws UnsupportedLocaleException {
        // first try with full locale name (for exemple en-US)
        String localeString = (locale.getLanguage() + "-" + locale.getCountry()).toLowerCase();
        if (supportedLocales.contains(localeString)) {
            return localeString;
        }

        // then try with only base language (for exemple en)
        localeString = locale.getLanguage().toLowerCase();
        if (supportedLocales.contains(localeString)) {
            return localeString;
        }

        // then try with children languages of locale base language (like en-US, en-GB, en-UK, ...)
        for (final String supportedLocalePlus : supportedLocales) {
            for (final String supportedLocale : supportedLocalePlus.split("\\+")) {
                if (supportedLocale.split("-", 2)[0].equals(localeString)) {
                    return supportedLocalePlus;
                }
            }
        }

        // fail
        throw new UnsupportedLocaleException(locale);
    }

    public static LocaleListCompat getAvailableLocalesFromPreferences(final Context context) {
        final String language = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_key_language), null);
        if (language == null || language.trim().isEmpty()) {
            return ConfigurationCompat.getLocales(context.getResources().getConfiguration());
        } else {
            final String[] languageCountry = language.split("-");
            if (languageCountry.length == 1) {
                return LocaleListCompat.create(new Locale(language));
            } else {
                return LocaleListCompat.create(new Locale(languageCountry[0], languageCountry[1]));
            }
        }
    }
}
