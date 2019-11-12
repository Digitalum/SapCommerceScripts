# SapCommerceScripts


## generateLocaleProperties.groovy

Problem:

In older versions of SAP Commerce (Hybris) it as possible to export property files from the HMC with all the Items and Properties which made it easy to fill in all the translations. This feature unfortunately is not available any longer in the Backoffice.

Usage:
1) Run "Localize Types" under "Platform/Update" in the Hybris Admin Console
2) Copy generateLocaleProperties.groovy and replace XXX with your custom extension
3) Run the script in "Console/Scripting Languages Console"
4) Copy the result output tab to /[your_extension_name]/resources/localization/[your_extension_name]-locales_en.properties
