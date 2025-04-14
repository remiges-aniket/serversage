# Grafana Alert Provisioning Guide: JSON Alert File Placement

This guide outlines the steps for placing the JSON alert file within your Grafana setup to enable alert provisioning.

**Understanding Grafana Provisioning**

Grafana allows you to manage various configurations, including alerts, through provisioning. This involves placing configuration files in specific directories, which Grafana then reads during startup.

**Steps to Place the JSON Alert File**

1.  **Locate the Grafana Provisioning Directory:**
    * The primary directory for Grafana provisioning is typically found within the Grafana configuration directory.
    * The exact location can vary based on your operating system and Grafana installation method (e.g., Docker, package installation).
    * Common locations include:
        * `/etc/grafana/provisioning/` (for package installations)
        * `/var/lib/grafana/provisioning/`
        * If using Docker, it might be a volume mounted to `/etc/grafana/provisioning/` within the container.

2.  **Create an `alerting` Subdirectory:**
    * Inside the provisioning directory, create a subdirectory named `alerting`.
    * If it doesn't exist. Grafana will look for alert provisioning files within this specific directory.
    * The final path should look like `/etc/grafana/provisioning/alerting/` or similar, depending on your setup.

3.  **Place the JSON File:**
    * Place your JSON alert definition file (e.g., `alerts.json`) into the `alerting` subdirectory.
    * The file name is not strictly defined by Grafana, but using a descriptive name is recommended.

4.  **Grafana Configuration (Optional):**
    * In some cases, you might need to explicitly tell Grafana to load provisioning files. This is usually done in the `grafana.ini` configuration file.
    * Ensure that the `alerting` provisioning is configured.  This often involves a section like this (check your Grafana version's documentation):

        ```ini
        [alerting]
        enabled = true

        [provisioning.alerting]
        path = /etc/grafana/provisioning/alerting
        ```

5.  **Restart Grafana:**
    * After placing the JSON file, restart the Grafana server. This will allow Grafana to read the new alert definitions and provision them.

**Important Notes:**

* **File Format:** Ensure your JSON file is correctly formatted.  Grafana expects a specific structure for alert provisioning.  Refer to the Grafana documentation for the precise schema.
* **Permissions:** Make sure the Grafana process has the necessary permissions to read the JSON file.
* **Grafana Documentation:** Always consult the official Grafana documentation for your specific Grafana version.  Provisioning methods and file structures can change between versions.  The most reliable information will be found here: [https://grafana.com/docs/grafana/latest/alerting/set-up/provision-alerting-resources/file-provisioning/](https://grafana.com/docs/grafana/latest/alerting/set-up/provision-alerting-resources/file-provisioning/)

By following these steps, Grafana should correctly load and provision the alerts defined in your JSON file.
