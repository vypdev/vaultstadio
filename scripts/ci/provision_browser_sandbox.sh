#!/usr/bin/env bash
# Operator prerequisite for disposable hosted CI only; never called by the composite.
set -euo pipefail

[[ $(id -u) == 0 ]] || { echo 'Sandbox provisioning requires the CI operator.' >&2; exit 78; }
target=/opt/vaultstadio-verification-sandbox
[[ ! -e "$target" && ! -L "$target" ]] || { echo 'Refusing to replace an existing sandbox namespace.' >&2; exit 78; }
package_root=$(mktemp -d /var/tmp/vaultstadio-sandbox-package.XXXXXX)
trap 'rm -rf -- "$package_root"' EXIT
export GNUPGHOME="$package_root/gnupg"
mkdir -m 0700 "$GNUPGHOME"
fingerprint=EB4C1BFD4F042F6DDDCCEC917721F63BD38B4796
curl --fail --show-error --silent --location --proto '=https' --proto-redir '=https' \
    --tlsv1.2 --connect-timeout 30 --max-time 120 \
    https://dl.google.com/linux/linux_signing_key.pub --output "$package_root/google.asc"
gpg --batch --with-colons --show-keys "$package_root/google.asc" > "$package_root/key-metadata"
actual_primary=$(awk -F: '$1 == "pub" { primary = 1; next } primary && $1 == "fpr" { print $10; primary = 0 }' "$package_root/key-metadata")
[[ "$actual_primary" == "$fingerprint" ]] || { echo 'Unexpected Google primary signing key.' >&2; exit 78; }
install -d -o root -g root -m 0755 "$target"
gpg --batch --dearmor --output "$target/google.gpg" "$package_root/google.asc"
test -s "$target/google.gpg"
chmod 0644 "$target/google.gpg"
gpg --batch --with-colons --show-keys "$target/google.gpg"
printf 'deb [arch=amd64 signed-by=%s/google.gpg] https://dl.google.com/linux/chrome/deb/ stable main\n' "$target" > "$target/google.list"
mkdir "$target/lists"
apt_options=(-o "Dir::Etc::sourcelist=$target/google.list" -o 'Dir::Etc::sourceparts=-'
    -o "Dir::State::lists=$target/lists" -o 'APT::Update::Error-Mode=any')
apt-get "${apt_options[@]}" update
cd "$package_root"
apt-get "${apt_options[@]}" download google-chrome-stable
shopt -s nullglob
packages=(google-chrome-stable_*_amd64.deb)
[[ ${#packages[@]} == 1 ]]
dpkg-deb --field "${packages[0]}" Package Version
dpkg-deb --extract "${packages[0]}" "$package_root/extracted"
install -o root -g root -m 4755 \
    "$package_root/extracted/opt/google/chrome/chrome-sandbox" "$target/chrome-sandbox"
stat --format='Sandbox owner=%u group=%g mode=%a' "$target/chrome-sandbox"
sha256sum "$target/chrome-sandbox"
