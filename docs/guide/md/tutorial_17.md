# Tutorial 17: Exports

How to model for countries or systems with exports.

## Motivation

When we referred to **sales** previously, that meant the combination of imported substance and substance which is both produced and sold domestically. However, let's consider the three production streams individually:

- **Domestic**: Substance for initial charge and servicing produced in the country and consumed inside of the same country.
- **Import**: Substance for servicing which is produced outside of the country but which, through trade, enters and is used in the country. This may also include local "assembly" where the initial charge of equipment happens in country (as opposed to before importing).
- **Export**: Substance for initial charge or servicing produced in the country and, through trade, is consumed outside of that country.

So far, we've focused just on domestic and import. In this next tutorial, we will see how exports can be added which are substances contributing to a country's consumption but does not cause its domestic equipment to increase.

Note that this tutorial will use QubecTalk code but exports can also be specified in the UI-based editor.

## Adding exports

Specifying exports is similar to the other streams. Let's make an example of HFC-134a in domestic refrigeration that has both domestic production and exports.

```qubectalk
uses substance "HFC-134a"
  enable domestic
  enable export
  initial charge with 1 kg / unit for domestic
  initial charge with 1 kg / unit for export
  equals 1430 kgCO2e / kg
  equals 1 kwh / unit
  set priorEquipment to 10,000 units during year 1
  set export to 500 units during year 2 to onwards
  set sales to 1,000 units during year 2 to onwards
end substance
```

You can incorporate this into a broader simulation with BAU, or **download the tutorial code** at [tutorial_17.qta](../tutorial_17.qta) which includes the snippet above in a larger example simulation.

## Reviewing export results

In the visualization of the results, let's consider that we had 10,000 units during year 1. Since there is no retirement or servicing, we expect 11,000 units in year 2 if we look at the number of units of equipment in bank. However, what about the exports? These don't appear in the bank because they are sent outside of the country but, under consumption, we still see 1500 kg because the initial charge for those exported units count towards the exporter's consumption.

Before moving on, note that changing sales to domestic for the one thousand units per year does not change the results. This is because **sales** refers to sales within the country's bank so represents the combination of domestic and import but excludes export.

## Conclusion

The exports stream provides another way to interact with trade. However, one or even all three streams can be active at the same time. Indeed, exports work the same way as any other sales stream. Just be careful to enable the stream before use, either by selecting the corresponding checkbox in the UI editor under consumption or by using the enable statement in QubecTalk.

## Next Steps

That's the end of the tutorial series for now! Check back later for more updates.

[Previous: Tutorial 16](/guide/tutorial_16.html) | [Return to Guide Index](/guide)

---

*This tutorial is part of the Advanced Techniques series demonstrating specialized aspects of Montreal Protocol policy modeling using Kigali Sim.*
