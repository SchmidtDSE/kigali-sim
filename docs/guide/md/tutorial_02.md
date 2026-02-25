# Tutorial 2: Single Application and Substance

We start with the basic building blocks of a simulation: applications and substances.

## Motivation

In this tutorial, we examine ABC Country, a hypothetical country that we can use to explore Kigali Sim's functionality. Specifically, we will make a first HFC consumption model by using volume-based inputs. This approach helps us understand the relationship between populations and refrigerant consumption while working with initial charges, retirement rates, and servicing patterns. All of these parameters will work together to project consumption trends over time.

Let's start this analysis with domestic refrigeration using HFC-134a. We will use this to demonstrate how to model a single application with basic equipment data. This is the simplest simulation possible within the tool.

## Setting Up Your First Application

Let's start by creating a new simulation for ABC:

- Click **Add Application** and enter "Domestic Refrigeration" without quotes.
- Leave Subapplication empty for now
- Click **Finish**

You won't see results yet because we haven't added any substances or equipment data.

## Adding HFC-134a Substance

Next, let's add our refrigerant substance:

- Click **Add Consumption**.
- Enter **HFC-134a** for the substance name.
- Set the GHG equivalency to **1430 kgCO2e/kg**.
- Leave energy consumption at **1 kWh/unit** for now.
- **Enable the domestic manufacture checkbox** (domestic production stream).
- **Leave import and export unchecked** as ABC Country doesn't trade HFC-134a for domestic refrigeration.

Don't click save yet! We need to configure equipment properties first.

> **More about greenhouse gas emissions**: In Kigali Sim the GHG equivalency or intensity is the same as the global warming potential (GWP) of the substance. Our value means 1430 kg of CO2 equivalent per kilogram of substance. You can also use other units like 1.43 tCO2e/kg.

## Equipment Properties

Now let's define the equipment characteristics that drive HFC consumption:

- Go to the **Equipment tab**
- Leave equipment type/model empty for now
- Enter **0.15 kg/unit** for initial charge (refrigerant per new unit)
- Set annual retirement rate to **5% each year** (corresponds to 20-year average equipment lifetime)
- Leave **"Retirement reduces in-service equipment"** checked. This is recommended for most simulations but a later tutorial will revisit this option.

> **More about retirement rate**: The retirement rate is sometimes called the **hazard rate** or **scrap rate** in equipment lifecycle analysis. Kigali Sim, by default, assumes a constant hazard rate when you specify a percentage. However, this can be modified by using a non-percentage retirement amount if you need more complex retirement patterns.

These basic parameters are enough to start modeling, but we also will want to account for servicing existing equipment.

## Servicing Configuration

Refrigerants are needed both for new equipment (initial charge) and maintenance of existing equipment:

- Go to the **Servicing tab**
- Click **Add Recharge**
- Specify that **10% of equipment** is recharged each year on average
- Set recharge amount to **0.15 kg/unit** as a top-up
- Specify that this happens **in all years** of the simulation

We're almost ready to run our first simulation! We just need initial conditions.

## Initial Conditions

Head to the **Set tab** to specify ABC Country's starting conditions:

- Click **Add Setpoint**
- Set **prior equipment** to **1,000,000.0 units** in year 2025
- Set **domestic** manufacture to **25 mt / yr** in year 2025

This 25 mt/year gives us a good demonstrative curve showing how consumption patterns evolve. However, until we specify changes in later tutorials, this production rate will remain steady based on tonnage. It will be used both for initial charge and recharge.

> **More about default sales assumptions**: Below the setpoints, you may see a "Default sales assumption in a new year" dropdown. For this tutorial (and most simulations), leave this set to "Continue from last year (recommended)" which is the default. This setting controls how sales carry over from one year to the next. The default option maintains existing sales patterns. In other words, it assumes that prior sales levels persist until other indication is provided.

## Running Your First Simulation

Now let's see our model in action. After clicking save for our consumption record, do the following:

