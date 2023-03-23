package de.fuchsteufels.confluence.contentwizard;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.macro.annotation.Format;
import com.atlassian.confluence.content.render.xhtml.macro.annotation.RequiresFormat;
import com.atlassian.confluence.content.render.xhtml.storage.macro.MacroId;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.themes.ColourScheme;
import com.atlassian.confluence.themes.ColourSchemeManager;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.atlassian.plugin.spring.scanner.annotation.component.ConfluenceComponent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ConfluenceImport;
import com.atlassian.sal.api.message.I18nResolver;
// import com.atlassian.upm.api.license.PluginLicenseManager;
// import com.atlassian.upm.api.license.entity.PluginLicense;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import de.fuchsteufels.confluence.contentwizard.data.LicenseInfo;
import org.beryx.awt.color.ColorFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;

@ConfluenceComponent
public class ContentWizardMacro implements Macro {

    private static final Logger log = LoggerFactory.getLogger(ContentWizardMacro.class);

    @ConfluenceImport
    private final PageBuilderService pageBuilderService;
    @ConfluenceImport
    private final XhtmlContent xhtmlUtils;
    @ConfluenceImport
    private final ColourSchemeManager colourSchemeManager;
    @ConfluenceImport
    private final I18nResolver i18n;
    // @ConfluenceImport
    // private final PluginLicenseManager pluginLicenseManager;

    public ContentWizardMacro(PageBuilderService pageBuilderService, XhtmlContent xhtmlUtils, ColourSchemeManager colourSchemeManager, I18nResolver i18n /*, PluginLicenseManager pluginLicenseManager*/) {
        this.pageBuilderService = pageBuilderService;
        this.xhtmlUtils = xhtmlUtils;
        this.colourSchemeManager = colourSchemeManager;
        this.i18n = i18n;
        // this.pluginLicenseManager = pluginLicenseManager;
    }

    @Override
    @RequiresFormat(Format.View)
    public String execute(Map<String, String> map, String body, ConversionContext conversionContext) throws MacroExecutionException {
        setDefaultParameterValues(map);
        loadAdditionalResources(map);
        return getRenderedTemplate(conversionContext, map, body);
    }

    @Override
    public BodyType getBodyType() {
        return BodyType.RICH_TEXT;
    }

