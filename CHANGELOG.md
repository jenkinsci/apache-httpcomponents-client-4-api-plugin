Release Notes
===

# 4.5.5-3.0

Release date: June 12, 2018

* Developer: Add `RobustHTTPClient` utility API for making HTTP connections with protection against transient failures. API is marked as beta and may change at any time.
* Developer: Add support for incremental Maven releases. ([JEP-305](https://github.com/jenkinsci/jep/tree/master/jep/305))

# 4.5.5-2.1

Release date: May 22, 2018

* Developer: Remove unhelpful tests as a workaround for [JENKINS-51483](https://issues.jenkins-ci.org/browse/JENKINS-51483) to fix the [PCT](https://github.com/jenkinsci/plugin-compat-tester) when this plugin is tested against Jenkins core 2.112 and newer.

# 4.5.5-2.0

Release date: May 1, 2018

* [JENKINS-48357](https://issues.jenkins-ci.org/browse/JENKINS-48357)/[PR #6](https://github.com/jenkinsci/apache-httpcomponents-client-4-api-plugin/pull/6) - Also bundle Apache HttpComponents HttpAsyncClient 4.1.3 to prevent class loading issues if `httpasyncclient` or `httpasyncclient-cache` are used with this API plugin.

# 4.5.5-1.0

Release date: Apr 26, 2018

* [PR #5](https://github.com/jenkinsci/apache-httpcomponents-client-4-api-plugin/pull/5) -
Update to Apache HttpComponents Client 4.5.5. ([full changelog](https://github.com/apache/httpcomponents-client/blob/4.5.5/RELEASE_NOTES.txt))

# 4.5.3-2.1

Release date: Jan 23, 2018

* [PR #4](https://github.com/jenkinsci/apache-httpcomponents-client-4-api-plugin/pull/4) -
Use `commons-codec` library from the Jenkins core instead of bundling a custom one.

# 4.5.3-2.0

Release date: Sep 14, 2017

* Bundle all components of HttpComponents Client 4.5.x instead of just the core client library

# 4.5.3-1.0

Release date: Aug 15, 2017

* Initial release with Apache HttpComponents Client 4.5.3
* The plugin does not bundle any additional API

