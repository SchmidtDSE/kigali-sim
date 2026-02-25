# Tutorial 12: Advanced Sales Assumptions

We can modify the default sales assumptions to capture more advanced behavior.

## Motivation

In our simulations so far, we've left the default sales assumption in a new year dropdown to continue from last year. This means that the sales from the prior year are assumed to remain the same unless there is different information provided for that year. For example, consider this simulation below where we don't actually specify what the sales were in 2026 or 2027:

- set import to 4 mt in year 2024
- set import to 5 mt in year 2025
- change import by +20% in year 2027

What should happen in 2026 and 2027? Different users might expect different things. Does it make sense for consumption to drop to zero? Maybe imports just cover servicing? However, is omitting the year saying that there were no new sales or maybe just that there is no new data? Kigali Sim has three options for responding to this ambiguity:

- The default option of continue from previous year assumes that 2026 has 5 mt and 2027 has 6 mt (+20%).
- The servicing only option assumes that there are still imports to cover the servicing of existing equipment but no sales of new equipment.
- The zero option assumes no imports and, thus, no servicing.

We will explore these options further in this tutorial.

## Create a typical simulation

Let's start our exploration with the typical situation. Either through QubecTalk code or the UI-based editor, let's make a simulation with a business as usual scenario from 2024 to 2034 with HFC-134a used only in commercial refrigeration. Let's have the scenario above: set imports to 4 mt in 2024, 5 mt in 2025, and then add a change statement of +20% in 2027. Ensure there is an initial charge of 1 kg per unit and servicing of 5% with 0.85 kg / unit during all years.

```qubectalk
start default

  define application "Commercial Refrigeration"

    uses substance "HFC-134a"
      enable import
      initial charge with 0 kg / unit for domestic
      initial charge with 1 kg / unit for import
      initial charge with 0 kg / unit for export
      equals 1430 kgCO2e / kg
      equals 1 kwh / unit
      set sales to 4 mt during year 2024
      set sales to 5 mt during year 2025
      change sales by 20 % / year during year 2027
      retire 5 % / year
      recharge 5 % with 0.85 kg / unit
    end substance

  end application

end default


start simulations

  simulate "Business as Usual"
  from years 2024 to 2034

end simulations
```

## Duplicate the substance

Next, let's duplicate this substance for R-600a. To do this, use the duplicate button at the bottom of the editor panel. However, this time, set the "default sales assumption in a new year" dropdown to cover only servicing.

If you are using QubecTalk, place `assume only recharge sales` at the top of the definition of the substance like so:

```qubectalk
uses substance "R-600a"
  enable import
  assume only recharge sales
  initial charge with 0 kg / unit for domestic
  initial charge with 1 kg / unit for import
  initial charge with 0 kg / unit for export
  equals 1430 kgCO2e / kg
  equals 1 kwh / unit
  set sales to 4 mt during year 2025
  set sales to 5 mt during year 2026
  change sales by 20 % / year during year 2027
  retire 5 % / year
  recharge 5 % with 0.85 kg / unit
end substance
```

Note that the UI-based editor will generate `assume only recharge sales` but you can replace `sales` with a specific stream if you want to have different behaviors for domestic vs import, for example.

## Understanding the impact

For analyzing the results, let's ensure that we are looking at import consumption with the "Attribute initial charge consumption to importer" checkbox checked. This can be found at the bottom of the General tab. Then, click the radio button to compare substances.

We see that, in the case of HFC-134a, the levels sustain in 2026 and go up in 2027. In contrast, sales for R-600a only support servicing of existing equipment so we don't see zero consumption but we see minimal. This operates similar to setting imports to 0 units which, as mentioned in prior tutorials, assumes recharge but strictly nothing more.

## Conclusion

There's no single correct assumption for what sales should be assumed in years in which that information is not specified. Depending on the specifics of your data collection, Kigali Sim offers some options from continuing prior trends to dropping to zero. Consider which one makes the most sense given your data and what it means if a year is without information.

**Download the completed tutorial**: [tutorial_12.qta](../tutorial_12.qta) - this contains the complete advanced sales assumptions model

## Next Steps

[Tutorial 13](/guide/md/tutorial_13.md) will explore assumed replacement for retired equipment. You'll learn how to configure automatic replacement of retired equipment on top of other sales values when your sales data excludes replacement equipment.

[Previous: Tutorial 11](/guide/md/tutorial_11.md) | [Return to Guide Index](/guide/md/index.md) | [Next: Tutorial 13](/guide/md/tutorial_13.md)

---

_This tutorial is part of the Advanced Techniques series demonstrating specialized aspects of Montreal Protocol policy modeling using Kigali Sim._
