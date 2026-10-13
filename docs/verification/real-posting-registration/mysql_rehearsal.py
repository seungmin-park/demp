import importlib.util,json,hashlib,os,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
import argparse
parser=argparse.ArgumentParser()
parser.add_argument('--evidence',type=Path,required=True)
EVIDENCE=parser.parse_args().evidence;EVIDENCE.mkdir(parents=True,exist_ok=True)
sys.argv=[sys.argv[0],'--evidence',str(EVIDENCE)]
spec=importlib.util.spec_from_file_location('fixture',ROOT/'docs/verification/separate-employment-type/mysql_rehearsal.py')
fixture=importlib.util.module_from_spec(spec);spec.loader.exec_module(fixture)
db=fixture.legacy
sys.path.insert(0,str(ROOT/'scripts'))
from verify_announcement_contract import run
status='failed'
try:
    fixture.prepare_container()
    fixture.migrate(db.DATABASE,True)
    db.sql((ROOT/'src/main/resources/db/manual/announcement-view-count.sql').read_text())
    before=db.sql('SELECT id,title,name,payment,started_date,dead_line_date FROM announcement ORDER BY id;').stdout
    db.start_server(db.DATABASE,'missing-new-schema',expected_failure='missing column')
    for name in ['announcement-source-identity.sql','announcement-recruitment-facts.sql']:
        db.sql((ROOT/'src/main/resources/db/manual'/name).read_text())
    db.check(db.sql('SELECT id,title,name,payment,started_date,dead_line_date FROM announcement ORDER BY id;').stdout==before,'new migrations preserve legacy row values')
    db.check(db.sql('SELECT source_identity_key,salary_period,work_period,conversion_note FROM announcement WHERE id=900;').stdout.strip()=='NULL\tNULL\tNULL\tNULL','new facts stay unknown on existing records')
    db.sql((ROOT/'src/main/resources/local/data.sql').read_text())
    db.sql("INSERT INTO member_roles(member_member_id,roles) VALUES(-1,'ROLE_ADMIN');")
    base,process=db.start_server(db.DATABASE,'new-schema-contract')
    result=run(base,'r'*32)
    admin=db.request(base,'/api/member/login','POST',form={'username':'local-member','password':'password'})['jwt']
    legacy=db.request(base,'/api/announce/detail/900?recordView=false',token=admin)
    db.check(legacy['salaryPeriod']=='YEAR' and legacy['payment']==4500,'legacy salary keeps annual interpretation')
    db.check(legacy['roles']==[] and legacy['referenceUrls']==[],'legacy relations remain empty')
    db.check(db.sql('SELECT COUNT(*) FROM announcement;').stdout.strip()=='2','HTTP contract fixtures cleaned up leaving legacy and seed records')
    (EVIDENCE/'http-contract.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
    status='passed'
finally:
    for process,log in db.SERVERS:
        db.stop_server(process);log.close()
    if status=='passed': db.sql('DROP DATABASE `'+db.DATABASE+'`;',admin=True)
    if fixture.STARTED_CONTAINER: subprocess.run(['docker','stop',db.CONTAINER],check=True)
    (EVIDENCE/'result.json').write_text(json.dumps({'status':status,'checks':db.CHECKS,'productionDatabaseTouched':False,'database':db.DATABASE,'databaseRemoved':status=='passed','jarSha256':hashlib.sha256((ROOT/'build/libs/demp-0.0.1-SNAPSHOT.jar').read_bytes()).hexdigest()},ensure_ascii=False,indent=2))
assert status=='passed'
