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
