# Tutorial 15: Variable Retirement and Servicing

How to specify variable hazard rates for complex retirement and servicing patterns.

## Contents

- [Motivation](#motivation)
- [Using default assumptions](#using-default-assumptions)
- [Increasing hazard rates with age](#increasing-hazard-rates-with-age)
- [Alternatives to hazard rate](#alternatives-to-hazard-rate)
- [Conclusion](#conclusion)
- [Next Steps](#next-steps)

## Motivation

In prior tutorials, we mentioned that a 5% retirement rate corresponds to a 20 year average lifespan. However, in practice some equipment will retire before 20 years and some after. That in mind, how does Kigali Sim actually interpret that 5% directive? In survivor analysis, we specifically say that there is a constant "hazards rate" meaning that, each year, each unit of equipment has a 5% chance of retiring. In practice, this makes sense for many countries which may not know the specific age of equipment already in service or might not have a good estimate for how the rate of failure changes over time. With few assumptions, we still get realistic dynamics like a long tail of equipment on phase out and some retirement prior to the amortized lifespan estimate.

All that said, Kigali Sim can take in more sophisticated estimates if available. In this tutorial, we will create a hazard rate which changes according to the age of equipment in a simulation that reaches back to 1990 which we assume to be the first year in which the equipment we are studying appeared within the country.

## Using default assumptions

Before we model the complex lifecycle, let's set up a simulation in which we model HFC-134a (1 kg / unit initial charge and no servicing). In order to observe how retirement works, let's have 1990 start with 1 mt of imports, 1991 to 2024 have an increase in imports of 10%, and set import to 0 mt in 2025. Let's assume a 5% retirement rate and simulate a business as usual scenario from 1990 to 2050. 

We see that equipment does drop but, if we took the 5% as 20 year lifespan seriously, we wouldn't have any population (see bank and set to units of equipment) after 2045. Instead, we see that a small amount of equipment keeps "getting lucky" and surviving that annual retirement probability (hazard rate) of 5%. 

## Increasing hazard rates with age

In some ways, this lingering population of equipment actually reflects what we would expect, especially if a substance suddenly became no longer available. However, let's change this hazard rate such that the rate of failure is 1% multiplied by the age of the equipment in years. To do this, modify the simulation using QubecTalk code like follows:

```
uses substance "HFC-134a"
  enable import
  initial charge with 0 kg / unit for domestic
  initial charge with 1 kg / unit for import
  initial charge with 0 kg / unit for export
  equals 1430 kgCO2e / kg
  equals 1 kwh / unit
  set import to 1 mt during year 1990
  set import to 0 mt during year 2025
  change import by 10 % / year during years 1991 to 2024
  retire (get age as years) * 1% / year
end substance
```

By the time we get to 2025, much of the equipment is already a few years old so its hazard rate is already higher than the 5% we previously assumed. However, you'll notice that we drop below 2 units of equipment around 2043.

**Note**: This age-dependent retirement feature is only available in the Advanced Editor using QubecTalk code. It cannot be configured through the UI-based designer.

## Alternatives to hazard rate

Note that, in this simulation, we still specify hazard rates (rates of retirement) as a percentage. However, you can also specify an absolute number of units of equipment or volume equivalent (kg, mt, CO2e) to retire each year.

## Conclusion

If you have survival analysis data on equipment and detailed information on the age of equipment, specifying variable retirement may lead to higher accuracy in predictions. Additionally, though we looked at hazard rates for retirement, the same approach can apply to servicing as well if you use QubecTalk.

**Download the completed tutorial**: [tutorial_15.qta](tutorial_15.qta) - this contains the complete variable retirement model with age-dependent hazard rates

## Next Steps

This tutorial completes the Advanced Techniques series! You've learned how to leverage sophisticated features like variable retirement rates, bank-based modeling, and advanced sales assumptions. For next steps, consider exploring the other tutorials in the series or applying these techniques to your own Montreal Protocol policy analysis.

[Previous: Tutorial 14](/guide/tutorial_14.html) | [Guide Index](/guide/)

[Previous: Tutorial 14](/guide/tutorial_14.html) | [Return to Guide Index](/guide) | [Next: Tutorial 16](/guide/tutorial_16.html)

---

_This tutorial is part of the Advanced Techniques series demonstrating specialized aspects of Montreal Protocol policy modeling using Kigali Sim._
