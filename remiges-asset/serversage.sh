#!/bin/bash
# Apply all serversage chnges

##################################################################
# VISUAL CUSTOMIZATION - Replace branding assets
##################################################################

cp -f img/fav32.png /usr/share/grafana/public/img/fav32.png
cp -f img/apple-touch-icon.png /usr/share/grafana/public/img/apple-touch-icon.png
cp -f img/grafana_icon.svg /usr/share/grafana/public/img/grafana_icon.svg
cp img/g8_login_dark.svg /usr/share/grafana/public/img/g8_login_dark.svg
cp img/g8_login_light.svg /usr/share/grafana/public/img/g8_login_light.svg


##################################################################
# HTML & JS CUSTOMIZATION - Update titles, menus, and branding
##################################################################
# Update title and loading text in index.html
sed -i 's|<title>\[\[.AppTitle\]\]</title>|<title>ServerSage</title>|g' /usr/share/grafana/public/views/index.html && \
sed -i 's|Loading Grafana|Loading ServerSage|g' /usr/share/grafana/public/views/index.html

find /usr/share/grafana/public/build/ -name *.js -exec sed -i 's|{target:"_blank",id:"version",text:`${e.edition}${s}`,url:t.licenseUrl}||g' {} \;

find /usr/share/grafana/public/build/ -name *.js -exec sed -i 's|{target:"_blank",id:"version",text:`v${e.version} (${e.commit})`,url:i?"https://github.com/grafana/grafana/blob/main/CHANGELOG.md":void 0}||g' {} \;

find /usr/share/grafana/public/build/ -name *.js -exec sed -i 's|{target:"_blank",id:"updateVersion",text:"New version available!",icon:"download-alt",url:"https://grafana.com/grafana/download?utm_source=grafana_footer"}||g' {} \;


# Customize Mega and Help menu in index.html
sed -i "s|\[\[.NavTree\]\],|nav,|g; \
    s|window.grafanaBootData = {| \
    let nav = [[.NavTree]]; \
    const dashboards = nav.find((element) => element.id === 'dashboards/browse'); \
    if (dashboards) { dashboards['children'] = [];} \
    const connections = nav.find((element) => element.id === 'connections'); \
    if (connections) { connections['url'] = '/datasources'; connections['children'].shift(); } \
    const help = nav.find((element) => element.id === 'help'); \
    if (help) { help['subTitle'] = 'ServerSage 12.0'; help['children'] = [];} \
    window.grafanaBootData = {|g" \
    /usr/share/grafana/public/views/index.html && \
    sed -i "s|window.grafanaBootData = {| \
    nav.splice(3, 2); \
    window.grafanaBootData = {|g" \
    /usr/share/grafana/public/views/index.html

# Update JavaScript files for branding and feature toggles
find /usr/share/grafana/public/build/ -name "*.js" -type f \
    -exec sed -i 's|AppTitle="Grafana"|AppTitle="ServerSage"|g' {} \; \
    -exec sed -i 's|LoginTitle="Welcome to Grafana"|LoginTitle="Welcome to ServerSage"|g' {} \; \
    -exec sed -i 's|\[{target:"_blank",id:"documentation".*grafana_footer"}\]|\[\]|g' {} \; \
    -exec sed -i 's|({target:"_blank",id:"license",.*licenseUrl})|()|g' {} \; \
    -exec sed -i 's|({target:"_blank",id:"version",text:..versionString,url:.?"https://github.com/grafana/grafana/blob/main/CHANGELOG.md":void 0})|()|g' {} \; \
    -exec sed -i 's|(0,t.jsx)(d.I,{tooltip:(0,b.t)("dashboard.toolbar.switch-old-dashboard","Switch to old dashboard page"),icon:"apps",onClick:()=>{s.Ny.partial({scenes:!1})}},"view-in-old-dashboard-button")|null|g' {} \; \
    -exec sed -i 's|.push({target:"_blank",id:"version",text:`${..edition}${.}`,url:..licenseUrl,icon:"external-link-alt"})||g' {} \;

# Update feature toggles in configuration
sed -i 's|\[feature_toggles\]|\[feature_toggles\]\npinNavItems = false\nonPremToCloudMigrations = false\ncorrelations = false|g' /usr/share/grafana/conf/defaults.ini


