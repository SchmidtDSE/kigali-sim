# Tutorial 8: Equipment Units-Based Modeling

Alternative consumption specification using equipment sales data.

## Motivation

Let's say ABC Country's equipment manufacturers have provided direct sales data in units rather than refrigerant volumes. In this tutorial, we'll demonstrate how to model consumption based on equipment sales, which automatically calculates refrigerant consumption (both for new equipment and servicing) based on populations.

## Understanding Unit-Based vs. Volume-Based Modeling

Up to this point, we've been specifying consumption in mass (mt/year or kg/year). However, sometimes we may have equipment sales data in units/year instead where one unit refers to a single refrigerator or AC unit. QubecTalk supports both approaches:

- **Volume-based**: `set domestic to 13 mt during year 2025`
- **Unit-based**: `set domestic to 80000 units during year 2025`

## Converting to Unit-Based Specification

Let's modify our ABC Country model to use unit sales for HFC-134a and R-600a in domestic refrigeration. In the **Editor** tab, find the domestic refrigeration substances and update their initial conditions:

For **HFC-134a** (0.15 kg/unit domestic, 0.2 kg/unit import initial charge):

- Change `set domestic to 13 mt / year during year 2025` to `set domestic to 87,000 units during year 2025`
- Change `set import to 11 mt / year during year 2025` to `set import to 55,000 units during year 2025`

For **R-600a** (0.07 kg/unit initial charge):

- Change `set domestic to 1 mt during year 2025` to `set domestic to 14,000 units during year 2025`
- Change `set import to 1 mt during year 2025` to `set import to 14,000 units during year 2025`

Your code should now look like this:

```
define application "Domestic Refrigeration"
  uses substance "HFC-134a"
    # ... other configuration ...
    set domestic to 87,000 units during year 2025
    set import to 55,000 units during year 2025
    # ... rest of substance definition ...
  end substance
  
  uses substance "R-600a"
    # ... other configuration ...
    set domestic to 14,000 units during year 2025
    set import to 14,000 units during year 2025
    # ... rest of substance definition ...
  end substance
end application
```

## Updating Policy to Use Units

Since we're now thinking in terms of equipment units, let's also update our Sales Permit policy to use unit-based caps:

```
start policy "Sales Permit"
  modify application "Domestic Refrigeration"
    modify substance "HFC-134a"
      cap sales to 80 % displacing "R-600a" during years 2027 to 2034
      cap sales to 0 units displacing "R-600a" during years 2035 to onwards
    end substance
  end application
end policy
```

Note that we changed from `0 kg` to `0 units` for the final phase-out.

## Observing the Results

After making these changes, examine the simulation results with **Attribute initial charge to importer** checked and compare the unit-based model to your previous volume-based approach.

First, you may notice that switching to units slightly increased overall consumption. This happens because  we're setting new equipment sales to specific numbers of units so additional consumption is then needed for recharging existing equipment. In unit-based modeling, servicing consumption is added on top of whatever is needed to support the specified equipment sales numbers.

Second, let's look closer at HFC-134a under the Permit scenario with the **Substances** and **Consumption** radio buttons selected. The combined import and domestic consumption doesn't go to 0 mt. That's because we specified a cap of 0 new units sold but we still have servicing. Try setting the consumption cap back to 0 mt and it will drop all the way.

## Conclusion

You've successfully demonstrated equipment unit-based modeling as an alternative to volume specification. This tutorial showed how:

- **Equipment sales data** can drive consumption calculations automatically
- **Unit specification** affects total consumption by adding servicing on top of sales needs
- **Policy design** must consider whether targets are equipment sales or total substance consumption
- **Modeling approach selection** depends on data availability and policy objectives

The choice between unit-based and volume-based approaches depends on your data sources and policy questions, with each offering distinct advantages for different analytical needs.

Before we finish up, note that QubecTalk allows flexibility to mix units. Specifically, our simulation now uses both unit-based and volume-based specifications. However, we can do the same even within the same substance. For example, you might specify domestic manufacturing in units while specifying imports in mass terms based on your available data sources.

## Next Steps

**Tutorial 9** will introduce command line tools and Monte Carlo simulation for advanced uncertainty analysis. You'll learn to test model sensitivity to key assumptions and develop robust policy recommendations under uncertainty using probabilistic QubecTalk programming.

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._