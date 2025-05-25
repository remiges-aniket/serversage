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
