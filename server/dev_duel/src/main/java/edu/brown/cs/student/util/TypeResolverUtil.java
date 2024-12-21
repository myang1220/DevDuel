package edu.brown.cs.student.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeResolverUtil {
  // Mapping of simple names to fully qualified class names
  private static final Map<String, String> TYPE_MAPPINGS =
      new HashMap<>() {
        {
          // Common Java Collections
          put("List", "java.util.List");
          put("ArrayList", "java.util.ArrayList");
          put("LinkedList", "java.util.LinkedList");

          put("Map", "java.util.Map");
          put("HashMap", "java.util.HashMap");
          put("TreeMap", "java.util.TreeMap");

          put("Set", "java.util.Set");
          put("HashSet", "java.util.HashSet");
          put("TreeSet", "java.util.TreeSet");

          // Primitive wrapper types
          put("String", "java.lang.String");
          put("Integer", "java.lang.Integer");
          put("Long", "java.lang.Long");
          put("Double", "java.lang.Double");
          put("Boolean", "java.lang.Boolean");
          put("Float", "java.lang.Float");
          put("Short", "java.lang.Short");
          put("Byte", "java.lang.Byte");
          put("Character", "java.lang.Character");
        }
      };

  /**
   * Resolves a type from its string representation using simple or full class names.
   *
   * @param typeString The string representation of the type (e.g., "List<String>")
   * @return The resolved Type object
   * @throws ClassNotFoundException If the base class or generic type cannot be found
   */
  public static Type resolveType(String typeString) throws ClassNotFoundException {
    // Remove all whitespaces
    typeString = typeString.replaceAll("\\s+", "");

    // Base case: simple type without generics
    if (!typeString.contains("<")) {
      return resolveClassName(typeString);
    }

    // Parse the base type and generic types
    return parseParameterizedType(typeString);
  }

  /** Resolve a class name, supporting both simple and fully qualified names */
  private static Class<?> resolveClassName(String className) throws ClassNotFoundException {
    // Check if it's a mapped simple name
    String capClassName =
        className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase();
    if (TYPE_MAPPINGS.containsKey(capClassName)) {
      return Class.forName(TYPE_MAPPINGS.get(capClassName));
    }

    // If not mapped, try to load as-is (could be fully qualified or custom class)
    return Class.forName(capClassName);
  }

  /** Recursively parse complex parameterized types */
  private static Type parseParameterizedType(String typeString) throws ClassNotFoundException {
    // Extract base type and generic part
    int genericStart = typeString.indexOf("<");
    String baseTypeName = typeString.substring(0, genericStart).trim();
    String genericContent = typeString.substring(genericStart + 1, typeString.length() - 1);

    // Get the base class (now using flexible resolution)
    Class<?> baseClass = resolveClassName(baseTypeName);

    // Parse generic type arguments
    final List<Type> typeArguments = parseGenericTypeArguments(genericContent);

    // Create a ParameterizedType
    return new ParameterizedType() {
      @Override
      public Type[] getActualTypeArguments() {
        return typeArguments.toArray(new Type[0]);
      }

      @Override
      public Type getRawType() {
        return baseClass;
      }

      @Override
      public Type getOwnerType() {
        return null;
      }

      @Override
      public String toString() {
        StringBuilder sb = new StringBuilder(baseClass.getSimpleName());
        sb.append("<");
        for (int i = 0; i < typeArguments.size(); i++) {
          sb.append(typeArguments.get(i).toString());
          if (i < typeArguments.size() - 1) {
            sb.append(", ");
          }
        }
        sb.append(">");
        return sb.toString();
      }
    };
  }

  /** Recursively parse generic type arguments */
  private static List<Type> parseGenericTypeArguments(String genericContent)
      throws ClassNotFoundException {
    List<Type> typeArguments = new ArrayList<>();
    int depth = 0;
    StringBuilder currentArg = new StringBuilder();

    for (char c : genericContent.toCharArray()) {
      if (c == '<') {
        depth++;
        currentArg.append(c);
      } else if (c == '>') {
        depth--;
        currentArg.append(c);

        if (depth == 0) {
          // Complete generic type argument found
          typeArguments.add(resolveType(currentArg.toString()));
          currentArg = new StringBuilder();
        }
      } else if (c == ',' && depth == 0) {
        // Argument separator at top level
        if (currentArg.length() > 0) {
          typeArguments.add(resolveType(currentArg.toString()));
          currentArg = new StringBuilder();
        }
      } else {
        currentArg.append(c);
      }
    }

    // Add the last argument if exists
    if (currentArg.length() > 0) {
      typeArguments.add(resolveType(currentArg.toString()));
    }

    return typeArguments;
  }

  /** Creates an instance of the type with the specified generic type. */
  @SuppressWarnings("unchecked")
  public static Object createInstance(String typeString) throws Exception {
    Type type = resolveType(typeString);

    if (type instanceof ParameterizedType parameterizedType) {
      Class<?> rawType = (Class<?>) parameterizedType.getRawType();
      return rawType.getDeclaredConstructor().newInstance();
    }

    return ((Class<?>) type).getDeclaredConstructor().newInstance();
  }

  /**
   * Check whether a resolved type implements the List Interface
   *
   * @param resolvedType the resolved type
   * @return true if resolvedType implements List interface else false
   */
  public static boolean isListType(Type resolvedType) {
    if (resolvedType instanceof ParameterizedType paramType) {
      Type rawType = paramType.getRawType();

      return rawType instanceof Class && List.class.isAssignableFrom((Class<?>) rawType);
    } else
      return resolvedType instanceof Class && List.class.isAssignableFrom((Class<?>) resolvedType);
  }

  /**
   * Check whether a resolved type implements the List Interface
   *
   * @param resolvedType the resolved type
   * @return true if resolvedType implements List interface else false
   */
  public static boolean isSetType(Type resolvedType) {
    if (resolvedType instanceof ParameterizedType paramType) {
      Type rawType = paramType.getRawType();

      return rawType instanceof Class && Set.class.isAssignableFrom((Class<?>) rawType);
    } else
      return resolvedType instanceof Class && Set.class.isAssignableFrom((Class<?>) resolvedType);
  }
}
