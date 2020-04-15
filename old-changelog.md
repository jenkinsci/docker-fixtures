For everything newer, see [GitHub Releases](https://github.com/jenkinsci/docker-fixtures/releases).

## 1.8 (2018 May 11)

* Standard containers no longer attempt to pin package versions at all.

## 1.7 (2018 Apr 05)

* Updating standard containers to run in UTF-8 system locale, enabling non-ASCII filenames.

## 1.6 (2018 Feb 05)

* Again fixing standard containers.

## 1.5 (2018 Jan 25)

* Standard containers were again unbuildable after a security update. Now checking this properly in CI.
* Updated to Guice 4.

## 1.4 (2017 Dec 07)

* Added `DockerClassRule` as an alternative to `DockerRule`; preferable when combined with rules imposing per-test timeouts.
* Added support for UDP port binding.

## 1.3 (2017 Dec 05)

* `JavaContainer` was unbuildable after a security update.

## 1.2 (2017 Nov 14)

* Updates to `SshdContainer` and `JavaContainer`.

## 1.1 (2017 Oct 12)

* Fix of status reports for older Docker versions (1.7 claimed).
* Metadata fixes to enable easier consumption from newer parent POMs.
* Avoiding use of stdio redirects which can confuse Surefire.

## 1.0 (2017 Feb 02)

Split out of the Jenkins acceptance test harness,
which continues to host [additional instructions](https://github.com/jenkinsci/acceptance-test-harness/blob/master/docs/FIXTURES.md) for use in that mode.
