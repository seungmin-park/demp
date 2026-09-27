#!/usr/bin/env python3
"""Run sequential independent JVMs; never benchmark two versions concurrently."""
import argparse, pathlib, subprocess, os
p=argparse.ArgumentParser();p.add_argument('--baseline',type=pathlib.Path,required=True);p.add_argument('--current',type=pathlib.Path,required=True);p.add_argument('--java11',required=True);p.add_argument('--java25',required=True);p.add_argument('--output',type=pathlib.Path,required=True);p.add_argument('--labels',default='baseline,current,current-control');p.add_argument('--heap',default='2048m');p.add_argument('--sizes',default='1000,10000,100000');args=p.parse_args()
here=pathlib.Path(__file__).resolve().parent;out=args.output.resolve();out.mkdir(parents=True,exist_ok=True)
labels=args.labels.split(',')
for label in labels:
 if (out/(label+'.jsonl')).exists():raise SystemExit('Refuse appending to existing measurement '+label)
for round in range(1,4):
 order=labels[round-1:]+labels[:round-1]
 for label in order:
  legacy=label=='baseline';checkout=(args.baseline if legacy else args.current).resolve();env=os.environ.copy();env.update(JAVA_HOME=args.java11 if legacy else args.java25,AWS_EC2_METADATA_DISABLED='true')
  cmd=['./gradlew','-I',str(here/'benchmark.init.gradle'),'queryBenchmark','-PbenchLabel='+label,'-PbenchSizes='+args.sizes,'-PbenchHeap='+args.heap,'-PbenchSamples=15','-PbenchWarmups=5','-PbenchRound='+str(round),'-PbenchOutput='+str(out/(label+'.jsonl')),'--console=plain']
  if subprocess.run(['pgrep','-f','Gradle Test Executor'],stdout=subprocess.DEVNULL).returncode == 0:
   raise SystemExit('Another Gradle test JVM is running; refuse contaminated measurements')
  print('START',label,'round',round,flush=True)
  with (out/(label+'-round-'+str(round)+'.log')).open('w') as log:
   result=subprocess.run(cmd,cwd=checkout,env=env,stdout=log,stderr=subprocess.STDOUT)
  print('DONE',label,'round',round,'exit',result.returncode,flush=True)
  if result.returncode:raise SystemExit(result.returncode)
