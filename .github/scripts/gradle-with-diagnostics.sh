#!/usr/bin/env bash
set -uo pipefail
log_file="${RUNNER_TEMP:-/tmp}/orion-gradle.log"
./gradlew --no-daemon "$@" 2>&1 | tee "$log_file"
status=${PIPESTATUS[0]}
if (( status != 0 )); then
  # Surface the tail as a check annotation, while retaining the full runner log.
  # Do not pass credentials or private input to this CI invocation.
  tail_text=$(tail -n 80 "$log_file")
  tail_text=${tail_text//'%'/'%25'}
  tail_text=${tail_text//$'\r'/'%0D'}
  tail_text=${tail_text//$'\n'/'%0A'}
  echo "::error title=Gradle failure::$tail_text"
  python3 - <<'PYTEST'
import glob, xml.etree.ElementTree as ET
for path in glob.glob('app/build/outputs/androidTest-results/**/*.xml', recursive=True):
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    for case in root.iter('testcase'):
        for failure in case.findall('failure'):
            text = f"{case.get('classname')}.{case.get('name')}: {failure.text or failure.get('message', '')}"
            text = text[:8000].replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
            print(f'::error title=Instrumentation failure::{text}')
PYTEST
fi
exit "$status"
