
**Steps for the System Administrator (Root User):**

1.  **Create the `systemd` Service Unit File:**
    * Log in as `root` or use `sudo`.
    * Create a file, for example, `/etc/systemd/system/brodside-email.service`:
        ```bash
        sudo nano /etc/systemd/system/brodside-email.service
        ```
    * Paste the following content into the file:
        ```ini
        [Unit]
        Description=Brodside Email Application
        After=network.target

        [Service]
        User=serversage
        WorkingDirectory=/var/serversage/demo-application/
        ExecStart=/var/serversage/demo-application/brodside-email
        # Optional: Restart the service if it crashes
        Restart=on-failure
        RestartSec=5s # Wait 5 seconds before attempting restart
        StandardOutput=journal # Redirect stdout to journald
        StandardError=journal  # Redirect stderr to journald

        [Install]
        WantedBy=multi-user.target
        ```
    * **Explanation of the `systemd` file:**
        * `Description`: A human-readable description of your service.
        * `After=network.target`: Ensures the network is up before starting your service.
        * `User=serversage`: **Crucially, this specifies that the service will run as the `serversage` user.** This is exactly what you need.
        * `WorkingDirectory`: Sets the directory where the binary expects to be run from.
        * `ExecStart`: The full path to your binary.
        * `Restart=on-failure`: Important for resilience; restarts the service if it exits unexpectedly.
        * `StandardOutput/Error=journal`: Directs logs to `systemd`'s journal, which `serversage` can then view using `journalctl --user` or `journalctl -u brodside-email.service`.
        * `WantedBy=multi-user.target`: Ensures the service starts when the system reaches a multi-user state (normal boot).

2.  **Reload `systemd` Daemon:**
    ```bash
    sudo systemctl daemon-reload
    ```

3.  **Enable the Service (for auto-start on boot):**
    ```bash
    sudo systemctl enable brodside-email.service
    ```

4.  **Start the Service Now:**
    ```bash
    sudo systemctl start brodside-email.service
    ```

5.  **Verify Status:**
    ```bash
    sudo systemctl status brodside-email.service
    ```
    (Or `systemctl status brodside-email.service` if logged in as root).

---

