"use strict";(self.webpackChunkdocusaurus=self.webpackChunkdocusaurus||[]).push([[5101],{3824:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>s,contentTitle:()=>o,default:()=>h,frontMatter:()=>r,metadata:()=>a,toc:()=>d});var i=t(4848),l=t(8453);const r={},o="File Connector Introduction",a={id:"application-development/connector/file",title:"File Connector Introduction",description:"GeaFlow support read data from file and write data to file.",source:"@site/../docs-en/source/5.application-development/3.connector/2.file.md",sourceDirName:"5.application-development/3.connector",slug:"/application-development/connector/file",permalink:"/tugraph-analytics/en/application-development/connector/file",draft:!1,unlisted:!1,tags:[],version:"current",sidebarPosition:2,frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Introduction to Connector Basics",permalink:"/tugraph-analytics/en/application-development/connector/common"},next:{title:"Console Connector Introduction",permalink:"/tugraph-analytics/en/application-development/connector/console"}},s={},d=[{value:"Syntax",id:"syntax",level:2},{value:"Options",id:"options",level:2},{value:"Example",id:"example",level:2}];function c(e){const n={code:"code",h1:"h1",h2:"h2",header:"header",p:"p",pre:"pre",table:"table",tbody:"tbody",td:"td",th:"th",thead:"thead",tr:"tr",...(0,l.R)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.header,{children:(0,i.jsx)(n.h1,{id:"file-connector-introduction",children:"File Connector Introduction"})}),"\n",(0,i.jsx)(n.p,{children:"GeaFlow support read data from file and write data to file."}),"\n",(0,i.jsx)(n.h2,{id:"syntax",children:"Syntax"}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-sql",children:"CREATE TABLE file_table (\n  id BIGINT,\n  name VARCHAR,\n  age INT\n) WITH (\n\ttype='file',\n    geaflow.dsl.file.path = '/path/to/file'\n)\n"})}),"\n",(0,i.jsx)(n.h2,{id:"options",children:"Options"}),"\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n",(0,i.jsxs)(n.table,{children:[(0,i.jsx)(n.thead,{children:(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.th,{children:"Key"}),(0,i.jsx)(n.th,{children:"Required"}),(0,i.jsx)(n.th,{children:"Description"})]})}),(0,i.jsxs)(n.tbody,{children:[(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"geaflow.file.persistent.config.json"}),(0,i.jsx)(n.td,{children:"false"}),(0,i.jsx)(n.td,{children:"The JSON format DFS configuration will override the system environment configuration."})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"geaflow.dsl.file.path"}),(0,i.jsx)(n.td,{children:"true"}),(0,i.jsx)(n.td,{children:"The file path to read or write."})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"geaflow.dsl.column.separator"}),(0,i.jsx)(n.td,{children:"false"}),(0,i.jsx)(n.td,{children:"The column separator for split text to columns.Default value is ','."})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"geaflow.dsl.line.separator"}),(0,i.jsx)(n.td,{children:"false"}),(0,i.jsx)(n.td,{children:"The line separator for split text to columns..Default value is '\\n'."})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"geaflow.dsl.file.name.regex"}),(0,i.jsx)(n.td,{children:"false"}),(0,i.jsx)(n.td,{children:"The regular expression filter rule for file name reading is empty by default."})]}),(0,i.jsxs)(n.tr,{children:[(0,i.jsx)(n.td,{children:"geaflow.dsl.file.format"}),(0,i.jsx)(n.td,{children:"false"}),(0,i.jsx)(n.td,{children:"The file format for reading and writing supports Parquet and TXT, with the default format being TXT."})]})]})]}),"\n",(0,i.jsx)(n.h2,{id:"example",children:"Example"}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-sql",children:"CREATE TABLE file_source (\n  id BIGINT,\n  name VARCHAR,\n  age INT\n) WITH (\n\ttype='file',\n    geaflow.dsl.file.path = '/path/to/file'\n);\n\nCREATE TABLE file_sink (\n  id BIGINT,\n  name VARCHAR,\n  age INT\n) WITH (\n\ttype='file',\n    geaflow.dsl.file.path = '/path/to/file'\n);\n\nINSERT INTO file_sink\nSELECT * FROM file_source;\n"})})]})}function h(e={}){const{wrapper:n}={...(0,l.R)(),...e.components};return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(c,{...e})}):c(e)}},8453:(e,n,t)=>{t.d(n,{R:()=>o,x:()=>a});var i=t(6540);const l={},r=i.createContext(l);function o(e){const n=i.useContext(r);return i.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function a(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(l):e.components||l:o(e.components),i.createElement(r.Provider,{value:n},e.children)}}}]);