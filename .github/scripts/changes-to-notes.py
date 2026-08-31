#!/usr/bin/env python3
# Copyright 2026 Norconex Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Render one CHANGES.xml release block as markdown, for GitHub Release notes.

CHANGES.xml (Maven Changes Plugin format) is the source of truth for the 2.x
line, because opensource.norconex.com renders that XML directly. Rather than
maintain a second, hand-written changelog that could drift from it, the release
workflow derives the GitHub Release body from it with this script.

Parsed with ElementTree rather than grep/sed so that <action> text spanning
several lines, and any XML entities in it, survive intact.

Usage:  changes-to-notes.py <version> [changes-xml-path]
Writes markdown to stdout. Exits non-zero if that version is not present.
"""

import sys
import xml.etree.ElementTree as ET

# The <action> "type" values used across this project's history, mapped to the
# headings they appear under. Order here is the order they are rendered in.
HEADINGS = [
    ("add", "Added"),
    ("update", "Changed"),
    ("fix", "Fixed"),
    ("remove", "Removed"),
]


def render(version, path="CHANGES.xml"):
    root = ET.parse(path).getroot()

    # Matched by suffix rather than a namespace map: the file declares the
    # Maven changes namespace, but tolerating a missing declaration costs
    # nothing and avoids a silent "no releases found" if it is ever dropped.
    release = next(
        (
            el
            for el in root.iter()
            if el.tag.endswith("release") and el.get("version") == version
        ),
        None,
    )
    if release is None:
        sys.exit(
            'ERROR: no <release version="%s"> found in %s' % (version, path)
        )

    buckets = {}
    for action in release:
        if not action.tag.endswith("action"):
            continue
        # Collapse the whitespace that XML indentation introduces, so a
        # multi-line <action> becomes one clean bullet.
        text = " ".join((action.text or "").split())
        if text:
            buckets.setdefault(action.get("type", "update"), []).append(text)

    lines = []
    description = release.get("description")
    if description:
        lines += [description, ""]

    for key, heading in HEADINGS:
        if buckets.get(key):
            lines.append("### %s" % heading)
            lines += ["- %s" % t for t in buckets[key]]
            lines.append("")

    # Any type not listed in HEADINGS is still rendered rather than silently
    # dropped, so an unexpected value cannot quietly lose release notes.
    known = dict(HEADINGS)
    for key, items in buckets.items():
        if key not in known:
            lines.append("### %s" % key.capitalize())
            lines += ["- %s" % t for t in items]
            lines.append("")

    if not buckets:
        lines.append("See CHANGES.xml for details.")

    return "\n".join(lines).rstrip() + "\n"


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit("Usage: changes-to-notes.py <version> [changes-xml-path]")
    xml_path = sys.argv[2] if len(sys.argv) > 2 else "CHANGES.xml"
    sys.stdout.write(render(sys.argv[1], xml_path))
