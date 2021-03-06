package move.action.processor;

import static javax.tools.StandardLocation.SOURCE_OUTPUT;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import move.action.ActionLocator;
import move.action.ActionProducer;
import move.action.ActionProvider;
import move.action.Actor;
import move.action.ActorProducer;
import move.action.ActorProvider;
import move.action.Daemon;
import move.action.DaemonProducer;
import move.action.DaemonProvider;
import move.action.Http;
import move.action.HttpActionProducer;
import move.action.HttpActionProvider;
import move.action.IJobAction;
import move.action.Internal;
import move.action.InternalActionProducer;
import move.action.InternalActionProducerWithBuilder;
import move.action.InternalActionProvider;
import move.action.Worker;
import move.action.WorkerActionProducer;
import move.action.WorkerActionProducerWithBuilder;
import move.action.WorkerActionProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Action annotation processor.
 *
 * @author Clay Molocznik
 */
@AutoService(Processor.class)
public class ActionProcessor extends AbstractProcessor {

  static final String LOCATOR_SUFFIX = "_Locator";
  static final String LOCATOR_ROOT_SUFFIX = "_LocatorRoot";
  static final String MODULE_SUFFIX = "_Module";
  static final String ROOT_MODULE_NAME = "Move_GeneratedModule";
  static final String PRODUCER_SUFFIX = "_Producer";
  static final String PROVIDER_SUFFIX = "_Provider";
  static final ParameterizedTypeName ACTION_PROVIDER_NAME = ParameterizedTypeName.get(
      ClassName.bestGuess("move.action.ActionProvider"),
      WildcardTypeName.subtypeOf(TypeName.OBJECT),
      WildcardTypeName.subtypeOf(TypeName.OBJECT),
      WildcardTypeName.subtypeOf(TypeName.OBJECT)
  );
  static final ParameterizedTypeName ACTION_PRODUCER_NAME = ParameterizedTypeName.get(
      ClassName.bestGuess("move.action.ActionProducer"),
      WildcardTypeName.subtypeOf(TypeName.OBJECT),
      WildcardTypeName.subtypeOf(TypeName.OBJECT),
      WildcardTypeName.subtypeOf(TypeName.OBJECT),
      WildcardTypeName.subtypeOf(TypeName.OBJECT)
  );
  static final ParameterizedTypeName ACTOR_PROVIDER_NAME = ParameterizedTypeName.get(
      ClassName.bestGuess("move.action.ActorProvider"),
      WildcardTypeName.subtypeOf(TypeName.OBJECT)
  );
  static final ParameterizedTypeName ACTOR_PRODUCER_NAME = ParameterizedTypeName.get(
      ClassName.bestGuess("move.action.ActorProducer"),
      WildcardTypeName.subtypeOf(TypeName.OBJECT),
      WildcardTypeName.subtypeOf(TypeName.OBJECT)
  );
  static ClassName VERTX_CLASSNAME = ClassName.bestGuess(
      "io.vertx.rxjava.core.Vertx"
  );

  final TreeMap<String, Holder> actionMap = new TreeMap<>();
  final ActionPackage rootPackage = new ActionPackage("Action", "");
  Types typeUtils;
  Elements elementUtils;
  Filer filer;
  Messager messager;

  static String packageName(String name) {
    final String[] parts = name.split("[.]");
    if (parts.length == 1) {
      return "";
    } else {
      final String lastPart = parts[parts.length - 1];
      return name.substring(0, name.length() - lastPart.length() - 1);
    }
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
  }

  /**
   * @return
   */
  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  /**
   * @return
   */
  @Override
  public Set<String> getSupportedAnnotationTypes() {
    final Set<String> annotataions = new LinkedHashSet<>();
    annotataions.add(Internal.class.getCanonicalName());
    annotataions.add(Worker.class.getCanonicalName());
    annotataions.add(Http.class.getCanonicalName());
    annotataions.add(Actor.class.getCanonicalName());
    annotataions.add(Daemon.class.getCanonicalName());
    return annotataions;
  }

  /**
   * @param annotations
   * @param roundEnv
   * @return
   */
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      final Set<? extends Element> internalElements = roundEnv
          .getElementsAnnotatedWith(Internal.class);
      final Set<? extends Element> workerElements = roundEnv
          .getElementsAnnotatedWith(Worker.class);
      final Set<? extends Element> httpElements = roundEnv
          .getElementsAnnotatedWith(Http.class);
      final Set<? extends Element> actorElements = roundEnv
          .getElementsAnnotatedWith(Actor.class);
      final Set<? extends Element> daemonElements = roundEnv
          .getElementsAnnotatedWith(Daemon.class);

      final HashSet<Element> elements = new HashSet<>();

      if (internalElements != null) {
        elements.addAll(internalElements);
      }
      if (workerElements != null) {
        elements.addAll(workerElements);
      }
      if (httpElements != null) {
        elements.addAll(httpElements);
      }
      if (actorElements != null) {
        elements.addAll(actorElements);
      }
      if (daemonElements != null) {
        elements.addAll(daemonElements);
      }

