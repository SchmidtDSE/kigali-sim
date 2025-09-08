# Tutorial 9: Command Line and Monte Carlo Analysis

Advanced uncertainty analysis using probabilistic QubecTalk programming.

## Contents

- [Motivation](#motivation)
- [Setting Up Command Line Tools](#setting-up-command-line-tools)
- [Running Command Line Analysis](#running-command-line-analysis)
- [Understanding Uncertainty in Our Model](#understanding-uncertainty-in-our-model)
- [Adding Growth Rate Uncertainty](#adding-growth-rate-uncertainty)
- [Adding Equipment Population Uncertainty](#adding-equipment-population-uncertainty)
- [Configuring Monte Carlo Simulation](#configuring-monte-carlo-simulation)
- [Interpreting Uncertainty Results](#interpreting-uncertainty-results)
- [Conclusion](#conclusion)
- [Next Steps](#next-steps)

## Motivation

Let's say ABC Country's policymakers want to understand how uncertainty may change simulation outocmes. This requires advanced analysis capabilities beyond the web interface using Monte Carlo, a technique that can allow us to provide ranges of numbers instead of discrete estimates for our model parameters.

In this tutorial, we'll transition from the web interface to command line tools and implement probabilistic modeling using QubecTalk's uncertainty features. This allows us to test thousands of scenarios automatically and understand the range of possible outcomes for our policy interventions.

## Setting Up Command Line Tools

First, we need to download and set up the Kigali Sim command line interface:

- Scroll to the bottom of the Kigali Sim web page
- Click the **Download JAR** button to get the command line version
- Save your current QubecTalk model by copying it from the Editor tab to a text file (e.g., `abc_country.qta`)

The JAR file enables batch processing, automation, and Monte Carlo simulation that aren't available in the web interface. This is particularly valuable for uncertainty analysis where we need to run hundreds or thousands of model iterations.

## Running Command Line Analysis

In this tutorial, we will assume you know how to use the command line. Save your updated QubecTalk model and validate it.

```bash
java -jar kigalisim-fat.jar validate script.qta
```

Then, run it using the following command:

```bash
java -jar kigalisim-fat.jar run -o output.csv script.qta
```

Note that you will need to use the name of the file where you saved your results. This will execute your simulation and put the results in the specified CSV file.

## Understanding Uncertainty in Our Model

Looking at our current ABC Country model, we have several sources of uncertainty:

- **Economic growth rates**: Our projections assume specific growth percentages, but actual economic performance varies
- **Equipment populations**: Prior equipment estimates are based on surveys and may have measurement uncertainty
- **Policy implementation**: Real-world policy effectiveness can vary from planned targets

Let's focus on the first source. Note that we will use normal distributions but uniform distributions can also be used.

## Adding Growth Rate Uncertainty

Economic growth projections inherently contain uncertainty. Let's modify our model to reflect this by adding probabilistic elements to our growth rates.

In your QubecTalk code, we'll replace fixed growth rates with probability distributions. For example, instead of:

```
change sales by 5 % / year during years 2025 to 2030
```

We'll use:

```
change sales by sample normally from mean of 5 std of 1 % / year during years 2025 to 2030
```

Update all growth rates in your model with this approach:

**For R-600a in Domestic Refrigeration:**

```
change sales by sample normally from mean of 5 std of 1 % / year during years 2025 to 2030
change sales by sample normally from mean of 3 std of 1 % / year during years 2031 to 2035
```

**For HFC-134a in Domestic Refrigeration:**

```
change sales by sample normally from mean of 6 std of 1 % / year during years 2025 to 2030
change sales by sample normally from mean of 4 std of 1 % / year during years 2031 to 2035
```

**For HFC-32 in Domestic AC:**

```
change domestic by sample normally from mean of 10 std of 1 % / year during years 2025 to 2035
```

**For R-410A in Domestic AC:**

```
change domestic by sample normally from mean of 6 std of 1 % / year during years 2025 to 2035
```

## Adding Equipment Population Uncertainty

Equipment population estimates often contain measurement uncertainty from surveys and data collection challenges. We can model this uncertainty in our prior equipment estimates as well.

Instead of fixed prior equipment values, we can use probability distributions to represent our confidence in baseline equipment populations. However, for this tutorial, we'll focus primarily on growth rate uncertainty while keeping equipment populations deterministic for simplicity.

## Configuring Monte Carlo Simulation

Now we need to configure our simulations to run multiple trials. Update the simulations section to include Monte Carlo analysis:

```
start simulations

  simulate "BAU Uncertainty"
  from years 2025 to 2035
  across 100 trials

  simulate "Permit Uncertainty"
    using "Sales Permit"
  from years 2025 to 2035
  across 100 trials

  simulate "Combined Uncertainty"
    using "Sales Permit"
    then "Domestic Recycling"
  from years 2025 to 2035
  across 100 trials

  simulate "Recycling Uncertainty"
    using "Domestic Recycling"
  from years 2025 to 2035
  across 100 trials

end simulations
```

The `across 100 trials` specification tells Kigali Sim to run 100 different scenarios, each time sampling new values from the probability distributions we defined.

## Interpreting Uncertainty Results

Go ahead and give this another run. When you open up the resulting CSV file, notice individual trial results as denoted by the trial column. You can use this to run sensitivity analysis or plug into other tools like spreadsheet software, R, or Python.

## Conclusion

You've successfully implemented Monte Carlo uncertainty analysis for ABC Country's HFC strategy. This kind of simulation provides policymakers with confidence intervals and risk assessments that support more informed decision-making under uncertainty. Rather than single projections, they now have probability distributions that show both best-case and worst-case scenarios.

**Download the complete case study**: [case_study.qta](case_study.qta) - this contains the full ABC Country model with Monte Carlo uncertainty analysis

## Next Steps

This concludes our ABC Country tutorial series! You now have comprehensive skills in:

- Equipment-based HFC modeling
- Multi-sector, multi-substance analysis
- Economic growth and trade flow modeling
- Policy intervention design and evaluation
- QubecTalk programming
- Uncertainty quantification and Monte Carlo analysis

These tools provide a complete foundation for supporting Kigali Amendment Implementation Plans and HFC phase-down policy analysis. The combination of deterministic modeling and probabilistic analysis enables robust policy recommendations that account for real-world uncertainty and implementation challenges.

[Previous: Tutorial 8](/guide/tutorial_08.html) | [Next: Tutorial 10](/guide/tutorial_10.html)

---

_This tutorial completes the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim for now. Additional tutorials will be added later._