# Apache HttpComponents Client 4.x API Plugin for Jenkins

This plugin bundles all the components of [Apache HttpComponents Client 4.5.x](https://hc.apache.org/httpcomponents-client-4.5.x/index.html) except `httpclient-win` because of the dependency on jna.
These components can be used by other plugins as a dependency.
It allows managing library updates independently from plugins.

## How to introduce to your plugin

### Plugins directly depending on httpclient

Replace the dependency to `org.apache.httpcomponents:httpclient` with the dependency to `apache-httpcomponents-client-4-api`.

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
        <version>4.5.10-1.0</version>
      </dependency>
      ...
    </dependencies>
    ```

### Plugins using libraries depending on httpclient

Add the dependency to `apache-httpcomponents-client-4-api` BEFORE any of dependencies to those libraries to force maven to use `httpclient` declared by `apache-httpcomponents-client-4-api`.

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
        <version>4.5.10-1.0</version>
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

See the [Changelog](CHANGELOG.md).

## License

* Plugin codebase - [MIT License](http://opensource.org/licenses/MIT) 
* Nested library is licensed with [Apache License, Version 2.0](http://www.apache.org/licenses/)

