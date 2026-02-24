# Tutorial 3: Multiple Applications and Substances

Expanding to multiple sectors and refrigerants.

**Note that this is the conventional version of this tutorial.** An AI-assisted alternative is available at [Tutorial 3a](/guide/tutorial_03a.html).

## Motivation

In ABC, we have more than just domestic refrigeration! In this tutorial, we'll expand our model to capture more of the broader national profile, helping us understand where policy may put its focus. Specifically, this tutorial builds directly on our Tutorial 2 Domestic Refrigeration model, adding Domestic AC. We'll also introduce multiple refrigerant substances with different Global Warming Potentials (GWPs), demonstrating how volume and climate impact interact.

## Adding New Applications and Substances

Let's practice the functionality we explored in the prior tutorial. Use the same procedures from Tutorial 2 and let's add additional applications and substances. Remember to have **recharge** in all years. However, the set points for prior equipment and domestic manufacture should be on 2025 only. Also, while we will look at trade in the next tutorial, **enable domestic manufacture** for all substances. Here's the configuration data:

| Property | HFC-134a | R-600a | HFC-32 | R-410A |
|----------|----------|--------|--------|--------|
| **Application** | Domestic Refrigeration | Domestic Refrigeration | Domestic AC | Domestic AC |
| **GWP (kgCO2e/kg)** | 1430 | 3 | 675 | 2088 |
| **Initial Charge (kg/unit)** | 0.15 | 0.07 | 0.85 | 1.00 |
| **Retirement (%/year)** | 5% | 5% | 7% | 7% |
| **Recharge (% @ kg/unit)** | 10% @ 0.15 | 10% @ 0.07 | 15% @ 0.85 | 15% @ 1.00 |
| **Prior Equipment (units)** | 1,000,000.0 | 100,000.0 | 40,000.0 | 20,000.0 |
| **Domestic Manufacture (mt/yr)** | 25 | 2 | 15 | 5 |

We will add in socioeconomic projections soon but, for now, consumption volumes will continue unchanged into the future.

**Note**: Kigali Sim requires decimal point number formatting (comma for thousands separator, period for decimal point, ex: `123,456.7` or `40,000.0`). If you use alternative formatting (ex: `123.456,7`), Kigali Sim will display a message with a suggestion for the equivalent supported format. Both formatting conventions are internationally valid, but Kigali Sim uses the DecimalFormat format for consistency with the technologies it leverages. This means `40,000` is interpreted as forty thousand.

## Interpreting Multi-Application Results

As you work, the simulation will update automatically.

Examine the results to understand how multiple applications and substances add together. You can do this by looking at results by selecting the **Application** or **Substances** radio buttons. To get a complete picture with the **Emissions** radio button, try clicking **"configure custom"** under emissions and combining both end-of-life and recharge emissions. This represents the total leakage throughout the equipment lifetime.

Before concluding, let's also pause to understand if these results make sense. First, the custom emissions which combines both end of life and recharge emissions is higher than either alone. Second, consider that the HFC-134a has higher volume and higher GWP than R-600a. Therefore, focusing on HFC-134a, we notice that these two factors intersect through a larger gap to R-600a in emissions relative to consumption when we have selected the **Substances** radio button.

## Conclusion

You've successfully expanded ABC Country's model to include multiple applications and substances. Together, we considered:

- **Multi-application modeling**: Different sectors with distinct equipment characteristics and service patterns.
- **Multi-substance analysis**: Comparing different refrigerants within and across applications.
- **GWP diversity**: Understanding how different substances have varying climate impacts.
- **Equipment population dynamics**: How different applications scale and behave over time.

The model now provides a foundation for understanding how substance choice and application type interact to determine overall consumption and climate impact patterns.

**Download the completed tutorial** result at [tutorial_03.qta](tutorial_03.qta) which contains the complete multi-application and multi-substance model. It differs from the [prior tutorial result](tutorial_02.qta) in that it adds new substances and applications.

## Next Steps

[Tutorial 4](/guide/tutorial_04.html) will add economic growth projections and business-as-usual forecasting to your multi-application model. You'll learn to model how economic expansion drives consumption changes over time, creating realistic baseline scenarios for policy comparison.

[Previous: Tutorial 2](/guide/tutorial_02.html) | [Return to Guide Index](/guide) | [Next: Tutorial 4](/guide/tutorial_04.html)

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._
