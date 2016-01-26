package io.clickhandler.action.compiler;

import com.squareup.javapoet.ClassName;
import io.clickhandler.action.*;

import javax.lang.model.element.TypeElement;

/**
 *
 */
public class ActionHolder {
    RemoteAction remoteAction;
    QueueAction queueAction;
    InternalAction internalAction;
    ActionConfig config;
    TypeElement type;
    DeclaredTypeVar inType;
    DeclaredTypeVar outType;

    String pkgName = null;

    public boolean isRemote() {
        return remoteAction != null;
    }

    public boolean isQueue() {
        return queueAction != null;
    }

    public boolean isInternal() {
        return internalAction != null;
    }

    public ClassName getProviderTypeName() {
        Class providerClass = null;
        if (config != null) {
            providerClass = config.provider();
        }

        if (providerClass != null && !providerClass.equals(ActionProvider.class)) {
            return ClassName.get(providerClass);
        }

        if (isRemote()) {
            return ClassName.get(RemoteActionProvider.class);
        } else if (isInternal()) {
            return ClassName.get(InternalActionProvider.class);
        } else if (isQueue()) {
            return ClassName.get(QueueActionProvider.class);
        } else {
            return ClassName.get(ActionProvider.class);
        }
    }

    public String getName() {
        return type.getQualifiedName().toString();
    }

    public String getFieldName() {
        final String name = type.getSimpleName().toString();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    public String getPackage() {
        if (pkgName != null) {
            return pkgName;
        }

        final String qname = type.getQualifiedName().toString();

        final String[] parts = qname.split("[.]");
        if (parts.length == 1) {
            return pkgName = "";
        } else {
            final String lastPart = parts[parts.length - 1];
            return pkgName = qname.substring(0, qname.length() - lastPart.length() - 1);
        }
    }
}
