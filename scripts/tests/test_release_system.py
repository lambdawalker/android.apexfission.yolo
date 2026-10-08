import hashlib
import io
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import urllib.error
import zipfile
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import module_release as m
import release_identity as identity
import import_docs
import documentation_history as history
import jitpack_build as build
import jitpack_release as jp


def record(version='0.2.2',source='a'*40,target='maven-central'):
    r=dict(module='yolo',version=version,source=source,repository=target,repository_url=m.common.CENTRAL,group='com.apexfission.android',artifact='yolo',phase='confirmed-public',sha256={s:'b'*64 for s in m.common.SUFFIXES})
    if target=='jitpack':r.update(group=jp.GROUP,artifact=jp.ARTIFACT,consumer_version=f'yolo~v{version}',repository_url=jp.REPOSITORY)
    return r

def zipped(entries):
    f=io.BytesIO()
    with zipfile.ZipFile(f,'w') as z:
        for n,v in entries.items():z.writestr(n,v)
    return f.getvalue()

class ReleaseTests(unittest.TestCase):
    def test_latest_numeric_and_destination_catchup(self):
        a=record('0.2.9');b=record('0.2.10',target='jitpack')
        rendered=import_docs.render({('maven-central','yolo'):a,('jitpack','yolo'):b},{'yolo':('com.apexfission.android','yolo')})
        self.assertNotIn('### maven-central',rendered);self.assertIn('yolo~v0.2.10',rendered)
        a=record('0.2.10');self.assertIn('### maven-central',import_docs.render({('maven-central','yolo'):a,('jitpack','yolo'):b},{'yolo':('com.apexfission.android','yolo')}))
        b['source']='c'*40
        with self.assertRaisesRegex(ValueError,'Conflicting'):import_docs.render({('maven-central','yolo'):a,('jitpack','yolo'):b},{'yolo':('com.apexfission.android','yolo')})
    def test_archive_retains_docs_identity_and_rejects_conflict(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);path=history.archive_record(root,record());entry=json.loads((root/path).read_text());entry['documentation_ref']='d'*40;(root/path).write_text(json.dumps(entry))
            history.archive_record(root,record(target='jitpack'))
            entry=history.load_catalog(root)[0];self.assertEqual(entry['documentation_ref'],'d'*40);self.assertEqual(len(entry['destinations']),2)
            with self.assertRaisesRegex(ValueError,'source conflict'):history.archive_record(root,record(source='c'*40))
    def test_pending_destination_blocks_reupload_but_other_destination_allowed(self):
        tags=['release-pending/yolo/0.2.2']
        with self.assertRaisesRegex(ValueError,'Unresolved'):m.ensure_available('yolo',tags)
        with patch.dict(m.os.environ,{'RELEASE_REPOSITORY':'jitpack'}):m.ensure_available('yolo',tags)
        with self.assertRaisesRegex(ValueError,'legacy'):m.ensure_available('yolo',['release-pending/0.2.2'])
    def test_stable_jitpack_selection_and_task(self):
        for value in ('yolo/v1.2.3','yolo~v1.2.3'):self.assertEqual(build.selection(value),('yolo','1.2.3'))
        for value in ('main','v1.2.3','yolo/v01.2.3','yolo/v1.2.3-SNAPSHOT','app/v1.2.3'):
            with self.assertRaises(ValueError):build.selection(value)
        self.assertEqual(build.command('yolo','1.2.3')[2],':yolo:publishToMavenLocal')
        with patch.dict(build.os.environ,{'VERSION':'yolo~v1.2.3','GIT_COMMIT':'b'*40}),patch.object(build,'run',return_value='a'*40):
            with self.assertRaises(ValueError):build.main()
    def fixture(self):
        r=record(target='jitpack');v=r['consumer_version'];meta='<name>n</name><description>d</description><url>u</url><licenses><license><name>l</name><url>u</url></license></licenses><developers><developer><id>i</id><name>n</name></developer></developers><scm><url>u</url><connection>c</connection></scm>'
        pom=f'<project><groupId>{jp.GROUP}</groupId><artifactId>{jp.ARTIFACT}</artifactId><version>{v}</version><packaging>aar</packaging>{meta}<dependencies><dependency><groupId>com.apexfission.android.math</groupId><artifactId>coordinates</artifactId><version>0.1.0</version><scope>compile</scope></dependency></dependencies></project>'.encode()
        data={'.pom':pom,'.aar':zipped({'AndroidManifest.xml':'manifest','classes.jar':zipped({'com/apexfission/android/yolo/Permission.class':b'\xca\xfe\xba\xbe\x00\x00\x00\x3d'})}),'-sources.jar':zipped({'Permission.kt':'class Permission'}),'-javadoc.jar':zipped({'docs/api.md':'API'})}
        r['content_sha256']={s:m.common.zip_contents(data[s]) for s in ('-sources.jar','-javadoc.jar')}
        files={jp.API_BASE+'/'+v:json.dumps({'status':'ok','commit':r['source']}).encode(),**{jp.artifact_base(r)+s:d for s,d in data.items()}}
        return r,files
    def test_jitpack_provenance_content_partial_and_retries(self):
        r,files=self.fixture();read=lambda url,missing=False:files.get(url)
        self.assertEqual(len(jp.verify(r,read)),4)
        api=jp.API_BASE+'/'+r['consumer_version']
        for status in ('queued','building','none'):
            files[api]=json.dumps({'status':status}).encode()
            with self.assertRaises(jp.NotReady):jp.verify(r,read)
        files[api]=json.dumps({'status':'ok','commit':'b'*40}).encode()
        with self.assertRaisesRegex(ValueError,'source'):jp.verify(r,read)
        files[api]=json.dumps({'status':'ok','commit':r['source']}).encode()
        base=jp.artifact_base(r);files[base+'-sources.jar']=zipped({'Permission.kt':'changed source'})
        with self.assertRaisesRegex(ValueError,'content differs'):jp.verify(r,read)
        del files[base+'-sources.jar']
        with self.assertRaises(jp.NotReady):jp.verify(r,read)
    def test_jitpack_missing_pom_reports_failed_build_instead_of_timeout(self):
        r,files=self.fixture()
        del files[jp.artifact_base(r)+'.pom']
        files[jp.API_BASE+'/'+r['consumer_version']]=json.dumps({'status':'Error','commit':r['source']}).encode()
        def read(url,missing=False):
            if url in files:return files[url]
            if missing:return None
            raise urllib.error.HTTPError(url,404,'Not Found',None,None)
        with patch.dict(m.os.environ,{'RELEASE_REPOSITORY':'jitpack'}),patch.object(m,'fetch',side_effect=read),patch.object(m,'read_record',return_value=r),patch.object(m,'git',return_value='COORDINATES_DEPENDENCY=com.apexfission.android.math:coordinates:0.1.0'):
            with self.assertRaisesRegex(ValueError,'JitPack build failed'):
                m.confirm('yolo','0.2.2',timeout=0)

    def test_jitpack_missing_pom_still_retries_queued_build(self):
        r,files=self.fixture()
        del files[jp.artifact_base(r)+'.pom']
        files[jp.API_BASE+'/'+r['consumer_version']]=json.dumps({'status':'queued'}).encode()
        def read(url,missing=False):
            if url in files:return files[url]
            if missing:return None
            raise urllib.error.HTTPError(url,404,'Not Found',None,None)
        with patch.dict(m.os.environ,{'RELEASE_REPOSITORY':'jitpack'}),patch.object(m,'fetch',side_effect=read):
            with self.assertRaises(jp.NotReady):m.verify_public(r)

    def test_timeout_keeps_journal_and_docs(self):
        r=record();r['phase']='reserved-before-upload'
        with patch.object(m,'read_record',return_value=r),patch.object(m,'git',return_value='COORDINATES_DEPENDENCY=com.apexfission.android.math:coordinates:0.1.0'),patch.object(m,'verify_public',side_effect=urllib.error.URLError('missing')):
            with self.assertRaises(TimeoutError):m.confirm('yolo','0.2.2',timeout=0)
    def test_registry_and_generated_docs(self):
        from publishing_config import generate
        generate(check=True);m.documentation(verify=True);history.verify(m.ROOT)

class GitTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)
        self.git('init','-q');self.git('config','user.email','test@example.com');self.git('config','user.name','test')
        self.write('gradle.properties','GROUP=com.apexfission.android\nPOM_ARTIFACT_ID=yolo\n');self.write('yolo/src/main/code.kt','one');self.commit();self.source=self.git('rev-parse','HEAD');self.git('tag','yolo/v0.2.9')
    def git(self,*args):return subprocess.check_output(['git',*args],cwd=self.root,text=True,stderr=subprocess.DEVNULL).strip()
    def write(self,path,value):
        p=self.root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(value)
    def commit(self):self.git('add','.');self.git('commit','-qm','change')
    def select(self,requested=''):return identity.select('yolo',self.git('rev-parse','HEAD'),self.git,self.git('tag').splitlines(),[],requested)
    def test_docs_only_unchanged_reuses_original_and_changed_input_advances(self):
        self.write('IMPORT.md','updated');self.write('docs/es/index.md','translation');self.commit();self.assertEqual(self.select(),('0.2.9',self.source))
        self.write('yolo/src/main/code.kt','two');self.commit();self.assertEqual(self.select()[0],'0.2.10')
    def test_pending_cross_destination_and_legacy_conflicts(self):
        self.git('tag','release-pending/jitpack/yolo/0.2.10');self.assertEqual(self.select(),('0.2.10',self.source))
        self.write('gradle/libs.versions.toml','change');self.commit();self.git('tag','v0.2.10')
        with self.assertRaisesRegex(ValueError,'Conflicting'):self.select()
    def test_major_override_and_older_rejection(self):
        self.assertEqual(self.select('1.0.0'),('1.0.0',self.source))
        with self.assertRaises(ValueError):self.select('0.2.8')


class FinalizationTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.base=Path(self.temp.name);self.remote=self.base/'remote.git';self.root=self.base/'repo'
        self.call(self.base,'init','--bare','--initial-branch=main',str(self.remote));self.call(self.base,'clone',str(self.remote),str(self.root));self.git('config','user.email','test@example.com');self.git('config','user.name','test')
        for path in ('docs/templates/IMPORT.md.template','gradle.properties'):
            destination=self.root/path;destination.parent.mkdir(parents=True,exist_ok=True);destination.write_bytes((m.ROOT/path).read_bytes())
        (self.root/'IMPORT.md').write_text('previous installation');self.git('add','.');self.git('commit','-qm','release source');self.source=self.git('rev-parse','HEAD');self.git('push','origin','main')
        patcher=patch.object(m,'ROOT',self.root);patcher.start();self.addCleanup(patcher.stop)
    def call(self,cwd,*args):return subprocess.check_output(['git',*args],cwd=cwd,text=True,stderr=subprocess.PIPE).strip()
    def git(self,*args):return self.call(self.root,*args)
    def reserve_fixture(self):
        r=record(source=self.source);r['phase']='reserved-before-upload';self.git('tag','-a',m.pending('yolo','0.2.2'),self.source,'-m',json.dumps(r));self.git('tag','yolo/v0.2.2',self.source);self.git('push','origin','--tags');return r
    def test_guard_is_durable_and_second_invocation_rejected(self):
        self.reserve_fixture();m.guard('yolo','0.2.2')
        with self.assertRaisesRegex(ValueError,'already started'):m.guard('yolo','0.2.2')
        self.assertTrue(m.remote_ref(m.uploading('yolo','0.2.2')))
    def test_finalization_preserves_advanced_main_and_retry_noops(self):
        r=self.reserve_fixture();r['phase']='confirmed-public';(self.root/'build').mkdir();(self.root/'build/confirmed-yolo.json').write_text(json.dumps(r))
        (self.root/'unrelated.md').write_text('concurrent change');self.git('add','unrelated.md');self.git('commit','-qm','concurrent update');self.git('push','origin','main');self.git('checkout','--detach',self.source)
        m.finalize('yolo','0.2.2',self.source)
        self.assertEqual(self.call(self.remote,'show','main:unrelated.md'),'concurrent change')
        archive=json.loads(self.call(self.remote,'show','main:docs/releases/history/yolo/0.2.2.json'));self.assertEqual(archive['source'],self.source)
        self.assertFalse(m.remote_ref(m.pending('yolo','0.2.2')))
        self.assertEqual(m.prepare_finalization('yolo','0.2.2',self.source)['skip'],'true')
    def test_refused_atomic_push_retains_remote_journal_and_docs(self):
        r=self.reserve_fixture();r['phase']='confirmed-public';(self.root/'build').mkdir();(self.root/'build/confirmed-yolo.json').write_text(json.dumps(r))
        hook=self.remote/'hooks/pre-receive';hook.write_text('#!/bin/sh\nexit 1\n');hook.chmod(0o755)
        with self.assertRaises(subprocess.CalledProcessError):m.finalize('yolo','0.2.2',self.source)
        self.assertTrue(m.remote_ref(m.pending('yolo','0.2.2')))
        self.assertEqual(self.call(self.remote,'show','main:IMPORT.md'),'previous installation')

    def test_proven_legacy_identity_remains_reusable(self):
        legacy=record('0.1.0',self.source);legacy['legacy_tag']='v0.1.0'
        self.git('tag','v0.1.0',self.source)
        self.assertEqual(identity.select('yolo',self.source,self.git,['v0.1.0'],[legacy]),('0.1.0',self.source))
        legacy['source']='c'*40
        with self.assertRaisesRegex(ValueError,'source tag'):
            identity.select('yolo',self.source,self.git,['v0.1.0'],[legacy])


    def test_legacy_pending_recovers_original_source_and_hashes(self):
        r=record(source=self.source);r['phase']='reserved-before-upload'
        for key in ('module','repository','repository_url'):r.pop(key)
        self.git('tag','-a','release-pending/0.2.2',self.source,'-m',json.dumps(r))
        self.git('tag','-a','release-uploading/0.2.2',self.source,'-m','upload started')
        self.git('push','origin','--tags')
        # Recovery uses updated trusted tooling, not scripts from the upload source.
        (self.root/'scripts').mkdir();(self.root/'scripts/protocol.py').write_text('updated recovery protocol')
        props=self.root/'gradle.properties';props.write_text(props.read_text().replace('coordinates:0.1.0','coordinates:9.9.9'))
        self.git('add','.');self.git('commit','-qm','release protocol migration');self.git('push','origin','main')
        selected=m.prepare_finalization('yolo','0.2.2')
        self.assertEqual(selected['source'],self.source)
        with self.assertRaisesRegex(ValueError,'finalization-only'):m.guard('yolo','0.2.2')
        recovered=m.read_record('yolo','0.2.2')
        self.assertEqual(recovered['sha256'],r['sha256'])
        self.assertEqual(recovered['legacy_tag'],'v0.2.2')
        with patch.object(m,'verify_public',return_value=None) as public:
            confirmed=m.confirm('yolo','0.2.2',timeout=0)
        self.assertEqual(confirmed['source'],self.source)
        self.assertEqual(public.call_args.args[1],('com.apexfission.android.math','coordinates','0.1.0'))
        m.finalize('yolo','0.2.2',self.source)
        self.assertEqual(self.call(self.remote,'tag','--list','release-*'),'')
        self.assertEqual(self.call(self.remote,'rev-parse','v0.2.2^{commit}'),self.source)
        self.assertEqual(m.prepare_finalization('yolo','0.2.2')['skip'],'true')
        # A later destination pointer must not erase the legacy completed identity.
        later=record('0.2.3',source=self.source)
        (self.root/'docs/releases/yolo.json').write_text(json.dumps(later))
        history.archive_record(self.root,later)
        self.git('add','.');self.git('commit','-qm','newer confirmed pointer')
        self.git('tag','yolo/v0.2.3',self.source);self.git('push','origin','HEAD:main','refs/tags/yolo/v0.2.3')
        self.assertEqual(m.prepare_finalization('yolo','0.2.2')['skip'],'true')


