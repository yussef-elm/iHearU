package com.ensias.ihearu.skills.fallback.text;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.ensias.ihearu.R;
import org.dicio.skill.Skill;
import org.dicio.skill.SkillContext;
import org.dicio.skill.SkillInfo;

public class TextFallbackInfo extends SkillInfo {

    public TextFallbackInfo() {
        super("text", R.string.skill_fallback_name_text, 0, R.drawable.ic_short_text_white, false);
    }

    @Override
    public boolean isAvailable(final SkillContext context) {
        return true;
    }

    @Override
    public Skill build(final SkillContext context) {
        return new TextFallback();
    }

    @Nullable
    @Override
    public PreferenceFragmentCompat getPreferenceFragment() {
        return null;
    }
}
