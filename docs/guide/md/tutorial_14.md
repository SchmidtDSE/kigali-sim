# Tutorial 14: Bank-Based Modeling

How to use Kigali Sim when you have longitudinal bank estimates. 

## Motivation

Sometimes also called the reservoir, the bank refers to the amount of substance held within the country. This is a "stock" of substance which may not have been used yet but already exists distributed across tanks and in individual units before it is emitted. Typically Kigali Sim calculates this bank by looking at the amount of equipment at the start of the year, the amount lost to retirement (scrap), the servicing needs which may indicate some emissions, recycling or recovery if active, and sales including through trade. That said, some countries might have estimates of the bank either in terms of number of units of equipment or volume of substance and, from that, need to go the other way to determine what Kigali Sim typically treats as inputs. We refer to this as "bank-based" tracking and it can be used both in the historic and future sections of a dataset.

## Specifying the bank level

Let's model HFC-134a used within commercial refrigeration and let's say this is imported. Let's then specify 1 kg / unit initial charge and 5% retirement with 5% serviced with 0.85 kg / unit during all years. However, let's indicate the following bank levels each year:

- 0.5 mt in 2020
- 0.9 mt in 2021
- 1.2 mt in 2022
- 1.4 mt in 2023
- 1.5 mt in 2024
- 1.7 mt in 2025

If you are building this simulation in the UI-based designer, select "bank" for the stream instead of the "import" or "domestic" option you would normally select.

If you are using QubecTalk, it can be specified like so:

```qubectalk
uses substance "HFC-134a"
  enable import
  initial charge with 0 kg / unit for domestic
  initial charge with 1 kg / unit for import
  initial charge with 0 kg / unit for export
  equals 1430 kgCO2e / kg
  equals 1 kwh / unit
  set bank to 0.5 mt during year 2020
  set bank to 0.9 mt during year 2021
  set bank to 1.2 mt during year 2022
  set bank to 1.4 mt during year 2023
  set bank to 1.5 mt during year 2024
  set bank to 1.7 mt during year 2025
  retire 5 % / year
  recharge 5 % with 0.85 kg / unit
end substance
```

## Understand translation

Let's start by running a simulation just from 2020 to 2025. Under emissions or import consumption (either with or without "Attribute initial charge consumption to importer" checked), we see that Kigali Sim has calculated consumption amounts corresponding to those bank changes. Finally, under bank, we can select mt and see the exact values we specified reflected back.

Under the hood, Kigali Sim's engine is determining each year what sales needed to have been in order for the bank values to be true. It then can determine the overall equipment unit count as a result. However, it does this after taking into account retirement and servicing. 

## Changing the bank level

So far, we have modeled historic data but let's say we have forecast of how this reservoir will also change in the future. Let's add a change of 10% during years 2026 to 2030.

However, what will that 10% be an increase over? Recall from recent tutorials that we assume sales from the prior year continue to the next so, under typical defaults, we are increasing the bank by 10% each year after also applying the sales from the year prior.

This might be appropriate given how your forecasting model is designed but, for this tutorial, let's assume we are forecasting the bank size without that assumed continued sales. Therefore, let's set the default sales assumption to recharge only. For those working in the UI-based editor, change "Default sales assumption in a new year" to "Cover only servicing" in the General tab. Alternatively, for those in QubecTalk, add an `assume` command like as follows:

```qubectalk
assume only recharge sales
```

Finally, change your business as usual simulation to run from 2020 to 2030. You should see a steady increase in the bank levels into the future!

## Conclusion

In this tutorial, we used a bank measured in volume (mt) but we can also measure it in terms of units of equipment. In this case, you can also use the "equipment" stream and it will have the same effect.

One last thing to consider is that currently we assume retired equipment is not replaced so the 10% increase in bank is after retirement from the year prior. In most simulations, this doesn't have a large effect but, if you want to assume that retired equipment is replaced such that it is strictly a 10% increase over the bank level specified in the year prior, you can turn on this automatic back fill. For those using the UI-based designer, please uncheck "Retirement reduces in-service equipment" in the Equipment tab. For those working with QubecTalk, change retirement like as follows:

```qubectalk
retire 5 % / year with replacement
```

Nevertheless, bank-based tracking provides a valuable alternative if you have overall metrics for your system but need to calculate what that means in terms of different streams like imports.

**Download the completed tutorial**: [tutorial_14.qta](../tutorial_14.qta) - this contains the complete bank-based modeling example

## Next Steps

[Tutorial 15](/guide/md/tutorial_15.md) will explore variable retirement and servicing rates. You'll learn how to specify hazard rates that change according to equipment age for more sophisticated lifecycle modeling.

[Previous: Tutorial 13](/guide/md/tutorial_13.md) | [Return to Guide Index](/guide/md/index.md) | [Next: Tutorial 15](/guide/md/tutorial_15.md)

---

_This tutorial is part of the Advanced Techniques series demonstrating specialized aspects of Montreal Protocol policy modeling using Kigali Sim._

---

[View HTML version](../tutorial_14.html)
