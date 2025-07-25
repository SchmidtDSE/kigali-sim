# Tutorial 7: First Code

Discovering QubecTalk and implementing multi-substance recycling policies.

## Motivation

Though it may not seem like it at first, you have actually been programming all this time! In this tutorial, we'll reveal the QubecTalk code underlying your UI-based modeling and use this new understanding to implement a multi-substance recycling program that covers both HFC-134a and R-600a in domestic refrigeration.

## The Programming Revelation

Let's discover the code you've already written:

- Click the **Editor** tab to reveal your QubecTalk program
- Review the code structure: `start default`, `define application`, `uses substance`
- Notice how your UI actions automatically generated this code.

You'll see that all your work from Tutorials 2-6 has been translated into QubecTalk, a domain-specific language designed specifically for Kigali Sim. We call the following **stanzas**:

- **Applications**: `define application "Domestic Refrigeration"` and `"Domestic AC"`
- **Substances**: `uses substance "HFC-134a"`, `"R-600a"`, `"HFC-32"`, `"R-410A"`
- **Policies**: `start policy "Sales Permit"` and `"Domestic Recycling"`
- **Simulations**: `start simulations` with BAU, Permit, Recycling, and Combined scenarios

Within each stanza, there are **commands** like `initial charge with 0.07 kg / unit for domestic`.

## Editing Code Directly

Let's make a simple change to get comfortable with code editing:

- In the Editor tab, find the HFC-134a recharge line: `recharge 10 % with 0.15 kg / unit`
- Change it to: `recharge 10 % with 0.14 kg / unit`
- Return to the **Design** tab and verify the change appears in the UI by clicking **edit** for HFC-134a

This demonstrates the two-way connection between UI and code - changes in either location update the model.

(tutorial07_01.gif, alt text: animated gif showing how to edit the code directly and see the results updated in the UI)

## Implementing R-600a Recycling

Now let's use QubecTalk to implement a recycling program for R-600a. The UI cannot handle multi-substance policies within the same application but code makes this straightforward.

Let's update your recycling policy like so:

```
start policy "Domestic Recycling"

  modify application "Domestic Refrigeration"

    # Make recycling program for HFC-134a
    modify substance "HFC-134a"
      recover 20 % with 90 % reuse during years 2027 to onwards
    end substance

	# Make recycling program for R-600a
	modify substance "R-600a"
      recover 20 % with 90 % reuse during years 2027 to onwards
    end substance
  
  end application

end policy
```

The existing simulations should update shortly after you finish. Note that indentation and extra spaces do not change the function of the code but they can help others more easily read the code later. Also, `#` denotes a comment which allows you to leave notes for yourself or other humans who might read your code. However, Kigali Sim ignores everything after `#` on the same line.

## Results

Recall that, in the prior tutorial where we didn't have a recycling program for R-600a so there was less HFC-134a to recycle late in the simulation.

With **Attribute initial charge to importer** checked, Let's compare these two outcomes by first commenting like the following (adding the leading `#`) while having the **Consumption** radio button selected.

```
	  modify substance "R-600a"
      # recover 20 % with 90 % reuse during years 2027 to onwards
    end substance
```

In 2035, combined import and domestic for the combined policy case is higher without the R-600a recycling program. However, let's remove that `#` again:

```
	  modify substance "R-600a"
      recover 20 % with 90 % reuse during years 2027 to onwards
    end substance
```

Now, the combined version sees lower consumption than recycling alone because the demand "displaced" from HFC-134a to R-600a now has a pathway to reuse.

(tutorial07_02.gif, alt text: animated gif showing how the policy change impacts overall consumption)

## Conclusion

You've successfully transitioned from UI-based to code-based modeling while implementing a comprehensive multi-substance recycling strategy. This tutorial revealed that you've been programming: all UI actions generate QubecTalk code automatically. However, we also saw that code enables complexity like, for example, multi-substance policies.

QubecTalk provides the foundation for advanced analysis techniques we'll explore in later tutorials, including uncertainty assessment and complex policy interactions.

## Next Steps

**Tutorial 8** will demonstrate equipment unit-based modeling as an alternative to volume-based consumption specification. You'll learn when to use unit sales data versus volume data and how QubecTalk handles both approaches seamlessly.

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._