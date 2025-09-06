# Beta Change Log

Hello! Thank you for your help in refining Kigali Sim. During the beta testing period, we will be making changes in response to user and partner feedback.

**About this document:** This document tracks modifications which are expected to concern a majority or substantial minority of beta testers. Note that the [GitHub commit log](https://github.com/SchmidtDSE/kigali-sim/commits) provides further public detail on all changes no matter how small.

**Scope:** Note that this concerns both those using the UI-based designer which does not require use of any code as well those making sophisticated simulations using the editor tab where custom code written in [QubecTalk](https://kigalisim.org/guide/tutorial_07.html) is to be used.

**Contact us:** If you have any questions, feel free to open issues on our GitHub or, if you do not want to participate on GitHub, feel free to email us at hello@kigalisim.org for private dialogue. We are here to help you and support your usage of the tool both during this beta period and after launch.

Finally, we want to again express our gratitude for your feedback and time.

## Completed

The following changes have been adopted and released.

### Fix replace for mid-year equipment calcualtion

**Status**: Released September 5, 2025

**Classification**: Bug

Prior testing caught an issue with cap operations but this was accidentially not extended to include replace operations. This caused issues with mid-year (or mid-timestep) recalculation of equipment. See [#522](https://github.com/SchmidtDSE/kigali-sim/pull/522). Automated tests added to prevent future issues.

### Add kgCO2e Option

**Status**: Released August 25, 2025

**Classification**: Enhancement

Added support for kgCO2e siimlar to tCO2e. This is across the engine for those using QubecTalk as well as those in the UI-based edtior. There were also some documentation updates. See [#513](https://github.com/SchmidtDSE/kigali-sim/issues/513).

### Show "-all" in charts

**Status**: Released August 25, 2025

**Classification**: Bug

Very minor bug where the "- All" option for equipment subtypes was not being displayed in the visualizations.

### Feature-Specific Tutorials

**Status:** Released August 20, 2025

**Classification:** Documentation

Added two new feature-specific tutorials to the guide demonstrating Montreal Protocol policy modeling capabilities. Tutorial 11 focuses on Global Warming Potential (GWP) comparison between HFC-134a and R-600a refrigerants, showing how refrigerant substitution policies can reduce climate impact. Tutorial 12 demonstrates equipment energy efficiency improvements, comparing high-energy and low-energy equipment models to illustrate how efficiency policies can reduce overall energy consumption. Both tutorials use UI-based instructions and progressive policy implementation patterns.

### Privacy clarification

**Status:** Released August 20, 2025

**Classification:** Documentation

The privacy brief is now shown prior to accessing the full privacy details. Users can access privacy information through a confirmation dialog from multiple interface locations, providing better visibility of privacy practices before viewing the complete privacy policy. See [#508](https://github.com/SchmidtDSE/kigali-sim/issues/508).

### Volume-Based Sales Recycling Material Balance

**Status:** Released August 19, 2025

**Classification:** Clarification / Bug

Clarified that, when recycling reduces virgin material demand in volume-based sales scenarios, any loss of recycling availability in subsequent years will be immediately back-filled by virgin material to maintain total demand. Previously, in volume-based scenarios (using `set sales to X mt`), recycling would correctly displace virgin material in the year it was active. However, at year boundaries, virgin material would not go up in response to lost recycling volumes. This makes sense for induced demand but, as most users are not modeling that complex logic, this version is more appropriate for majority of users. See [#506](https://github.com/SchmidtDSE/kigali-sim/issues/506). We apologize for the inconvenience.

### Policy Auto-Update on Name Changes

**Status:** Released August 19, 2025

**Classification:** Enhancement

Added automatic policy code updates when application or substance names are changed through the UI-based designer. Previously, it was preferred that users explicitly indicate how prior entries should be updated. However, this could generate error messages that could be confusing for new users. The system now automatically propagates name changes across all relevant policies, allowing users to rename definitions without encountering errors or needing to manually update code in the editor tab. This enhancement improves the user experience for those using the UI-based designer, though this issue was infrequently encountered. See [#498](https://github.com/SchmidtDSE/kigali-sim/issues/498).

### Improved Error Messages for Disabled Streams

**Status:** Released August 19, 2025

**Classification:** Enhancement

Enhanced error messaging when users interact with disabled streams. While streams must still be enabled before use (via enable commands and checkboxes like enable-import), the previous error messages could be confusing to users. The system now provides more informative and user-friendly error text to help guide users toward updating outdated commands when attempting to use streams that haven't been enabled yet. See [#496](https://github.com/SchmidtDSE/kigali-sim/issues/496).

### Filter UI Reset on File Load

**Status:** Released August 19, 2025

**Classification:** Bug

Fixed a minor issue where the filter UI state was not properly reset when loading simulation files. While the underlying filter logic was correctly reset to avoid programming errors when substances or simulations were not found, the UI elements themselves remained in their previous state. This could lead to user confusion. See [#494](https://github.com/SchmidtDSE/kigali-sim/issues/494).

### Population-based Energy Efficiency

**Status:** Released August 19, 2025

**Classification:** Clarification

Modified energy efficiency calculations based on the full equipment population rather than broken out by sales streams. Previously, energy consumption was attributed to trade sources through stream attribution tied to substance consumption. This could lead to confusion since energy consumption typically occurs across a single unified grid and an annual operating amortization is better aligned with user conceptual expectations. The system now converts directly from total equipment population to energy consumption (kWh), simplifying the model to more closely align with majority user expectation. However, stream-level tracking can still be achieved through equipment model naming if preferred. This change moves energy options from the sales/consumption section to the equipment section in the UI, reflecting the equipment-based calculation. See [#493](https://github.com/SchmidtDSE/kigali-sim/issues/493).

### Initial Charge Emissions Calculation

**Status:** Released August 4, 2025

**Classification:** Feature

Added option to select initial charge on emissions. This is technically future emissions as the substance will later appear in recharge and end of life (or recycling), options which are already available. However, this was user-requested and may help validate some calculations and help in some policy making contexts. See [#486](https://github.com/SchmidtDSE/kigali-sim/issues/486). Some smaller changes linked from that issue.

### Force Update Functionality

**Status:** Released August 3, 2025

**Classification:** Enhancement

Enhanced the update functionality to give users more control over updates. The "Update" button in the footer now allows users to manually check for updates and force an update even when they're already running the latest version. The dialog now shows different messaging based on whether updates are available or if the user is already up to date. See [#484](https://github.com/SchmidtDSE/kigali-sim/issues/484).

### Tutorial Downloads

**Status:** Released August 1, 2025

**Classification:** Enhancement

We added download links to the guide so that users may get the qta files showing the completed work for each tutorial. These files are also automatically tested (run and results rechecked) ahead of every software update. See [#482](https://github.com/SchmidtDSE/kigali-sim/issues/482).

### Percent change with units fix

**Status:** Released August 1, 2025

**Classification:** Bug

As part of changes ahead of the open beta, we clarified that specifying volumes in units indicates that the simulation should treat that as number of new units sold such that recharge is automatically added on top of whatever initial charge is required for those new unit sales. This had some unintended interactions with changes made to support end of life recycling. More information about specifying sales in units can be found at https://kigalisim.org/guide/tutorial_08.html. This caused some complications with the expended recycling functionality added after [OEWG](https://ozone.unep.org/meetings/47th-meeting-open-ended-working-group-parties) but prior to the open beta starting. THis had an unexpected manifestation that depended on the order of operations used in specific models.

See [#481](https://github.com/SchmidtDSE/kigali-sim/issues/481) for more details. **We apologize for this issue** and the tool has been updated to have the intended behavior.

Note that a prior report on July 31 was modified after the issue was better understood.

### Recycle time displacement

**Status:** Released July 30, 2025

**Classification:** Clarification

We added end of life recycling after the [OEWG](https://ozone.unep.org/meetings/47th-meeting-open-ended-working-group-parties) demonstration based on user feedback. However, this introduced some complexities around time displacement, specifically if recycled material should be immediately available or if there is some delay between recycling collection and its actual re-use. We have simplified our original logic to have recycled material be available in the same year it is collected. While we recognize that both approaches may be valid and this may be an over-simplification for end of life recycling, its impact on policy evaluation is minimal because over the full multi-year duration of a simulation, the results are generally the same or show negligible differences. Therefore, the benefits of the simplification were deemed to outweigh the costs.

### Shading on tabs

**Status:** Released July 29, 2025

**Classification:** Enhancement

Note that this was queued to go to deployment so the release date may be approximate. To improve accessibility shading and a non-color (outline) indicator are both used to show which tab is currently active. This was done for usability and accessibility.

## Needs Discussion

Information about issues or discussions still underway, with target completion before December launch.

### Servicing of new equipment

**Status:** In discussion

Some prior modeling efforts outside of Kigali Sim would apply servicing (top up or repair) to all equipment in a year while others would only apply it to equipment over a certain age, avoiding maintenance on equipment just sold. Kigali Sim currently takes the later approach where only existing equipment from previous years are subject to servicing and new equipment are not assumed to need recharge or repair. Multiple proposals have been made:

- Keep the current logic which only applies servicing to existing equipment from previous years.
- Use simpler logic where recharge applies to all equipment not old equipment to simplify manual calculations of users validating Kigali Sim models using manual calculations.
- Allow the user to specify which behavior is desired. However, this adds additional complexity to the tool and may lead to confusion when comparing results between Kigali Sim models.

This is being discussed in [#469](https://github.com/SchmidtDSE/kigali-sim/issues/469) and we welcome feedback. Note that the default behavior of Kigali Sim is to retire old equipment and then recharge from the remaining population but this can be modified through [QubecTalk](https://kigalisim.org/guide/tutorial_07.html) code (see the editor tab for advanced capabilities).

### Initial charge / recharge consumption options

**Status:** In discussion

In addition to the emissions radio button, there is discussion of adding initial charge and recharge options under consumption. In some ways these are redundant to the emissions optinos but with more unit conversions though it causes potential confusion when setting custom metrics and in understanding trade attribution.

This is being discussed in [#490](https://github.com/SchmidtDSE/kigali-sim/issues/490). Please let us know what you think!


