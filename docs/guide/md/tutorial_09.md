# Tutorial 9: Command Line and Monte Carlo Analysis

Advanced uncertainty analysis using probabilistic QubecTalk programming.

## Motivation

ABC Country's policymakers want to understand how their comprehensive strategy performs under different economic growth rates, equipment population uncertainties, and implementation challenges. This requires advanced analysis capabilities beyond the web interface using Monte Carlo simulation to quantify uncertainty and test policy robustness.

In this tutorial, we'll transition from the web interface to command line tools and implement probabilistic modeling using QubecTalk's uncertainty features. This allows us to test thousands of scenarios automatically and understand the range of possible outcomes for our policy interventions.

## Setting Up Command Line Tools

First, we need to download and set up the Kigali Sim command line interface:

- Scroll to the bottom of the Kigali Sim web page
- Click the **Download JAR** button to get the command line version
- Save your current QubecTalk model by copying it from the Editor tab to a text file (e.g., `abc_country.qta`)

The JAR file enables batch processing, automation, and Monte Carlo simulation that aren't available in the web interface. This is particularly valuable for uncertainty analysis where we need to run hundreds or thousands of model iterations.

## Understanding Uncertainty in Our Model

Looking at our current ABC Country model, we have several sources of uncertainty:

- **Economic growth rates**: Our projections assume specific growth percentages, but actual economic performance varies
- **Equipment populations**: Prior equipment estimates are based on surveys and may have measurement uncertainty
- **Policy implementation**: Real-world policy effectiveness can vary from planned targets

Let's focus on the first two sources where we have quantitative data to work with.

## Adding Growth Rate Uncertainty

Economic growth projections inherently contain uncertainty. Let's modify our model to reflect this by adding probabilistic elements to our growth rates.

In your QubecTalk code, we'll replace fixed growth rates with probability distributions. For example, instead of:

```
change sales by 5 % / year during years 2025 to 2030
```

We'll use:

```
change sales by sample normally from mean of 5 % std of 1 % / year during years 2025 to 2030
```

Update all growth rates in your model with this approach:

**For R-600a in Domestic Refrigeration:**

```
change sales by sample normally from mean of 5 % std of 1 % / year during years 2025 to 2030
change sales by sample normally from mean of 3 % std of 1 % / year during years 2031 to 2035
```

**For HFC-134a in Domestic Refrigeration:**

```
change sales by sample normally from mean of 6 % std of 1 % / year during years 2025 to 2030
change sales by sample normally from mean of 4 % std of 1 % / year during years 2031 to 2035
```

**For HFC-32 in Domestic AC:**

```
change domestic by sample normally from mean of 10 % std of 1 % / year during years 2025 to 2035
```

**For R-410A in Domestic AC:**

```
change domestic by sample normally from mean of 6 % std of 1 % / year during years 2025 to 2035
```

## Adding Equipment Population Uncertainty

Prior equipment populations are estimates that contain measurement uncertainty. Let's add 10% standard deviation to reflect this uncertainty.

Replace the fixed `priorEquipment` values with probabilistic specifications:

**For R-600a:**

```
set priorEquipment to sample normally from mean of 100000 std of (100000 * 0.1) units during year 2025
```

**For HFC-134a:**

```
set priorEquipment to sample normally from mean of 1000000 std of (1000000 * 0.1) units during year 2025
```

**For HFC-32:**

```
set priorEquipment to sample normally from mean of 40000 std of (40000 * 0.1) units during year 2025
```

**For R-410A:**

```
set priorEquipment to sample normally from mean of 20000 std of (20000 * 0.1) units during year 2025
```

## Configuring Monte Carlo Simulation

Now we need to configure our simulations to run multiple trials. Update the simulations section to include Monte Carlo analysis:

