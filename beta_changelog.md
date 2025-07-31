# Beta Change Log

Hello! Thank you for your help in refining Kigali Sim. During the beta testing period, we will be making changes in response to user and partner feedback.

**About this document:** This document tracks modifications which are expected to concern a majority or substantial minority of beta testers. Note that the [GitHub commit log](https://github.com/SchmidtDSE/kigali-sim/commits) provides further public detail on all changes no matter how small.

**Scope:** Note that this concerns both those using the UI-based designer which does not require use of any code as well those making sophisticated simulations using the editor tab where custom code written in [QubecTalk](https://kigalisim.org/guide/tutorial_07.html) is to be used.

**Contact us:** If you have any questions, feel free to open issues on our GitHub or, if you do not want to participate on GitHub, feel free to email us at hello@kigalisim.org for private dialogue. We are here to help you and support your usage of the tool both during this beta period and after launch.

Finally, we want to again express our gratitude for your feedback and time.

## Needs Discussion

Information about issues or discussions still underway, with target completion before December launch.

### Servicing of new equipment

**Status:** In discussion

Some prior modeling efforts outside of Kigali Sim would apply servicing (top up or repair) to all equipment in a year while others would only apply it to equipment over a certain age, avoiding maintenance on equipment just sold. Kigali Sim currently takes the later approach where only existing equipment from previous years are subject to servicing and new equipment are not assumed to need recharge or repair. Multiple proposals have been made:

- Keep the current logic which only applies servicing to existing equipment from previous years.
- Use simpler logic where recharge applies to all equipment not old equipment to simplify manual calculations of users validating Kigali Sim models using manual calculations.
- Allow the user to specify which behavior is desired. However, this adds additional complexity to the tool and may lead to confusion when comparing results between Kigali Sim models.

This is being discussed in [#469](https://github.com/SchmidtDSE/kigali-sim/issues/469) and we welcome feedback. Note that the default behavior of Kigali Sim is to retire old equipment and then recharge from the remaining population but this can be modified through [QubecTalk](https://kigalisim.org/guide/tutorial_07.html) code (see the editor tab for advanced capabilities).

## Completed

The following changes have been adopted and released.

### Percent change with units fix

**Status:** Released July 31, 2025

**Classification:** Bug

As part of changes ahead of the open beta, we clarified that specifying volumes in units indicates that the simulation should treat that as number of new units sold such that recharge is automatically added on top of whatever initial charge is required for those new unit sales. More information about specifying sales in units can be found at https://kigalisim.org/guide/tutorial_08.html. This caused some complications with the expended recycling functionality added after [OEWG](https://ozone.unep.org/meetings/47th-meeting-open-ended-working-group-parties) but prior to the open beta starting including a bug where percent change was being applied to both initial charge as well as recharge when it should have only applied to the former (new equipment sales).

See [#468](https://github.com/SchmidtDSE/kigali-sim/pull/468) and [#474](https://github.com/SchmidtDSE/kigali-sim/pull/474) for more details. **We apologize for this issue** and the tool has been updated to have the intended behavior.

### Recycle time displacement

**Status:** Released July 30, 2025

**Classification:** Clarification

We added end of life recycling after the [OEWG](https://ozone.unep.org/meetings/47th-meeting-open-ended-working-group-parties) demonstration based on user feedback. However, this introduced some complexities around time displacement, specifically if recycled material should be immediately available or if there is some delay between recycling collection and its actual re-use. We have simplified our original logic to have recycled material be available in the same year it is collected. While we recognize that both approaches may be valid and this may be an over-simplification for end of life recycling, its impact on policy evaluation is minimal because over the full multi-year duration of a simulation, the results are generally the same or show negligible differences. Therefore, the benefits of the simplification were deemed to outweigh the costs.
