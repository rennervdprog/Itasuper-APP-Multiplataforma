"""CI guard: every component declared in the merged AndroidManifest must exist in the DEX.

Catches the v13 crash (2026-10-26): the manifest pointed the launcher at
app.itasuper.cliente.MainActivity, a class that does not exist (the real class
is com.example.itasuper.MainActivity). The manifest merger does not validate
class existence, so a green CI passed a broken APK.

Usage: python3 scripts/check_manifest_dex.py <apk-path>
Exit 0 = every component class found in the DEX; exit 1 = missing class(es).
No Android SDK needed — parses the binary manifest (AXML) and the DEX bytes
with the standard library only.
"""
import struct
import sys
import zipfile

ANDROID_NS = 'http://schemas.android.com/apk/res/android'
# Elements whose android:name is a real class that must exist in the DEX.
# activity-alias is excluded: its android:name is an alias, not a class
# (its targetActivity is checked instead).
CHECKED_TAGS = {'activity', 'service', 'receiver', 'provider'}


def u16(b, o):
    return struct.unpack_from('<H', b, o)[0]


def u32(b, o):
    return struct.unpack_from('<I', b, o)[0]


def i32(b, o):
    return struct.unpack_from('<i', b, o)[0]


def parse_string_pool(b, off):
    header_size = u16(b, off + 2)
    string_count = u32(b, off + 8)
    flags = u32(b, off + 16)
    strings_start = u32(b, off + 20)
    is_utf8 = bool(flags & 0x100)
    strings = []
    for i in range(string_count):
        str_off = off + strings_start + u32(b, off + header_size + i * 4)
        if is_utf8:
            p = str_off

            def read_len(p):
                b1 = b[p]
                if b1 & 0x80:
                    return ((b1 & 0x7F) << 8) | b[p + 1], p + 2
                return b1, p + 1

            _, p = read_len(p)
            blen, p = read_len(p)
            strings.append(b[p:p + blen].decode('utf-8', 'replace'))
        else:
            p = str_off
            wlen = u16(b, p)
            p += 2
            if wlen & 0x8000:
                wlen = ((wlen & 0x7FFF) << 16) | u16(b, p)
                p += 2
            strings.append(b[p:p + wlen * 2].decode('utf-16-le', 'replace'))
    return strings


def typed_value(b, off, strings):
    dtype = b[off + 3]
    data = u32(b, off + 4)
    if dtype == 0x03:  # string
        return strings[data] if 0 <= data < len(strings) else f'<str#{data}>'
    if dtype == 0x12:  # boolean
        return 'true' if data else 'false'
    if dtype == 0x10:  # int dec
        return str(i32(b, off + 4))
    if dtype == 0x11:  # int hex
        return f'0x{data:08x}'
    if dtype == 0x01:  # reference
        return f'@0x{data:08x}'
    return f'(type {dtype} data 0x{data:08x})'


def parse_manifest(xml_bytes):
    """Return (package, [(tag, android:name), ...]) from binary AndroidManifest.xml."""
    assert u16(xml_bytes, 0) == 0x0003, 'not binary xml'
    strings = []
    package = None
    components = []
    off = 8
    size = len(xml_bytes)
    while off < size:
        ctype = u16(xml_bytes, off)
        csize = u32(xml_bytes, off + 4)
        if ctype == 0x0001:  # string pool
            strings = parse_string_pool(xml_bytes, off)
        elif ctype == 0x0102:  # start element
            name_idx = i32(xml_bytes, off + 20)
            attr_start = u16(xml_bytes, off + 24)
            attr_size = u16(xml_bytes, off + 26)
            attr_count = u16(xml_bytes, off + 28)
            tag = strings[name_idx] if 0 <= name_idx < len(strings) else '?'
            attrs = {}
            aoff = off + 16 + attr_start
            for i in range(attr_count):
                a_ns = i32(xml_bytes, aoff + i * attr_size + 0)
                a_name = i32(xml_bytes, aoff + i * attr_size + 4)
                a_raw = i32(xml_bytes, aoff + i * attr_size + 8)
                aname = strings[a_name] if 0 <= a_name < len(strings) else '?'
                ans = strings[a_ns] if 0 <= a_ns < len(strings) else ''
                val = typed_value(xml_bytes, aoff + i * attr_size + 12, strings)
                if a_raw != -1 and val.startswith('@'):
                    raw = strings[a_raw] if 0 <= a_raw < len(strings) else ''
                    val = raw if raw else val
                attrs[(ans, aname)] = val
            if tag == 'manifest':
                package = attrs.get(('', 'package'))
            if tag == 'application':
                name = attrs.get((ANDROID_NS, 'name'))
                if name:
                    components.append(('application', name))
            if tag in CHECKED_TAGS:
                name = attrs.get((ANDROID_NS, 'name'))
                if name:
                    components.append((tag, name))
            if tag == 'activity-alias':
                target = attrs.get((ANDROID_NS, 'targetActivity'))
                if target:
                    components.append(('activity-alias targetActivity', target))
        off += csize if csize > 0 else 8
    return package, components


def resolve(package, name):
    if name.startswith('.'):
        return package + name
    if '.' not in name:
        return package + '.' + name
    return name


def main():
    if len(sys.argv) != 2:
        print('usage: check_manifest_dex.py <apk-path>', file=sys.stderr)
        return 2
    apk_path = sys.argv[1]
    with zipfile.ZipFile(apk_path) as z:
        manifest = z.read('AndroidManifest.xml')
        dex = b''.join(
            z.read(n) for n in z.namelist()
            if n.startswith('classes') and n.endswith('.dex')
        )
    package, components = parse_manifest(manifest)
    if not package:
        print('ERROR: could not read package from manifest', file=sys.stderr)
        return 2
    print(f'package: {package}')
    print(f'components checked: {len(components)}')
    missing = []
    for tag, name in components:
        resolved = resolve(package, name)
        path = resolved.replace('.', '/').encode()
        found = path in dex
        print(f"  [{'OK ' if found else 'MISS'}] {tag}: {resolved}")
        if not found:
            missing.append((tag, name, resolved))
    if missing:
        print('ERROR: manifest declares components missing from the DEX:', file=sys.stderr)
        for tag, name, resolved in missing:
            print(f'  {tag}: {name} -> {resolved}', file=sys.stderr)
        return 1
    print('OK: all manifest components exist in the DEX')
    return 0


if __name__ == '__main__':
    sys.exit(main())
