'use strict';
  
var addStep = function(macroNode) {
    //var $macroDiv = AJS.$(macroNode);
    var macrobody = AJS.$("td.wysiwyg-macro-body p", macroNode).last();
    
    //console.log("macrobody ", macrobody);
    //console.log("without first ", AJS.$("td.wysiwyg-macro-body p", macroNode));
    AJS.Rte.getEditor().selection.setCursorLocation(macrobody[0]);
    AJS.Rte.BookmarkManager.storeBookmark();

    var macroRenderRequest = {
        contentId: Confluence.Editor.getContentId(),
        macro: {
            name: "contentwizard-step-macro",
            params: {},
            defaultParameterValue: "",
            body : ""
        }
    };

    tinymce.confluence.MacroUtils.insertMacro(macroRenderRequest).done(
        function(newNode, newNodeMarkup) {
            tinymce.confluence.macrobrowser.editMacro(newNode);
        }
    );
};

AJS.Confluence.PropertyPanel.Macro.registerButtonHandler("contentwizard-addstep", function(e, macroNode) {
    addStep(macroNode);
});