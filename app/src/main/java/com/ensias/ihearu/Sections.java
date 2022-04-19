package com.ensias.ihearu;

import static com.ensias.ihearu.SectionsGenerated.localeSectionsMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.LocaleListCompat;

import com.ensias.ihearu.util.LocaleUtils;

import org.dicio.skill.standard.StandardRecognizerData;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

public final class Sections {

    private Sections() {
    }

    private static Locale currentLocale = null;
    private static Map<String, StandardRecognizerData> sectionsMap = null;


    @NonNull
    public static Locale setLocale(final LocaleListCompat availableLocales)
            throws LocaleUtils.UnsupportedLocaleException {
        final LocaleUtils.LocaleResolutionResult localeResolutionResult =
                LocaleUtils.resolveSupportedLocale(availableLocales, localeSectionsMap.keySet());
        sectionsMap = localeSectionsMap.get(localeResolutionResult.supportedLocaleString);
        currentLocale = localeResolutionResult.availableLocale;
        return currentLocale;
    }


    @Nullable
    public static Locale getCurrentLocale() {
        return currentLocale;
    }


    public static boolean isSectionAvailable(final String sectionName) {
        return sectionsMap.containsKey(sectionName);
    }


    @Nullable
    public static StandardRecognizerData getSection(final String sectionName) {
        return sectionsMap.get(sectionName);
    }
}
