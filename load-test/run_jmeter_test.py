# 物业账单导入功能 - JMeter 命令行压测脚本
# 直接运行此脚本即可完成登录+压测全流程

import subprocess
import os
import sys

# 配置
JMETER_HOME = r"D:\apache-jmeter-5.4.1"
JMX_FILE = r"e:\后端\项目练习\smartPark\load-test\bill-import-test.jmx"
RESULT_DIR = r"e:\后端\项目练习\smartPark\load-test\results"

# 创建结果目录
os.makedirs(RESULT_DIR, exist_ok=True)

def run_jmx(jmx_name, description):
    """运行指定的 JMX 文件"""
    jmx_path = os.path.join(RESULT_DIR, jmx_name)
    result_file = os.path.join(RESULT_DIR, f"{jmx_name.replace('.jmx', '')}-result.jtl")
    report_dir = os.path.join(RESULT_DIR, f"{jmx_name.replace('.jmx', '')}-report")
    
    print(f"\n{'='*60}")
    print(f"运行: {description}")
    print(f"{'='*60}")
    
    cmd = [
        os.path.join(JMETER_HOME, "bin", "jmeter.bat"),
        "-n",
        "-t", jmx_path,
        "-l", result_file,
        "-e",
        "-o", report_dir
    ]
    
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=600)
        print("STDOUT:", result.stdout[-500:] if result.stdout else "")
        if result.stderr:
            print("STDERR:", result.stderr[-500:] if result.stderr else "")
        print(f"\n结果已保存到: {report_dir}/index.html")
        return result.returncode == 0
    except subprocess.TimeoutExpired:
        print("超时！测试可能还在运行...")
        return False

def create_login_test():
    """创建登录测试 JMX"""
    jmx_content = '''<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.4.1">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="登录测试" enabled="true">
      <boolProp name="TestPlan.serialize_threadgroups">true</boolProp>
    </TestPlan>
    <hashTree>
      <ConfigTestElement guiclass="HttpDefaultsGui" testclass="ConfigTestElement" testname="HTTP Request Defaults" enabled="true">
        <stringProp name="HTTPSampler.domain">localhost</stringProp>
        <stringProp name="HTTPSampler.port">8086</stringProp>
        <stringProp name="HTTPSampler.protocol">http</stringProp>
        <stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>
      </ConfigTestElement>
      <hashTree/>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="登录线程组" enabled="true">
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControlPanel" testclass="LoopController" testname="循环" enabled="true">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">1</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">1</stringProp>
        <stringProp name="ThreadGroup.ramp_time">1</stringProp>
      </ThreadGroup>
      <hashTree>
        <JSR223Sampler guiclass="TestBeanGUI" testclass="JSR223Sampler" testname="JSR223-直接HTTP登录" enabled="true">
          <stringProp name="cacheKey">login</stringProp>
          <stringProp name="scriptLanguage">groovy</stringProp>
          <stringProp name="script">import org.apache.http.client.methods.*
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.*
import org.apache.http.util.EntityUtils
import groovy.json.JsonSlurper

def client = HttpClients.createDefault()
def request = new HttpPost("http://localhost:8086/auth/login")
request.setHeader("Content-Type", "application/json")
request.setEntity(new StringEntity('{"username":"cxk","password":"1234"}', "UTF-8"))

def response = client.execute(request)
def responseCode = response.getStatusLine().getStatusCode()
def responseBody = EntityUtils.toString(response.getEntity(), "UTF-8")
log.info("Login response: " + responseCode + " - " + responseBody)

def json = new JsonSlurper().parseText(responseBody)
if (responseCode == 200 && json.token) {
    vars.put("authToken", json.token)
    log.info("Token saved successfully!")
} else {
    log.error("Login failed: " + responseBody)
}
return "Status: " + responseCode</stringProp>
        </JSR223Sampler>
        <hashTree/>
      </hashTree>
      <ResultCollector guiclass="StatVisualizer" testclass="ResultCollector" testname="聚合报告" enabled="true">
        <boolProp name="ResultCollector.error_logging">false</boolProp>
      </ResultCollector>
      <hashTree/>
    </hashTree>
  </hashTree>
</jmeterTestPlan>'''
    
    jmx_path = os.path.join(RESULT_DIR, "login-test.jmx")
    with open(jmx_path, 'w', encoding='utf-8') as f:
        f.write(jmx_content)
    print(f"已创建: {jmx_path}")
    return jmx_path

