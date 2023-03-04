# Apache HTTP Components Client 4.x API Plugin

This plugin bundles all the components of [Apache HttpComponents Client 4.5.x](https://hc.apache.org/httpcomponents-client-4.5.x/index.html) except `httpclient-win` because of the dependency on jna.
These components can be used by other plugins as a dependency.
It allows managing library updates independently from plugins.

## How to introduce to your plugin

### Plugins directly depending on httpclient

Replace the dependency to `org.apache.httpcomponents:httpclient` with the dependency to `apache-httpcomponents-client-4-api`.
To avoid version conflicts it is suggested not to depend on a specific version, but use the [Jenkins plugin BOM](https://github.com/jenkinsci/bom#readme).

* Before:
    ```
    <dependencies>
      ...
      <dependency>
        <groupId>org.apache.httpcomponents</groupId>
        <artifactId>httpclient</artifactId>
        <version>4.5</version>
      </dependency>
      ...
    </dependencies>
    ```
* After:
    ```
    <dependencies>
      ...
      <dependency>
        <groupId>org.jenkins-ci.plugins</groupId>
        <artifactId>apache-httpcomponents-client-4-api</artifactId>
      </dependency>
      ...
    </dependencies>
    ```

### Plugins using libraries depending on httpclient

Add the dependency to `apache-httpcomponents-client-4-api` BEFORE any of dependencies to those libraries to force maven to use `httpclient` declared by `apache-httpcomponents-client-4-api`.
To avoid version conflicts it is suggested not to depend on a specific version, but use the [Jenkins plugin BOM](https://github.com/jenkinsci/bom#readme).

* Before:
    ```
    <dependencies>
      ...
      <dependency>
        <artifactId>somelibrary-using-httpclient</artifactId>
        <version>1.0.0</version>
      </dependency>
      <dependency>
        <artifactId>anotherlibrary-using-httpclient</artifactId>
        <version>1.0.0</version>
      </dependency>
      ...
    </dependencies>
    ```
* After:
    ```
    <dependencies>
      ...
      <dependency>
        <groupId>org.jenkins-ci.plugins</groupId>
        <artifactId>apache-httpcomponents-client-4-api</artifactId>
      </dependency>
      <dependency>
        <artifactId>somelibrary-using-httpclient</artifactId>
        <version>1.0.0</version>
      </dependency>
      <dependency>
        <artifactId>anotherlibrary-using-httpclient</artifactId>
        <version>1.0.0</version>
      </dependency>
      ...
    </dependencies>
    ```

## Release Notes

See [GitHub releases](https://github.com/jenkinsci/apache-httpcomponents-client-4-api-plugin/releases) for current release notes.
Releases before 2019 are described in the archived [Changelog](https://github.com/jenkinsci/apache-httpcomponents-client-4-api-plugin/blob/apache-httpcomponents-client-4-api-4.5.5-2.0/CHANGELOG.md).

## License

* Plugin codebase - [MIT License](http://opensource.org/licenses/MIT) 
* Nested library is licensed with [Apache License, Version 2.0](http://www.apache.org/licenses/)

