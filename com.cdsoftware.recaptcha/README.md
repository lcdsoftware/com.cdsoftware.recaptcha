# com.cdsoftware.recaptcha

- Copyright: 2026 https://www.casadelsoftware.com
- Repository: https://bitbucket.org/cdsoftware/com.cdsoftware.recaptcha
- License: GPL 2

## Description

This plugin integrates Google reCAPTCHA v2 (Checkbox) into the iDempiere ZK login window as a fully decoupled OSGi Fragment. It adds client-side token fetching and secure server-side verification before iDempiere processes user authentication, preventing automated brute-force attacks on the login system.

## Contributors

- 2026 Carlo González <carlo.gonzalez@casadelsoftware.com>.

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

### Components

#### ZK Login Listener (`RecaptchaLoginListener`)
A ZK lifecycle listener (`UiLifeCycle`) that intercepts the creation and attachment of `LoginPanel` components.
- **Frontend Injection**: Injects the Google reCAPTCHA v2 checkbox widget into the login form table, scaled at `0.70` (211px wide by 53px tall) with rounded corners to match the theme's text fields.
- **Client-Side Sync**: Adds widget event listeners on the OK button (`onClick`) and window (`onOK`) to retrieve the captcha response token and store it in a hidden textbox.
- **Server-Side Validation**: Adds a high-priority (`1000`) listener that intercepts the login click and submits the captcha token to Google's verification API. If verification fails, it halts execution (`event.stopPropagation()`), resets the widget, and raises a ZK `WrongValueException`.

### Utils

#### FileTemplateBuilder
Creates text files dynamically by injecting data into FreeMarker templates.
- **Usage example**:
  ```java
  FileTemplateBuilder.builder()
      .file("template.xml")
      .inject("data", myDataInstance)
      .export("output.xml")
  ```

#### KeyValueLogger
A wrapper around `CLogger` to write formatted key-value pair log messages.
- **Usage example**:
  ```java
  KeyValueLogger.instance(MyClass.class)
      .message("Action completed")
      .info();
  ```

#### SqlBuilder
Loads SQL queries from file templates.
- **Usage example**:
  ```java
  String sql = SqlBuilder.builder().file("query.sql").build();
  ```

#### TimestampUtil
Helper class for creating and formatting `Timestamp` instances.

## Instructions

### Installation

1. Deploy the `com.cdsoftware.recaptcha` plugin bundle into the iDempiere OSGi container.
2. Since it is configured as a fragment of `org.adempiere.ui.zk`, it will merge automatically with ZK's classloader context.
3. If required, import the plugin's 2Pack metadata to register validation messages.

### OSGi Fragment Resolution (Troubleshooting)

Because this plugin is packaged as an OSGi Fragment, it does not have its own active lifecycle (it cannot be started directly and will never reach the `ACTIVE` state). 

When you install it on a running server, it defaults to the `INSTALLED` state:
1. Locate the bundle ID of the fragment and its host bundle (`org.adempiere.ui.zk`):
   ```osgi
   ss | grep recaptcha         # e.g. Bundle ID: 42
   ss | grep adempiere.ui.zk   # e.g. Bundle ID: 15
   ```
2. To attach the fragment, run `refresh` on the host bundle (or the fragment):
   ```osgi
   refresh 15
   ```
3. The fragment bundle status will transition from `INSTALLED` to `RESOLVED`, indicating it has successfully attached to the host web client.

### Configuration

Log in to iDempiere as `System` (or your Client Admin) and create/update the following configuration keys in the **System Configurator** window:

| Name | Search Key | Description |
| --- | --- | --- |
| `RECAPTCHA_ENABLED` | `Y` | Set to `Y` to enable the verification check, or `N` to disable it. |
| `RECAPTCHA_SITE_KEY` | *Your Site Key* | The public Google reCAPTCHA v2 Site Key. |
| `RECAPTCHA_SECRET_KEY` | *Your Secret Key* | The private Google reCAPTCHA v2 Secret Key. |