def create_upload_test(token):
    """创建上传测试 JMX"""
    jmx_content = f'''<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.4.1">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="上传测试" enabled="true">
      <boolProp name="TestPlan.serialize_threadgroups">true</boolProp>
    </TestPlan>
    <hashTree>
      <ConfigTestElement guiclass="HttpDefaultsGui" testclass="ConfigTestElement" testname="HTTP Request Defaults" enabled="true">
        <stringProp name="HTTPSampler.domain">localhost</stringProp>
        <stringProp name="HTTPSampler.port">8086</stringProp>
        <stringProp name="HTTPSampler.protocol">http</stringProp>
        <stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>
      </ConfigTestElement>
      <hashTree/>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="上传线程组" enabled="true">
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControlPanel" testclass="LoopController" testname="循环" enabled="true">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">5</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">3</stringProp>
        <stringProp name="ThreadGroup.ramp_time">3</stringProp>
      </ThreadGroup>
      <hashTree>
        <CSVDataSet guiclass="TestBeanGUI" testclass="CSVDataSet" testname="CSV Data Set Config" enabled="true">
          <stringProp name="delimiter">,</stringProp>
          <stringProp name="fileEncoding">UTF-8</stringProp>
          <stringProp name="filename">E:/后端/项目练习/smartPark/load-test/bill-files.csv</stringProp>
          <boolProp name="ignoreFirstLine">true</boolProp>
          <boolProp name="recycle">true</boolProp>
          <stringProp name="shareMode">shareMode.all</stringProp>
          <stringProp name="variableNames">file_path,file_name,file_size_kb,row_count</stringProp>
        </CSVDataSet>
        <hashTree/>
        <JSR223Sampler guiclass="TestBeanGUI" testclass="JSR223Sampler" testname="JSR223-上传Excel文件" enabled="true">
          <stringProp name="cacheKey">upload</stringProp>
          <stringProp name="scriptLanguage">groovy</stringProp>
          <stringProp name="script">import org.apache.http.client.methods.*
import org.apache.http.entity.mime.*
import org.apache.http.entity.mime.content.*
import org.apache.http.impl.client.*
import org.apache.http.util.EntityUtils
import groovy.json.JsonSlurper

def token = "{token}"
def filePath = vars.get("file_path") ?: "E:/后端/项目练习/smartPark/load-test/test-files/bill-1k-50kb.xlsx"
def rowCount = vars.get("row_count") ?: "unknown"

log.info("Uploading: " + filePath + " (rows: " + rowCount + ")")

def client = HttpClients.createDefault()
def request = new HttpPost("http://localhost:8086/api/bill/import")
request.setHeader("token", token)

def file = new File(filePath)
def entity = MultipartEntityBuilder.create()
    .addBinaryBody("file", new FileInputStream(file), ContentType.create("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), file.getName())
    .build()
request.setEntity(entity)

def response = client.execute(request)
def responseCode = response.getStatusLine().getStatusCode()
def responseBody = EntityUtils.toString(response.getEntity(), "UTF-8")
log.info("Upload response: " + responseCode + " - " + responseBody)

if (responseCode == 200) {{
    def json = new JsonSlurper().parseText(responseBody)
    log.info("Task ID: " + json.data.taskId + ", Status: " + json.data.status)
}}

return "Upload " + rowCount + " rows: " + responseCode</stringProp>
        </JSR223Sampler>
        <hashTree/>
      </hashTree>
      <ResultCollector guiclass="StatVisualizer" testclass="ResultCollector" testname="聚合报告" enabled="true">
        <boolProp name="ResultCollector.error_logging">false</boolProp>
      </ResultCollector>
      <hashTree/>
    </hashTree>
  </hashTree>
</jmeterTestPlan>'''
    
    jmx_path = os.path.join(RESULT_DIR, "upload-test.jmx")
    with open(jmx_path, 'w', encoding='utf-8') as f:
        f.write(jmx_content)
    print(f"已创建: {jmx_path}")
    return jmx_path

if __name__ == "__main__":
    print("=" * 60)
    print("物业账单导入 JMeter 压测脚本")
    print("=" * 60)
    
    # Step 1: 登录测试
    print("\n[Step 1] 创建并运行登录测试...")
    login_jmx = create_login_test()
    success = run_jmx("login-test.jmx", "登录获取Token")
    
    if success:
        # 读取 Token
        token = "placeholder_token"
        print("\n请查看 login-test-result.jtl 文件获取 Token，或直接使用下面的方式手动测试:")
        print("python -c \"import urllib.request,json; d=json.dumps({'username':'cxk','password':'1234'}).encode(); r=urllib.request.Request('http://localhost:8086/auth/login',data=d,headers={'Content-Type':'application/json'}); print(urllib.request.urlopen(r).read().decode())\"")
        
        # Step 2: 上传测试
        print("\n[Step 2] 创建上传测试...")
        upload_jmx = create_upload_test(token)
        run_jmx("upload-test.jmx", "Excel上传压测")
    
    print("\n" + "=" * 60)
    print("压测完成！")
    print("=" * 60)