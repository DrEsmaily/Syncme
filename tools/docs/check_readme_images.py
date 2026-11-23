# Copyright (c) 2026 DrEsmaily (SyncMe Project).
# SPDX-License-Identifier: AGPL-3.0-or-later

"""Check that local HTML image sources in the repository README exist."""

from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit


class ReadmeImages(HTMLParser):
    def handle_starttag(self, tag, attrs):
        if tag != "img":
            return
        source = dict(attrs).get("src")
        if not source:
            raise ValueError("README image is missing its src attribute")
        url = urlsplit(source)
        if url.netloc or url.scheme:
            return
        path = Path(unquote(url.path))
        if not path.is_file():
            raise FileNotFoundError(f"README image does not exist: {source}")


if __name__ == "__main__":
    parser = ReadmeImages()
    parser.feed(Path("README.md").read_text(encoding="utf-8"))
    parser.close()
