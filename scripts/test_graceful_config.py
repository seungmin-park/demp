"""운영 profile이 기존 요청에 종료 시간을 명시해 제공하는지 확인한다."""
from pathlib import Path
import unittest


def property_value(source, segments):
    # 현재 설정은 단순 YAML mapping이다. 부모 경로를 같이 읽어 다른 profile 값을 혼동하지 않는다.
    values = {}
    parents = []
    for raw in source.splitlines():
        text = raw.split('#', 1)[0].rstrip()
        if not text.strip() or ':' not in text:
            continue
        indent = len(text) - len(text.lstrip())
        while parents and parents[-1][0] >= indent:
            parents.pop()
        name, value = text.strip().split(':', 1)
        key = tuple(item[1] for item in parents) + (name,)
        if value.strip():
            values[key] = value.strip().strip('"\'')
        else:
            parents.append((indent, name))
    return values.get(tuple(segments))


class GracefulConfigTests(unittest.TestCase):
    def test_filesystem_profile_gives_existing_requests_thirty_seconds(self):
        source = (Path(__file__).parents[1] / 'src/main/resources/application-filesystem.yml').read_text()
        self.assertEqual(property_value(source, ('server', 'shutdown')), 'graceful')
        self.assertEqual(property_value(source, ('spring', 'lifecycle', 'timeout-per-shutdown-phase')), '30s')


if __name__ == '__main__':
    unittest.main()
