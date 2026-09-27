#!/usr/bin/env python3
"""Summarize complete rounds only. Raw samples remain the source of truth."""
import argparse, pathlib, json, csv, math, statistics, collections, hashlib
p=argparse.ArgumentParser();p.add_argument('results',type=pathlib.Path);p.add_argument('--plot',action='store_true');p.add_argument('--labels',default='baseline,current,current-control');p.add_argument('--sizes',default='1000,10000,100000');p.add_argument('--matrix',type=pathlib.Path);args=p.parse_args()
groups=collections.defaultdict(list)
for path in sorted(args.results.glob('*.jsonl')):
 for line in path.read_text().splitlines():
  row=json.loads(line);groups[(row['label'],row['size'],row['scenario'])].append(row)
scenarios=['announcement_first','announcement_deep','announcement_filtered','question_list','question_detail','answer_list']
matrix=json.loads(args.matrix.read_text()) if args.matrix else {label:list(map(int,args.sizes.split(','))) for label in args.labels.split(',')}
expected={(label,size,scenario) for label,sizes in matrix.items() for size in sizes
          for scenario in (scenarios[:4] if label=='current-control' else scenarios)}
missing=expected-set(groups);unexpected=set(groups)-expected
if missing or unexpected:
 raise SystemExit(f'Incomplete comparison matrix: missing={sorted(missing)}, unexpected={sorted(unexpected)}')
summary=[]
for (label,size,scenario),rows in sorted(groups.items()):
 assert len(rows)==45,(label,size,scenario,len(rows))
 for round in [1,2,3]:assert {r['sample'] for r in rows if r['round']==round}==set(range(15)),(label,size,scenario,round)
 times=sorted(r['ms'] for r in rows)
 medians=[statistics.median(r['ms'] for r in rows if r['round']==i) for i in [1,2,3]]
 record={'label':label,'size':size,'scenario':scenario,'samples':len(rows),'p50_ms':statistics.median(times),'p95_ms':times[math.ceil(.95*len(times))-1],
         'sequential_rps':len(times)*1000/sum(times),'round_median_min':min(medians),'round_median_max':max(medians)}
 for field in ['sql','entities','rows','bytes']:
  record[field+'_min']=min(r[field] for r in rows);record[field+'_max']=max(r[field] for r in rows)
 summary.append(record)
assert summary,'No samples'
output=args.results.parent
(output/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
with (output/'summary.csv').open('w',newline='') as f:
 writer=csv.DictWriter(f,fieldnames=list(summary[0]),lineterminator='\n');writer.writeheader();writer.writerows(summary)
checksums={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(args.results.glob('*.jsonl'))}
(output/'raw-sha256.json').write_text(json.dumps(checksums,indent=2)+'\n')
lines=['# 반복 측정 결과','', '단위: 서버 내부 HTTP 처리 ms. 각 행은 3개 JVM × 15개 표본이며 워밍업 제외. p95는 nearest rank. RPS는 순차 요청 처리량으로 최대 동시 용량을 뜻하지 않는다.', '', '| 비교군 | 데이터 규모 | 경로 | p50 ms | p95 ms | 순차 RPS | SQL | 로딩 엔티티 | 반환 건수 |', '|---|---:|---|---:|---:|---:|---:|---:|---:|']
for r in summary:
 lines.append(f"| {r['label']} | {r['size']:,} | {r['scenario']} | {r['p50_ms']:.3f} | {r['p95_ms']:.3f} | {r['sequential_rps']:.1f} | {r['sql_min']}–{r['sql_max']} | {r['entities_min']}–{r['entities_max']} | {r['rows_min']}–{r['rows_max']} |")
(output/'results.md').write_text('\n'.join(lines)+'\n')
if args.plot:
 import matplotlib
 matplotlib.use('Agg')
 import matplotlib.pyplot as plt
 labels=['baseline','current-control','current','current-optimized']
 colors=['#9ca3af','#ef9c38','#635bff','#009e73']
 fig,axes=plt.subplots(1,2,figsize=(11,4.7))
 for ax,scenario,title in zip(axes,['announcement_first','answer_list'],['Announcement: first 20 items','Answers: 50 distinct authors']):
  for label,color in zip(labels,colors):
   series=sorted([r for r in summary if r['label']==label and r['scenario']==scenario],key=lambda r:r['size'])
   if series:ax.plot([r['size'] for r in series],[r['p50_ms'] for r in series],marker='o',label=label,color=color)
  ax.set_xscale('log');ax.set_yscale('log');ax.set_xlabel('Announcements / questions in DB');ax.set_ylabel('p50 server processing (ms, log scale)');ax.set_title(title);ax.grid(True,alpha=.2);ax.legend(fontsize=8)
 fig.suptitle('DEMP query benchmark — M2 / 2 GiB JVM / warm local H2',fontsize=12)
 fig.tight_layout();fig.savefig(output/'comparison.png',dpi=180);fig.savefig(output/'comparison.svg')
print('Complete groups:',len(summary),'samples:',sum(r['samples'] for r in summary))
