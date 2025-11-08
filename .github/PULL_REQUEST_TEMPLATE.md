## Description

<!-- Provide a clear and concise description of what this PR does -->

## Type of Change

<!-- Mark the relevant option with an "x" -->

- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Code refactoring
- [ ] Build/CI configuration change
- [ ] Other (please describe):

## Related Issues

<!-- Link to related issues using #issue_number -->

Fixes #
Closes #
Related to #

## Changes Made

<!-- List the main changes in this PR -->

-
-
-

## Platforms Tested

<!-- Check all platforms where you tested this change -->

- [ ] Web (WASM)
- [ ] Desktop (JVM)
- [ ] Android
- [ ] iOS
- [ ] Backend Services
- [ ] N/A (documentation/config only)

## Testing

<!-- Describe the tests you ran and how to reproduce them -->

### Test Steps

1.
2.
3.

### Test Coverage

- [ ] Added unit tests for new functionality
- [ ] Added integration tests
- [ ] Updated existing tests
- [ ] Manual testing performed
- [ ] No tests needed (documentation/config only)

### Test Results

```
# Paste relevant test output here
./gradlew allTests
```

## Screenshots/Videos

<!-- If applicable, add screenshots or videos to demonstrate the changes -->

## Checklist

<!-- Mark completed items with an "x" -->

### Code Quality

- [ ] My code follows the project's coding style and conventions
- [ ] I have performed a self-review of my own code
- [ ] I have commented my code, particularly in hard-to-understand areas
- [ ] My changes generate no new warnings or errors
- [ ] I have removed any debug/console.log statements

### Security & Best Practices

- [ ] I have verified multi-tenant isolation (all queries filter by `tenant_id`)
- [ ] I use NUMERIC for all monetary values (never FLOAT)
- [ ] I added audit logging for financial operations
- [ ] I validated all user inputs
- [ ] I checked for SQL injection vulnerabilities
- [ ] No sensitive data (API keys, passwords) in the code
- [ ] N/A (no security implications)

### Documentation

- [ ] I have updated relevant documentation (README, docs/, CLAUDE.md)
- [ ] I have updated API documentation (if API changes were made)
- [ ] I have added/updated code comments
- [ ] I have updated the CHANGELOG.md
- [ ] N/A (no documentation needed)

### Database Changes

- [ ] I created a new Flyway migration (if schema changes were made)
- [ ] Migration is reversible (or documented why not)
- [ ] Migration tested on a copy of production-like data
- [ ] All indexes are properly defined
- [ ] N/A (no database changes)

### Dependencies

- [ ] I used version catalog (`libs.*`) for any new dependencies
- [ ] New dependencies are necessary and justified
- [ ] Dependencies are compatible with all target platforms
- [ ] N/A (no new dependencies)

### Breaking Changes

- [ ] This PR contains no breaking changes
- [ ] Breaking changes are documented below and in CHANGELOG.md
- [ ] Migration guide provided for users

**Breaking Changes Description:**
<!-- If applicable, describe what breaks and how users should migrate -->

## Additional Notes

<!-- Any additional information reviewers should know -->

## Reviewer Checklist

<!-- For reviewers - do not modify -->

- [ ] Code review completed
- [ ] Security implications assessed
- [ ] Multi-tenant isolation verified
- [ ] Tests are adequate and passing
- [ ] Documentation is clear and complete
- [ ] No obvious performance issues
- [ ] Changes align with project architecture
