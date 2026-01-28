# Beta Change Log

Hello! Thank you for your help in refining Kigali Sim. During the beta testing period, we will be making changes in response to user and partner feedback.

**About this document:** This document tracks modifications which are expected to concern a majority of users. Note that the [GitHub commit log](https://github.com/SchmidtDSE/kigali-sim/commits) provides further public detail on all changes no matter how small. This document is now only updated for major changes after the close of the beta test. Please see [GitHub commit log](https://github.com/SchmidtDSE/kigali-sim/commits) for further details.

**Scope:** This concerns both those using the UI-based designer which does not require use of any code as well those making sophisticated simulations using the editor tab where custom code written in [QubecTalk](https://kigalisim.org/guide/tutorial_07.html) is used.

**Contact us:** If you have any questions, feel free to open issues on our GitHub or, if you do not want to participate on GitHub, feel free to email us at hello@kigalisim.org for private dialogue. We are here to help you and support your usage of the tool both during this beta period and after launch.

Finally, we want to again express our gratitude for your feedback and time.

## Completed

The following changes have been adopted and released.

### Sales clarification in documentation

**Status**: Released January 28, 2026

**Classification**: Clarification

Expanded documentation regarding the meaning and use of the `sales` keyword. See [#731](https://github.com/SchmidtDSE/kigali-sim/issues/731).

### AI troubleshooting guidance

**Status**: Released January 27, 2026

**Classification**: Enhancement

Expanded Tutorial 11 to provide further guidance on specific AI assistants.

### Improved tutorials

**Status**: Released January 26, 2026

**Classification**: Enhancement

Further improved tutorials 2 - 6 based on user testing.

### Manual ordering of policies in UI editor

**Status**: Released January 24, 2026

**Classification**: Enhancement

Added ability to manually specify the ordering of policy application / policy stacking. This allows for order-dependent policy scenarios to be defined without requiring use of the code editor. For example, if a percent decrease (like from an educational program) should be applied before or after a cap (like from a permitting system). Further described in [#703](https://github.com/SchmidtDSE/kigali-sim/issues/703).

### Fix basis for stacked change policy

**Status**: Released January 24, 2026.

**Classification**: Bug / clarification

When stacking policies, it is possible for change statements with `% current` to use the original basis from BAU and not that set by another policy like floor or cap. See [#711](https://github.com/SchmidtDSE/kigali-sim/pull/711) and [#716](https://github.com/SchmidtDSE/kigali-sim/pull/716). There is some ambiguity in what is desireable but, given explicit control of stacking order, this was fixed to use the modified value as basis (like if cap or floor were violated) instead of the original value for `% current`. Corresponding changes made to documentation to provide clarity. Our apologies for this ambiguity. Tests have been added to prevent regression. See [#690](https://github.com/SchmidtDSE/kigali-sim/issues/690) and [#691](https://github.com/SchmidtDSE/kigali-sim/issues/691) (new features released on Dec 30, 2025).

### Alphabetical sorting in UI lists

**Status**: Released January 20, 2026

**Classification**: Enhancement

Added alphabetical sorting to all UI-based editor lists and checkboxes. Applications, substances (consumptions), policies, and simulations are now displayed in alphabetical order in their respective list views. Policy checkboxes in the simulation dialog are also sorted alphabetically, and when policies are selected and converted to a simulation stanza via `policy-check-label`, they now appear in alphabetical order in the generated code.

### Displacement options

**Status**: Released December 30, 2025

**Classification**: Enhancement

Added support for `displacing volume of` and `displacing units of` to give more control over how displacement should translate between substances (by kg or by ynits of equipment). See [#693](https://github.com/SchmidtDSE/kigali-sim/issues/693).

### Percent options

**Status**: Released December 30, 2025

**Classification**: Enhancement

Added support for `% prior year` and `% current` to give more control over what percentages should mean (relative to current value after some operations may have been performed in the current year or relative to the value at the end of the year prior). See [#690](https://github.com/SchmidtDSE/kigali-sim/issues/690) and [#691](https://github.com/SchmidtDSE/kigali-sim/issues/691).

### Percent cap and floor from prior year

**Status**: Released December 30, 2025

**Classification**: Clarification

Clarified that percentage-based caps and floors are calculated relative to the prior year's actual stream value. For example, "cap sales to 85%" will cap to 85% of the prior year's sales value. This ensures consistent year-over-year compounding behavior for policy restrictions.

### Include assume in duplication

**Status**: Released October 30, 2025

**Classification**: Bug

Include the assume mode in duplication of substances (duplicate dialog box in editor). See [#629](https://github.com/SchmidtDSE/kigali-sim/pull/629).

### Formaula hazard rates

**Status**: Released October 30, 2025

**Classification**: Enhancement

Added support for user access to age metrics in support of formaula-based hazard rates. See [#622](https://github.com/SchmidtDSE/kigali-sim/issues/622) and [#626](https://github.com/SchmidtDSE/kigali-sim/issues/626).

### Improved help text for default sales

**Status**: Released October 28, 2025

**Classification**: Enhancement

Updated the tooltip text for the "Default sales assumption in a new year" dropdown based on user feedback.

### Fuzzier matching and substance list update

**Status**: Released October 28, 2025

**Classification**: Enhancement

Expanded the substance list to include non-Kigali substances (all registered with Ozone Secretariat) and expanded fuzzy matching. See [#611](https://github.com/SchmidtDSE/kigali-sim/pull/611).

### Empty simulation handling

**Status**: Released October 23, 2025

**Classification**: Bug

Fixed error when clearing simulations with the new file button to gracefully handle empty simulation state. See [#606](https://github.com/SchmidtDSE/kigali-sim/pull/606).

### Trial sampling options

**Status**: Released October 22, 2025

**Classification**: Bug

In follow up to Monte Carlo features recently released, fixed the normal and uniform distribution sampling. Our sincere apologies for this issue. See [#604](https://github.com/SchmidtDSE/kigali-sim/pull/604).

### Servicing in policy UI

**Status**: Released October 21, 2025

**Classification**: Enhancement

Allow the user to include servicing into a policy. In other words, allows UI-based editor to express changes to servicing schedule previuosly only possible in QubecTalk code. See [#600](https://github.com/SchmidtDSE/kigali-sim/pull/600).

### Parallel Monte Carlo

**Status**: Released October 20, 2025

**Classification**: Enghancement

Added support for parallelized Monte Carlo for those running simulations from the Jar. See [#594](https://github.com/SchmidtDSE/kigali-sim/pull/594).

### Multiple retire / recharge

**Status**: Released October 20, 2025

**Classification**: Enhancement

Allow the user to specify multiple retire and recharge commands for a single app / substance pair. See [#598](https://github.com/SchmidtDSE/kigali-sim/pull/598).

### Fix recharge calculation on retire with replace

**Status**: Released October 20, 2025

**Classification**: Bug

The retire with replacement option was still including an outdated equipment count in determining servicing requirements. For consistency with the Kigali Sim convention that newly sold eqiupment are not subject to servicing rates, the servicing calculation is now using the correct value. This only impacted simulations using the new `with replacement` feature. We sincerely apologize for this issue. See [#598](https://github.com/SchmidtDSE/kigali-sim/pull/598). 

### Speed boost 2

**Status**: Released October 20, 2025

**Classification**: Enhancement

Various speed improvements. See [#593](https://github.com/SchmidtDSE/kigali-sim/pull/593), [#594](https://github.com/SchmidtDSE/kigali-sim/pull/594), [#596](https://github.com/SchmidtDSE/kigali-sim/pull/596). Some users may have seen this prior to release date on early rollout after Oct 8.

### Add bank to visualization and grammar

**Status**: Released October 20, 2025

**Classification**: Enhancement

Added option to visualize the bank in equipment or volume and use bank keyword in QubecTalk code. See [#591](https://github.com/SchmidtDSE/kigali-sim/pull/591) and [#592](https://github.com/SchmidtDSE/kigali-sim/pull/592). Some users may have seen this prior to release date on early rollout after Oct 8.

### Retire with replacement option

**Status**: Released October 8, 2025

**Classification**: Enhancement

Added support for the retire command to automatically marginally increase sales to offset any lost equipment. See [#587](https://github.com/SchmidtDSE/kigali-sim/pull/587).

### Speed boost 1

**Status**: Released October 7, 2025

**Classification**: Enhancement

Speed boost in unit conversion. See [#584](https://github.com/SchmidtDSE/kigali-sim/pull/584).

### Color coordiantion in subapps

**Status**: Release October 6, 2025 (all users Oct 7)

**Classification**: Enhancement

Added a feature where all of the sub-applications now have the same color as their parent application if there are more than 5 substances. This makes compelx simulations easier to read. See [#581](https://github.com/SchmidtDSE/kigali-sim/pull/581).

### Set equipment meta-command

**Status**: Released October 6, 2025 (all users Oct 7)

**Classification**: Clarification

It was unclear if setting the equipment stream would cause retirement actions or not to happen based on the prior year. This was ambiguous because we encourage users to set priorEquipment which does not cause retirement. However, based on user feedback, we are having this perform retirement operations. See [#580](https://github.com/SchmidtDSE/kigali-sim/pull/580).

### Non-amortized retirement

**Status**: Released October 6, 2025 (all users Oct 7)

**Classification**: Bug

Previously the retirement emissions was amortized but spikes in retirement could cause EOL emissions to be spread out. This correctly puts the retirement in the year impacted. See [#579](https://github.com/SchmidtDSE/kigali-sim/pull/579).

### Streamline llms.txt

**Status**: Released September 28, 2025

**Classification**: Enhancement

Streamline the llms-full and llms.txt to better clarify what files are necessary and when. See [#577](https://github.com/SchmidtDSE/kigali-sim/pull/577).

### Change application of duplicate

**Status**: Released September 27, 2025

**Classification**: Bug

Now allow users to specify the application in which a duplicate substance consumption record is to be registered. Also fixes bug on numeric validation that was causing an error message. See [#575](https://github.com/SchmidtDSE/kigali-sim/pull/575).

### LLMs Exports

**Status**: Released September 27, 2025

**Classification**: Enhancement

Clarify to LLMs what the export columns are in order to allow them to more reliably perform analysis on simluation outputs.

### Change apply post-intervention

**Status**: Released September 27, 2025

**Classification**: Clarification

The simulation was preivously not applying percent changes to the post-intervention values on displacement targets. We now clarify this to always apply to the result of values after intervention. See [#570](https://github.com/SchmidtDSE/kigali-sim/pull/570).

### Unit conversion expansion

**Status**: Released September 27, 2025

**Classification**: Bug

Only tCO2e / mt was supported for conversions to masses and not kgCO2e / kg. This is fixed see [#572](https://github.com/SchmidtDSE/kigali-sim/pull/572)

### Label for domestic

**Status**: Released September 18, 2025

**Classification**: Clarification

Clarified in the UI-based editor that domestic is domestic virgin production as distinct to the recycling stream.

### Induced demand

**Status**: Released September 18, 2025

**Classification**: Clarification

Allow the user to directly control induced demand from recycling but suggesting 100% by default as alternatives require very careful modeling.

### Workshop tweaks

**Status**: Released September 18, 2025

**Classification**: Enhancement

Various small tweaks from workshops including the duplicate button and improved error messages for alternative number formats like 1.000.000,0 (instead of 1,000,000.0).

### Clarify inferred stream for change in sales

**Status**: Released September 10 (preview) and September 18 (all users), 2025

**Classification**: Bug

If one enables streams and then sets all sales and then specifies a percent change in domestic only (as opposed to all sales), the percentage did not get calculated correctly. Fixed - see [#539](https://github.com/SchmidtDSE/kigali-sim/pull/539).

### GWP lookup

**Status**: Released September 7, 2025

**Classification**: Enhancement

Added a lookup button to see if a given substance is a known substance and it can autofill the GHG equivalency. See [#519](https://github.com/SchmidtDSE/kigali-sim/issues/519).

### AI assistant support

**Status**: Released September 7, 2025

**Classification**: Enhancement

Added buttons which offer instruction for using AI assistants via llms.txt through QubecTalk. See [#520](https://github.com/SchmidtDSE/kigali-sim/issues/520).

### Metadata import / export

**Status**: Released September 7, 2025

**Classification**: Enhancement

By popular demand, we now support using spreadsheets to export data on substances and applications registered in simulations as well as using spreadsheets to import similar metadata. This makes it easier to register substances and applications in bulk. See [#518](https://github.com/SchmidtDSE/kigali-sim/issues/518).

### Fix replace for mid-year equipment calculation

**Status**: Released September 5, 2025 (preview) and September 7, 2025 (all users)

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

**Classification:** Clarification

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

**Classification:** Enhancement

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


