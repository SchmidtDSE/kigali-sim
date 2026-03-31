# Documentation

This directory contains documentation for Kigali Sim.

## Purpose

The documentation provides comprehensive information about the tool's functionality, technical specifications, and user guidance for both basic and advanced usage.

### Specifications

The technical specifications in the `spec` directory contain detailed information about:

- QubecTalk language syntax and semantics
- Engine behavior and computational models
- API specifications and data formats

The user guides in the `guide` directory are organized as tutorials though these could be thought of as chapters. These provide:

- Step-by-step instructions for common workflows
- Case study examples and modeling approaches
- Interactive help and troubleshooting guidance
- Programming reference for advanced users

## Development

When contributing to documentation, please try to:

1. Follow existing formatting and style conventions
2. Ensure all examples are tested and working
3. Update both technical specs and user guides when making functional changes
4. Test all links and references

Note that many files are HTML-based and include interactive elements and embedded examples. They are automatically deployed as part of the main website. See the GitHub actions for details (see `build_spec.yaml`).

The `guide/md/` directory is **not tracked in the repository**. It is generated automatically during CI/CD (`generateGuideMd` job in `build.yaml`) by converting the HTML guide files to GitHub-Flavored Markdown using Pandoc. The generated files exist only in the deploy artifact and on the hosted site, where they serve `llms.txt`. Do not add or edit files in `guide/md/` directly — edit the corresponding `.html` source instead.