- Click **Add Simulation**
- Create a "Business as Usual" simulation (call it **BAU**).
- Set no policies (we'll add those later).
- Indicate a duration from **2025 to 2035**
- Click **Finish**

## Interpreting Results

Let's examine what our model shows us.

- First, select the **Consumption** radio button and you'll see HFC-134a consumption steady at **25 mt/year**, which the model automatically allocates between initial charge for new equipment and servicing existing equipment.
- Next, select **Bank** to see that equipment population grows but the growth rate decreases over time. This happens because as the equipment population increases, more of the manufactured substance goes to recharge existing units, leaving less for new equipment growth.
- Finally, with **Emissions** selected, **recharge emissions** and **end of life emissions** also grow but at a decreasing rate, reflecting the sub-linear equipment population growth.

We will add additional dynamics but this starts building an intuition for how Kigali Sim interpreted our very simple model.

> **More about emissions**: Emissions are volumes of substance which leak into the environment. We typically determine that substance was lost during servicing when those levels are topped up or at end of life where any remaining substance is assumed to eventually leak. That said, emissions is the sum of:
>
> - **Initial charge emissions**, typically a small to negligible amount of emissions as part of the manufacture or assembly process.
> - **Recharge emissions**, the amount determined to have been emitted between either 1) manufacture and the current servicing or 2) between the prior servicing and the current one.
> - **End of life emissions**, the amount assumed to eventually leak, typically after retirement ("scrap").
>
> These happen over the lifetime of equipment. However, the specific curve describing volume emitted over time is often variable or unknown. Therefore, the exact timing of emissions is sometimes unclear. That in mind, these three volumes of emissions are associated with the time of manufacture or assembly, servicing, or retirement. In practice, substance which was not reported in recharge emissions or initial charge emissions but which is still expected to emit because it was not captured (recycled or destroyed) is reported in end of life emissions. While individual units' emissions curves may be unclear, the population-wide amount as a whole tends to resolve to a strong volume estimate and good temporal approximation using this approach.
>
> In any case, emissions excludes substance captured prior to emitting. A topic revisited in a later tutorial, this includes substance captured and destroyed or recycled.

> **More about bank**: We will return to the concept of the bank in later sections of this tutorial series. However, at a high level, this refers to all of the equipment and substance not yet emitted within the country. The "bank measures" allow us to see what that population of machinery and reservoir of substance looks like. Even if policies and treaty mechanisms often do not directly address these metrics, this may be important to understand when modeling.

## Conclusion

You've successfully created ABC Country's first HFC consumption model! This basic simulation shows how 1 million existing refrigerators that, when combined with steady domestic production, create predictable consumption patterns. While we used simplified assumptions (uniform equipment characteristics, steady production), this foundation will flourish into much more sophisticated analysis as we add more information in. Specifically, we explored:

- **Equipment-based consumption modeling**: Understanding how equipment drives substance consumption.
- **Initial charge vs. recharge**: Distinguishing between new equipment needs and servicing requirements.
- **Equipment lifecycles**: How retirement rates affect long-term consumption patterns.
- **Supply chain basics**: Domestic manufacturing without imports.

For some readers, this may seem like a lot of data to specify. Still, don't forget that we can use some basic assumptions to begin our analysis and get a rough idea of a system. These rough estimates can give way to more detailed information as we gather it. This includes adding actual historical values. For other readers, this may seem too simplistic. However, we will grow sophistication in this case study over time as we add in new dynamics.

Note that we specified HFC-134a to be both domestically produced and consumed during this first tutorial. This simplifies our ability to reason about the results: we don't have to consider trade attribution rules yet. However, a later tutorial will consider trade and how to analyze results under treaty conventions.

**Download the completed tutorial**: [tutorial_02.qta](tutorial_02.qta) - this contains the complete single application and substance model

## Next Steps

[Tutorial 3](/guide/md/tutorial_03.md) will expand our model to include multiple equipment applications and refrigerant substances across ABC Country's complete national inventory. You'll learn to model commercial refrigeration, air conditioning, and other sectors while comparing different refrigerants and their climate impacts - building toward a comprehensive national HFC profile.

[Previous: Tutorial 1](/guide/md/tutorial_01.md) | [Return to Guide Index](/guide/md/index.md) | [Next: Tutorial 3](/guide/md/tutorial_03.md)

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._
