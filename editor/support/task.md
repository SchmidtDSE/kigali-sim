# Case study clean up

This stask is a mix of small tweaks we want to make based on the process of going through the case study. In each of these, please be sure that the automated checks work after finishing work on each item and please make a commit after each is done and the checks are green.

 - gradlew test
 - gradlew checkstyleMain
 - gradlew checkstyleTest
 - npx lint (see github actions for the specific commands)
 - npx grunt

## Do not hide simulations

Right now we have logic that changes the visibility of the options available in simulations-dimension. If one selects the all radio, it shows all simulation options. If you select an individual simulation option, the other simulations are hidden. I want to change this such that the radio buttons for all simulations are always available. See "index.html" for more.

## Catch filter set not available

When a new filter set is made, I want to check that the simulation, application, and substance selected by the user are found in the results. If they are not found, we should reset it to the all option and update the UI. See "constrainFilterSet" which may be able to help. Note that the user may have selected all on their own.

## Hide results on failure

If there is a simulation run failure, we should show a panel similar to "running-indicator" that is overlaid on top of the results panel indicating that there was an error.

## Change visibility of options if attribute to exporter

If "importer-assignment-check" is unchecked, then we should not have import or all available in sales-submetric. If it is checked, all options should be shown. If the user had selected all or import and then unchecks the "importer-assignment-check" checkbox, then it should go to domestic.
