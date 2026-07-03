# com.cdsoftware.recaptcha
- Copyright: 2026 https://www.casadelsoftware.com
- Repository: https://bitbucket.org/cdsoftware/com.cdsoftware.recaptcha.git
- License: GPL 2

## Description
The `com.cdsoftware.recaptcha` plugin is a custom extension for iDempiere. It extends standard system capabilities by providing Application Dictionary configurations (2Pack) to support customized business workflows.

## Contributors
- 2026 Carlo González <carlo.gonzalez@casadelsoftware.com>.

## Components
- iDempiere Plugin [com.cdsoftware.recaptcha](com.cdsoftware.recaptcha)
- iDempiere Unit Test Fragment [com.cdsoftware.recaptcha.test](com.cdsoftware.recaptcha.test)

## Prerequisites
- Java 11, commands `java` and `javac`.
- iDempiere 11

## Features/Documentation
### Source Structure
```
├── com/
        ├── cdsoftware/
            ├── recaptcha/
                ├── util/
                    ├── FileTemplateBuilder.java
                    ├── KeyValueLogger.java
                    ├── SqlBuilder.java
                    ├── TimestampUtil.java
                ├── base/
                    ├── BundleInfo.java
                    ├── CustomCallout.java
                    ├── CustomForm.java
                    ├── CustomProcess.java
                ├── listener/
                    ├── RecaptchaLoginListener.java
```





### Application Dictionary Metadata (2Pack)

| Package / File Name | Purpose & Dictionary Configurations |
| --- | --- |
| `config.xml` | Metadata package containing Application Dictionary (AD) configurations. |
| `xml-invoice.xml` | Metadata package containing Application Dictionary (AD) configurations. |


## Instructions
1. Deploy the `com.cdsoftware.recaptcha` OSGi bundle in your iDempiere environment.
2. Restart iDempiere and refresh OSGi bundles to register factories.
3. Configure dictionary and role access rules as needed.
