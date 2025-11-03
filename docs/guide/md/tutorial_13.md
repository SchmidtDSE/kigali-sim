# Tutorial 13: Assumed Replacement

We can assume that retired equipment are automatically replaced regardless of sales data.

## Contents

- [Motivation](#motivation)
- [Retiring normally](#retiring-normally)
- [Evaluating default behavior](#evaluating-default-behavior)
- [Changing the assumption](#changing-the-assumption)
- [Conclusion](#conclusion)
- [Next Steps](#next-steps)

## Motivation

When we added equipment retirement in previous tutorials, that equipment was removed from the population. Then, depending on the level of sales (import, domestic, or recycling), that equipment may or may not get replaced. That said, what if your sales data already excluded replacement equipment? Perhaps you have data from a permitting system so know when equipment is sold but not when it is replaced or transferred. We can instruct Kigali Sim that retired equipment should be replaced on top of other sales values specified.

## Retiring normally

Let's set up a basic simulation with HFC-134a in commercial refrigeration. Let's have this be imported and set the prior equipment to 1,000,000 units in 2025 but let's not set a specific sales value. Instead, let's use the "default sales assumption in a new year" dropdown menu to select cover only servicing, meaning that we will only assume sales for repair or top up of existing equipment. Finally, after specifying a 5% retirement rate, let's have this take 1 kg / unit in initial charge and recharge 5% each year with 0.85 kg / unit during all years.

## Evaluating default behavior

Let's add a business as usual scenario that runs from 2025 to 2035. Then, look at the bank of equipment under this default behavior. We see the equipment population decline over time as expected: we are servicing but we aren't replacing equipment that goes to scrap.

## Changing the assumption

Let's go back to HFC-134a's definition. If you are using the UI-based designer, go to the Equipment tab and uncheck "Retirement reduces in-service equipment" or, if you are using QubecTalk, add `with replacement` to the end of the retire command like so:

```
uses substance "HFC-134a"
  enable import
  assume only recharge sales
  initial charge with 0 kg / unit for domestic
  initial charge with 1 kg / unit for import
  initial charge with 0 kg / unit for export
  equals 1430 kgCO2e / kg
  equals 1 kwh / unit
  set priorEquipment to 1,000,000 units during year 2025
  retire 5 % / year with replacement
  recharge 5 % with 0.85 kg / unit
end substance
```

Now, we still see the servicing consumption but, under total units of equipment in bank, we find that the size of the population does not decrease. Note that we still also see end of life emissions but old equipment in Kigali Sim is required and automatically replaced with new equipment sales.

## Conclusion

This option to retire with replacement can be helpful in a number of modeling circumstances. For example, let's say you have estimates of how much kg is in bank for a substance but don't know the servicing or new equipment consumption. Kigali Sim can automatically calculate what servicing demands were and what retirement happened which required replacement.

**Download the completed tutorial**: [tutorial_13.qta](tutorial_13.qta) - this contains the complete assumed replacement model

## Next Steps

[Tutorial 14](/guide/tutorial_14.html) will explore bank-based modeling. You'll learn how to use Kigali Sim when you have longitudinal bank estimates and need to work backwards to determine consumption patterns.

[Previous: Tutorial 12](/guide/tutorial_12.html) | [Next: Tutorial 14](/guide/tutorial_14.html)

---

_This tutorial is part of the Advanced Techniques series demonstrating specialized aspects of Montreal Protocol policy modeling using Kigali Sim._
