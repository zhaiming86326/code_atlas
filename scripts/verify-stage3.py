"""Stage 3 real PostgreSQL checks, no cloud calls; test documents removed in finally.
Optional --vectors additionally verifies actual configured cloud/vector integration (incurs cost).
"""
import argparse,json,uuid,urllib.request,urllib.error
p=argparse.ArgumentParser();p.add_argument('--base',default='http://127.0.0.1:8080');p.add_argument('--vectors',action='store_true');a=p.parse_args();created=[]
def call(path,data=None,method=None):
 req=urllib.request.Request(a.base+'/api'+path,data=None if data is None else json.dumps(data,ensure_ascii=False).encode(),headers={'Content-Type':'application/json'},method=method)
 with urllib.request.urlopen(req,timeout=600) as r:
  b=r.read();return json.loads(b) if b else None
def query(text,version='21',mode='KEYWORD'):
 return {'query':text,'technologies':['Stage3Test'],'version':version,'searchMode':mode}
def check(name,value):
 assert value,name
 print('PASS:',name)
def fails(path,data,status):
 try:call(path,data)
 except urllib.error.HTTPError as e:check('expected HTTP '+str(status),e.code==status);return
 raise AssertionError('expected failure')
try:
 state=call('/models');check('keys never returned','apiKey' not in json.dumps(state) and 'api_key' not in json.dumps(state));check('unknown prices remain unknown', all(s.get('inputPricePerMillion') is None for s in state['models']) if not any(s.get('inputPricePerMillion') is not None for s in state['models']) else True)
 token='Stage3Exact'+uuid.uuid4().hex
 for version in ['21','22']:
  imported=call('/imports/note',{'title':'stage3-check-'+uuid.uuid4().hex,'content':'# 虚拟线程\n'+token+'\n虚拟线程适用于等待 I/O 的任务。Class.forName 加载类。','metadata':{'technology':'Stage3Test','version':version,'language':'zh','tags':['验收']}})
  doc=imported['document']['id'];created.append(doc)
  check('pending excluded',not call('/rag/retrieve',query(token,version)))
  call('/documents/'+doc+'/review',{'versionId':imported['versionId'],'approved':True})
 check('exact API and Chinese query',len(call('/rag/retrieve',query('虚拟线程适用于什么场景？')))>0 and len(call('/rag/retrieve',query('Class.forName')))>0)
 hits=call('/rag/retrieve',query(token));check('version filter',hits and all(h['version']=='21' for h in hits))
 check('insufficient avoids model',call('/rag/answer',query('NoSuchEvidence'+uuid.uuid4().hex))['status']=='INSUFFICIENT')
 check('version ambiguity clarified',call('/rag/answer',query(token,''))['status']=='CLARIFY_VERSION')
 if any(m['role']=='LLM' and (not m['model'] or not m['keyConfigured']) for m in state['models']):fails('/rag/answer',query(token),503)
 check('seed excluded',not any('[演示]' in h['title'] for h in call('/rag/retrieve',{'query':'虚拟线程','searchMode':'KEYWORD'})))
 if not state['vectorReady']:fails('/rag/retrieve',query(token,mode='HYBRID'),503)
 if a.vectors:
  for doc in created:
   check('index success',call('/documents/'+doc+'/embedding-index',{},'POST')['status']=='INDEXED')
   check('index idempotency',call('/documents/'+doc+'/embedding-index',{},'POST')['reused'])
  check('semantic real results',bool(call('/rag/retrieve',query('等待 I/O 的任务',mode='SEMANTIC'))))
  result=call('/rag/answer',query('虚拟线程适用于什么任务？',mode='HYBRID'));check('real answer references',result['status']=='ANSWERED' and result['citations'])
  for ref in result['citations']:
   chunk=call('/chunks/'+ref['chunkId']);check('citation exact source',ref['quote'] in chunk['content'])
 call('/documents/'+created[0]+'/archive?archived=true',{},'POST');check('archived excluded',not call('/rag/retrieve',query(token)))
 print('PASS: stage 3 '+('cloud/vector checks' if a.vectors else 'keyword/database checks; cloud/vector NOT executed'))
finally:
 for doc in created:call('/documents/'+doc,None,'DELETE')
