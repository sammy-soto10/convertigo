<?xml version="1.0" encoding="UTF-8"?>
<?eclipse version="3.4"?>
<plugin>
   <extension
         point="org.eclipse.ui.splashHandlers">
      <splashHandler
            class="com.twinsoft.convertigo.studio.product.splashHandlers.InteractiveSplashHandler"
            id="com.twinsoft.convertigo.studio.product.splashHandlers.interactive">
      </splashHandler>
   </extension>
   <extension
         id="ConvertigoProduct"
         point="org.eclipse.core.runtime.products">
      <product
            application="org.eclipse.ui.ide.workbench"
            name="Studio">
         <property
               name="aboutText"
               value="@aboutText@">
         </property>
         <property
               name="appName"
               value="Studio">
         </property>
         <property
               name="aboutImage"
               value="images/about.png">
         </property>
         <property
               name="startupForegroundColor"
               value="FFFFFF">
         </property>
         <property
               name="startupMessageRect"
               value="20,230,480,20">
         </property>
         <property
               name="startupProgressRect"
               value="0,316,500,16">
         </property>
         <property
               name="windowImages"
               value="images/convertigo_16x16_32.png,images/convertigo_32x32_32.png,images/convertigo_48x48_32.png,images/convertigo_64x64_32.png,images/convertigo_128x128_32.png">
         </property>
         <property
               name="cssTheme"
               value="org.eclipse.e4.ui.css.theme.e4_dark">
         </property>
         <property
               name="applicationCSSResources"
               value="platform:/plugin/org.eclipse.platform/images/">
         </property>
         <property
               name="preferenceCustomization"
               value="plugin_customization.ini">
         </property>
      </product>
   </extension>
   <extension
         point="org.eclipse.e4.ui.css.swt.theme">
      <stylesheet
          uri="css/convertigo-dark.css">
        <themeid
            refid="org.eclipse.e4.ui.css.theme.e4_dark"></themeid>
      </stylesheet>
   </extension>

</plugin>
