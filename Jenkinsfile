pipeline {
    agent none
    environment {
        PLUGIN_NAME = "com.cdsoftware.recaptcha"
        IDEMPIERE_VERSION = "12.0.0"
    }
    stages {
        stage('Init') {
            agent any
            steps {
                script {
                    def branchJob = currentBuild.rawBuild.getParent()
                    def multibranchJob = branchJob.getParent()

                    def dependenciesStr = "Ninguna"

                    def branchDescription = """
                    <b>Plugin:</b> ${env.PLUGIN_NAME}<br/>
                    <b>iDempiere:</b> ${env.IDEMPIERE_VERSION}<br/>
                    <b>Branch:</b> ${env.BRANCH_NAME ?: 'N/A'}<br/>
                    <b>Job:</b> ${env.JOB_NAME}<br/>
                    <b>Dependencias:</b> ${dependenciesStr}<br/>
                    """

                    def mainDescription = """
                    <b>Plugin:</b> ${env.PLUGIN_NAME}<br/>
                    <b>iDempiere:</b> ${env.IDEMPIERE_VERSION}<br/>
                    <b>Repositorio:</b> Bitbucket<br/>
                    <b>Última rama ejecutada:</b> ${env.BRANCH_NAME}<br/>
                    <b>Último build:</b> #${env.BUILD_NUMBER}<br/>
                    <b>Dependencias:</b> ${dependenciesStr}<br/>
                    """

                    if (env.BRANCH_NAME == 'master') {
                        branchJob.setDescription(branchDescription)
                        multibranchJob.setDescription(mainDescription)
                    }

                    currentBuild.description = "${env.PLUGIN_NAME}-${env.IDEMPIERE_VERSION}.${env.BUILD_NUMBER}"

                    echo "Descripción del build (y descripciones del job/multibranch si es rama master) actualizadas."
                }
            }
        }
        stage('Compile') {
            agent {
                docker {
                    image 'carl0jgr/idempiere-source-builder:12'
                    args '--entrypoint=\'\' -u root:root -v /var/jenkins_home/.m2:/root/.m2'              
                }
            }
            steps {
                dir('target-platform') {
                    git branch: '12.0', url: 'https://github.com/ingeint/idempiere-target-platform-plugin.git'
					sh './plugin-builder build ../${PLUGIN_NAME}'
                    archiveArtifacts artifacts: "target/${PLUGIN_NAME}-${IDEMPIERE_VERSION}.${BUILD_NUMBER}.jar", fingerprint: true
                    sh 'rm -rf target ../${PLUGIN_NAME}/target'
                }
            }
        }
                        stage('Archive Readme') {
            agent {
                docker {
                    image 'python:3.9-alpine'
                }
            }
            steps {
                script {
                    if (fileExists('README.md')) {
                        writeFile file: 'generate_readme.py', text: '''import sys, re

def md_to_html(md_text):
    html = []
    in_list = False
    in_code = False
    in_table = False
    
    for line in md_text.splitlines():
        line_stripped = line.rstrip()
        
        if line_stripped.startswith("```"):
            if in_code:
                html.append("</code></pre>")
                in_code = False
            else:
                html.append("<pre><code>")
                in_code = True
            continue
            
        if in_code:
            escaped = line.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
            html.append(escaped)
            continue
            
        escaped = line_stripped.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
        
        if escaped.startswith("|") and escaped.endswith("|"):
            cells = [c.strip() for c in escaped.split("|")[1:-1]]
            if all(c.startswith("---") or c == "" for c in cells):
                continue
            if not in_table:
                html.append("<table>")
                in_table = True
                html.append("<thead><tr>")
                for cell in cells:
                    html.append(f"<th>{cell}</th>")
                html.append("</tr></thead><tbody>")
            else:
                html.append("<tr>")
                for cell in cells:
                    html.append(f"<td>{cell}</td>")
                html.append("</tr>")
            continue
        else:
            if in_table:
                html.append("</tbody></table>")
                in_table = False
        
        if escaped.startswith("# "):
            html.append(f"<h1>{escaped[2:]}</h1>")
        elif escaped.startswith("## "):
            html.append(f"<h2>{escaped[3:]}</h2>")
        elif escaped.startswith("### "):
            html.append(f"<h3>{escaped[4:]}</h3>")
        elif escaped.startswith("#### "):
            html.append(f"<h4>{escaped[5:]}</h4>")
        elif escaped.startswith("- ") or escaped.startswith("* "):
            if not in_list:
                html.append("<ul>")
                in_list = True
            html.append(f"<li>{escaped[2:]}</li>")
        else:
            if in_list:
                html.append("</ul>")
                in_list = False
            if not escaped.strip():
                html.append("<br/>")
            else:
                html.append(f"<p>{escaped}</p>")
                
    if in_list:
        html.append("</ul>")
    if in_code:
        html.append("</code></pre>")
    if in_table:
        html.append("</tbody></table>")
        
    result = "\\n".join(html)
    result = re.sub(r"\\*\\*(.*?)\\*\\*", r"<strong>\\1</strong>", result)
    result = re.sub(r"\\*(.*?)\\*", r"<em>\\1</em>", result)
    result = re.sub(r"`(.*?)`", r"<code>\\1</code>", result)
    result = re.sub(r"\\[(.*?)\\]\\((.*?)\\)", r'<a href="\\2" target="_blank">\\1</a>', result)
    
    return result

with open("README.md", "r", encoding="utf-8") as f:
    content = f.read()

html_body = md_to_html(content)

template = f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>README</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
    <style>
        :root {{
            --bg-color: #0b0f19;
            --container-bg: #111827;
            --text-color: #e5e7eb;
            --text-muted: #9ca3af;
            --primary: #6366f1;
            --primary-hover: #818cf8;
            --border-color: #1f2937;
            --code-bg: #1f2937;
            --pre-bg: #0f172a;
            --inline-code-color: #ff7b72;
            --table-header-bg: #1f2937;
            --table-even-bg: #111827;
            --table-odd-bg: #151f32;
        }}
        
        body {{
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
            background: var(--bg-color);
            color: var(--text-color);
            padding: 40px 20px;
            line-height: 1.7;
            max-width: 850px;
            margin: auto;
        }}
        
        .markdown-body {{
            background: var(--container-bg);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 40px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
        }}
        
        h1, h2, h3, h4 {{
            color: #ffffff;
            font-weight: 700;
            margin-top: 32px;
            margin-bottom: 16px;
            line-height: 1.3;
        }}
        
        h1 {{
            font-size: 2.25em;
            border-bottom: 2px solid var(--border-color);
            padding-bottom: 0.4em;
            margin-top: 0;
            background: linear-gradient(135deg, #ffffff 0%, #a5b4fc 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }}
        
        h2 {{
            font-size: 1.6em;
            border-bottom: 1px solid var(--border-color);
            padding-bottom: 0.3em;
        }}
        
        h3 {{ font-size: 1.3em; }}
        h4 {{ font-size: 1.1em; }}
        
        p {{ margin-top: 0; margin-bottom: 20px; color: var(--text-color); }}
        
        ul {{ padding-left: 2em; margin-top: 0; margin-bottom: 20px; }}
        li {{ margin-top: 0.5em; }}
        
        pre {{
            background: var(--pre-bg);
            border: 1px solid var(--border-color);
            padding: 20px;
            border-radius: 8px;
            overflow: auto;
            font-family: 'JetBrains Mono', ui-monospace, SFMono-Regular, monospace;
            font-size: 88%;
            line-height: 1.5;
            margin-bottom: 20px;
        }}
        
        code {{
            font-family: 'JetBrains Mono', ui-monospace, SFMono-Regular, monospace;
            background: var(--code-bg);
            color: var(--inline-code-color);
            padding: 0.2em 0.4em;
            border-radius: 4px;
            font-size: 88%;
        }}
        
        pre code {{
            background: none;
            padding: 0;
            border-radius: 0;
            font-size: 100%;
            color: #f8fafc;
        }}
        
        a {{
            color: var(--primary);
            text-decoration: none;
            font-weight: 500;
            border-bottom: 1px dashed var(--primary);
            transition: all 0.2s ease;
        }}
        
        a:hover {{
            color: var(--primary-hover);
            border-bottom-style: solid;
        }}
        
        table {{
            border-spacing: 0;
            border-collapse: collapse;
            margin-top: 0;
            margin-bottom: 20px;
            width: 100%;
            border-radius: 8px;
            overflow: hidden;
            border: 1px solid var(--border-color);
        }}
        
        table th, table td {{
            padding: 10px 15px;
            border: 1px solid var(--border-color);
        }}
        
        table th {{
            background-color: var(--table-header-bg);
            font-weight: 600;
            text-align: left;
        }}
        
        table tr {{
            background-color: var(--table-odd-bg);
        }}
        
        table tr:nth-child(2n) {{
            background-color: var(--table-even-bg);
        }}
        
        pre::-webkit-scrollbar {{
            width: 8px;
            height: 8px;
        }}
        pre::-webkit-scrollbar-track {{
            background: var(--pre-bg);
        }}
        pre::-webkit-scrollbar-thumb {{
            background: var(--border-color);
            border-radius: 4px;
        }}
        pre::-webkit-scrollbar-thumb:hover {{
            background: var(--text-muted);
        }}
    </style>
</head>
<body>
<div class="markdown-body">
{html_body}
</div>
</body>
</html>"""

with open("readme.html", "w", encoding="utf-8") as f:
    f.write(template)
'''
                        sh 'python3 generate_readme.py'
                        archiveArtifacts artifacts: 'readme.html', fingerprint: true
                        publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: '.',
                            reportFiles: 'readme.html',
                            reportName: 'Project README'
                        ])
                        sh 'rm -f generate_readme.py readme.html'
                    }
                }
            }
        }
    }
}