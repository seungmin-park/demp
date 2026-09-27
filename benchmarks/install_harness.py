#!/usr/bin/env python3
"""Install a benchmark-only source set; optionally restore the two known legacy compile errors."""
import argparse, pathlib, subprocess
parser=argparse.ArgumentParser();parser.add_argument('checkout',type=pathlib.Path);parser.add_argument('--legacy',action='store_true');args=parser.parse_args()
root=args.checkout.resolve();here=pathlib.Path(__file__).resolve().parent
if args.legacy:
    head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()
    if head!='a8b057b2ffabc34d17f836e5a8c2b52b6e028b94':raise SystemExit('Legacy checkout must be the documented a8b057b baseline')
    for path in ['src/main/java/com/inhatc/demp/InitDb.java','src/main/java/com/inhatc/demp/controller/AnnouncementController.java']:
        (root/path).write_bytes(subprocess.check_output(['git','show','735e768:'+path],cwd=root))
s=(here/'QueryBenchmarkTest.java.in').read_text()
s=s.replace('@AUTO_CONFIGURE_PACKAGE@','org.springframework.boot.test.autoconfigure.web.servlet' if args.legacy else 'org.springframework.boot.webmvc.test.autoconfigure')
s=s.replace('@PERSISTENCE@','javax' if args.legacy else 'jakarta').replace('@JACKSON@','com.fasterxml.jackson' if args.legacy else 'tools.jackson')
s=s.replace('@LEGACY@','true' if args.legacy else 'false')
s=s.replace('@CONTROL_IMPORT@','' if args.legacy else '@org.springframework.context.annotation.Import(LegacyQueryControl.class)')
out=root/'build/benchmark-src/bench';out.mkdir(parents=True,exist_ok=True);(out/'QueryBenchmarkTest.java').write_text(s)
if not args.legacy:(out/'LegacyQueryControl.java').write_text((here/'LegacyQueryControl.java.in').read_text())
print('Installed benchmark source:',out)
