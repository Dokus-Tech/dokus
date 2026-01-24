# Sample Invoice Fixture

Add a `document.pdf` file to this directory containing a sample invoice.

The expected.json file contains the expected extraction result that the AI should produce.

To update the expected values after adding a real PDF:
1. Add your PDF as `document.pdf`
2. Run: `./gradlew :features:ai:backend:recordExtractionFixtures -Pfixture=sample-invoice`
3. Review and adjust the generated `expected.json`