class NegativeValidationTests(unittest.TestCase):
    def test_module_metadata_exports_public_coordinates_dependency(self):
        from test_release import artifacts
        metadata=json.loads(artifacts()['.module'])
        def check():
            m.common.verify_artifact(json.dumps(metadata).encode(),'.module','org.example','yolo','1.2.3')
        check()
        variant=metadata['variants'][0]
        for dependencies in ([], [{'group':'com.apexfission.android.math','module':'coordinates','version':{'requires':'9.9.9'}}]):
            variant['dependencies']=dependencies
            with self.assertRaisesRegex(ValueError,'API metadata'):check()
        metadata['variants']=[]
        with self.assertRaisesRegex(ValueError,'API metadata'):check()

    def test_pom_and_aar_identity_and_shape(self):
        fixture=ReleaseTests();r,files=fixture.fixture();base=jp.artifact_base(r);pom=files[base+'.pom']
        for replacement in (pom.replace(jp.GROUP.encode(),b'wrong.group'),pom.replace(r['consumer_version'].encode(),b'9.9.9'),pom.replace(b'<packaging>aar',b'<packaging>jar')):
            with self.assertRaises(ValueError):m.common.verify_pom(replacement,r['group'],r['artifact'],r['consumer_version'])
        with self.assertRaises(ValueError):m.common.verify_artifact(zipped({'AndroidManifest.xml':'only manifest'}),'.aar',r['group'],r['artifact'],r['consumer_version'])
        bad=zipped({'AndroidManifest.xml':'manifest','classes.jar':zipped({'com/apexfission/android/yolo/Test.class':b'bad class'})})
        with self.assertRaises(ValueError):m.common.verify_artifact(bad,'.aar',r['group'],r['artifact'],r['consumer_version'])
    def test_registry_rejects_duplicates_unsupported_publishers_and_unsafe_urls(self):
        import publishing_config as config
        with tempfile.TemporaryDirectory() as d:
            path=Path(d)/'registry.yml'
            for value in ('repositories:\n  maven-central: {environment: central, publisher: central}\n  maven-central: {environment: second, publisher: central}\n', 'repositories:\n  maven-central: {environment: central, publisher: central}\n  private: {environment: private, publisher: maven}\n'):
                path.write_text(value)
                with self.assertRaises(ValueError):config.load_config(path)
        for value in ('http://example.org','https://user:password@example.org','https://example.org/\"code','https://example.org/?secret=yes'):
            with self.assertRaises(ValueError):config.validate_url(value)

if __name__=='__main__':unittest.main()
