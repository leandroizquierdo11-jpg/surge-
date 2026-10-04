#!/usr/bin/env bash
# Aplica los parches de surge sobre un checkout de mihonapp/mihon.
# Uso: scripts/aplicar.sh <ruta-al-checkout-de-mihon>
set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
MIHON_DIR="$1"
cd "$MIHON_DIR"

git config user.name "leandroizquierdo11-jpg"
git config user.email "336344874+leandroizquierdo11-jpg@users.noreply.github.com"

for patch in "$REPO_DIR"/patches/*.patch; do
    echo "Aplicando $(basename "$patch")"
    git am --3way "$patch"
done

# Otro nombre de paquete y de app (Surge): se instala junto a la Mihon oficial (que está
# firmada con otra clave) en lugar de chocar con ella.
gradle_file=app/build.gradle.kts
grep -q 'applicationId = "app.mihon"' "$gradle_file" || { echo "No encuentro applicationId en $gradle_file" >&2; exit 1; }
sed -i 's/applicationId = "app.mihon"/applicationId = "app.mihon.surge"/' "$gradle_file"

strings_file=i18n/src/commonMain/moko-resources/base/strings.xml
grep -q '<string name="app_name" translatable="false">Mihon</string>' "$strings_file" || { echo "No encuentro app_name en $strings_file" >&2; exit 1; }
sed -i 's|<string name="app_name" translatable="false">Mihon</string>|<string name="app_name" translatable="false">Surge</string>|' "$strings_file"

# Logo de Surge: icono, icono temático, pantalla de carga y notificaciones.
for icon in "$REPO_DIR"/recursos/drawable/*.xml; do
    target="app/src/main/res/drawable/$(basename "$icon")"
    [ -f "$target" ] || { echo "No encuentro $target" >&2; exit 1; }
    cp "$icon" "$target"
done

colors_file=app/src/main/res/values/colors.xml
grep -q '<color name="splash">' "$colors_file" || { echo "No encuentro el color splash en $colors_file" >&2; exit 1; }
sed -i 's|<color name="splash">[^<]*</color>|<color name="splash">#5B2BD6</color>|' "$colors_file"

echo "Parches aplicados."
