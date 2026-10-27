$junit = (Get-ChildItem -Path 'C:\Users\krish\.gradle\caches\modules-2\files-2.1\junit\junit\4.13.2' -Filter '*.jar' -Recurse | Select-Object -First 1).FullName
$hamcrest = (Get-ChildItem -Path 'C:\Users\krish\.gradle\caches\modules-2\files-2.1\org.hamcrest\hamcrest-core\1.3' -Filter '*.jar' -Recurse | Select-Object -First 1).FullName
$kotlin = (Get-ChildItem -Path 'C:\Users\krish\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib\2.0.21' -Filter '*.jar' -Recurse | Select-Object -First 1).FullName
$androidJar = 'C:\Users\krish\AppData\Local\Android\Sdk\platforms\android-35\android.jar'
$classes = 'C:\Users\krish\Downloads\ideathon\app\build\tmp\kotlin-classes\debug;C:\Users\krish\Downloads\ideathon\app\build\tmp\kotlin-classes\debugUnitTest;C:\Users\krish\Downloads\ideathon\app\build\intermediates\javac\debug\compileDebugJavaWithJavac\classes'

$cp = "$junit;$hamcrest;$kotlin;$androidJar;$classes"
Write-Host "Running SwasthAI Unit Tests..."
& "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" -cp $cp org.junit.runner.JUnitCore com.swasthai.app.DocumentChunkerTest com.swasthai.app.EmbeddingEngineTest com.swasthai.app.EmergencyTriageTest com.swasthai.app.LfmPromptBuilderTest com.swasthai.app.ClinicalKnowledgeEngineTest com.swasthai.app.PdfExtractorTest com.swasthai.app.ChatRegressionAndRagTest