      for (Element annotatedElement : elements) {
        final Internal internal = annotatedElement
            .getAnnotation(Internal.class);
        final Worker worker = annotatedElement
            .getAnnotation(Worker.class);
        final Http http = annotatedElement
            .getAnnotation(Http.class);
        final Actor actor = annotatedElement
            .getAnnotation(Actor.class);
        final Daemon daemon = annotatedElement
            .getAnnotation(Daemon.class);

        final TypeElement element = elementUtils.getTypeElement(annotatedElement.toString());

        int actionAnnotationCount = 0;
        boolean isAction = false;
        boolean isActor = false;

        if (internal != null) {
          isAction = true;
          actionAnnotationCount++;
        }
        if (worker != null) {
          isAction = true;
          actionAnnotationCount++;
        }
        if (http != null) {
          isAction = true;
          actionAnnotationCount++;
        }
        if (actor != null) {
          isActor = false;
          actionAnnotationCount++;
        }
        if (daemon != null) {
          isActor = true;
          actionAnnotationCount++;
        }

        if (actionAnnotationCount > 1) {
          messager.printMessage(
              Diagnostic.Kind.ERROR,
              element.getQualifiedName() +
                  "  has multiple Action annotations. Only one of the following may be used... " +
                  "@Worker or @Internal or @Actor or @Daemon"
          );
          continue;
        }

//        final PackageElement packageElement = elementUtils.getPackageOf(element);

//        messager.printMessage(Kind.WARNING, element.getQualifiedName());

//        messager.printMessage(Kind.WARNING, "Action: " + element.getQualifiedName().toString());
//        messager.printMessage(Kind.WARNING, "Package: " + packageElement);

//        final FileObject fileObject = filer.getResource(
//            SOURCE_OUTPUT,
//            element.getQualifiedName()
//                .subSequence(
//                    0,
//                    element.getQualifiedName().length() - element.getSimpleName().length() - 1).toString().replace(".", "/"),
//            element.getSimpleName() + "_Module.java"
//        );
//
//
//
//        if (fileObject != null && fileObject.getLastModified() > 0L) {
//          messager.printMessage(Kind.WARNING, "Action already processed: " + fileObject.toUri().toString());
////          continue;
//        }

//        if (actionMap.containsKey(element.getQualifiedName().toString())) {
//          messager.printMessage(Kind.WARNING, "Already Processed: " + element.getQualifiedName().toString());
//          continue;
//        }

        final String qualifiedName = element.getQualifiedName().toString();

        if (isAction) {
          Object holderObj = actionMap.get(qualifiedName);

          if (holderObj instanceof ActorHolder) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                qualifiedName +
                    "  has multiple Action/Actor annotations. Only one of the following may be used... "
                    +
                    "@Worker or @Internal or @Http or @Actor or @Daemon"
            );
            continue;
          }

          ActionHolder holder = holderObj != null ? (ActionHolder) holderObj : null;

          if (holder == null) {
            final TypeParameterResolver typeParamResolver = resolve(element);

            DeclaredTypeVar requestType = null;
            try {
              requestType = typeParamResolver.resolve(IJobAction.class, 0);
            } catch (Throwable e) {
              messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }

            DeclaredTypeVar responseType = null;
            try {
              responseType = typeParamResolver.resolve(IJobAction.class, 1);
            } catch (Throwable e) {
              messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }

            holder = new ActionHolder(element, requestType, responseType, internal, worker, http);
          }

