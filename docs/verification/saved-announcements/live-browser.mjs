import { chromium } from '/Users/seungmin/Desktop/repo/archive/demp/.worktrees/saved-announcements/frontend/node_modules/playwright/index.mjs';
import assert from 'node:assert/strict';
import { writeFileSync } from 'node:fs';
const root='/Users/seungmin/Desktop/repo/archive/demp/.worktrees/saved-announcements/backend/docs/verification/saved-announcements';
const browser=await chromium.launch({headless:true});
const context=await browser.newContext({viewport:{width:390,height:844}});
const page=await context.newPage();
const warnings=[],checks=[];
page.on('console',message=>{if(message.text().includes('[Vue warn]'))warnings.push(message.text());});
const check=(name)=>{checks.push(name);console.log('PASS:',name);};
const base='http://127.0.0.1:15150';
const waitPath=path=>page.waitForURL(url=>url.pathname===path);
async function login(){
 await page.getByLabel('아이디',{exact:true}).fill('local-member');await page.getByLabel('비밀번호',{exact:true}).fill('password');
 await page.getByRole('button',{name:'로그인',exact:true}).last().click();
}
async function token(){return page.evaluate(()=>JSON.parse(localStorage.getItem('vuex')).Login.token);}
async function api(path){const response=await page.request.get(base+path,{headers:{'X-AUTH-TOKEN':await token()}});assert.equal(response.status(),200);return response.json();}
try {
 await page.goto(base+'/?q='+encodeURIComponent('관심 공고 검증'));
 const card=page.getByRole('link',{name:'관심 공고 검증 · 정확한 마감',exact:true});
 await card.getByRole('button',{name:'관심 공고 저장',exact:true}).click();await waitPath('/login');
 let redirect=new URL(new URL(page.url()).searchParams.get('redirect'),base);
 assert.equal(redirect.searchParams.get('saveAnnouncement'),'501');assert.equal(redirect.searchParams.get('q'),'관심 공고 검증');
 await login();await waitPath('/');await card.getByRole('button',{name:'관심 공고 해제',exact:true}).waitFor();
 await page.reload();await card.getByRole('button',{name:'관심 공고 해제',exact:true}).waitFor();
 assert.deepEqual((await api('/api/member/me/bookmarks/status?announcementIds=501,502,503')).savedIds,[501]);
 check('anonymous save -> real login -> filtered list -> saved state survives reload and fresh HTTP');
 for(const [id,name] of [[502,'채용 시 마감'],[503,'상시 모집']]){
  const item=page.getByRole('link',{name:'관심 공고 검증 · '+name,exact:true});await item.getByRole('button',{name:'관심 공고 저장',exact:true}).click();await item.getByRole('button',{name:'관심 공고 해제',exact:true}).waitFor();
  const detail=await api('/api/announce/detail/'+id+'?recordView=false');assert.equal(detail.deadLineDate,null);assert.equal(detail.deadlineMode,id===502?'UNTIL_FILLED':'ALWAYS_OPEN');
 }
 await page.getByRole('link',{name:'관심 공고',exact:true}).click();await waitPath('/saved');await page.getByRole('heading',{name:'관심 공고',exact:true}).waitFor();
 let saved=await api('/api/member/me/bookmarks');assert.deepEqual(saved.content.map(entry=>entry.announcementId),[503,502,501]);
 await page.screenshot({path:root+'/assets/saved-mobile.png',fullPage:true});
 check('saved list shows persisted newest-first entries and explicit deadline modes at 390px');
 await page.getByRole('link',{name:'관심 공고 검증 · 채용 시 마감',exact:true}).click();await waitPath('/detail/502');
 await page.getByText('지원기간 : 2026.01.01 09:00 ~ 채용 시 마감').waitFor();assert.equal(await page.getByLabel('현재 모집 상태').textContent(),'모집 중');
 await page.goto(base+'/admin/announcements/502');await page.getByLabel('마감 방식').waitFor();
 assert.equal(await page.getByLabel('마감 방식').inputValue(),'UNTIL_FILLED');
 await page.getByLabel('모집 종료 (수동 마감)').check();await page.getByRole('button',{name:'변경 저장',exact:true}).click();await waitPath('/admin/announcements');
 await page.goto(base+'/detail/502');await page.getByText('모집 마감',{exact:true}).waitFor();
 assert.equal((await api('/api/announce/detail/502?recordView=false')).recruitmentClosed,true);
 assert.equal((await api('/api/member/me/bookmarks')).content.length,3);
 check('until-filled posting has no invented deadline and manual close persists while bookmark remains');
 await page.goto(base+'/admin/announcements/503');await page.getByLabel('게시 상태').selectOption('HIDDEN');await page.getByRole('button',{name:'변경 저장',exact:true}).click();await waitPath('/admin/announcements');
 await page.goto(base+'/saved');await page.getByText('이용할 수 없는 공고',{exact:true}).waitFor();
 saved=await api('/api/member/me/bookmarks');const hidden=saved.content.find(entry=>entry.announcementId===503);assert.equal(hidden.available,false);assert.equal(hidden.announcement,null);
 await page.locator('.saved-card').filter({has:page.getByText('이용할 수 없는 공고',{exact:true})}).getByRole('button',{name:'관심 공고 해제',exact:true}).click();
 await page.getByText('이용할 수 없는 공고',{exact:true}).waitFor({state:'hidden'});
 check('hidden posting exposes no summary and can be removed from saved list');
 const geometry=await page.evaluate(()=>({document:document.documentElement.scrollWidth,viewport:innerWidth}));assert.ok(geometry.document<=geometry.viewport);assert.deepEqual(warnings,[]);
 check('mobile layout contains controls and no Vue warnings');
 await page.screenshot({path:root+'/assets/saved-final.png',fullPage:true});
 writeFileSync(root+'/live-results.json',JSON.stringify({status:'passed',headless:true,database:'owned in-memory Spring/H2',checks,warnings},null,2)+'\n');
} catch(error){await page.screenshot({path:root+'/assets/live-failure.png',fullPage:true});writeFileSync(root+'/live-results.json',JSON.stringify({status:'failed',headless:true,checks,error:String(error),warnings},null,2)+'\n');throw error;}
finally {await browser.close();}
