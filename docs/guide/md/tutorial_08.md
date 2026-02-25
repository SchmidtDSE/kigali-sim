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

- Change `set domestic to 13 mt / year during year 2025` to `set domestic to 87,000.0 units during year 2025`
- Change `set import to 11 mt / year during year 2025` to `set import to 55,000.0 units during year 2025`

For **R-600a** (0.07 kg/unit initial charge):

- Change `set domestic to 1 mt during year 2025` to `set domestic to 14,000.0 units during year 2025`
- Change `set import to 1 mt during year 2025` to `set import to 14,000.0 units during year 2025`

Your code should now look like this:

```qubectalk
define application "Domestic Refrigeration"
  uses substance "HFC-134a"
    # ... other configuration ...
    set domestic to 87,000.0 units during year 2025
    set import to 55,000.0 units during year 2025
    # ... rest of substance definition ...
  end substance
  
  uses substance "R-600a"
    # ... other configuration ...
    set domestic to 14,000.0 units during year 2025
    set import to 14,000.0 units during year 2025
    # ... rest of substance definition ...
  end substance
end application
```

You may also use `units / year`.

## Updating Policy to Use Units

Since we're now thinking in terms of equipment units, let's also update our Sales Permit policy to use unit-based caps:

```qubectalk
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

First, you may notice that switching to units slightly increased overall consumption. This happens because  we're setting new equipment sales to specific numbers of units so additional consumption is then needed for recharging existing equipment. In other words, when using unit-based modeling, servicing consumption is added on top of whatever is needed to support the specified equipment sales numbers.

Second, let's look closer at HFC-134a under the Permit scenario with the **Substances** and **Consumption** radio buttons selected. The combined import and domestic consumption doesn't go to 0 mt. That's because we specified a cap of 0 new units sold but we still have servicing. Try setting the consumption cap back to 0 mt and it will drop all the way.

Before we wrap up, we are displacing from HFC-134a to R-600a and it is worth discussing how calculations work when operating across multiple substances. When using volume-based caps (0 kg), we are determining how much volume of HFC-134a is lost and then translating that to the same number of kilograms of volume in R-600a. However, when we use units-based caps (0 units), we are determining how many new units of equipment are lost in HFC-134a and then adding that number of units to R-600a. The former doesn't result in the same number of units lost in HFC-134a being added to R-600a just as the later doesn't result in the same number of kilograms lost being added to R-600a. This is because their equipment initial charges are different! In other words, when doing calculations in units, units are translated. However, when doing calculations in volumes (kg or mt), volumes are translated.

## Displacement Type Considerations

When working with unit-based modeling, you may want to explicitly control how displacement operates. By default, `displacing` uses "equivalent" behavior. In other words, it preserves units when you specify in units, or volume when you specify in kg. However, you can override this:

- Use `displacing by units` to always maintain equipment population, regardless of how consumption is specified
- Use `displacing by volume` to always maintain substance mass, regardless of specification units

In the Designer UI, you can select these options in the displacement type dropdown when configuring cap or floor limits. In the Advanced Editor, simply add `by volume` or `by units` after `displacing`:

```qubectalk
cap sales to 0 units displacing by units "R-600a" during years 2035 to onwards
```

This explicit control is particularly valuable when you're mixing unit-based and volume-based specifications within the same model. For example, if you want to ensure equipment population remains constant even when some streams are specified in kg, use `displacing by units`.

## Conclusion

You've successfully demonstrated equipment unit-based modeling as an alternative to volume specification. This tutorial showed how:

- **Equipment sales data** can drive consumption calculations automatically.
- **Unit specification** affects total consumption by adding servicing on top of sales needs.
- **Policy design** must consider whether targets are equipment sales or total substance consumption.
- **Modeling approach selection** can adapt to data availability and policy objectives.

The choice between unit-based and volume-based approaches depends on your data sources and policy questions, with each offering distinct advantages for different analytical needs.

Finally, note that QubecTalk allows flexibility to mix units. Specifically, our simulation now uses both unit-based and volume-based specifications simultaneously. We can do the same even within the same substance! For example, this may be helpful when specifying domestic manufacturing in units but imports in mass terms based on your available data.

**Download the completed tutorial** result at [tutorial_08.qta](../tutorial_08.qta) which uses unit-based modeling for domestic refrigeration. It differs from the [prior tutorial result](../tutorial_07.qta) in that the consumption numbers are different so, for example, the emissions numbers under the combined policy are different.

## Next Steps

Continue exploring the Kigali Sim guide with additional tutorials on modeling co-benefits, AI assistants, and advanced modeling techniques.

[Previous: Tutorial 7](/guide/tutorial_07.html) | [Return to Guide Index](/guide) | [Next: Tutorial 9](/guide/tutorial_09.html)

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._
