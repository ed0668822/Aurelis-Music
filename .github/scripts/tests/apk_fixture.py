"""Synthetic metadata fixtures, never installable APKs or Android runtime evidence."""
import json
import struct
from pathlib import Path


def dex(names=('fixture/App',)):
    def uleb(value):
        out=bytearray()
        while True:
            byte=value & 127;value >>= 7;out.append(byte | (128 if value else 0))
            if not value:return bytes(out)
    count=len(names);string_off=112;type_off=string_off+count*4;class_off=type_off+count*4
    data_off=class_off+count*32;strings=bytearray();offsets=[]
    for name in names:
        text='L'+name+';';offsets.append(data_off+len(strings))
        strings+=uleb(len(text.encode('utf-16-le'))//2)+text.encode()+b'\0'
    out=bytearray(data_off+len(strings));out[:8]=b'dex\n039\0'
    for offset,value in [(32,len(out)),(36,112),(40,0x12345678),(56,count),(60,string_off),(64,count),(68,type_off),(96,count),(100,class_off)]:
        struct.pack_into('<I',out,offset,value)
    for i,offset in enumerate(offsets):
        struct.pack_into('<I',out,string_off+i*4,offset)
        struct.pack_into('<I',out,type_off+i*4,i)
        struct.pack_into('<I',out,class_off+i*32,i)
    out[data_off:]=strings
    return bytes(out)


def jvm(name='fixture/App', source='App.kt', private_text=None):
    def utf(value):
        data=value.encode();return b'\x01'+struct.pack('>H',len(data))+data
    pool=utf(name)+b'\x07\x00\x01'+utf('java/lang/Object')+b'\x07\x00\x03'+utf('SourceFile')+utf(source)
    if private_text is not None: pool+=utf(private_text)
    return b'\xca\xfe\xba\xbe'+struct.pack('>HHH',0,61,7 if private_text is None else 8)+pool+struct.pack('>HHHHHHH',33,2,4,0,0,0,1)+struct.pack('>HIH',5,2,6)


def policy():
    return {'schema_version':1,'application_id':'fixture','abi':'arm64-v8a',
            'required_dex_classes':['fixture/App'],'required_manifest_classes':['fixture/App'],
            'required_resources':['string/app_name'],'required_assets':[],
            'byte_exact_resources':[],'required_native_libraries':[]}


def write_policy(root, value=None):
    file=Path(root)/'.github/apk-audit-policy.json';file.parent.mkdir(parents=True,exist_ok=True)
    file.write_text(json.dumps(policy() if value is None else value))


RESOURCES='Package name=fixture id=7f\n resource 0x7f010001 string/app_name\n'
MANIFEST='N: android=http://schemas.android.com/apk/res/android\nE: manifest\n E: application\n  A: http://schemas.android.com/apk/res/android:name(0x01010003)="fixture.App"\n'
