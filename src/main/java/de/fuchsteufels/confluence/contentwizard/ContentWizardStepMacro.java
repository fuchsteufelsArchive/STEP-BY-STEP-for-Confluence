package de.fuchsteufels.confluence.contentwizard;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.storage.macro.MacroId;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.plugin.spring.scanner.annotation.component.ConfluenceComponent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ConfluenceImport;
import com.atlassian.sal.api.message.I18nResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@ConfluenceComponent
public class ContentWizardStepMacro implements Macro {

    private static final Logger log = LoggerFactory.getLogger(ContentWizardMacro.class);

    @ConfluenceImport
    private final I18nResolver i18n;

    public ContentWizardStepMacro(I18nResolver i18n) {
        this.i18n = i18n;
    }

    @Override
    public String execute(Map<String, String> map, String s, ConversionContext conversionContext) {
        final String macroId = getMacroId(conversionContext);
        final String warning = i18n.getText("de.fuchsteufels.confluence.contentwizard.contentwizard-step-macro.placeinsidemacro");
        final String steptitle = map.get("steptitle") == null ? "" : map.get("steptitle");
        final String stepsubtitle = map.get("stepsubtitle") == null ? "&nbsp;" : map.get("stepsubtitle");

        return "<div class=\"cw-step-macro\" macro-id=\"" + macroId + "\" cw-steptitle=\"" + steptitle + "\" cw-stepsubtitle=\"" + stepsubtitle + "\">" +
                    "<div id=\"contentwizard-step-warning\" style=\"font-weight:bold; color: red;\">" + warning + "</div>" +
                    "<div id=\"cw-bodywrapper\">" +
                        s +
                    "</div>" +
                "</div>";
    }

    @Override
    public BodyType getBodyType() {
        return BodyType.RICH_TEXT;
    }

    @Override
    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }

    private String getMacroId(ConversionContext conversionContext) {
        // Getting macro id
        String macroId = "";
        if (!conversionContext.getOutputType().equals("preview")) {
            MacroDefinition macroDefinition = (MacroDefinition)conversionContext.getProperty("macroDefinition");
            Optional<MacroId> option = macroDefinition.getMacroIdentifier();
            try {
                macroId = option.get().getId();
            } catch (NoSuchElementException e) {
                log.error("Could not get macro id: ", e);
            }
        }
        return  macroId;
    }

}
