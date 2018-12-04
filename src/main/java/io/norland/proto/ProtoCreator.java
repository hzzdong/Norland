package io.norland.proto;

import io.norland.annotations.Proto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ProtoCreator {
    private static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";
    private String resourcePattern = DEFAULT_RESOURCE_PATTERN;


    private static ProtoCreator creator;

    private static Map<String, Class<?>> classMap;

    /**
     * 消除Jvm加载jar包时导致的class内存地址不一致问题
     * 必须由引用方传入一个class,并以此获取classLoader
     */
    private ProtoCreator(Class<?> clazz) {
        ClassLoader loader = clazz.getClassLoader();
        String[] optPaths = getOptPaths(clazz);
        for (String path : optPaths) {
            scanPackageForProtocols(path, loader);
            if (!classMap.isEmpty()) {
                return;
            }
        }
        if (classMap.isEmpty()) {
            throw new RuntimeException("in path " + optPathsToString(optPaths)
                    + " don't find @proro class");
        }
    }

    private ProtoCreator(ClassLoader loader, String[] optPaths) {
        for (String path : optPaths) {
            scanPackageForProtocols(path, loader);
        }
        if (classMap.isEmpty()) {
            throw new RuntimeException("in path " + optPathsToString(optPaths)
                    + " don't find @proro class");
        }
    }

    private String optPathsToString(String[] optPaths) {
        StringBuilder sb = new StringBuilder();
        for (String path : optPaths) {
            sb.append("[")
                    .append(path)
                    .append("] ");
        }
        return sb.toString();
    }

    public Object create(String protoName) {
        Class<?> clazz = classMap.get(protoName);
        if (clazz != null) {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    private void scanPackageForProtocols(String protoPath, ClassLoader loader) {
        classMap = new HashMap<>();
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                resolveBasePackage(protoPath) + '/' + this.resourcePattern;
        try {
            Resource[] resources =
                    getResourcePatternResolver()
                            .getResources(packageSearchPath);
            MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(
                    loader);
            for (Resource resource : resources) {
                if (log.isDebugEnabled()) {
                    log.info("Scanning " + resource);
                }
                Class<?> clazz = loadClass(loader, metadataReaderFactory, resource);
                if (clazz == null)
                    continue;
                int mf = clazz.getModifiers();
                if (!Modifier.isInterface(mf) && !Modifier.isAbstract(mf)) {
                    Proto proto = clazz.getAnnotation(Proto.class);
                    if (proto != null) {
                        String protoName = proto.value();
                        if (!protoName.isEmpty()) {
                            classMap.put(protoName, clazz);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Class<?> loadClass(ClassLoader loader, MetadataReaderFactory readerFactory,
                               Resource resource) {
        try {
            MetadataReader reader = readerFactory.getMetadataReader(resource);
            return ClassUtils.forName(reader.getClassMetadata().getClassName(), loader);
        } catch (ClassNotFoundException | LinkageError ex) {
            handleFailure(resource, ex);
            return null;
        } catch (Throwable ex) {
            if (log.isWarnEnabled()) {
                log.warn(
                        "Unexpected failure when loading class resource " + resource, ex);
            }
            return null;
        }
    }

    private void handleFailure(Resource resource, Throwable ex) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Ignoring candidate class resource " + resource + " due to " + ex);
        }
    }

    private String resolveBasePackage(String basePackage) {
        return ClassUtils.convertClassNameToResourcePath(getEnvironment().resolveRequiredPlaceholders(basePackage));
    }

    private Environment getEnvironment() {
        return new StandardEnvironment();
    }

    private ResourcePatternResolver getResourcePatternResolver() {
        return new PathMatchingResourcePatternResolver();
    }

    /**
     * 这里必须从外部传递一个classLoader
     * 否则就会造成class的不一致现象
     */
    public static ProtoCreator getInstance(@NonNull Class<?> clazz) {
        if (creator == null)
            synchronized (ProtoCreator.class) {
                if (creator == null) {
                    creator = new ProtoCreator(clazz);
                }
            }
        return creator;
    }

    public static ProtoCreator getInstance(ClassLoader loader, String[] optPaths) {
        if (creator == null)
            synchronized (ProtoCreator.class) {
                if (creator == null) {
                    creator = new ProtoCreator(loader, optPaths);
                }
            }
        return creator;
    }

    /**
     * 包名的一级和二级目录
     * 例:io.norland.proto
     * optPaths=[io,io.norland]
     */
    private String[] getOptPaths(Class<?> clazz) {
        String pkgName = clazz.getPackage().getName();
        String[] pkgNameItems = pkgName.split("[.]");
        String[] opts;
        if (pkgNameItems.length >= 2) {
            opts = new String[2];
            opts[0] = pkgNameItems[0] + "." + pkgNameItems[1];
            opts[1] = pkgNameItems[0];
        } else {
            opts = new String[1];
            opts[0] = pkgNameItems[0];
        }
        return opts;
    }

    public static String getProtoName(Object protoObject) {
        Proto proto = protoObject.getClass().getAnnotation(Proto.class);
        if (proto == null)
            return null;
        return proto.value();
    }
}
