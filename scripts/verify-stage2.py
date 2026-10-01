"""Python stdlib only. Run against the real API and a DISPOSABLE/test database.
python scripts/verify-stage2.py --base http://127.0.0.1:8080 [--url https://openjdk.org/jeps/444]
Creates test documents and deletes them in finally; never executes imported code.
"""
import argparse
import concurrent.futures
import json
import uuid
import urllib.request
import urllib.error
from datetime import datetime, timezone

p = argparse.ArgumentParser()
p.add_argument('--base', default='http://127.0.0.1:8080')
p.add_argument('--url')
args = p.parse_args()
created = set()

def request(path, data=None, method=None, raw=False):
    payload = None if data is None else json.dumps(data, ensure_ascii=False).encode('utf-8')
    req = urllib.request.Request(args.base + '/api' + path, data=payload,
                                 headers={'Content-Type': 'application/json'}, method=method)
    with urllib.request.urlopen(req, timeout=40) as response:
        body = response.read()
        return body if raw else (json.loads(body) if body else None)

def search(query, version='21'):
    return request('/search/answer', {'query': query, 'technologies': ['Java'], 'version': version, 'searchMode': 'KEYWORD'})['results']

def check(label, condition):
    assert condition, label
    print('PASS:', label)

suffix = str(uuid.uuid4())
name = '阶段2验收-' + suffix
token = '精确检索-' + suffix
metadata = {'technology': 'Java', 'version': '21', 'language': 'zh', 'tags': ['验收'], 'license': '未知'}
content = '# 真实手写笔记\n## 虚拟线程\n' + token + '\n中文排障：虚拟线程与 IllegalStateException、Class.forName。\n\n```java\n  System.out.println("仅保存，不执行");\n```'
payload = {'title': name, 'content': content, 'metadata': metadata}
try:
    check('real PostgreSQL mode', not request('/health')['demoMode'])
    first = request('/imports/note', payload)
    doc_id = first['document']['id']; created.add(doc_id)
    check('new import awaits review', first['document']['status'] == 'PENDING_REVIEW')
    check('pending content excluded from search', not search(token))
    duplicate = request('/imports/note', payload)
    check('repeat import is idempotent', duplicate['duplicate'] and duplicate['document']['id'] == doc_id and len(request('/documents/' + doc_id + '/versions')) == 1)
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        repeated = list(pool.map(lambda _: request('/imports/note', payload), range(4)))
    check('concurrent duplicate imports', all(item['duplicate'] for item in repeated))
    approved = request('/documents/' + doc_id + '/review', {'versionId': first['versionId'], 'approved': True})
    check('approval creates keyword index', approved['status'] == 'INDEXED' and approved['chunks'] > 0)
    version_detail = request('/documents/' + doc_id + '/versions/' + first['versionId'])
    check('version detail returns metadata and excludes raw bytea', version_detail['id'] == first['versionId'] and isinstance(version_detail['tags'], list) and 'raw_content' not in version_detail)
    chunk_page = request('/documents/' + doc_id + '/versions/' + first['versionId'] + '/chunks?pageSize=1')
    check('chunk pagination matches real chunk count', len(chunk_page['items']) == 1 and chunk_page['total'] == approved['chunks'])
    check('chunk detail preserves version association', request('/chunks/' + chunk_page['items'][0]['id'])['version_id'] == first['versionId'])
    audit_page = request('/review-records?documentId=' + doc_id + '&decision=APPROVED')
    check('approval audit record stored once', audit_page['total'] == 1)
    check('audit detail links correct document', request('/review-records/' + audit_page['items'][0]['id'])['document_id'] == doc_id)
    request('/documents/' + doc_id + '/review', {'versionId': first['versionId'], 'approved': True})
    check('repeat approval does not duplicate audit', request('/review-records?documentId=' + doc_id)['total'] == 1)

    check('Chinese keyword search', any(d['id'] == doc_id for d in search('虚拟线程')))
    check('class/API/error exact keyword search', any(d['id'] == doc_id for d in search('Class.forName')))
    filtered = {'query': token, 'technologies': ['Java'], 'version': '21', 'searchMode': 'KEYWORD', 'sourceType': 'MANUAL_NOTE', 'language': 'zh'}
    check('source and language filters match', bool(request('/search/answer', filtered)['results']))
    check('wrong source excluded', not request('/search/answer', {**filtered, 'sourceType': 'FILE'})['results'])
    check('wrong language excluded', not request('/search/answer', {**filtered, 'language': 'en'})['results'])
    today = datetime.now(timezone.utc).date().isoformat()
    check('UTC collection date filter', bool(request('/search/answer', {**filtered, 'from': today, 'to': today})['results']))
    check('out of date range excluded', not request('/search/answer', {**filtered, 'to': '2000-01-01'})['results'])
    listing = request('/documents?status=INDEXED&pageSize=1')
    check('server pagination and status filter', len(listing['items']) == 1 and listing['total'] >= 1)
    check('wrong version excluded', not search(token, '17'))
    check('missing knowledge returns no fabricated results', not search('不存在-' + suffix))
    check('code indentation retained', '  System.out.println' in request('/documents/' + doc_id)['content'])
    request('/documents/' + doc_id, {**metadata, 'title': name, 'tags': ['中文标签'], 'author': '用户填写'}, method='PUT')
    check('metadata persists', request('/documents/' + doc_id)['tags'] == ['中文标签'] and request('/documents/' + doc_id + '/metadata')['author'] == '用户填写')
    check('raw snapshot round-trip', request('/documents/' + doc_id + '/versions/' + first['versionId'] + '/raw', raw=True) == content.encode())
    newer = request('/imports/note', {**payload, 'content': content + '\n新版本内容', 'metadata': {**metadata, 'version': '22'}})
    check('update creates history', len(request('/documents/' + doc_id + '/versions')) == 2)
    check('pending new version leaves approved version searchable', bool(search(token, '21')) and not search(token, '22'))
    check('search links point to approved snapshot', request('/search/answer', {**filtered})['resultVersions'][doc_id] == first['versionId'])
    try:
        request('/documents/' + doc_id + '/review', {'versionId': first['versionId'], 'approved': True})
        raise AssertionError('stale review must fail')
    except urllib.error.HTTPError as e:
        check('stale review rejected', e.code == 409)
    request('/documents/' + doc_id + '/review', {'versionId': newer['versionId'], 'approved': True})
    check('approval switches searchable version', not search(token, '21') and bool(search(token, '22')))
    request('/documents/' + doc_id + '/reindex', method='POST')
    check('reindex remains searchable', bool(search(token, '22')))
    request('/documents/' + doc_id + '/archive', method='POST')
    check('archive excluded from search', not search(token, '22'))
    request('/documents/' + doc_id + '/archive?archived=false', method='POST')
    check('unarchive restores search', bool(search(token, '22')))
    request('/documents/' + doc_id + '/review', {'versionId': newer['versionId'], 'approved': False})
    check('rejected version excluded', not search(token, '22'))
    appended_payload = {'content': content + '\n手工修订版本', 'format': 'MARKDOWN'}
    appended = request('/documents/' + doc_id + '/versions', appended_payload)
    check('explicit manual revision creates pending version', appended['document']['status'] == 'PENDING_REVIEW' and len(request('/documents/' + doc_id + '/versions')) == 3)
    check('manual revision inherits current metadata', appended['document']['version'] == '22')
    check('manual revision is idempotent', request('/documents/' + doc_id + '/versions', appended_payload)['duplicate'])
    check('manual revision origin marked', 'manual-revision' in request('/documents/' + doc_id + '/versions/' + appended['versionId'])['parser_version'])
    check('pending manual revision has no chunks', request('/documents/' + doc_id + '/versions/' + appended['versionId'] + '/chunks')['total'] == 0)
    check('audit history retains previous decisions', request('/review-records?documentId=' + doc_id)['total'] == 3)

    # Multipart file import uses no third party libraries.
    boundary = 'atlas-' + suffix
    html = '<title>HTML 验收</title><nav>无关导航</nav><main><h1>中文正文</h1><p>' + token + '</p><pre>  code();</pre></main>'
    body = ('--' + boundary + '\r\nContent-Disposition: form-data; name="file"; filename="' + suffix + '.html"\r\nContent-Type: text/html\r\n\r\n' + html + '\r\n--' + boundary + '--\r\n').encode()
    req = urllib.request.Request(args.base + '/api/imports/file', data=body, headers={'Content-Type': 'multipart/form-data; boundary=' + boundary})
    with urllib.request.urlopen(req) as response:
        imported = json.load(response); created.add(imported['document']['id'])
    check('HTML file parser strips navigation', '无关导航' not in imported['document']['content'] and '  code();' in imported['document']['content'])
    try:
        request('/documents/' + imported['document']['id'] + '/versions/' + first['versionId'])
        raise AssertionError('cross-document version access accepted')
    except urllib.error.HTTPError as e:
        check('cross-document version rejected', e.code == 404)
    try:
        request('/documents/' + doc_id + '/versions', {'content': 'test', 'format': 'PDF'})
        raise AssertionError('invalid format accepted')
    except urllib.error.HTTPError as e:
        check('unsupported manual revision format rejected', e.code == 400)
    dashboard = request('/dashboard')
    check('dashboard reads persisted activity and seven day trend', bool(dashboard['recentActivities']) and len(dashboard['ingestionTrend']) == 7 and dashboard['monthlyCost'] is None)
    for unsafe in ['http://127.0.0.1', 'http://169.254.169.254', 'file:///etc/passwd']:
        try:
            request('/imports/url', {'url': unsafe})
            raise AssertionError('unsafe URL accepted')
        except urllib.error.HTTPError as e:
            check('URL access blocked: ' + unsafe, e.code in (400, 502))
    if args.url:
        imported = request('/imports/url', {'url': args.url})
        # Avoid deleting a pre-existing user's document.
        if not imported['duplicate']: created.add(imported['document']['id'])
        check('real public URL imported', len(imported['document']['content']) > 100 and imported['document']['url'].startswith('https://'))
        check('real URL duplicate detected', request('/imports/url', {'url': args.url})['duplicate'])
finally:
    for doc_id in created:
        request('/documents/' + doc_id, method='DELETE')
        try:
            request('/documents/' + doc_id)
            raise AssertionError('deleted document still exists')
        except urllib.error.HTTPError as e:
            check('delete removes document: ' + doc_id, e.code == 404)
print('Stage 2 verification complete.')