##################################################################
# ALERT : change alrt logo in email
##################################################################
find /usr/share/grafana/public/emails/ -name "*.html" -type f \
    -exec sed -i 's|"https://grafana.com/static/assets/img/logo_new_transparent_light_400x100.png"|"https://github.com/remiges-aniket/serversage/blob/main/remiges-asset/img/Remiges-Logo-ServerSage-Straight-2048x577.png"|g' {} \;
    
find /usr/share/grafana/public/emails/ -name "*.html" -type f \
    -exec sed -i 's|"https://grafana.com/static/assets/img/logo_new_transparent_light_400x100.png"|"https://www.remiges.tech/wp-content/uploads/2024/07/Remiges-Logo-ServerSage-Straight-2048x577.png"|g' {} \;
    
find /usr/share/grafana/public/emails/ -name "*.txt" -type f \
    -exec sed -i 's|"https://grafana.com/static/assets/img/logo_new_transparent_light_400x100.png"|"https://www.remiges.tech/wp-content/uploads/2024/07/Remiges-Logo-ServerSage-Straight-2048x577.png"|g' {} \;  
    
find /usr/share/grafana/public/emails/ -name "*.txt" -type f \
    -exec sed -i 's|"https://grafana.com/static/assets/img/logo_new_transparent_light_400x100.png"|"https://github.com/remiges-aniket/serversage/blob/main/remiges-asset/img/Remiges-Logo-ServerSage-Straight-2048x577.png"|g' {} \;




##################################################################
# CLEANUP - Remove unused data sources and panels
##################################################################
# Remove native data sources
rm -rf \
    /usr/share/grafana/public/app/plugins/datasource/elasticsearch \
    /usr/share/grafana/public/build/elasticsearch* \
    /usr/share/grafana/public/app/plugins/datasource/graphite \
    /usr/share/grafana/public/build/graphite* \
    /usr/share/grafana/public/app/plugins/datasource/opentsdb \
    /usr/share/grafana/public/build/opentsdb* \
    /usr/share/grafana/public/app/plugins/datasource/influxdb \
    /usr/share/grafana/public/build/influxdb* \
    /usr/share/grafana/public/app/plugins/datasource/mssql \
    /usr/share/grafana/public/build/mssql* \
    /usr/share/grafana/public/app/plugins/datasource/mysql \
    /usr/share/grafana/public/build/mysql* \
    /usr/share/grafana/public/app/plugins/datasource/tempo \
    /usr/share/grafana/public/build/tempo* \
    /usr/share/grafana/public/app/plugins/datasource/jaeger \
    /usr/share/grafana/public/build/jaeger* \
    /usr/share/grafana/public/app/plugins/datasource/zipkin \
    /usr/share/grafana/public/build/zipkin* \
    /usr/share/grafana/public/app/plugins/datasource/azuremonitor \
    /usr/share/grafana/public/build/azureMonitor* \
    /usr/share/grafana/public/app/plugins/datasource/cloudwatch \
    /usr/share/grafana/public/build/cloudwatch* \
    /usr/share/grafana/public/app/plugins/datasource/cloud-monitoring \
    /usr/share/grafana/public/build/cloudMonitoring* \
    /usr/share/grafana/public/app/plugins/datasource/parca \
    /usr/share/grafana/public/build/parca* \
    /usr/share/grafana/public/app/plugins/datasource/phlare \
    /usr/share/grafana/public/build/phlare* \
    /usr/share/grafana/public/app/plugins/datasource/grafana-pyroscope-datasource \
    /usr/share/grafana/public/build/pyroscope*

# Remove Cloud and Enterprise categories from JS files
find /usr/share/grafana/public/build/ -name "*.js" -type f \
    -exec sed -i 's|.id==="enterprise"|.id==="notanenterprise"|g' {} \; \
    -exec sed -i 's|.id==="cloud"|.id==="notacloud"|g' {} \;

# Remove native panels
rm -rf \
    /usr/share/grafana/public/app/plugins/panel/news \
    /usr/share/grafana/public/app/plugins/panel/alertlist \
    /usr/share/grafana/public/app/plugins/panel/annolist \
    /usr/share/grafana/public/app/plugins/panel/dashlist \
    /usr/share/grafana/public/app/plugins/panel/geomap \
    /usr/share/grafana/public/app/plugins/panel/table-old \
    /usr/share/grafana/public/app/plugins/panel/traces \
    /usr/share/grafana/public/app/plugins/panel/flamegraph \

