//'use strict';

;(function ($, window, document, AJS, undefined) {
  
  if (window.ConfluenceMobile) {
    ConfluenceMobile.contentEventAggregator.on("displayed", function() {
      initWizards();
    });
  } else {
    $(window).load(function(){
      initWizards();
    });
  }

  function initWizards() {
    $(".ft-contentwizard").each(function (index) {
      var wizard = $(this).children(".ft-contentwizard-base");

      // TODO: Better solution for paramter?
      //var settings = JSON.parse($(this).children(".ft-contentwizard-settings")[0].innerHTML.replace("//<![CDATA[", "").replace("//]]>", ""));
      var settings = JSON.parse($(this).children(".ft-contentwizard-settings")[0].innerHTML);
      if (window.ConfluenceMobile) {
        settings.showStepURLhash = false;
        settings.useURLhash = false;
      }
      wizard.smartWizard(settings);
      //console.log("cw: after reading settings");
      

      if (settings.theme === "confluence" && $(wizard).parent().find(".aui-buttons.ft-contentwizard-conf-theme").length < 1) {
        initAuiButtons(wizard, settings);
      } else if (settings.theme === "dots" ) {
        themeAdaptions(wizard);
      }
      scrollingBehaviour(wizard);
    });
  }

  function initAuiButtons(wizard, settings) {
    var template = "";
    var parameters = {
      position: settings.toolbarSettings.toolbarButtonPosition, 
      langNext: settings.lang.next, 
      langPrev: settings.lang.previous
    };
    if (window.ConfluenceMobile) {
      template = de.fuchsteufels.confluence.contentwizard.templates.soyhelper.auibuttonsmobile(parameters);
    } else {
      template = de.fuchsteufels.confluence.contentwizard.templates.soyhelper.auibuttons(parameters);
    }
    
    $(wizard).parent().append(template);

    var prevBtn = $(wizard).parent().find(".ft-cw-aui-btn-prev");
    var nextBtn = $(wizard).parent().find(".ft-cw-aui-btn-next");
    
    $(prevBtn).on("click", function() {
      wizard.smartWizard("prev");
      return true;
    });
    $(nextBtn).on("click", function() {
      wizard.smartWizard("next");
      return true;
    });

    // Enable, Disable Buttons when loading page initially
    var currStepIdx = $(wizard).smartWizard("getCurrentIndex");
    var numSteps = $(wizard).find(".nav-tabs li").length;
    if (0 >= currStepIdx) {
      $(prevBtn).prop("disabled", true);
    } else {
      $(prevBtn).prop("disabled", false);
    }
    if (numSteps - 1 <= currStepIdx) {
      $(nextBtn).prop("disabled", true);
    } else {
      $(nextBtn).prop("disabled", false);
    }

    $(wizard).on("showStep", function(e, anchorObject, stepNumber, stepDirection, stepPosition) {
      if(stepPosition === 'first'){
        $(prevBtn).prop("disabled", true);
        $(nextBtn).prop("disabled", false);
      }else if(stepPosition === 'final'){
        $(nextBtn).prop("disabled", true);
        $(prevBtn).prop("disabled", false);
      }else{
        $(prevBtn).prop("disabled", false);
        $(nextBtn).prop("disabled", false);
      }
    });
  }

  function themeAdaptions(wizard) {
    if ($(wizard).find("a.nav-link small").length == 0) {
      $(wizard).addClass("nosubtitles");
    }
  }

  function scrollingBehaviour(wizard) {
    $(wizard).on("showStep", function(e, anchorObject, stepNumber, stepDirection, stepPosition) {
      var wizard = $(anchorObject).closest(".ft-contentwizard.conf-macro");
      if ($(wizard).height() > $(window).height() || $(wizard).offset().top < $(window).scrollTop() ) {
        $('html, body').animate({
          scrollTop: $(anchorObject).offset().top - 20
        }, 600);
      }
    });
  }

})(jQuery, window, document, AJS);