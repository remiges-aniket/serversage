---

# 🔐 Integrating Keycloak with Grafana for SSO

Enable seamless Single Sign-On (SSO) for Grafana using Keycloak as your Identity Provider (IdP). This guide walks you through configuring Keycloak and Grafana to work together using OAuth2.

---

## 📋 Prerequisites

Before you begin, ensure you have:

* **Keycloak** installed and running.
* **Grafana** installed and running.
* Administrative access to both Keycloak and Grafana.
* Basic understanding of Keycloak concepts like realms, clients, and roles.

---

## 🛠️ Step 1: Configure Keycloak

### 1.1 Create a Realm (if not already created)

1. Log in to the Keycloak Admin Console.
2. From the top-left dropdown, select **Add Realm**.
3. Enter a name for your realm (e.g., `grafana`) and click **Create**.

### 1.2 Create a Client for Grafana

1. In the Keycloak Admin Console, navigate to your realm.
2. Go to **Clients** and click **Create**.
3. Enter the following details:

   * **Client ID**: `grafana`
   * **Client Protocol**: `openid-connect`
   * **Root URL**: `http://<GRAFANA_DOMAIN>:<GRAFANA_PORT>/`
4. Click **Save**.

### 1.3 Configure Client Settings

1. In the **Settings** tab for the `grafana` client:

   * **Access Type**: `confidential`
   * **Standard Flow Enabled**: `ON`
   * **Direct Access Grants Enabled**: `ON`
   * **Valid Redirect URIs**: `http://<GRAFANA_DOMAIN>:<GRAFANA_PORT>/*`
   * **Web Origins**: `*`
2. Click **Save**.

### 1.4 Obtain Client Credentials

1. Navigate to the **Credentials** tab of the `grafana` client.
2. Note down the **Client Secret**. You'll need this for Grafana configuration.

### 1.5 Configure Mappers (Optional for Role Mapping)

To map user roles from Keycloak to Grafana:

1. Go to the **Mappers** tab of the `grafana` client.
2. Click **Create** and add the following mappers:

   * **Name**: `roles`
   * **Mapper Type**: `User Realm Role`
   * **Token Claim Name**: `roles`
   * **Claim JSON Type**: `String`
   * **Add to ID token**: `ON`
   * **Add to access token**: `ON`
   * **Add to userinfo**: `ON`

---

## 📄 Step 2: Configure Grafana

### 2.1 Update Grafana Configuration

Edit the `grafana.ini` file (usually located at `/etc/grafana/grafana.ini`) and add the following under the `[auth.generic_oauth]` section:

```ini
[auth.generic_oauth]
enabled = true
name = Keycloak
allow_sign_up = true
client_id = grafana
client_secret = <CLIENT_SECRET>
scopes = openid email profile
auth_url = http://<KEYCLOAK_DOMAIN>:<KEYCLOAK_PORT>/realms/<REALM_NAME>/protocol/openid-connect/auth
token_url = http://<KEYCLOAK_DOMAIN>:<KEYCLOAK_PORT>/realms/<REALM_NAME>/protocol/openid-connect/token
api_url = http://<KEYCLOAK_DOMAIN>:<KEYCLOAK_PORT>/realms/<REALM_NAME>/protocol/openid-connect/userinfo
login_attribute_path = preferred_username
email_attribute_path = email
name_attribute_path = name
role_attribute_path = contains(roles[*], 'admin') && 'Admin' || contains(roles[*], 'editor') && 'Editor' || 'Viewer'
```

Replace the placeholders:

* `<CLIENT_SECRET>`: The client secret obtained from Keycloak.
* `<KEYCLOAK_DOMAIN>`: Your Keycloak server domain.
* `<KEYCLOAK_PORT>`: Your Keycloak server port (default is 8080).
* `<REALM_NAME>`: The name of your Keycloak realm.

### 2.2 Restart Grafana

After updating the configuration, restart the Grafana service:

```bash
sudo systemctl restart grafana-server
```

---

## 🔍 Step 3: Verify the Integration

1. Open your Grafana URL in a web browser: `http://<GRAFANA_DOMAIN>:<GRAFANA_PORT>/`
2. Click on **Sign in with Keycloak**.
3. You should be redirected to the Keycloak login page.
4. Enter your Keycloak credentials.
5. Upon successful authentication, you should be redirected back to Grafana and logged in.

---

## 🧪 Troubleshooting

* **Invalid Redirect URI**: Ensure that the redirect URI in Keycloak matches the Grafana URL.
* **Client Secret Issues**: Verify that the client secret in Grafana matches the one in Keycloak.
* **Role Mapping Not Working**: Check the mappers in Keycloak to ensure roles are being sent in the token.

For more clarification refer [this](https://github.com/remiges-aniket/serversage/blob/main/Documentation/other-supporting-docs/Integrating%20Keycloak%20with%20Grafana%20Example.md) raw example documentation.

---

## 📚 References

* [Grafana Documentation: Configure Keycloak OAuth2 Authentication](https://grafana.com/docs/grafana/latest/setup-grafana/configure-security/configure-authentication/keycloak/)
* [Keycloak Documentation](https://www.keycloak.org/documentation)

---