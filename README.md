# com.cdsoftware.recaptcha

- Copyright: 2026 https://www.casadelsoftware.com
- Repository: https://bitbucket.org/cdsoftware/com.cdsoftware.recaptcha
- License: GPL 2

## Description

This plugin integrates batch processes into iDempiere. It handles core configurations for the `com.cdsoftware.recaptcha` module, including user interface validations, database event triggers, and workflow parameters.

## Contributors

- 2026 cdsoftware <info@casadelsoftware.com>.

## Components

- iDempiere Plugin [com.cdsoftware.recaptcha](com.cdsoftware.recaptcha)
- iDempiere Unit Test Fragment [com.cdsoftware.recaptcha.test](com.cdsoftware.recaptcha.test)

## Prerequisites

- Java 17, commands `java` and `javac`.
- iDempiere 12.0.0

## Features/Documentation

### Source Structure

```text
com.cdsoftware.recaptcha/src
└── com
    └── cdsoftware
        └── recaptcha
            ├── base
            │   ├── BundleInfo.java
            │   ├── CustomCallout.java
            │   ├── CustomForm.java
            │   └── CustomProcess.java
            ├── listener
            │   └── RecaptchaLoginListener.java
            └── util
                ├── FileTemplateBuilder.java
                ├── KeyValueLogger.java
                ├── SqlBuilder.java
                └── TimestampUtil.java
```

### Processes

| Class Name | Purpose | Main Parameters | Key Logic & Results |
| --- | --- | --- | --- |
| `CustomProcess` | Handles business logic related to Custom process. | None | Executes batch operation for Custom process. |

## Instructions

1. Deploy the `com.cdsoftware.recaptcha` bundle into the iDempiere OSGi container.
2. The activator `Incremental2PackActivator` will automatically load and import all 2Pack zip packages from the `META-INF` folder (if present).
3. Restart or refresh the OSGi bundle context to ensure all services and factories are registered in the application.
