The Docker fixture system originally in [`acceptance-test-harness`](https://github.com/jenkinsci/acceptance-test-harness) but now available also for use from any JUnit tests, especially using `JenkinsRule`.

[Original instructions](https://github.com/jenkinsci/acceptance-test-harness/blob/076069e04b96ba54965d18a66b57885d470da5b6/docs/FIXTURES.md) pending updated documentation

## Changelog

### 1.1 (2017 Oct 12)

* Fix of status reports for older Docker versions (1.7 claimed).
* Metadata fixes to enable easier consumption from newer parent POMs.
* Avoiding use of stdio redirects which can confuse Surefire.
