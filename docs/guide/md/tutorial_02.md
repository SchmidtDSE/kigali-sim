# Tutorial 2: Single Application and Substance

We start with the basic building blocks of a simulation: applications and substances.

## Motivation

In this tutorial, we start ABC's first HFC consumption model by using volume-based inputs. This approach helps us understand the relationship between populations and refrigerant consumption while working with initial charges, retirement rates, and servicing patterns. All of these parameters will work together to project consumption trends over time.

Let's start this analysis with domestic refrigeration using HFC-134a. We will use this to demonstrate how to model a single application with basic equipment data. This is the simplest simulation possible within the tool.

## Setting Up Your First Application

Let's start by creating a new simulation for ABC:

- Click **Add Application** and enter "Domestic Refrigeration" without quotes.
- Leave Subapplication empty for now
- Click **Save**

You won't see results yet because we haven't added any substances or equipment data.

(tutorial02_step01.gif - alt: animated gif showing how to add a new application)

## Adding HFC-134a Substance

Next, let's add our refrigerant substance:

- Click **Add Consumption**.
- Enter **HFC-134a** for the substance name.
- Set the Global Warming Potential to **1430 kgCO2e/kg**. This means 1430 kg of CO2 equivalent per kilogram of substance. You can also use other units like 1.43 tCO2e/kg.
- Leave energy consumption at **1 kWh/unit** for now.
- **Enable the domestic manufacture checkbox** (domestic production stream).
- **Leave import and export unchecked** as ABC Country doesn't trade HFC-134a for domestic refrigeration.

Don't click save yet! We need to configure equipment properties first.

(tutorial02_step02.gif - alt: animated gif showing how to add a new substance)

## Equipment Properties

Now let's define the equipment characteristics that drive HFC consumption:

- Go to the **Equipment tab**
- Leave equipment type/model empty for now
- Enter **0.15 kg/unit** for initial charge (refrigerant per new unit)
- Set annual retirement rate to **5% each year** (corresponds to 20-year average equipment lifetime)

These basic parameters are enough to start modeling, but we also will want to account for servicing existing equipment.

(tutorial02_step03.gif - alt: animated gif showing how to specify equipment properties)

## Servicing Configuration

Refrigerants are needed both for new equipment (initial charge) and maintenance of existing equipment:

- Click **Add Recharge**
- Specify that **10% of equipment** is recharged each year on average
- Set recharge amount to **0.15 kg/unit** as a top-up
- Specify that this happens **in all years** of the simulation

We're almost ready to run our first simulation! We just need initial conditions.

(tutorial02_step04.gif - alt: animated gif showing how to specify servicing properties)

## Initial Conditions

Head to the **Set tab** to specify ABC Country's starting conditions:

- Click **Add Setpoint**
- Set **prior equipment** to **1,000,000 units** in year 2025
- Set **domestic** manufacture to **25 mt / yr** in year 2025

This 25 mt/year gives us a good demonstrative curve showing how consumption patterns evolve. However, until we specify changes in later tutorials, this production rate will remain steady based on tonnage. It will be used both for initial charge and recharge.

(tutorial02_step05.gif - alt: animated gif showing how to specify initial conditions)

## Running Your First Simulation

Now let's see our model in action. After clicking save for our consumption record, do the following:

- Click **Add Simulation**
- Create a "Business as Usual" simulation (call it **BAU**).
- Set no policies (we'll add those later).
- Indicate a duration from **2025 to 2035**
- Click **Save**

(tutorial02_step06.gif - alt: animated gif showing how to create a simulation)

## Interpreting Results

Let's examine what our model shows us.

- First, select the **Consumption** radio button and you'll see HFC-134a consumption steady at **25 mt/year**, which the model automatically allocates between initial charge for new equipment and servicing existing equipment.
- Next, select **Equipment** to see that equipment population grows but the growth rate decreases over time. This happens because as the equipment population increases, more of the manufactured substance goes to recharge existing units, leaving less for new equipment growth.
- Finally, with **Emissions** selected, recharge emissions (substance lost during service) also grow but at a decreasing rate, reflecting the sub-linear equipment population growth.

We will add additional dynamics but this starts building an intuition for how Kigali Sim interpreted our very simple model.

(tutorial02_step07.gif - alt: animated gif showing how to use the visualization panel)

## Conclusion

You've successfully created ABC Country's first HFC consumption model! This basic simulation shows how 1 million existing refrigerators that, when combined with steady domestic production, create predictable consumption patterns. While we used simplified assumptions (uniform equipment characteristics, steady production), this foundation will flourish into much more sophisticated analysis as we add more information in. Specifically, we explored:

- **Equipment-based consumption modeling**: Understanding how equipment drives substance consumption
- **Initial charge vs. recharge**: Distinguishing between new equipment needs and servicing requirements
- **Equipment lifecycles**: How retirement rates affect long-term consumption patterns
- **Supply chain basics**: Domestic manufacturing without imports

For some readers, this may seem like a lot of data to specify. Still, don't forget that we can use some basic assumptions to begin our analysis and get a rough idea of a system. These rough estimates can give way to more detailed information as we gather it. This includes adding actual historical values. For other readers, this may seem too simplistic. However, we will grow sophistication in this case study over time as we add in new dynamics.

## Next Steps

**Tutorial 3** will expand our model to include multiple equipment applications and refrigerant substances across ABC Country's complete national inventory. You'll learn to model commercial refrigeration, air conditioning, and other sectors while comparing different refrigerants and their climate impacts - building toward a comprehensive national HFC profile.

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._
