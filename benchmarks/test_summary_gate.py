import json, pathlib, subprocess, tempfile
script=pathlib.Path(__file__).resolve().with_name('summarize.py')
with tempfile.TemporaryDirectory() as temp:
 raw=pathlib.Path(temp)/'raw';raw.mkdir()
 scenarios=['announcement_first','announcement_deep','announcement_filtered','question_list','question_detail','answer_list']
 def write(label):
  rows=[dict(label=label,size=size,scenario=s,round=r,sample=i,ms=1,sql=1,entities=1,rows=20,bytes=100) for size in [1000,10000,100000] for s in (scenarios[:4] if label=='current-control' else scenarios) for r in [1,2,3] for i in range(15)]
  (raw/(label+'.jsonl')).write_text(''.join(json.dumps(row)+'\n' for row in rows))
 write('baseline')
 incomplete=subprocess.run(['python3',str(script),str(raw)],capture_output=True,text=True)
 assert incomplete.returncode != 0,'Missing current/control groups were incorrectly accepted'
 for label in ['current','current-control']:write(label)
 complete=subprocess.run(['python3',str(script),str(raw)],capture_output=True,text=True)
 assert complete.returncode == 0,complete.stderr
 assert len(json.loads((raw.parent/'summary.json').read_text()))==48
 print('PASS: incomplete matrix refused; complete 48 groups accepted')