    @Override
    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }

    private String getRenderedTemplate(ConversionContext conversionContext, Map<String, String> map, String body) {
        if (isExport(conversionContext)) {
            return VelocityUtils.getRenderedTemplate("velocity/wizard_base_print.vm", getParameterContext(map, conversionContext, body));
        }
        return VelocityUtils.getRenderedTemplate("velocity/wizard_base.vm", getParameterContext(map, conversionContext, body));
    }

    private boolean isExport(ConversionContext conversionContext) {
        switch (conversionContext.getOutputType()) {
            case "pdf":
            case "word":
            case "html_export":
                return true;
            default:
                return false;

        }
    }

    private Map getParameterContext(Map<String, String> map, ConversionContext conversionContext, String body) {
        Map context = MacroUtils.defaultVelocityContext();

        // Getting space color scheme and deriving done color
        String spaceSchemeLinkColor = colourSchemeManager.getSpaceColourScheme(conversionContext.getSpaceKey()).get(ColourScheme.LINK);
        String selectedColor = map.get("selectedcolor") == null ? spaceSchemeLinkColor : map.get("selectedcolor");
        String doneColor = map.get("donecolor") == null ? computeColor(selectedColor, .35f, .5f): map.get("donecolor");
        String hoverColor = computeColor(selectedColor, .7f, 1f);

        // Getting next and previous values
        String next = map.get("langnext") == null ? i18n.getText("de.fuchsteufels.confluence.contentwizard.contentwizard-macro.nextbtn") : map.get("langnext");
        String prev = map.get("langprev") == null ? i18n.getText("de.fuchsteufels.confluence.contentwizard.contentwizard-macro.prevbtn") : map.get("langprev");

        // Params
        context.put("contentwizard-macroid", getMacroId(conversionContext));
        context.put("contentwizard-enableallanchors", map.get("enableallanchors") != null && map.get("enableallanchors").equals("true") ? "true" : "false");
        context.put("contentwizard-theme", map.get("theme"));
        context.put("contentwizard-transitionEffect", map.get("transitioneffect"));
        context.put("contentwizard-buttonPosition", map.get("buttonposition"));
        context.put("contentwizard-toolbarPosition", getToolbarPosition(map.get("theme")));
        context.put("contentwizard-stepcontenttitle", map.get("showstepcontenttitle") != null && map.get("showstepcontenttitle").equals("true") ? "true" : "false hidden");
        //context.put("contentwizard-stepcontenttitle", map.get("showstepcontenttitle") != null && map.get("showstepcontenttitle").equals("true") ? "true" : "false");

        context.put("contentwizard-selectedcolor", selectedColor);
        context.put("contentwizard-donecolor", doneColor);
        context.put("contentwizard-hovercolor", hoverColor);
        //context.put("contentwizard-steps", parseStorageFormatForSteps(body, conversionContext));
        context.put("contentwizard-steps", parseStorageFormatForStepsNew(body, conversionContext));
        context.put("contentwizard-next", next);
        context.put("contentwizard-prev", prev);

        context.put("contentwizard-license", getLicenseInfo());

        return context;
    }

    private String computeColor(String baseColor, float saturation, float brightness) {
        Color color = ColorFactory.web(baseColor);
        float[] hsbvals = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsbvals);

        hsbvals[1] = saturation;
        hsbvals[2] = brightness;

        color = Color.getHSBColor(hsbvals[0], hsbvals[1], hsbvals[2]);

        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
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

    private void setDefaultParameterValues(Map<String, String> map) {
        if (map.get("theme") == null) {
            map.put("theme", "confluence");
        }
        if (map.get("transitioneffect") == null) {
            map.put("transitioneffect", "fade");
        }
        if (map.get("buttonposition") == null) {
            map.put("buttonposition", "left");
        }
    }

    private void loadAdditionalResources(Map<String, String> map) {
        pageBuilderService.assembler().resources().requireWebResource("de.fuchsteufels.confluence.contentwizard:contentwizard-resources");

        if (map.get("theme") != null) {

            switch (map.get("theme")) {
                case "arrows":
                    pageBuilderService.assembler().resources().requireWebResource("de.fuchsteufels.confluence.contentwizard:contentwizard-resources-theme-arrows");
                    break;
                case "circles":
                    pageBuilderService.assembler().resources().requireWebResource("de.fuchsteufels.confluence.contentwizard:contentwizard-resources-theme-circles");
                    break;
                case "dots":
                    pageBuilderService.assembler().resources().requireWebResource("de.fuchsteufels.confluence.contentwizard:contentwizard-resources-theme-dots");
                    break;
                case "confluence":
                    pageBuilderService.assembler().resources().requireWebResource("de.fuchsteufels.confluence.contentwizard:contentwizard-resources-theme-confluence");
                    break;
                default:
                    // nothing
                    break;
            }
        }
    }

    private List<Map<String, String>> parseStorageFormatForStepsNew(String storageFormat, ConversionContext conversionContext) {
        List<Map<String, String>> returnList = new ArrayList<>();
        Document doc = Jsoup.parseBodyFragment(storageFormat);
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.html);

        for (Element elem : doc.getElementsByClass("cw-step-macro")) {
            Map<String, String> currMap = new HashMap<>();


            // Saving info
            currMap.put("macroid", elem.attr("macro-id"));
            currMap.put("steptitle", elem.attr("cw-steptitle"));
            currMap.put("stepsubtitle", elem.attr("cw-stepsubtitle"));
            currMap.put("body", elem.getElementById("cw-bodywrapper").html()); // id = cw-bodywrapper

            returnList.add(currMap);
        }
        return  returnList;
    }

    private String getToolbarPosition(String theme) {
        if (theme.equals("confluence")) {
            return "none";
        }
        return "bottom";
    }

//    private void checkForSubtitles(List<Map<String, String>> list) {
//        boolean hasSubtitles = false;
//        for (Map<String, String> macro : list) {
//            if (macro.containsKey("stepsubtitle")) {
//                hasSubtitles = true;
//                break;
//            }
//        }
//        if (hasSubtitles) {
//            for (Map<String, String> macro : list) {
//                if (macro.containsKey("stepsubtitle")) {
//                    continue;
//                }
//                // Adding a space character as subtitle to make step arrow height equal
//                macro.put("stepsubtitle", "&nbsp;");
//            }
//        }
//    }

    private LicenseInfo getLicenseInfo() {
        // since is open source and free now, return valid.
        return new LicenseInfo(true, "License is valid");

//        if (pluginLicenseManager.getLicense().isDefined()) {
//            PluginLicense license = pluginLicenseManager.getLicense().get();
//            if (license.getError().isDefined()) {
//                String message = "";
//                switch (license.getError().get()) {
//                    case USER_MISMATCH:
//                        message = i18n.getText("de.fuchsteufels.confluence.contentwizard.license.usermismatch");
//                        break;
//                    case VERSION_MISMATCH:
//                        message = i18n.getText("de.fuchsteufels.confluence.contentwizard.license.versionmismatch");
//                        break;
//                    default:
//                        message = i18n.getText("de.fuchsteufels.confluence.contentwizard.license.expired");
//                        break;
//                }
//                return new LicenseInfo(false, message);
//            } else {
//                return new LicenseInfo(true, "License is valid");
//            }
//        } else {
//            return new LicenseInfo(false, i18n.getText("de.fuchsteufels.confluence.contentwizard.license.missing"));
//        }
    }
}