```
start simulations

  simulate "BAU Uncertainty"
  from years 2025 to 2035
  across 1000 trials

  simulate "Permit Uncertainty"
    using "Sales Permit"
  from years 2025 to 2035
  across 1000 trials

  simulate "Combined Uncertainty"
    using "Sales Permit"
    then "Domestic Recycling"
  from years 2025 to 2035
  across 1000 trials

  simulate "Recycling Uncertainty"
    using "Domestic Recycling"
  from years 2025 to 2035
  across 1000 trials

end simulations
```

The `across 1000 trials` specification tells Kigali Sim to run 1000 different scenarios, each time sampling new values from the probability distributions we defined.

## Running Command Line Analysis

Save your updated QubecTalk model and run it using the command line interface:

```bash
java -jar kigali-sim.jar abc_country_uncertainty.qta
```

This will execute all 1000 trials for each simulation scenario, generating probability distributions of outcomes rather than single point estimates.

## Interpreting Uncertainty Results

The Monte Carlo analysis will generate statistical distributions showing:

- **Range of outcomes**: Minimum and maximum values across all trials
- **Confidence intervals**: 95% confidence bounds on consumption and emissions projections
- **Policy robustness**: How consistently policies perform across different scenarios
- **Risk assessment**: Probability of exceeding certain consumption thresholds

For example, instead of a single emissions projection, you'll see:

- **Mean emissions**: Average across all 1000 trials
- **Standard deviation**: Measure of variability
- **Percentiles**: 5th, 25th, 50th, 75th, 95th percentile outcomes

## Policy Robustness Analysis

Compare the uncertainty ranges across your four scenarios:

1. **BAU Uncertainty**: Shows baseline consumption variability without intervention
2. **Permit Uncertainty**: Tests sales permitting effectiveness under economic uncertainty
3. **Recycling Uncertainty**: Evaluates recycling program performance across scenarios
4. **Combined Uncertainty**: Assesses comprehensive policy strategy robustness

Key questions to explore:

- Which policies maintain effectiveness even in worst-case economic scenarios?
- How much does equipment population uncertainty affect policy outcomes?
- Are there scenarios where policies might underperform expectations?
- What's the minimum guaranteed benefit from each policy intervention?

## Understanding Probabilistic QubecTalk

The probabilistic features we used demonstrate QubecTalk's advanced capabilities:

- **Normal distributions**: `sample normally from mean of X std of Y` for variables with symmetric uncertainty
- **Mathematical expressions**: `(100000 * 0.1)` for calculating standard deviations
- **Multiple trials**: `across 1000 trials` for Monte Carlo simulation
- **Uncertainty propagation**: How input uncertainties combine to affect final outcomes

These features enable sophisticated policy analysis that accounts for real-world variability and implementation challenges.

## Conclusion

You've successfully implemented Monte Carlo uncertainty analysis for ABC Country's HFC strategy. This advanced analysis demonstrates:

- **Probabilistic modeling**: Using distributions rather than point estimates
- **Uncertainty quantification**: Understanding the range of possible outcomes
- **Policy robustness testing**: Evaluating performance under various scenarios
- **Command line capabilities**: Automated batch processing for complex analysis
- **Risk assessment**: Probability-based policy evaluation

This uncertainty analysis provides policymakers with confidence intervals and risk assessments that support more informed decision-making under uncertainty. Rather than single projections, they now have probability distributions that show both best-case and worst-case scenarios.

## Next Steps

This concludes our ABC Country tutorial series! You now have comprehensive skills in:

- Equipment-based HFC modeling
- Multi-sector, multi-substance analysis
- Economic growth and trade flow modeling
- Policy intervention design and evaluation
- QubecTalk programming
- Uncertainty quantification and Monte Carlo analysis

These tools provide a complete foundation for supporting Kigali Amendment Implementation Plans and HFC phase-down policy analysis. The combination of deterministic modeling and probabilistic analysis enables robust policy recommendations that account for real-world uncertainty and implementation challenges.

---

_This tutorial completes the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._