          // Make sure it's concrete.
          if (element.getModifiers().contains(Modifier.ABSTRACT)
              || (element.getTypeParameters() != null && !element.getTypeParameters().isEmpty())) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "@Remote was placed on a non-concrete class " +
                    element.getQualifiedName() +
                    " It cannot be abstract or have TypeParameters."
            );
          }

          actionMap.put(element.getQualifiedName().toString(), holder);
        } else if (isActor) {
          Object holderObj = actionMap.get(qualifiedName);

          if (holderObj instanceof ActionHolder) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                qualifiedName +
                    "  has multiple Action/Actor annotations. Only one of the following may be used... "
                    +
                    "@Worker or @Internal or @Http or @Actor or @Daemon"
            );
            continue;
          }

          ActorHolder holder = holderObj != null ? (ActorHolder) holderObj : null;

          if (holder == null) {
            holder = new ActorHolder(element, actor, daemon);
          }

          // Make sure it's concrete.
          if (element.getModifiers().contains(Modifier.ABSTRACT)) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "@Actor or @Deamon was placed on a non-concrete class " +
                    qualifiedName +
                    " It cannot be abstract."
            );
          }

          actionMap.put(element.getQualifiedName().toString(), holder);
        }
      }

      // Build Packages.
      for (Holder holder : actionMap.values()) {
        String packageName = holder.getPackageName();

        // Split package parts down.
        final String[] parts = packageName.split("[.]");
        final String firstName = parts[0];

        // Is it a Root Action?
        if (parts.length == 1 && firstName.isEmpty()) {
          if (holder instanceof ActionHolder) {
            rootPackage.actions.put(holder.getName(), (ActionHolder) holder);
          } else if (holder instanceof ActorHolder) {
            rootPackage.actors.put(holder.getName(), (ActorHolder) holder);
          }
        } else {
          // Let's find it's Package and construct the tree as needed during the process.
          ActionPackage parent = rootPackage;
          for (int i = 0; i < parts.length; i++) {
            final String nextName = parts[i];
            ActionPackage next = parent.children.get(nextName);
            if (next == null) {
              next = new ActionPackage(nextName, parent.path == null || parent.path.isEmpty()
                  ? nextName
                  : parent.path + "." + nextName, parent);
              parent.children.put(nextName, next);
            }
            parent = next;
          }

          // Add Descriptor.
          if (holder instanceof ActionHolder) {
            parent.actions.put(holder.getName(), (ActionHolder) holder);
          } else if (holder instanceof ActorHolder) {
            parent.actors.put(holder.getName(), (ActorHolder) holder);
          }
        }
      }

      rootPackage.generate();
    } catch (Throwable e) {
      e.printStackTrace();
      error(null, e.getMessage());
    }

    return true;
  }

  /**
   * Prints an error message
   *
   * @param e The element which has caused the error. Can be null
   * @param msg The error message
   */
  public void error(Element e, String msg) {
    messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
  }

  TypeParameterResolver resolve(TypeElement element) {
    final TypeParameterResolver resolver = new TypeParameterResolver(element);
    resolver.resolve();
    return resolver;
  }

  interface Holder {

    String getName();

    String getPackageName();
  }

  public static class ActorHolder implements Holder {

    final Actor actor;
    final Daemon daemon;
    final ClassName className;
    final TypeElement type;
    final TypeName typeName;
    final ClassName providerClassName;
    final ParameterizedTypeName providerTypeName;
    final String name;
    final String simpleName;
    final String fieldName;
    final String packageName;
    final String moduleName;
    final String generatedProviderName;
    final ClassName generatedProviderClassName;
    final boolean hasParameterlessCtor;
    final boolean hasInjectCtor;
    final boolean hasFieldsInject;
    final ClassName producerClassName;
    final ParameterizedTypeName producerTypeName;
    final String generatedProducerName;
    final ClassName generatedProducerClassName;
    boolean generated;

    public ActorHolder(
        TypeElement type,
        Actor actor,
        Daemon daemon) {
      this.type = type;
      this.actor = actor;
      this.daemon = daemon;
      this.className = ClassName.get(type);
      this.name = type.getQualifiedName().toString();
      this.simpleName = type.getSimpleName().toString();
      final String f = type.getSimpleName().toString();
      this.fieldName = Character.toLowerCase(f.charAt(0)) + f.substring(1);
      this.moduleName = simpleName + MODULE_SUFFIX;
      this.generatedProviderName = simpleName + PROVIDER_SUFFIX;
      this.generatedProducerName = simpleName + PRODUCER_SUFFIX;

      typeName = ClassName.get(type);

      boolean parameterlessCtor = false;
      boolean hasInjectCtor = false;
      boolean hasFieldsInject = false;

      for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
        if (field.getAnnotation(Inject.class) != null) {
          hasFieldsInject = true;
          break;
        }
      }
      this.hasFieldsInject = hasFieldsInject;

      for (ExecutableElement cons :
          ElementFilter.constructorsIn(type.getEnclosedElements())) {
        if (cons.getParameters() == null || cons.getParameters().isEmpty()) {
          parameterlessCtor = true;
        }

        if (cons.getAnnotation(Inject.class) != null) {
          hasInjectCtor = true;
        }
      }

      this.hasParameterlessCtor = parameterlessCtor;
      this.hasInjectCtor = hasInjectCtor;
      this.packageName = packageName(name);

      generatedProviderClassName = ClassName.bestGuess(packageName + "." + generatedProviderName);
      generatedProducerClassName = ClassName.bestGuess(packageName + "." + generatedProducerName);

      if (daemon != null) {
        providerClassName = ClassName.get(DaemonProvider.class);
        producerClassName = ClassName.get(DaemonProducer.class);

        // Provider Type.
        providerTypeName = ParameterizedTypeName.get(
            providerClassName,
            className
        );

        // Producer Type.
        producerTypeName = ParameterizedTypeName.get(
            producerClassName,
            className,
            generatedProviderClassName
        );
      } else {
        providerClassName = ClassName.get(ActorProvider.class);
        producerClassName = ClassName.get(ActorProducer.class);

        // Provider Type.
        providerTypeName = ParameterizedTypeName.get(
            providerClassName,
            className
        );

        // Producer Type.
        producerTypeName = ParameterizedTypeName.get(
            producerClassName,
            className,
            generatedProviderClassName
        );
      }
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getPackageName() {
      return packageName;
    }
  }

  /**
   *
   */
  public static class ActionHolder implements Holder {

    // Configure Remote options
    final Internal internal;
    final Worker worker;
    final Http http;
    final ClassName className;
    final TypeElement type;
    final TypeName typeName;
    final ClassName providerClassName;
    final ParameterizedTypeName providerTypeName;
    final String name;
    final String simpleName;
    final String fieldName;
    final String packageName;
    final String moduleName;
    final String generatedProviderName;
    final ClassName generatedProviderClassName;
    final DeclaredTypeVar requestType;
    final ClassName requestTypeName;
    final DeclaredTypeVar replyType;
    final ClassName replyTypeName;
    final boolean hasParameterlessCtor;
    final boolean hasInjectCtor;
    final boolean hasFieldsInject;
    final ClassName producerClassName;
    final ParameterizedTypeName producerTypeName;
    final String generatedProducerName;
    final ClassName generatedProducerClassName;
    boolean requestParameterlessCtor;
    boolean generated;

    public ActionHolder(TypeElement type,
        DeclaredTypeVar requestType,
        DeclaredTypeVar replyType,
        Internal internal,
        Worker worker,
        Http http) {
      this.type = type;
      this.requestType = requestType;
      this.replyType = replyType;
      this.internal = internal;
      this.worker = worker;
      this.http = http;
      this.className = ClassName.get(type);
      this.name = type.getQualifiedName().toString();
      this.simpleName = type.getSimpleName().toString();
      final String f = type.getSimpleName().toString();
      this.fieldName = Character.toLowerCase(f.charAt(0)) + f.substring(1);
      this.moduleName = simpleName + MODULE_SUFFIX;
      this.generatedProviderName = simpleName + PROVIDER_SUFFIX;
      this.generatedProducerName = simpleName + PRODUCER_SUFFIX;

      typeName = ClassName.get(type);

      boolean parameterlessCtor = false;
      boolean hasInjectCtor = false;
      boolean hasFieldsInject = false;

      for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
        if (field.getAnnotation(Inject.class) != null) {
          hasFieldsInject = true;
          break;
        }
      }
      this.hasFieldsInject = hasFieldsInject;

      for (ExecutableElement cons :
          ElementFilter.constructorsIn(type.getEnclosedElements())) {
        if (cons.getParameters() == null || cons.getParameters().isEmpty()) {
          parameterlessCtor = true;
        }

        if (cons.getAnnotation(Inject.class) != null) {
          hasInjectCtor = true;
        }
      }

      try {
        for (ExecutableElement cons :
            ElementFilter.constructorsIn(requestType.getResolvedElement().getEnclosedElements())) {
          if (cons.getParameters() == null || cons.getParameters().isEmpty()) {
            requestParameterlessCtor = true;
          }
        }
      } catch (Throwable e) {
        // Ignore.
        requestParameterlessCtor = false;
      }

      this.hasParameterlessCtor = parameterlessCtor;
      this.hasInjectCtor = hasInjectCtor;
      this.packageName = packageName(name);

      // Get Action IN resolved name.
      this.requestTypeName = ClassName.get(requestType.getResolvedElement());
      // Get Action OUT resolved name.
      this.replyTypeName = ClassName.get(replyType.getResolvedElement());

      generatedProviderClassName = ClassName.bestGuess(packageName + "." + generatedProviderName);
      generatedProducerClassName = ClassName.bestGuess(packageName + "." + generatedProducerName);

      if (isInternal()) {
        providerClassName = ClassName.get(InternalActionProvider.class);
        if (requestParameterlessCtor) {
          producerClassName = ClassName.get(InternalActionProducerWithBuilder.class);
        } else {
          producerClassName = ClassName.get(InternalActionProducer.class);
        }

        // Provider Type.
        providerTypeName = ParameterizedTypeName.get(
            providerClassName,
            className,
            requestTypeName,
            replyTypeName
        );

        // Producer Type.
        producerTypeName = ParameterizedTypeName.get(
            producerClassName,
            className,
            requestTypeName,
            replyTypeName,
            generatedProviderClassName
        );
      } else if (isWorker()) {
        providerClassName = ClassName.get(WorkerActionProvider.class);
        if (requestParameterlessCtor) {
          producerClassName = ClassName.get(WorkerActionProducerWithBuilder.class);
        } else {
          producerClassName = ClassName.get(WorkerActionProducer.class);
        }

        // Provider Type.
        providerTypeName = ParameterizedTypeName.get(
            providerClassName,
            className,
            requestTypeName,
            replyTypeName
        );

        // Producer Type.
        producerTypeName = ParameterizedTypeName.get(
            producerClassName,
            className,
            requestTypeName,
            replyTypeName,
            generatedProviderClassName
        );
      } else if (isHttp()) {
        providerClassName = ClassName.get(HttpActionProvider.class);
        producerClassName = ClassName.get(HttpActionProducer.class);

        // Provider Type.
        providerTypeName = ParameterizedTypeName.get(
            providerClassName,
            className
        );

        // Producer Type.
        producerTypeName = ParameterizedTypeName.get(
            producerClassName,
            className,
            generatedProviderClassName
        );
      } else {
        providerClassName = ClassName.get(ActionProvider.class);
        producerClassName = ClassName.get(ActionProducer.class);

        // Provider Type.
        providerTypeName = ParameterizedTypeName.get(
            providerClassName,
            className,
            requestTypeName,
            replyTypeName
        );

        // Producer Type.
        producerTypeName = ParameterizedTypeName.get(
            producerClassName,
            className,
            requestTypeName,
            replyTypeName,
            generatedProviderClassName
        );
      }
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getPackageName() {
      return packageName;
    }

    boolean isInternal() {
      return internal != null;
    }

    boolean isWorker() {
      return worker != null;
    }

    boolean isHttp() {
      return http != null;
    }
  }

  /**
   * Resolves a single DECLARED Type Variable.
   * <p/>
   * Searches down the superclass, interface, and superinterface paths.
   *
   * @author Clay Molocznik
   */
  public static class DeclaredTypeVar {

    private final Map<String, ResolvedTypeVar> resolvedMap = new HashMap<>();
    private DeclaredType type;
    private TypeElement element;
    private int index;
    private String varName;
    private TypeMirror varType;
    private TypeElement varTypeElement;

    public DeclaredTypeVar(DeclaredType type, TypeElement element, int index, String varName,
        TypeMirror varType) {
      this.type = type;
      this.element = element;
      this.index = index;
      this.varName = varName;
      this.varType = varType;

      if (varType instanceof DeclaredType) {
        this.varTypeElement = (TypeElement) ((DeclaredType) varType).asElement();
      }
    }

    public DeclaredType getType() {
      return type;
    }

    public TypeElement getElement() {
      return element;
    }

    public int getIndex() {
      return index;
    }

    public String getVarName() {
      return varName;
    }

    public TypeMirror getResolvedType() {
      return varType;
    }

    public TypeElement getResolvedElement() {
      return varTypeElement;
    }

    public ResolvedTypeVar getResolved(String name) {
      return resolvedMap.get(name);
    }

    public void resolve() {
      // Find 'varName' on the SuperClass or any Interfaces.
      final TypeMirror superClass = element.getSuperclass();

      if (superClass != null) {
        search(varName, superClass);
      }
    }

    private void search(String varName, TypeMirror mirror) {
      if (mirror == null || !(mirror instanceof DeclaredType)) {
        return;
      }

      final DeclaredType type = (DeclaredType) mirror;
      final TypeElement element = (TypeElement) type.asElement();
      final List<? extends TypeMirror> typeArgs = type.getTypeArguments();
      if (typeArgs == null || typeArgs.isEmpty()) {
        return;
      }

      for (int i = 0; i < typeArgs.size(); i++) {
        final TypeMirror typeArg = typeArgs.get(i);

        if (typeArg.getKind() == TypeKind.TYPEVAR
            && typeArg.toString().equals(varName)) {
          // Found it.
          // Get TypeVariable name.
          varName = element.getTypeParameters().get(i).getSimpleName().toString();

          // Add to resolved map.
          resolvedMap.put(element.toString(), new ResolvedTypeVar(type, element, i, varName));

          // Go up to the SuperClass.
          search(varName, element.getSuperclass());

          final List<? extends TypeMirror> interfaces = element.getInterfaces();
          if (interfaces != null && !interfaces.isEmpty()) {
            for (TypeMirror iface : interfaces) {
              search(varName, iface);
            }
          }
        }
      }
    }
  }

  /**
   * @author Clay Molocznik
   */
  public static class ResolvedTypeVar {

    private DeclaredType type;
    private TypeElement element;
    private int index;
    private String varName;

    public ResolvedTypeVar(DeclaredType type, TypeElement element, int index, String varName) {
      this.type = type;
      this.element = element;
      this.index = index;
      this.varName = varName;
    }

    public DeclaredType getType() {
      return type;
    }

    public TypeElement getElement() {
      return element;
    }

    public int getIndex() {
      return index;
    }

    public String getVarName() {
      return varName;
    }
  }

  /**
   * @author Clay Molocznik
   */
  public class TypeParameterResolver {

    private final TypeElement element;
    private final List<DeclaredTypeVar> vars = new ArrayList<>();
    private boolean resolved;

    public TypeParameterResolver(TypeElement element) {
      this.element = element;
    }

    void resolve() {
      if (resolved) {
        return;
      }

      resolveDeclaredTypeVars(element.getSuperclass());

      final List<? extends TypeMirror> interfaces = element.getInterfaces();
      if (interfaces != null && !interfaces.isEmpty()) {
        for (TypeMirror iface : interfaces) {
          resolveDeclaredTypeVars(iface);
        }
      }

      resolved = true;
    }

    public DeclaredTypeVar resolve(Class cls, int typeVarIndex) {
      if (!resolved) {
        resolve();
      }

      for (DeclaredTypeVar var : vars) {
        ResolvedTypeVar actionTypeVar = var.getResolved(cls.getCanonicalName());

        if (actionTypeVar == null) {
          actionTypeVar = var.getResolved(cls.getName());
        }
        if (actionTypeVar != null) {
          if (actionTypeVar.getIndex() == typeVarIndex) {
            return var;
          }
        }
      }
      return null;
    }

    private void resolveDeclaredTypeVars(TypeMirror type) {
      if (type == null) {
        return;
      }

      if (type.getKind() != TypeKind.DECLARED) {
        return;
      }

      if (type.toString().equals(Object.class.getName())) {
        return;
      }

      if (!(type instanceof DeclaredType)) {
        return;
      }

      final DeclaredType declaredType = (DeclaredType) type;
      final TypeElement element = ((TypeElement) ((DeclaredType) type).asElement());

      if (element.getQualifiedName().toString().equals(Object.class.getName())) {
        return;
      }

      final List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
      if (typeArgs != null && !typeArgs.isEmpty()) {
        for (int i = 0; i < typeArgs.size(); i++) {
          final TypeMirror typeArg = typeArgs.get(i);

          if (typeArg.getKind() == TypeKind.DECLARED) {
            final List<? extends TypeParameterElement> typeParams =
                ((TypeElement) ((DeclaredType) type).asElement()).getTypeParameters();
            final String typeVarName = typeParams.get(i).getSimpleName().toString();

            final DeclaredTypeVar declaredTypeVar =
                new DeclaredTypeVar(declaredType, element, i, typeVarName, typeArg);
            declaredTypeVar.resolve();
            vars.add(declaredTypeVar);
          }
        }
      }

      resolveDeclaredTypeVars(element.getSuperclass());

      final List<? extends TypeMirror> interfaces = element.getInterfaces();
      if (interfaces != null && !interfaces.isEmpty()) {
        for (TypeMirror iface : interfaces) {
          resolveDeclaredTypeVars(iface);
        }
      }
    }
  }

  /**
   *
   */
  @SuppressWarnings("all")
  public class ActionPackage {

    final String name;
    final String simpleName;
    final String moduleSimpleName;
    final ActionPackage parent;
    final TreeMap<String, ActionPackage> children = new TreeMap<>();
    final TreeMap<String, ActionHolder> actions = new TreeMap<>();
    final TreeMap<String, ActorHolder> actors = new TreeMap<>();
    final boolean root;
    final ClassName moduleName;
    public String path;
    private boolean processed = false;

    public ActionPackage(String name, String path) {
      this(name, path, null);
    }

    public ActionPackage(String name, String path, ActionPackage parent) {
      this.root = parent == null;
      this.name = name;
      this.path = path;
      this.parent = parent;

      this.simpleName = capitalize(name);

      if (path == null || path.isEmpty()) {
        this.moduleSimpleName = ROOT_MODULE_NAME;
        this.moduleName = ClassName.bestGuess(moduleSimpleName);
      } else {
        this.moduleSimpleName = simpleName + MODULE_SUFFIX;
        this.moduleName = ClassName.bestGuess(path + "." + moduleSimpleName);
      }
    }

    public String getClassName() {
      return name == null || name.isEmpty()
          ? "Root"
          : Character.toUpperCase(name.charAt(0)) + name.substring(1) + (root
              ? LOCATOR_ROOT_SUFFIX
              : LOCATOR_SUFFIX);
    }

    String capitalize(String value) {
      if (value == null || value.isEmpty()) {
        return "";
      }

      if (value.length() == 1) {
        return value.toUpperCase();
      }

      return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    String existingPackageModuleSource() {
      try {
        final FileObject fileObject = filer.getResource(
            SOURCE_OUTPUT,
            path,
            moduleSimpleName + ".java"
        );
        final CharSequence content = fileObject.getCharContent(true);
        return content.toString();
      } catch (Throwable e) {
        return "";
      }
    }

    FileObject getModuleFile() {
      try {
        final FileObject fileObject = filer.getResource(
            SOURCE_OUTPUT,
            path,
            moduleSimpleName + ".java"
        );
        return fileObject;
      } catch (Throwable e) {
        return null;
      }
    }

    void generateSource() {
      if (processed) {
        return;
      }

      processed = true;

      // Generate action modules.
      actions.values().stream().filter(a -> !a.generated).forEach(action -> {
        action.generated = true;
        generateProvider(action);
        generateProducer(action);
        generateModule(action);
      });

      actors.values().stream().filter(a -> !a.generated).forEach(actor -> {
        actor.generated = true;
        generateProvider(actor);
        generateProducer(actor);
        generateModule(actor);
      });

      // Package module.
      final FileObject packageModuleSource = getModuleFile();
      final List<ClassName> classNames = new ArrayList<>();

      if (root) {
//        classNames.add(ClassName.get(MoveModule.class));
      }

      // Generate a new package module.
      final AnnotationSpec.Builder builder = AnnotationSpec.builder(Module.class);

      final CodeBlock.Builder codeBlock = CodeBlock.builder();
      codeBlock.beginControlFlow("");

      final List<ClassName> subModuleClasses = children.values().stream()
          .map(p -> p.moduleName)
          .collect(Collectors.toList());

      final List<ClassName> actorClasses = actors
          .values()
          .stream()
          .map(a -> ClassName.bestGuess(path + "." + a.moduleName)).collect(
              Collectors.toList());

      final List<ClassName> actionClasses = actions
          .values()
          .stream()
          .map(a -> ClassName.bestGuess(path + "." + a.moduleName)).collect(
              Collectors.toList());

      classNames.addAll(subModuleClasses);
      classNames.addAll(actorClasses);
      classNames.addAll(actionClasses);

      for (int i = 0; i < classNames.size(); i++) {
        codeBlock.add(i < classNames.size() - 1 ? "$L.class," : "$L.class",
            classNames.get(i).toString());
        codeBlock.add("");
      }

      codeBlock.endControlFlow();
      builder.addMember("includes", codeBlock.build());

      final TypeSpec.Builder typeBuilder = TypeSpec
          .classBuilder(moduleSimpleName)
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(builder.build());

      final JavaFile providerJavaFile = JavaFile.builder(path, typeBuilder.build()).build();

      try {
        // Write .java source code file.
        writeTo(providerJavaFile, packageModuleSource, filer);
      } catch (Throwable e) {
        // Ignore.
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate Source File: " + e.getMessage());
      }

      generateLocator();
    }

    public void writeTo(JavaFile file, FileObject fileObject, Filer filer) throws IOException {
      String fileName = file.packageName.isEmpty()
          ? file.typeSpec.name
          : file.packageName + "." + file.typeSpec.name;
      List<Element> originatingElements = file.typeSpec.originatingElements;

      if (fileObject.getLastModified() == 0) {
        JavaFileObject filerSourceFile = filer.createSourceFile(fileName,
            originatingElements.toArray(new Element[originatingElements.size()]));

        try (Writer writer = filerSourceFile.openWriter()) {
          file.writeTo(writer);
        } catch (Exception e) {
          try {
            filerSourceFile.delete();
          } catch (Exception ignored) {
          }
          throw e;
        }
      } else {
        try (StringWriter writer = new StringWriter()) {
          file.writeTo(writer);
          writer.flush();
          Files.write(Paths.get(fileObject.toUri()), writer.getBuffer().toString().getBytes(
              StandardCharsets.UTF_8),
              StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE,
              StandardOpenOption.CREATE);
        } catch (Exception e) {
          try {
            fileObject.delete();
          } catch (Exception ignored) {
          }
          throw e;
        }
      }
    }

    private void generateProvider(ActionHolder action) {
      final MethodSpec.Builder providerCtor = MethodSpec.constructorBuilder()
          .addAnnotation(Inject.class)
          .addParameter(ParameterSpec
              .builder(VERTX_CLASSNAME, "vertx").build())
          .addParameter(ParameterSpec
              .builder(
                  ParameterizedTypeName.get(ClassName.get(Provider.class), action.className),
                  "actionProvider").build())
          .addStatement("super(vertx, actionProvider)");

      final TypeSpec.Builder providerType = TypeSpec
          .classBuilder(action.generatedProviderName)
          .addModifiers(Modifier.PUBLIC)
          .superclass(action.providerTypeName)
          .addAnnotation(Singleton.class);

      providerType.addMethod(providerCtor.build());

      providerType.addMethod(MethodSpec.methodBuilder("getActionClass")
          .addAnnotation(NotNull.class)
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC)
          .returns(ParameterizedTypeName.get(ClassName.get(Class.class), action.className))
          .addStatement("return $T.class", action.className)
          .build());

      if (action.isInternal() || action.isWorker()) {
        providerType.addMethod(MethodSpec.methodBuilder("getRequestClass")
            .addAnnotation(NotNull.class)
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(ClassName.get(Class.class), action.requestTypeName))
            .addStatement("return $T.class", action.requestTypeName)
            .build());

        providerType.addMethod(MethodSpec.methodBuilder("getReplyClass")
            .addAnnotation(NotNull.class)
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(ClassName.get(Class.class), action.replyTypeName))
            .addStatement("return $T.class", action.replyTypeName)
            .build());
      }

      // Build java file.
      final JavaFile providerJavaFile = JavaFile.builder(path, providerType.build()).build();

      try {
        // Write .java source code file.
        providerJavaFile.writeTo(filer);
      } catch (Throwable e) {
        // Ignore.
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate Source File: " + e.getMessage());
      }
    }

    private void generateProducer(ActionHolder action) {
      final MethodSpec.Builder providerCtor = MethodSpec.constructorBuilder()
          .addAnnotation(Inject.class)
          .addModifiers(Modifier.PUBLIC);

      final TypeSpec.Builder producerType = TypeSpec
          .classBuilder(action.generatedProducerName)
          .addModifiers(Modifier.PUBLIC)
          .superclass(action.producerTypeName)
          .addAnnotation(Singleton.class);

      producerType.addMethod(providerCtor.build());

      producerType.addMethod(MethodSpec.methodBuilder("getActionClass")
          .addAnnotation(NotNull.class)
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC)
          .returns(ParameterizedTypeName.get(ClassName.get(Class.class), action.className))
          .addStatement("return $T.class", action.className)
          .build());

      if (action.requestParameterlessCtor) {
        producerType.addMethod(MethodSpec.methodBuilder("createRequest")
            .addAnnotation(NotNull.class)
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(action.requestTypeName)
            .addStatement("return new $T()", action.requestTypeName)
            .build());
      }

      // Build java file.
      final JavaFile providerJavaFile = JavaFile.builder(path, producerType.build()).build();

      try {
        // Write .java source code file.
        providerJavaFile.writeTo(filer);
      } catch (Throwable e) {
        // Ignore.
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate Source File: " + e.getMessage());
      }
    }

    private void generateModule(ActionHolder action) {
      final TypeSpec.Builder actionModuleType = TypeSpec
          .classBuilder(action.moduleName)
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(Module.class);

      if (action.hasParameterlessCtor && !action.hasInjectCtor) {
        final MethodSpec.Builder provideActionMethod = MethodSpec.methodBuilder("_1")
            .addAnnotation(Provides.class)
            .returns(action.className)
            .addStatement("return new $T()", action.className);
        actionModuleType.addMethod(provideActionMethod.build());
      }

      actionModuleType.addMethod(
          MethodSpec.methodBuilder("_2")
              .addAnnotation(Provides.class)
              .addAnnotation(IntoSet.class)
//          .addAnnotation(AnnotationSpec
//              .builder(ClassKey.class)
//              .addMember("value", CodeBlock
//                  .builder()
//                  .add("$T.class", action.type)
//                  .build())
//              .build())
              .returns(ACTION_PROVIDER_NAME)
              .addParameter(ParameterSpec
                  .builder(action.generatedProviderClassName, "provider")
                  .build()
              )
              .addStatement("return provider")
              .build());

      actionModuleType.addMethod(
          MethodSpec.methodBuilder("_3")
              .addAnnotation(Provides.class)
              .addAnnotation(IntoSet.class)
//          .addAnnotation(AnnotationSpec
//              .builder(ClassKey.class)
//              .addMember("value", CodeBlock
//                  .builder()
//                  .add("$T.class", action.generatedProducerClassName)
//                  .build())
//              .build())
              .returns(ACTION_PRODUCER_NAME)
              .addParameter(ParameterSpec
                  .builder(action.generatedProducerClassName, "producer")
                  .build()
              )
              .addStatement("return producer")
              .build());

      // Build java file.
      final JavaFile moduleFile = JavaFile.builder(path, actionModuleType.build())
          .build();

      try {
        // Write .java source code file.
        moduleFile.writeTo(filer);
      } catch (Throwable e) {
        // Ignore.
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate Source File: " + e.getMessage());
      }
    }


    private void generateProvider(ActorHolder actor) {
      final MethodSpec.Builder providerCtor = MethodSpec.constructorBuilder()
          .addAnnotation(Inject.class)
          .addParameter(ParameterSpec
              .builder(
                  ParameterizedTypeName.get(ClassName.get(Provider.class), actor.className),
                  "actorProvider").build())
          .addStatement("super(actorProvider)");

      final TypeSpec.Builder providerType = TypeSpec
          .classBuilder(actor.generatedProviderName)
          .addModifiers(Modifier.PUBLIC)
          .superclass(actor.providerTypeName)
          .addAnnotation(Singleton.class);

      providerType.addMethod(providerCtor.build());

      providerType.addMethod(MethodSpec.methodBuilder("getActorClass")
          .addAnnotation(NotNull.class)
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC)
          .returns(ParameterizedTypeName.get(ClassName.get(Class.class), actor.className))
          .addStatement("return $T.class", actor.className)
          .build());

      // Build java file.
      final JavaFile providerJavaFile = JavaFile.builder(path, providerType.build()).build();

      try {
        // Write .java source code file.
        providerJavaFile.writeTo(filer);
      } catch (Throwable e) {
        // Ignore.
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate Source File: " + e.getMessage());
      }
    }

    private void generateProducer(ActorHolder actor) {
      final MethodSpec.Builder providerCtor = MethodSpec.constructorBuilder()
          .addAnnotation(Inject.class)
          .addModifiers(Modifier.PUBLIC);

      final TypeSpec.Builder producerType = TypeSpec
          .classBuilder(actor.generatedProducerName)
          .addModifiers(Modifier.PUBLIC)
          .superclass(actor.producerTypeName)
          .addAnnotation(Singleton.class);

      producerType.addMethod(providerCtor.build());

      producerType.addMethod(MethodSpec.methodBuilder("getActorClass")
          .addAnnotation(NotNull.class)
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC)
          .returns(ParameterizedTypeName.get(ClassName.get(Class.class), actor.className))
          .addStatement("return $T.class", actor.className)
          .build());

      // Build java file.
      final JavaFile providerJavaFile = JavaFile.builder(path, producerType.build()).build();

      try {
        // Write .java source code file.
        providerJavaFile.writeTo(filer);
      } catch (Throwable e) {
        // Ignore.
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate Source File: " + e.getMessage());
      }
    }

    private void generateModule(ActorHolder actor) {
      final TypeSpec.Builder actorModuleType = TypeSpec
          .classBuilder(actor.moduleName)
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(Module.class);

      if (actor.hasParameterlessCtor && !actor.hasInjectCtor) {
        final MethodSpec.Builder provideActionMethod = MethodSpec.methodBuilder("_1")
            .addAnnotation(Provides.class)
            .returns(actor.className)
            .addStatement("return new $T()", actor.className);
        actorModuleType.addMethod(provideActionMethod.build());
      }

      actorModuleType.addMethod(
          MethodSpec.methodBuilder("_2")
              .addAnnotation(Provides.class)
              .addAnnotation(IntoSet.class)
              .returns(ACTOR_PROVIDER_NAME)
              .addParameter(ParameterSpec
                  .builder(actor.generatedProviderClassName, "provider")
                  .build()
              )
              .addStatement("return provider")
              .build());

      actorModuleType.addMethod(
          MethodSpec.methodBuilder("_3")
              .addAnnotation(Provides.class)
              .addAnnotation(IntoSet.class)
              .returns(ACTOR_PRODUCER_NAME)
              .addParameter(ParameterSpec
                  .builder(actor.generatedProducerClassName, "producer")
                  .build()
              )
              .addStatement("return producer")
              .build());

      // Build java file.
      final JavaFile moduleFile = JavaFile.builder(path, actorModuleType.build())
          .build();

      try {
        // Write .java source code file.
        moduleFile.writeTo(filer);
      } catch (Throwable e) {
        // Ignore.
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate Source File: " + e.getMessage());
      }
    }

    private void generateLocator() {
      // Build empty @Inject constructor.
      final MethodSpec.Builder ctor = MethodSpec.constructorBuilder()
          .addAnnotation(Inject.class);

      // Init Class.
      final TypeSpec.Builder type = TypeSpec.classBuilder(getClassName())
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .superclass(TypeName.get(ActionLocator.class))
          .addAnnotation(Singleton.class);

      // Generate SubPackage locators.
      for (ActionPackage childPackage : children.values()) {
        // Build type name.
        final TypeName typeName = ClassName.get(childPackage.path, childPackage.getClassName());

        // Add field.
        type.addField(
            FieldSpec.builder(typeName, childPackage.name)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build()
        );

        ctor.addParameter(
            ParameterSpec.builder(typeName, childPackage.name, Modifier.FINAL).build()
        );

        ctor.addStatement("this.$L = $L", childPackage.name, childPackage.name);

        // Add getter method.
        type.addMethod(
            MethodSpec.methodBuilder(childPackage.name)
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName)
                .addStatement("return " + childPackage.name)
                .build()
        );
      }

      // Go through all actions.
      actions.forEach((classPath, action) -> {
        type.addField(
            FieldSpec.builder(action.generatedProducerClassName, action.fieldName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build()
        );
        type.addField(
            FieldSpec.builder(action.generatedProducerClassName, capitalize(action.fieldName))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build()
        );

        ctor.addParameter(
            ParameterSpec
                .builder(action.generatedProducerClassName, action.fieldName, Modifier.FINAL)
                .build()
        );

        ctor.addStatement("this.$L = $L", action.fieldName, action.fieldName);
        ctor.addStatement("this.$L = $L", capitalize(action.fieldName), action.fieldName);

        type.addMethod(
            MethodSpec.methodBuilder(action.fieldName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(action.generatedProducerClassName)
                .addStatement("return " + action.fieldName)
                .build()
        );
      });

      // Go through all actions.
      actors.forEach((classPath, actor) -> {
        type.addField(
            FieldSpec.builder(actor.generatedProducerClassName, actor.fieldName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build()
        );
        type.addField(
            FieldSpec.builder(actor.generatedProducerClassName, capitalize(actor.fieldName))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build()
        );

        ctor.addParameter(
            ParameterSpec
                .builder(actor.generatedProducerClassName, actor.fieldName, Modifier.FINAL)
                .build()
        );

        ctor.addStatement("this.$L = $L", actor.fieldName, actor.fieldName);
        ctor.addStatement("this.$L = $L", capitalize(actor.fieldName), actor.fieldName);

        type.addMethod(
            MethodSpec.methodBuilder(actor.fieldName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(actor.generatedProducerClassName)
                .addStatement("return " + actor.fieldName)
                .build()
        );
      });

//      if (!actions.isEmpty()) {
//        try {
//          final FileObject fileObject = filer.getResource(
//              SOURCE_OUTPUT,
//              path,
//              getClassName() + ".java"
//          );
//
//          if (fileObject.getLastModified() > 0) {
//            messager.printMessage(Kind.WARNING, "ActionLocator already exists: " + getClassName());
//
//            for (ActionPackage childPackage : children.values()) {
//              childPackage.generate();
//            }
//          }
////          final CharSequence content = fileObject.getCharContent(true);
////          final String contents = content.toString();
////
////          if (!contents.isEmpty()) {
////            for (ActionHolder action : actions.values()) {
////              if (!contents.contains(action.providerClassName.simpleName())) {
////                messager.printMessage(Kind.WARNING, "ActionLocator: " +
////                    action.name +" was already processed");
//////                messager.printMessage(
//////                    Diagnostic.Kind.ERROR,
//////                    "Action: " +
//////                        action.name +
//////                        " was created. Full Regeneration needed. \"clean\" and \"compile\""
//////                );
////                return;
////              }
////            }
////
////            // Generate child packages.
////            for (ActionPackage childPackage : children.values()) {
////              childPackage.generate();
////            }
////
////            return;
////          }
//        } catch (Throwable e) {
//          // Ignore.
//        }
//      }
//
//      if (!children.isEmpty()) {
//        try {
//          final FileObject fileObject = filer.getResource(
//              SOURCE_OUTPUT,
//              path,
//              getClassName() + ".java"
//          );
//
//          if (fileObject.getLastModified() > 0) {
//            messager.printMessage(Kind.WARNING, "ActionLocator already exists: " + getClassName());
//
//            for (ActionPackage childPackage : children.values()) {
//              childPackage.generate();
//            }
//            return;
//          }
//
////          final CharSequence content = fileObject.getCharContent(true);
////          final String contents = content.toString();
////
////          if (!contents.isEmpty()) {
////            for (ActionPackage child : children.values()) {
////              if (!contents.contains(child.getClassName() + " " + child.name + ";")) {
////                messager.printMessage(Kind.WARNING, "ActionLocator: " +
////                    child.path + " was already processed");
//////                messager.printMessage(
//////                    Diagnostic.Kind.ERROR,
//////                    "ActionLocator: " +
//////                        child.path +
//////                        " was created. Full Regeneration needed. \"clean\" and \"compile\""
//////                );
////                return;
////              }
////            }
////
////            // Generate child packages.
////            for (ActionPackage childPackage : children.values()) {
////              childPackage.generate();
////            }
////
////            return;
////          }
//        } catch (Throwable e) {
//          // Ignore.
//        }
//      }

      type.addMethod(ctor.build());

      // Build java file.
      final JavaFile javaFile = JavaFile.builder(path, type.build()).build();

      try {
        // Write .java source code file.
        javaFile.writeTo(filer);
      } catch (Throwable e) {
        // Ignore.
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate Source File: " + e.getMessage());
      }
    }

    public void generate() {
      if (processed) {
        return;
      }

      if (root) {
        if (children.isEmpty()) {
          return;
        }

        path = children.values().iterator().next().path;
      }

      generateSource();
      processed = true;

      // Generate child packages.
      children.values().forEach(ActionPackage::generate);
    }
  }
}
